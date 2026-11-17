package kg.megalab.ministorebackend.dto;

public record ProductDayMovement(
        Long productId,
        String productName,
        int incoming,
        int outgoing,
        int net) {}