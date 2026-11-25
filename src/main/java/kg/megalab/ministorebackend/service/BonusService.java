package kg.megalab.ministorebackend.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import kg.megalab.ministorebackend.entity.BonusTransaction;
import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.entity.Sale;
import kg.megalab.ministorebackend.repository.BonusTransactionRepository;
import kg.megalab.ministorebackend.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BonusService {

    private static final long PERCENT_SCALE = 10_000L; // 100% = 10000, 1% = 100

    private final BonusTransactionRepository bonusTransactionRepository;
    private final ClientRepository clientRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public long accrueForSale(Sale sale, Client client) {
        int percent = client.getLoyaltyLevel().getCashbackPercentage();
        long base = sale.getPaidAmount();
        long bonus = base * percent / PERCENT_SCALE;

        if (bonus == 0) {
            return 0;
        }

        bonusTransactionRepository.save(
                BonusTransaction.accrual(client.getId(), sale.getId(), bonus, base, percent));
        clientRepository.addBonuses(client.getId(), bonus, OffsetDateTime.now(clock));
        return bonus;
    }
}
