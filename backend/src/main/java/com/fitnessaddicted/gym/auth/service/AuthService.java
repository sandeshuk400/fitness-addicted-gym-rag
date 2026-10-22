package com.fitnessaddicted.gym.auth.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fitnessaddicted.gym.auth.api.ChangePasswordRequest;
import com.fitnessaddicted.gym.auth.api.LoginRequest;
import com.fitnessaddicted.gym.auth.api.RegisterRequest;
import com.fitnessaddicted.gym.auth.api.TokenResponse;
import com.fitnessaddicted.gym.auth.api.UserResponse;
import com.fitnessaddicted.gym.auth.domain.AppUser;
import com.fitnessaddicted.gym.auth.domain.Role;
import com.fitnessaddicted.gym.auth.domain.UserStatus;
import com.fitnessaddicted.gym.auth.repository.AppUserRepository;
import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AuditService;
import com.fitnessaddicted.gym.member.domain.ConsentPurpose;
import com.fitnessaddicted.gym.member.service.ConsentService;
import com.fitnessaddicted.gym.member.service.MemberProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final MemberProfileService members;
    private final ConsentService consents;
    private final AuditService audit;

    /** Compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenService refreshTokens, MemberProfileService members,
                       ConsentService consents, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.members = members;
        this.consents = consents;
        this.audit = audit;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** Self-registration: always creates a MEMBER, with their profile and consent choices. */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Email is already registered");
        }
        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), Role.MEMBER));
        members.createProfile(user.getId(), request.fullName().trim(), blankToNull(request.phone()));
        consents.record(user.getId(), ConsentPurpose.TERMS, true);
        consents.record(user.getId(), ConsentPurpose.MARKETING, request.marketingConsent());
        consents.record(user.getId(), ConsentPurpose.AI_PROCESSING, request.aiProcessingConsent());
        audit.record(user.getId(), "USER_REGISTERED", "AppUser", String.valueOf(user.getId()), Map.of());
        return issueTokens(user);
    }

    /** Not transactional on purpose: failed-login audit rows must persist even though we throw. */
    public TokenResponse login(LoginRequest request) {
        String email = normalize(request.email());
        Optional<AppUser> found = users.findByEmail(email);
        String hashToCheck = found.map(AppUser::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (found.isEmpty() || !passwordMatches || found.get().getStatus() != UserStatus.ACTIVE) {
            audit.record(found.map(AppUser::getId).orElse(null), "LOGIN_FAILED", "AppUser", null,
                    Map.of("email", email));
            throw ApiException.unauthorized("Invalid email or password");
        }
        AppUser user = found.get();
        audit.record(user.getId(), "LOGIN_SUCCESS", "AppUser", String.valueOf(user.getId()), Map.of());
        return issueTokens(user);
    }

    public TokenResponse refresh(String rawRefreshToken) {
        Long userId = refreshTokens.consume(rawRefreshToken);
        AppUser user = users.findById(userId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        return issueTokens(user);
    }

    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> ApiException.unauthorized("Unknown user"));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Unknown user"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(userId);
        audit.record(userId, "PASSWORD_CHANGED", "AppUser", String.valueOf(userId), Map.of());
    }

    private TokenResponse issueTokens(AppUser user) {
        JwtService.AccessToken access = jwtService.create(user);
        String refresh = refreshTokens.create(user.getId());
        return TokenResponse.bearer(access.value(), refresh, access.expiresInSeconds());
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
