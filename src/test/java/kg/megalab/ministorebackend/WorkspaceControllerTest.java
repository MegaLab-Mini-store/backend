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
class WorkspaceControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void withoutToken_401() throws Exception {
        mvc.perform(get("/api/v1/workspace"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "cashier")
    void withUser_returnsFiveSections() throws Exception {
        mvc.perform(get("/api/v1/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(5))
                .andExpect(jsonPath("$.sections[0].enabled").value(true));
    }
}