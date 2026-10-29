package tz.go.tirdo.ciap.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.domain.catalogue.CatalogueService;
import tz.go.tirdo.ciap.domain.catalogue.ConsultancyCategoryRepository;
import tz.go.tirdo.ciap.domain.catalogue.PricingMethod;
import tz.go.tirdo.ciap.domain.catalogue.dto.SaveServiceRequest;
import tz.go.tirdo.ciap.domain.catalogue.dto.ServiceOfferResponse;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientService;
import tz.go.tirdo.ciap.domain.client.ClientType;
import tz.go.tirdo.ciap.domain.client.IndustryCategory;
import tz.go.tirdo.ciap.domain.client.dto.ClientRegistrationRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestService;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.training.FeeKind;
import tz.go.tirdo.ciap.domain.training.RegistrationStatus;
import tz.go.tirdo.ciap.domain.training.TrainingMode;
import tz.go.tirdo.ciap.domain.training.TrainingPaymentRepository;
import tz.go.tirdo.ciap.domain.training.TrainingRegistrationRepository;
import tz.go.tirdo.ciap.domain.training.TrainingService;
import tz.go.tirdo.ciap.domain.training.dto.CreateEventRequest;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.UserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Consultancy prices and training fees")
class CataloguePricingTest {

    @Autowired MockMvc mockMvc;
    @Autowired CatalogueService catalogueService;
    @Autowired ConsultancyCategoryRepository categoryRepository;
    @Autowired ConsultancyRequestService requestService;
    @Autowired ClientService clientService;
    @Autowired UserService userService;
    @Autowired TrainingService trainingService;
    @Autowired TrainingRegistrationRepository registrationRepository;
    @Autowired TrainingPaymentRepository trainingPaymentRepository;

    @AfterEach
    void clearActor() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    @DisplayName("finance cannot change a consultancy price or a training fee")
    void financeCannotChangePrices() throws Exception {
        mockMvc.perform(post("/consultancy-catalogue/services")
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"name":"Blocked","pricingMethod":"QUOTATION"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/training")
                        .contentType("application/json")
                        .content("""
                                {"title":"Blocked","mode":"ONLINE","startsAt":"2026-12-01T09:00:00Z","feeKind":"PAID","feeAmount":10}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    @DisplayName("a client cannot create a consultancy service")
    void clientCannotCreateService() throws Exception {
        mockMvc.perform(post("/consultancy-catalogue/services")
                        .contentType("application/json")
                        .content("""
                                {"categoryId":1,"name":"Blocked","pricingMethod":"FIXED","amount":1000}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a customer confirms the published price, and a later price must be reviewed")
    void customerConfirmsThePriceTheySaw() {
        String stamp = String.valueOf(System.nanoTime());
        as("admin." + stamp + "@test.local", "ADMIN");
        userService.createUser("admin." + stamp + "@test.local", "Staff@12345", "Admin", null, java.util.Set.of(RoleName.ADMIN));
        var category = categoryRepository.findByCodeIgnoreCase("INDUSTRIAL").orElseThrow();
        ServiceOfferResponse fixed = catalogueService.createService(new SaveServiceRequest(
                category.getId(), "Panel " + stamp, "Laboratory panel", PricingMethod.FIXED,
                new BigDecimal("1000.00"), "TZS", null, null, true, true, "Initial price"));
        ServiceOfferResponse quoted = catalogueService.createService(new SaveServiceRequest(
                category.getId(), "Assessment " + stamp, "Needs a visit", PricingMethod.QUOTATION,
                null, "TZS", null, null, true, true, null));
        assertThat(quoted.amount()).isNull();
        assertThat(quoted.customerLabel()).contains("No final payment amount has been established yet");

        Client client = client(stamp);
        as(client.getEmail(), "CLIENT");
        assertThatThrownBy(() -> requestService.submit(request(client, fixed, fixed.priceVersionId() - 1, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("The price changed");

        var submitted = requestService.submit(request(client, fixed, fixed.priceVersionId(), null));
        assertThat(submitted.status()).isEqualTo(RequestStatus.SUBMITTED);
        assertThat(submitted.snapshotAmount()).isEqualByComparingTo("1000.00");
        assertThat(submitted.priceLabel()).contains("Fixed fee");
        assertThat(submitted.consultancyType()).isEqualTo(ConsultancyType.INDUSTRIAL);

        var quotedRequest = requestService.submit(request(client, quoted, quoted.priceVersionId(), null));
        assertThat(quotedRequest.snapshotAmount()).isNull();
        assertThat(quotedRequest.priceLabel()).contains("No final payment amount");

        as("admin." + stamp + "@test.local", "ADMIN");
        ServiceOfferResponse updated = catalogueService.updateService(fixed.id(), new SaveServiceRequest(
                category.getId(), fixed.name(), fixed.description(), PricingMethod.FIXED,
                new BigDecimal("1500.00"), "TZS", null, null, true, true, "Approved revision"));
        assertThat(catalogueService.priceHistory(fixed.id())).hasSize(2);

        as(client.getEmail(), "CLIENT");
        assertThatThrownBy(() -> requestService.submit(request(client, fixed, fixed.priceVersionId(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("The price changed");
        var confirmed = requestService.submit(request(client, updated, updated.priceVersionId(), null));
        assertThat(confirmed.snapshotAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("free registration has no payment, and a paid registration keeps its original fee")
    void freeAndPaidTraining() {
        String stamp = String.valueOf(System.nanoTime());
        as("admin." + stamp + "@test.local", "ADMIN");
        userService.createUser("admin." + stamp + "@test.local", "Staff@12345", "Admin", null, java.util.Set.of(RoleName.ADMIN));
        as("manager." + stamp + "@test.local", "MANAGER");
        assertThatThrownBy(() -> trainingService.create(paid("Manager fee " + stamp, new BigDecimal("10"), 10)))
                .isInstanceOf(BusinessException.class);

        as("admin." + stamp + "@test.local", "ADMIN");
        assertThatThrownBy(() -> trainingService.create(paid("Missing fee " + stamp, null, 10)))
                .isInstanceOf(BusinessException.class);

        var free = trainingService.create(new CreateEventRequest(
                "Open day " + stamp, "No fee", TrainingMode.ONLINE, "https://example.test",
                Instant.now().plus(2, ChronoUnit.DAYS), Instant.now().plus(3, ChronoUnit.DAYS), 10, true));
        var paid = trainingService.create(paid("Workshop " + stamp, new BigDecimal("25000"), 1));

        Client first = client(stamp + "a");
        Client second = client(stamp + "b");
        as(first.getEmail(), "CLIENT");
        var freeReg = trainingService.register(free.getId(), first.getEmail());
        assertThat(freeReg.registrationStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(freeReg.feeKind()).isEqualTo(FeeKind.FREE);
        assertThat(trainingPaymentRepository.findByRegistrationId(
                registrationRepository.findByEventIdAndRegistrantEmail(free.getId(), first.getEmail()).orElseThrow().getId()))
                .isEmpty();

        var waiting = trainingService.register(paid.getId(), first.getEmail());
        assertThat(waiting.registrationStatus()).isEqualTo(RegistrationStatus.AWAITING_PAYMENT);
        assertThat(waiting.feeAmount()).isEqualByComparingTo("25000.00");
        assertThat(waiting.controlNumber()).isNotBlank();
        assertThatThrownBy(() -> trainingService.register(paid.getId(), first.getEmail()))
                .isInstanceOf(DuplicateResourceException.class);

        as(second.getEmail(), "CLIENT");
        assertThatThrownBy(() -> trainingService.register(paid.getId(), second.getEmail()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("full");

        as("finance." + stamp + "@test.local", "FINANCE");
        var recorded = trainingService.confirmPayment(waiting.controlNumber(), "SLIP-" + stamp, "Bank slip checked");
        assertThat(recorded.status().name()).isEqualTo("PAID");
        var again = trainingService.confirmPayment(waiting.controlNumber(), "SLIP-" + stamp, "Bank slip checked");
        assertThat(again.id()).isEqualTo(recorded.id());

        var registration = registrationRepository.findByEventIdAndRegistrantEmail(paid.getId(), first.getEmail()).orElseThrow();
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(registration.getFeeAmount()).isEqualByComparingTo("25000.00");

        as("admin." + stamp + "@test.local", "ADMIN");
        trainingService.update(paid.getId(), paid("Workshop " + stamp, new BigDecimal("40000"), 1));
        assertThat(registrationRepository.findById(registration.getId()).orElseThrow().getFeeAmount())
                .isEqualByComparingTo("25000.00");
    }

    private CreateRequestDto request(Client client, ServiceOfferResponse service, Long version, BigDecimal quantity) {
        return new CreateRequestDto(client.getId(), service.consultancyType(), service.name(),
                "Customer request", null, service.id(), version, quantity);
    }

    private CreateEventRequest paid(String title, BigDecimal amount, int capacity) {
        return new CreateEventRequest(title, "Paid workshop", TrainingMode.IN_PERSON, "TIRDO hall",
                Instant.now().plus(4, ChronoUnit.DAYS), Instant.now().plus(5, ChronoUnit.DAYS),
                capacity, true, FeeKind.PAID, amount, "TZS", null, null, "Practice", "Engineers", null, null);
    }

    private Client client(String stamp) {
        return clientService.register(new ClientRegistrationRequest(
                "Client " + stamp, "8" + stamp.substring(Math.max(0, stamp.length() - 8)),
                ClientType.SME, IndustryCategory.MANUFACTURING,
                "Desk", "+255700000001", "client." + stamp + "@test.local",
                "Client@12345", "Dar es Salaam", "Kinondoni", "Tanzania"));
    }

    private void as(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email.toLowerCase(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
