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

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("only an administrator can list users")
    void managerCannotListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an administrator can list users and cannot create a client login here")
    void adminManagesUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/staff")
                        .contentType("application/json")
                        .content("""
                                {"email":"client-via-staff@example.test","password":"Staff@12345","fullName":"Not A Client","roles":["CLIENT"]}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("finance queue requires a signed-in user")
    void financeQueueRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/payments/summary"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("a client cannot record or list finance payments")
    void clientCannotUseFinance() throws Exception {
        mockMvc.perform(get("/payments")).andExpect(status().isForbidden());
        mockMvc.perform(post("/payments/gepg-callback")
                        .contentType("application/json")
                        .content("""
                                {"controlNumber":"123456789012","gepgReference":"X","confirmationNote":"slip"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXPERT")
    @DisplayName("an expert cannot record or list finance payments")
    void expertCannotUseFinance() throws Exception {
        mockMvc.perform(get("/payments/summary")).andExpect(status().isForbidden());
        mockMvc.perform(post("/payments/gepg-callback")
                        .contentType("application/json")
                        .content("""
                                {"controlNumber":"123456789012","gepgReference":"X","confirmationNote":"slip"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("a manager cannot confirm a payment or open the finance queue")
    void managerCannotConfirmPayments() throws Exception {
        mockMvc.perform(get("/payments")).andExpect(status().isForbidden());
        mockMvc.perform(post("/payments/gepg-callback")
                        .contentType("application/json")
                        .content("""
                                {"controlNumber":"123456789012","gepgReference":"X","confirmationNote":"slip"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    @DisplayName("finance can open the queue and must give a reason to record a payment")
    void financeQueueRequiresAReason() throws Exception {
        mockMvc.perform(get("/payments/summary")).andExpect(status().isOk());
        mockMvc.perform(post("/payments/gepg-callback")
                        .contentType("application/json")
                        .content("""
                                {"controlNumber":"123456789012","gepgReference":"MISSING"}
                                """))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/payments/gepg-callback")
                        .contentType("application/json")
                        .content("""
                                {"controlNumber":"123456789012","gepgReference":"MISSING","confirmationNote":"slip checked"}
                                """))
                .andExpect(status().isNotFound());
    }
}
