package kg.megalab.ministorebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.megalab.ministorebackend.auth.AuthService;
import kg.megalab.ministorebackend.dto.DeliveryRequest;
import kg.megalab.ministorebackend.dto.DeliveryResponse;
import kg.megalab.ministorebackend.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "Приёмка поставки")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @Operation(summary = "Сохранить поставку и увеличить остатки товаров")
    public ResponseEntity<DeliveryResponse> create(
            @Valid @RequestBody DeliveryRequest request,
            Authentication authentication
    ) {
        AuthService.User user = (AuthService.User) authentication.getPrincipal();
        DeliveryResponse response = deliveryService.create(request, (long) user.id());
        return ResponseEntity.created(URI.create("/api/deliveries/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить сохранённую поставку")
    public DeliveryResponse get(@PathVariable Long id) {
        return deliveryService.get(id);
    }
}
