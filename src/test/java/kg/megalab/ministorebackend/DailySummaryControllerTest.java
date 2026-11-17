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
class DailySummaryControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void withoutToken_401() throws Exception {
        mvc.perform(get("/api/v1/summary/daily")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void dayWithoutOperations_returnsZerosAndEmptyList() throws Exception {
        mvc.perform(get("/api/v1/summary/daily").param("date", "2000-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesTotal").value(0))
                .andExpect(jsonPath("$.salesCount").value(0))
                .andExpect(jsonPath("$.purchasesTotal").value(0))
                .andExpect(jsonPath("$.deliveriesCount").value(0))
                .andExpect(jsonPath("$.products.length()").value(0));
    }

    @Test
    @WithMockUser
    void unknownProduct_404() throws Exception {
        mvc.perform(get("/api/v1/summary/daily/products/999999999").param("date", "2000-01-01"))
                .andExpect(status().isNotFound());
    }
}