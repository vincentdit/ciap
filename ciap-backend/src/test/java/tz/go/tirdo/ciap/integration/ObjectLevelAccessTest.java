package tz.go.tirdo.ciap.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.go.tirdo.ciap.domain.billing.QuotationService;
import tz.go.tirdo.ciap.domain.billing.dto.CreateQuotationRequest;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationItemDto;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientService;
import tz.go.tirdo.ciap.domain.client.ClientType;
import tz.go.tirdo.ciap.domain.client.IndustryCategory;
import tz.go.tirdo.ciap.domain.client.dto.ClientRegistrationRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestService;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;
import tz.go.tirdo.ciap.domain.knowledge.KnowledgeService;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.domain.knowledge.dto.CreateArticleRequest;
import tz.go.tirdo.ciap.domain.training.TrainingMode;
import tz.go.tirdo.ciap.domain.training.TrainingService;
import tz.go.tirdo.ciap.domain.training.dto.CreateEventRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirms a client cannot read or act on another organisation's records,
 * and that refresh tokens and logged-out tokens are not accepted as API credentials.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObjectLevelAccessTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClientService clientService;
    @Autowired ConsultancyRequestService requestService;
    @Autowired QuotationService quotationService;
    @Autowired KnowledgeService knowledgeService;
    @Autowired TrainingService trainingService;
    @Autowired UserRepository userRepository;

    private Client owner;
    private Client other;
    private String ownerToken;
    private String ownerRefresh;
    private String otherToken;
    private RequestResponse ownedRequest;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = register("owner-" + suffix + "@example.test", "111" + suffix);
        other = register("other-" + suffix + "@example.test", "222" + suffix);
        ownerToken = tokenFor(owner.getEmail(), "Client@12345", "accessToken");
        ownerRefresh = tokenFor(owner.getEmail(), "Client@12345", "refreshToken");
        otherToken = tokenFor(other.getEmail(), "Client@12345", "accessToken");
        ownedRequest = requestService.submit(new CreateRequestDto(
                owner.getId(), ConsultancyType.INDUSTRIAL, "Owner only",
                "Confidential scope", LocalDate.now().plusDays(7)));
    }

    @Test
    @DisplayName("another client cannot read, list, cancel, quote, or download a foreign engagement")
    void clientCannotCrossOrganisations() throws Exception {
        mockMvc.perform(get("/consultancy-requests/" + ownedRequest.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        MvcResult foreignList = mockMvc.perform(get("/consultancy-requests")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> visibleIds = com.jayway.jsonpath.JsonPath.read(
                foreignList.getResponse().getContentAsString(), "$.data.content[*].id");
        assertThat(visibleIds.stream().map(Number::longValue).toList()).doesNotContain(ownedRequest.id());

        mockMvc.perform(post("/consultancy-requests/" + ownedRequest.id() + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/consultancy-requests")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":%d,"consultancyType":"INDUSTRIAL","title":"Spoofed"}
                                """.formatted(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(other.getId()));

        requestService.startReview(ownedRequest.id());
        requestService.review(ownedRequest.id(), new ReviewDto(true, "ok"));
        var quote = quotationService.create(new CreateQuotationRequest(
                ownedRequest.id(), new BigDecimal("0.18"), LocalDate.now().plusDays(30), null,
                List.of(new QuotationItemDto("Audit", BigDecimal.ONE, new BigDecimal("1000")))));
        assertThat(quote.versionNo()).isEqualTo(1);
        quotationService.send(quote.id());

        mockMvc.perform(get("/quotations/" + quote.id())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/quotations/" + quote.id() + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/quotations/" + quote.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        MockMultipartFile good = new MockMultipartFile(
                "file", "scope.pdf", "application/pdf", "%PDF-1.4".getBytes());
        mockMvc.perform(multipart("/documents")
                        .file(good)
                        .param("requestId", ownedRequest.id().toString())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        MvcResult listed = mockMvc.perform(get("/documents/request/" + ownedRequest.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String body = listed.getResponse().getContentAsString();
        int idStart = body.indexOf("\"id\":");
        String documentId = body.substring(idStart + 5).split("[,}]")[0];

        mockMvc.perform(get("/documents/" + documentId + "/download")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        MockMultipartFile fakePdf = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "this is not a pdf".getBytes());
        mockMvc.perform(multipart("/documents")
                        .file(fakePdf)
                        .param("requestId", ownedRequest.id().toString())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableEntity());

        MockMultipartFile bad = new MockMultipartFile(
                "file", "payload.exe", "application/octet-stream", "MZ".getBytes());
        mockMvc.perform(multipart("/documents")
                        .file(bad)
                        .param("requestId", ownedRequest.id().toString())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("refresh tokens and revoked access tokens are rejected")
    void tokensAreBoundToTheirPurpose() throws Exception {
        mockMvc.perform(get("/clients/me")
                        .header("Authorization", "Bearer " + ownerRefresh))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/clients/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/clients/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + ownerRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("draft knowledge and training are hidden from clients; staff provisioning requires an admin")
    void draftsAndStaffProvisioningStayClosed() throws Exception {
        var draft = knowledgeService.create(new CreateArticleRequest(
                "Unpublished note", "ops", "summary", "internal only", false));
        mockMvc.perform(get("/knowledge/" + draft.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        String admin = tokenFor("admin@test.local", "Admin@Test123", "accessToken");
        mockMvc.perform(get("/knowledge/" + draft.getId())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        var event = trainingService.create(new CreateEventRequest(
                "Closed workshop", "draft", TrainingMode.ONLINE, null,
                Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS),
                10, false));
        mockMvc.perform(post("/training/" + event.getId() + "/register")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/auth/change-password")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Client@12345\",\"newPassword\":\"Replacement@123\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/clients/me")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + other.getEmail() + "\",\"password\":\"Replacement@123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.test","password":"Staff@12345","fullName":"Nobody","roles":["MANAGER"]}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("a disabled account cannot keep using its access token")
    void disabledAccountCannotUseItsToken() throws Exception {
        var user = userRepository.findByEmail(owner.getEmail()).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(get("/clients/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    private Client register(String email, String tin) {
        return clientService.register(new ClientRegistrationRequest(
                "Org " + tin, tin, ClientType.INDUSTRY, IndustryCategory.MANUFACTURING,
                "Contact", "+255700000000", email, "Client@12345", "Dar es Salaam", "Plot 1", null));
    }

    private String tokenFor(String email, String password, String field) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Token field missing from login response");
        }
        int from = start + marker.length();
        return json.substring(from, json.indexOf('"', from));
    }
}
