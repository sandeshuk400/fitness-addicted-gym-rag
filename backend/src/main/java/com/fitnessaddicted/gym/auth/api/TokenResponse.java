package com.fitnessaddicted.gym.auth.api;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
