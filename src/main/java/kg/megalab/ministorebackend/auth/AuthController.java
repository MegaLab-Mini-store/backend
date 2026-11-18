package kg.megalab.ministorebackend.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public AuthService.Session login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.username().trim(), request.password())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        AuthService.User user = (AuthService.User) authentication.getPrincipal();
        return new UserResponse(user.username(), user.role());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        auth.logout(authorization.substring(7));
        return ResponseEntity.noContent().build();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record UserResponse(String username, String role) {}
}
