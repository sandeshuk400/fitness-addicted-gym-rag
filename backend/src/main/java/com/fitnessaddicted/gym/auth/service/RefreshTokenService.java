package com.fitnessaddicted.gym.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import com.fitnessaddicted.gym.auth.domain.RefreshToken;
import com.fitnessaddicted.gym.auth.repository.RefreshTokenRepository;
import com.fitnessaddicted.gym.common.ApiException;
import com.fitnessaddicted.gym.common.SecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opaque refresh tokens with rotation: every refresh consumes the old token and issues a new one.
 * Presenting an already-used token is treated as theft and revokes all of that user's tokens.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final SecurityProperties props;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, SecurityProperties props) {
        this.repository = repository;
        this.props = props;
    }

    /** Creates a new refresh token and returns the raw value (shown to the client exactly once). */
    @Transactional
    public String create(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(props.jwtRefreshTtlDays(), ChronoUnit.DAYS);
        repository.save(new RefreshToken(userId, hash(raw), expiresAt));
        return raw;
    }

    /**
     * Validates and consumes a refresh token, returning the owning user id.
     * noRollbackFor: the "revoke everything" side effect on reuse must survive the 401.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Long consume(String raw) {
        RefreshToken token = repository.findByTokenHash(hash(raw))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        Instant now = Instant.now();
        if (token.getRevokedAt() != null) {
            repository.revokeAllForUser(token.getUserId(), now);
            throw ApiException.unauthorized("Invalid refresh token");
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw ApiException.unauthorized("Refresh token expired");
        }
        token.revoke(now);
        return token.getUserId();
    }

    @Transactional
    public void revoke(String raw) {
        repository.findByTokenHash(hash(raw)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.revoke(Instant.now());
            }
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
