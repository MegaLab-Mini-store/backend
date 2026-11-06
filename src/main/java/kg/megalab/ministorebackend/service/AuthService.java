package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.config.AppProperties;
import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
public class AuthService {
    private final AppProperties props;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public AuthService(AppProperties props, PasswordEncoder encoder, JwtEncoder jwtEncoder, Clock clock) {
        this.props = props; this.encoder = encoder; this.jwtEncoder = jwtEncoder; this.clock = clock;
    }

    public LoginResponse login(LoginRequest req) {
        var c = props.cashier();
        boolean ok = c.login().equals(req.login()) && encoder.matches(req.password(), c.passwordHash());
        if (!ok) throw new InvalidCredentialsException();

        Duration ttl = Duration.ofHours(props.jwt().ttlHours());
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(c.login()).issuedAt(now).expiresAt(now.plus(ttl)).build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, ttl.toSeconds());
    }
}