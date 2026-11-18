package kg.megalab.ministorebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CashSummaryControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void withoutToken_401() throws Exception {
        mvc.perform(get("/api/v1/summary/cash")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void dayWithoutData_returnsZeroTotals() throws Exception {
        mvc.perform(get("/api/v1/summary/cash").param("date", "2000-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashTotal").value(0))
                .andExpect(jsonPath("$.cardTotal").value(0))
                .andExpect(jsonPath("$.debtTotal").value(0))
                .andExpect(jsonPath("$.bonusSpentTotal").value(0))
                .andExpect(jsonPath("$.grandTotal").value(0))
                .andExpect(jsonPath("$.expectedCashInDrawer").value(0));
    }
}
