package kg.megalab.ministorebackend.dto;

public record LoginResponse(
        String accessToken,
        long expiresInSeconds) {
}
