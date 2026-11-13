package kg.megalab.ministorebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.megalab.ministorebackend.dto.ClientResponse;
import kg.megalab.ministorebackend.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Клиенты", description = "Поиск и идентификация клиентов по QR-коду")
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/by-code/{code}")
    @Operation(
            summary = "Найти клиента по QR-коду",
            description = "Находит активного клиента по коду из QR и возвращает его профиль, бонусный баланс и уровень лояльности"
    )
    public ResponseEntity<ClientResponse> getActiveClientByCode(@PathVariable String code) {
        return ResponseEntity.ok(clientService.getClientByCode(code));
    }
}
