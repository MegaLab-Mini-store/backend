package kg.megalab.ministorebackend.auth;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class OwnerBootstrap implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final String username;
    private final String password;

    public OwnerBootstrap(JdbcTemplate jdbc, PasswordEncoder passwords,
            @Value("${MINISTORE_OWNER_USERNAME:}") String username,
            @Value("${MINISTORE_OWNER_PASSWORD:}") String password) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() || password.isBlank()) return;
        if (password.length() < 12) throw new IllegalStateException("MINISTORE_OWNER_PASSWORD must have at least 12 characters");
        jdbc.update("""
                INSERT INTO users (username, password_hash, role, is_active, created_at)
                VALUES (?, ?, 'OWNER', true, ?)
                ON CONFLICT (username) DO NOTHING
                """, username.trim(), passwords.encode(password), Timestamp.from(Instant.now()));
    }
}
