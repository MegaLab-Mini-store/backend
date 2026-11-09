package kg.megalab.ministorebackend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Duration SESSION_LIFETIME = Duration.ofDays(30);
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
    }

    public Optional<Session> login(String username, String password) {
        var users = jdbc.query(
                "SELECT id, username, password_hash, role FROM users WHERE username = ? AND is_active = true",
                (rs, row) -> new User(rs.getInt("id"), rs.getString("username"),
                        rs.getString("password_hash"), rs.getString("role")), username);
        if (users.isEmpty() || !passwords.matches(password, users.getFirst().passwordHash())) {
            return Optional.empty();
        }
        User user = users.getFirst();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(SESSION_LIFETIME);
        jdbc.update("INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?)",
                user.id(), hash(token), Timestamp.from(expiresAt), Timestamp.from(Instant.now()));
        return Optional.of(new Session(token, expiresAt, user.username(), user.role()));
    }

    public Optional<User> userForToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        var users = jdbc.query("""
                SELECT u.id, u.username, u.password_hash, u.role
                FROM refresh_tokens t JOIN users u ON u.id = t.user_id
                WHERE t.token_hash = ? AND t.revoked_at IS NULL AND t.expires_at > ? AND u.is_active = true
                """, (rs, row) -> new User(rs.getInt("id"), rs.getString("username"),
                rs.getString("password_hash"), rs.getString("role")), hash(token), Timestamp.from(Instant.now()));
        return users.stream().findFirst();
    }

    public void logout(String token) {
        jdbc.update("UPDATE refresh_tokens SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL",
                Timestamp.from(Instant.now()), hash(token));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record User(int id, String username, String passwordHash, String role) {}
    public record Session(String token, Instant expiresAt, String username, String role) {}
}
