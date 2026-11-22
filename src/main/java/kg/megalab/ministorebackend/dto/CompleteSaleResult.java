package kg.megalab.ministorebackend.dto;

public record CompleteSaleResult(Long saleId, Long totalAmount, Long earnedBonuses, Long debtId) {
    public CompleteSaleResult(Long saleId, Long totalAmount, Long earnedBonuses) {
        this(saleId, totalAmount, earnedBonuses, null);
    }
}