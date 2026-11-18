package kg.megalab.ministorebackend.controller;

import kg.megalab.ministorebackend.auth.AuthService;
import kg.megalab.ministorebackend.dto.SyncPushRequest;
import kg.megalab.ministorebackend.dto.SyncPushResponse;
import kg.megalab.ministorebackend.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/push")
    public SyncPushResponse push(Authentication authentication, @RequestBody SyncPushRequest request) {
        AuthService.User user = (AuthService.User) authentication.getPrincipal();
        return syncService.push((long) user.id(), request);
    }
}
