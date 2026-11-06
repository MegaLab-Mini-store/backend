package kg.megalab.ministorebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WorkspaceControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void withoutToken_401() throws Exception {
        mvc.perform(get("/api/v1/workspace")).andExpect(status().isUnauthorized());
    }

    @Test
    void withToken_returnsFiveSections() throws Exception {
        mvc.perform(get("/api/v1/workspace").with(jwt().jwt(j -> j.subject("cashier"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(5))
                .andExpect(jsonPath("$.sections[0].enabled").value(true));
    }

    @Test
    void wrongPassword_401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"cashier\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }
}