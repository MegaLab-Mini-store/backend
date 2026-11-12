package kg.megalab.ministorebackend.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private JdbcTemplate jdbc;
    private PasswordEncoder passwords;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        passwords = mock(PasswordEncoder.class);
        auth = new AuthService(jdbc, passwords);
    }

    @Test
    void unknownUserDoesNotCreateSession() {
        doReturn(List.of()).when(jdbc).query(anyString(), any(RowMapper.class), any(Object.class));
        assertTrue(auth.login("missing", "password").isEmpty());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void wrongPasswordDoesNotCreateSession() {
        doReturn(List.of(new AuthService.User(1, "owner", "hash", "OWNER")))
                .when(jdbc).query(anyString(), any(RowMapper.class), any(Object.class));
        when(passwords.matches("wrong", "hash")).thenReturn(false);
        assertTrue(auth.login("owner", "wrong").isEmpty());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void unrecognizedTokenCannotRestoreSession() {
        doReturn(List.of()).when(jdbc).query(anyString(), any(RowMapper.class), any(Object.class), any(Object.class));
        assertTrue(auth.userForToken("not-a-session").isEmpty());
    }

    @Test
    void successfulLoginStoresOnlyHashAndLogoutRevokesIt() {
        doReturn(List.of(new AuthService.User(1, "owner", "hash", "OWNER")))
                .when(jdbc).query(anyString(), any(RowMapper.class), any(Object.class));
        when(passwords.matches("correct", "hash")).thenReturn(true);

        AuthService.Session session = auth.login("owner", "correct").orElseThrow();
        assertEquals("owner", session.username());
        assertEquals("OWNER", session.role());
        assertFalse(session.token().isBlank());
        ArgumentCaptor<Object[]> insertArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(startsWith("INSERT INTO refresh_tokens"), insertArgs.capture());
        assertEquals(1, insertArgs.getValue()[0]);
        assertNotEquals(session.token(), insertArgs.getValue()[1]);

        auth.logout(session.token());
        ArgumentCaptor<Object[]> logoutArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(startsWith("UPDATE refresh_tokens"), logoutArgs.capture());
        assertEquals(insertArgs.getValue()[1], logoutArgs.getValue()[1]);
    }
}
