package com.fitnessaddicted.gym.auth.service;

import java.util.Map;

import com.fitnessaddicted.gym.auth.api.CreateUserRequest;
import com.fitnessaddicted.gym.auth.api.UserResponse;
import com.fitnessaddicted.gym.auth.domain.AppUser;
import com.fitnessaddicted.gym.auth.domain.Role;
import com.fitnessaddicted.gym.auth.domain.UserStatus;
import com.fitnessaddicted.gym.auth.repository.AppUserRepository;
import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.AuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account management by admins: create staff/admin accounts, suspend and reactivate users. */
@Service
public class UserAdminService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final AuditService audit;

    public UserAdminService(AppUserRepository users, PasswordEncoder passwordEncoder,
                            RefreshTokenService refreshTokens, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
    }

    @Transactional
    public UserResponse createUser(Long actorId, CreateUserRequest request) {
        if (request.role() != Role.STAFF && request.role() != Role.ADMIN) {
            throw ApiException.badRequest("Only STAFF and ADMIN accounts can be created here");
        }
        String email = AuthService.normalize(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("Email is already registered");
        }
        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), request.role()));
        audit.record(actorId, "USER_CREATED_BY_ADMIN", "AppUser", String.valueOf(user.getId()),
                Map.of("role", request.role().name()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setStatus(Long actorId, Long targetId, UserStatus status) {
        if (actorId.equals(targetId)) {
            throw ApiException.badRequest("You cannot change your own status");
        }
        AppUser user = users.findById(targetId).orElseThrow(() -> ApiException.notFound("User not found"));
        user.setStatus(status);
        if (status == UserStatus.SUSPENDED) {
            refreshTokens.revokeAllForUser(targetId);
        }
        audit.record(actorId, "USER_STATUS_CHANGED", "AppUser", String.valueOf(targetId),
                Map.of("status", status.name()));
        return UserResponse.from(user);
    }
}
