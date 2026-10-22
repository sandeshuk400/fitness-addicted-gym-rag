package com.fitnessaddicted.gym.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fitnessaddicted.gym.auth.domain.AppUser;
import com.fitnessaddicted.gym.common.SecurityProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues short-lived access tokens (HS256 JWT). */
@Service
public class JwtService {

    /** URL-shaped on purpose: Spring Security parses the "iss" claim as a URL. */
    public static final String ISSUER = "https://fitness-addicted-gym.local";

    private final JwtEncoder encoder;
    private final SecurityProperties props;

    public JwtService(JwtEncoder encoder, SecurityProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public AccessToken create(AppUser user) {
        Instant now = Instant.now();
        long ttlSeconds = props.jwtAccessTtlMinutes() * 60;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(props.jwtAccessTtlMinutes(), ChronoUnit.MINUTES))
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, ttlSeconds);
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }
}
