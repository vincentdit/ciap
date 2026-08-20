package tz.go.tirdo.ciap.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the security wiring: public endpoints open, protected endpoints require auth,
 * and method-level role rules are enforced.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security access rules")
class SecurityAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("login endpoint is public (bad creds -> 401/403, not redirected)")
    void loginIsPublic() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@x.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("protected endpoint without a token -> 401/403")
    void protectedRequiresAuth() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("CLIENT role cannot list all clients (manager/admin only) -> 403")
    void clientRoleForbiddenFromClientList() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can list clients -> 200")
    void adminCanListClients() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EXPERT")
    @DisplayName("EXPERT cannot access reports (admin/manager/finance only) -> 403")
    void expertForbiddenFromReports() throws Exception {
        mockMvc.perform(get("/reports/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    @DisplayName("FINANCE can access reports -> 200")
    void financeCanAccessReports() throws Exception {
        mockMvc.perform(get("/reports/summary"))
                .andExpect(status().isOk());
    }
}
