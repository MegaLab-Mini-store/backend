package kg.megalab.ministorebackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayloadHasherTest {

    private final PayloadHasher hasher = new PayloadHasher();

    @Test
    void sameDataGivesSameHash() {
        Map<String, Object> payload = Map.of("payment_method", "cash");

        assertThat(hasher.hash("sale", payload))
                .isEqualTo(hasher.hash("sale", payload));
    }

    @Test
    void fieldOrderDoesNotChangeHash() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("payment_method", "cash");
        first.put("occurred_at", "2026-11-04T10:15:30+06:00");

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("occurred_at", "2026-11-04T10:15:30+06:00");
        second.put("payment_method", "cash");

        assertThat(hasher.hash("sale", first))
                .isEqualTo(hasher.hash("sale", second));
    }

    @Test
    void differentDataGivesDifferentHash() {
        Map<String, Object> twoItems = Map.of("items",
                List.of(Map.of("product_id", 1, "quantity", 2)));
        Map<String, Object> threeItems = Map.of("items",
                List.of(Map.of("product_id", 1, "quantity", 3)));

        assertThat(hasher.hash("sale", twoItems))
                .isNotEqualTo(hasher.hash("sale", threeItems));
    }

    @Test
    void differentOperationTypeGivesDifferentHash() {
        Map<String, Object> payload = Map.of("payment_method", "cash");

        assertThat(hasher.hash("sale", payload))
                .isNotEqualTo(hasher.hash("delivery", payload));
    }
}
