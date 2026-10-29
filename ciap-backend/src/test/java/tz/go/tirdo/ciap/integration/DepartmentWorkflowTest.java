package tz.go.tirdo.ciap.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.billing.BillingService;
import tz.go.tirdo.ciap.domain.catalogue.CatalogueService;
import tz.go.tirdo.ciap.domain.catalogue.dto.CategoryResponse;
import tz.go.tirdo.ciap.domain.billing.QuotationService;
import tz.go.tirdo.ciap.domain.billing.dto.CreateQuotationRequest;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationItemDto;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientService;
import tz.go.tirdo.ciap.domain.client.ClientType;
import tz.go.tirdo.ciap.domain.client.IndustryCategory;
import tz.go.tirdo.ciap.domain.client.dto.ClientRegistrationRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestService;
import tz.go.tirdo.ciap.domain.consultancy.RequestAuditService;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.domain.consultancy.dto.AssignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReassignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ConsultantService;
import tz.go.tirdo.ciap.domain.consultant.ExpertCategory;
import tz.go.tirdo.ciap.domain.consultant.dto.CreateConsultantRequest;
import tz.go.tirdo.ciap.domain.department.Department;
import tz.go.tirdo.ciap.domain.department.DepartmentRepository;
import tz.go.tirdo.ciap.domain.department.DepartmentService;
import tz.go.tirdo.ciap.domain.department.RoutingStatus;
import tz.go.tirdo.ciap.domain.department.ServiceRoute;
import tz.go.tirdo.ciap.domain.department.ServiceRouteRepository;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.User;
import tz.go.tirdo.ciap.domain.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Department head assignment")
class DepartmentWorkflowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ClientService clientService;
    @Autowired UserService userService;
    @Autowired ConsultantService consultantService;
    @Autowired ConsultancyRequestService requestService;
    @Autowired RequestAuditService requestAuditService;
    @Autowired QuotationService quotationService;
    @Autowired BillingService billingService;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired DepartmentService departmentService;
    @Autowired CatalogueService catalogueService;
    @Autowired ServiceRouteRepository routeRepository;

    @AfterEach
    void clearActor() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a request is routed to its department and only that head can assign an eligible expert")
    void hodAssignsWithinTheDepartment() {
        String stamp = String.valueOf(System.nanoTime());
        Department industrial = department("Industrial Research " + stamp, "IND-" + stamp);
        Department engineering = department("Engineering " + stamp, "ENG-" + stamp);
        route(ConsultancyType.INDUSTRIAL, industrial);

        User hod = userService.createUser("hod." + stamp + "@test.local", "Staff@12345",
                "Amina Selemani", null, java.util.Set.of(RoleName.HOD));
        User otherHod = userService.createUser("hod2." + stamp + "@test.local", "Staff@12345",
                "Peter Mwakalinga", null, java.util.Set.of(RoleName.HOD));
        User manager = userService.createUser("manager." + stamp + "@test.local", "Staff@12345",
                "Neema Juma", null, java.util.Set.of(RoleName.MANAGER));
        departmentService.appointHod(industrial.getId(), hod.getId());
        departmentService.appointHod(engineering.getId(), otherHod.getId());

        User expertUser = userService.createUser("expert." + stamp + "@test.local", "Staff@12345",
                "Eng. Baraka Mushi", null, java.util.Set.of(RoleName.EXPERT));
        User secondUser = userService.createUser("expert2." + stamp + "@test.local", "Staff@12345",
                "Dr. Rehema Lyimo", null, java.util.Set.of(RoleName.EXPERT));
        User outsiderUser = userService.createUser("outsider." + stamp + "@test.local", "Staff@12345",
                "Daniel Shao", null, java.util.Set.of(RoleName.EXPERT));
        Consultant expert = consultant(expertUser, "Eng. Baraka Mushi");
        Consultant second = consultant(secondUser, "Dr. Rehema Lyimo");
        Consultant outsider = consultant(outsiderUser, "Daniel Shao");
        departmentService.addMember(industrial.getId(), expertUser.getId(), true);
        departmentService.addMember(industrial.getId(), secondUser.getId(), true);

        Client client = clientService.register(new ClientRegistrationRequest(
                "Mwanza Sunflower " + stamp, "9" + stamp.substring(0, 8),
                ClientType.SME, IndustryCategory.AGRO_PROCESSING,
                "Quality desk", "+255700000001", "client." + stamp + "@test.local",
                "Client@12345", "Mwanza", "Nyamagana", "Tanzania"));

        as(client.getEmail(), "CLIENT");
        routeRepository.findByConsultancyType(ConsultancyType.ENVIRONMENTAL).ifPresent(existing -> {
            existing.setActive(false);
            routeRepository.save(existing);
        });
        RequestResponse unmapped = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.ENVIRONMENTAL, "Unrouted effluent check",
                "No environmental route is configured for this test.", null));
        assertThat(unmapped.routingStatus()).isEqualTo(RoutingStatus.UNMAPPED);
        assertThat(unmapped.departmentId()).isNull();

        RequestResponse submitted = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.INDUSTRIAL, "Sunflower oil quality",
                "Chemical and microbiological testing.", LocalDate.now().plusWeeks(2)));
        assertThat(submitted.routingStatus()).isEqualTo(RoutingStatus.ROUTED);
        assertThat(submitted.departmentId()).isEqualTo(industrial.getId());

        as(hod.getEmail(), "HOD");
        RequestResponse openedByHead = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.INDUSTRIAL, "Opened by the department head",
                "The head records the engagement for this department.", null));
        assertThat(openedByHead.departmentId()).isEqualTo(industrial.getId());
        assertThat(openedByHead.status()).isEqualTo(RequestStatus.SUBMITTED);
        assertThatThrownBy(() -> requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.ENGINEERING, "Wrong department",
                "This service line belongs to another department.", null)))
                .isInstanceOf(BusinessException.class);

        as(otherHod.getEmail(), "HOD");
        assertThatThrownBy(() -> requestService.get(submitted.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(requestService.list(null, PageRequest.of(0, 50)).getContent())
                .noneMatch(row -> row.id().equals(submitted.id()));

        as(hod.getEmail(), "HOD");
        assertThat(requestService.list(null, PageRequest.of(0, 50)).getContent())
                .anyMatch(row -> row.id().equals(submitted.id()));
        assertThat(requestService.eligibleAssignees(submitted.id()))
                .extracting(row -> row.consultantId())
                .contains(expert.getId(), second.getId())
                .doesNotContain(outsider.getId());
        RequestResponse namedEarly = requestService.assign(submitted.id(), new AssignDto(expert.getId()));
        assertThat(namedEarly.status()).isEqualTo(RequestStatus.SUBMITTED);
        assertThat(namedEarly.assignedConsultantName()).isEqualTo("Eng. Baraka Mushi");

        requestService.startReview(submitted.id());
        RequestResponse reviewed = requestService.review(submitted.id(),
                new ReviewDto(true, "Internal scope note for the department."));
        assertThat(reviewed.reviewNotes()).contains("Internal scope note");

        as(client.getEmail(), "CLIENT");
        assertThat(requestService.get(submitted.id()).reviewNotes()).isNull();
        assertThatThrownBy(() -> requestService.assignments(submitted.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        pay(submitted.id(), "GEPG-" + stamp);

        as(manager.getEmail(), "MANAGER");
        assertThatThrownBy(() -> requestService.assign(submitted.id(), new AssignDto(expert.getId(), "note", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Head of Department");

        as(hod.getEmail(), "HOD");
        assertThatThrownBy(() -> requestService.assign(submitted.id(), new AssignDto(outsider.getId(), "note", null)))
                .isInstanceOf(BusinessException.class);

        RequestResponse assigned = requestService.assign(submitted.id(),
                new AssignDto(expert.getId(), "Sample the crude oil first", LocalDate.now().plusDays(10)));
        assertThat(assigned.status()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(assigned.assignedConsultantName()).isEqualTo("Eng. Baraka Mushi");
        assertThat(assigned.reviewNotes()).doesNotContain("Sample the crude oil");

        as(expertUser.getEmail(), "EXPERT");
        assertThat(requestService.list(null, PageRequest.of(0, 20)).getContent())
                .anyMatch(row -> row.id().equals(submitted.id()));

        as(outsiderUser.getEmail(), "EXPERT");
        assertThatThrownBy(() -> requestService.get(submitted.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        as(hod.getEmail(), "HOD");
        requestService.reassign(submitted.id(), new ReassignDto(
                second.getId(), "The first expert is on another site visit", "Continue the microbiology panel", null));
        assertThat(requestService.assignments(submitted.id())).hasSize(2);
        assertThat(requestService.assignments(submitted.id()).get(1).previousConsultantName())
                .isEqualTo("Eng. Baraka Mushi");
        assertThat(requestService.get(submitted.id()).assignedConsultantName()).isEqualTo("Dr. Rehema Lyimo");

        as(expertUser.getEmail(), "EXPERT");
        assertThatThrownBy(() -> requestService.get(submitted.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        as(secondUser.getEmail(), "EXPERT");
        assertThat(requestService.get(submitted.id()).referenceNo()).isEqualTo(submitted.referenceNo());
    }

    @Test
    @WithMockUser(roles = "FINANCE")
    @DisplayName("finance cannot assign work or change the departmental decision")
    void financeCannotAssign() throws Exception {
        mockMvc.perform(post("/consultancy-requests/1/assign")
                        .contentType("application/json")
                        .content("{\"consultantId\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/consultancy-requests/1/reassign")
                        .contentType("application/json")
                        .content("{\"consultantId\":1,\"reason\":\"swap\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/payments/summary")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("a manager cannot assign from the API")
    void managerCannotAssignFromTheApi() throws Exception {
        mockMvc.perform(post("/consultancy-requests/1/assign")
                        .contentType("application/json")
                        .content("{\"consultantId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HOD")
    @DisplayName("a head of department cannot open the finance queue")
    void hodCannotUseFinance() throws Exception {
        mockMvc.perform(get("/payments/summary")).andExpect(status().isForbidden());
        mockMvc.perform(post("/payments/gepg-callback")
                        .contentType("application/json")
                        .content("""
                                {"controlNumber":"123456789012","gepgReference":"X","confirmationNote":"slip"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a rejection or cancellation needs a reason and can be undone")
    void rejectAndCancelCanBeUndone() {
        String stamp = String.valueOf(System.nanoTime());
        Department industrial = department("Industrial undo " + stamp, "UND-" + stamp);
        route(ConsultancyType.INDUSTRIAL, industrial);
        User hod = userService.createUser("hodundo." + stamp + "@test.local", "Staff@12345",
                "Amina Selemani", null, java.util.Set.of(RoleName.HOD));
        departmentService.appointHod(industrial.getId(), hod.getId());
        Client client = clientService.register(new ClientRegistrationRequest(
                "Undo Client " + stamp, "8" + stamp.substring(0, 8),
                ClientType.SME, IndustryCategory.AGRO_PROCESSING,
                "Desk", "+255700000002", "undoclient." + stamp + "@test.local",
                "Client@12345", "Dar es Salaam", "Ilala", "Tanzania"));

        as(client.getEmail(), "CLIENT");
        RequestResponse submitted = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.INDUSTRIAL, "Undo sample",
                "A request used to check reversal.", null));

        as(hod.getEmail(), "HOD");
        requestService.startReview(submitted.id());
        assertThatThrownBy(() -> requestService.review(submitted.id(), new ReviewDto(true, " ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("approval needs a reason");
        assertThatThrownBy(() -> requestService.review(submitted.id(), new ReviewDto(false, " ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rejection needs a reason");

        RequestResponse rejected = requestService.review(submitted.id(),
                new ReviewDto(false, "Outside the laboratory scope."));
        assertThat(rejected.status()).isEqualTo(RequestStatus.REJECTED);

        assertThatThrownBy(() -> requestService.undo(submitted.id(), " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("undo needs a reason");
        RequestResponse restored = requestService.undo(submitted.id(), "The client clarified the sample list.");
        assertThat(restored.status()).isEqualTo(RequestStatus.UNDER_REVIEW);

        assertThatThrownBy(() -> requestService.cancel(submitted.id(), " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancellation needs a reason");
        RequestResponse cancelled = requestService.cancel(submitted.id(), "The client withdrew the samples.");
        assertThat(cancelled.status()).isEqualTo(RequestStatus.CANCELLED);

        RequestResponse back = requestService.undo(submitted.id(), "The withdrawal was sent by mistake.");
        assertThat(back.status()).isEqualTo(RequestStatus.UNDER_REVIEW);

        assertThat(requestAuditService.list(submitted.id()))
                .extracting(event -> event.reason())
                .contains(
                        "Outside the laboratory scope.",
                        "The client clarified the sample list.",
                        "The client withdrew the samples.",
                        "The withdrawal was sent by mistake.");
    }

    @Test
    @DisplayName("a head and an expert see only their own department's services")
    void consultancyServicesStayInsideTheDepartment() {
        String stamp = String.valueOf(System.nanoTime());
        Department ict = department("ICT services " + stamp, "ICT-" + stamp);
        Department industrial = department("Industrial services " + stamp, "LAB-" + stamp);
        route(ConsultancyType.ICT, ict);
        route(ConsultancyType.INDUSTRIAL, industrial);

        User hod = userService.createUser("hodview." + stamp + "@test.local", "Staff@12345",
                "Grace Mushi", null, java.util.Set.of(RoleName.HOD));
        User expertUser = userService.createUser("expertview." + stamp + "@test.local", "Staff@12345",
                "Daniel Shao", null, java.util.Set.of(RoleName.EXPERT));
        User otherExpert = userService.createUser("otherview." + stamp + "@test.local", "Staff@12345",
                "Dr. Rehema Lyimo", null, java.util.Set.of(RoleName.EXPERT));
        User manager = userService.createUser("managerview." + stamp + "@test.local", "Staff@12345",
                "Neema Juma", null, java.util.Set.of(RoleName.MANAGER));
        departmentService.appointHod(ict.getId(), hod.getId());
        departmentService.addMember(ict.getId(), expertUser.getId(), true);
        departmentService.addMember(industrial.getId(), otherExpert.getId(), true);

        SecurityContextHolder.clearContext();
        Long industrialCategoryId = catalogueService.listCategories(false).stream()
                .filter(row -> row.consultancyType() == ConsultancyType.INDUSTRIAL)
                .map(CategoryResponse::id)
                .findFirst()
                .orElseThrow();

        as(hod.getEmail(), "HOD");
        assertThat(catalogueService.listCategories(false))
                .extracting(CategoryResponse::consultancyType)
                .containsExactly(ConsultancyType.ICT);
        assertThatThrownBy(() -> catalogueService.listServices(industrialCategoryId, false))
                .isInstanceOf(ResourceNotFoundException.class);

        as(expertUser.getEmail(), "EXPERT");
        assertThat(catalogueService.listCategories(false))
                .extracting(CategoryResponse::consultancyType)
                .containsExactly(ConsultancyType.ICT);

        as(otherExpert.getEmail(), "EXPERT");
        assertThat(catalogueService.listCategories(false))
                .extracting(CategoryResponse::consultancyType)
                .containsExactly(ConsultancyType.INDUSTRIAL);

        as(manager.getEmail(), "MANAGER");
        assertThat(catalogueService.listCategories(false))
                .extracting(CategoryResponse::consultancyType)
                .contains(ConsultancyType.ICT, ConsultancyType.INDUSTRIAL, ConsultancyType.ENGINEERING);
    }

    private void pay(Long requestId, String reference) {
        SecurityContextHolder.clearContext();
        var quote = quotationService.create(new CreateQuotationRequest(
                requestId, new BigDecimal("0.18"), LocalDate.now().plusDays(21),
                "Laboratory work.", List.of(new QuotationItemDto("Oil panel", BigDecimal.ONE, new BigDecimal("100000")))));
        quotationService.send(quote.id());
        quotationService.accept(quote.id());
        var payment = billingService.issueControlNumber(requestId);
        billingService.handleCallback(payment.controlNumber(), reference, "Recorded in the test");
    }

    private Department department(String name, String code) {
        return departmentRepository.save(Department.builder().name(name).code(code).active(true).build());
    }

    private void route(ConsultancyType type, Department department) {
        ServiceRoute existing = routeRepository.findByConsultancyType(type).orElse(null);
        if (existing != null) {
            existing.setDepartment(department);
            existing.setActive(true);
            routeRepository.save(existing);
            return;
        }
        routeRepository.save(ServiceRoute.builder()
                .consultancyType(type)
                .department(department)
                .active(true)
                .build());
    }

    private Consultant consultant(User user, String name) {
        return consultantService.create(new CreateConsultantRequest(
                name, ExpertCategory.INDUSTRIAL_TECHNOLOGIST, "Laboratory testing",
                10, new BigDecimal("150000"), user.getId()));
    }

    private void as(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email.toLowerCase(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
