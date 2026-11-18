package kg.megalab.ministorebackend.controller;

import kg.megalab.ministorebackend.dto.WorkspaceResponse;
import kg.megalab.ministorebackend.service.WorkspaceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {
    private final WorkspaceService service;
    public WorkspaceController(WorkspaceService service) { this.service = service; }

    @GetMapping
    public WorkspaceResponse get(Authentication auth) {
        return service.build(auth.getName());
    }
}