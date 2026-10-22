package com.fitnessaddicted.gym.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;

import com.fitnessaddicted.gym.auth.domain.AppUser;
import com.fitnessaddicted.gym.auth.domain.Role;
import com.fitnessaddicted.gym.auth.service.JwtService;
import com.fitnessaddicted.gym.common.SecurityProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long!!";
    private static final String OTHER_SECRET = "a-completely-different-secret-also-32-bytes-long";

    private static SecretKeySpec key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtDecoder decoder(String secret) {
        return NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static AppUser adminUser() {
        AppUser user = new AppUser("owner@example.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    private static JwtService service() {
        SecurityProperties props = new SecurityProperties(SECRET, 15, 7);
        return new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(key(SECRET))), props);
    }

    @Test
    void accessTokenCarriesExpectedClaims() {
        JwtService.AccessToken token = service().create(adminUser());

        Jwt jwt = decoder(SECRET).decode(token.value());

        assertEquals("7", jwt.getSubject());
        assertEquals("owner@example.com", jwt.getClaimAsString("email"));
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList("roles"));
        assertEquals(JwtService.ISSUER, jwt.getIssuer().toString());
        assertEquals(900, token.expiresInSeconds());

        Duration lifetime = Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt());
        assertEquals(Duration.ofMinutes(15), lifetime);
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = service().create(adminUser()).value();

        assertThrows(JwtException.class, () -> decoder(OTHER_SECRET).decode(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = service().create(adminUser()).value();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertThrows(JwtException.class, () -> decoder(SECRET).decode(tampered));
    }
}
