package kg.megalab.ministorebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kg.megalab.ministorebackend.dto.ClientPurchaseResponse;
import kg.megalab.ministorebackend.dto.ClientResponse;
import kg.megalab.ministorebackend.dto.CreateClientRequest;
import kg.megalab.ministorebackend.dto.PageResponse;
import kg.megalab.ministorebackend.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    @Operation(
            summary = "Добавить постоянного клиента",
            description = "Создаёт карточку постоянного клиента, генерирует уникальный Client ID и назначает уровень лояльности"
    )
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(
            summary = "Получить список постоянных клиентов",
            description = "Возвращает активных клиентов с возможностью поиска по имени, телефону или Client ID"
    )
    public ResponseEntity<List<ClientResponse>> getClients(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(clientService.getClients(search));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить карточку клиента по ID",
            description = "Возвращает профиль клиента, бонусный баланс, сумму долга и уровень лояльности"
    )
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @GetMapping("/{id}/purchases")
    @Operation(
            summary = "История покупок клиента",
            description = "Возвращает историю покупок клиента"
    )
    public ResponseEntity<PageResponse<ClientPurchaseResponse>> getClientPurchases(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return ResponseEntity.ok(clientService.getClientPurchases(id, limit, offset));
    }
}
