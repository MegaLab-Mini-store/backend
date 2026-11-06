package kg.megalab.ministorebackend.controller;

import jakarta.validation.Valid;
import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }
}
