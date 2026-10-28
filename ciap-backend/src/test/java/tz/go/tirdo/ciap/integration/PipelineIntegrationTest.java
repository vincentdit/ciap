package tz.go.tirdo.ciap.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.domain.billing.BillingService;
import tz.go.tirdo.ciap.domain.billing.PaymentStatus;
import tz.go.tirdo.ciap.domain.billing.QuotationStatus;
import tz.go.tirdo.ciap.domain.billing.QuotationService;
import tz.go.tirdo.ciap.domain.billing.dto.CreateQuotationRequest;
import tz.go.tirdo.ciap.domain.billing.dto.PaymentResponse;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationItemDto;
import tz.go.tirdo.ciap.domain.billing.dto.QuotationResponse;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientService;
import tz.go.tirdo.ciap.domain.client.ClientType;
import tz.go.tirdo.ciap.domain.client.IndustryCategory;
import tz.go.tirdo.ciap.domain.client.dto.ClientRegistrationRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestService;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.domain.consultancy.dto.AssignDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.CreateRequestDto;
import tz.go.tirdo.ciap.domain.consultancy.dto.RequestResponse;
import tz.go.tirdo.ciap.domain.consultancy.dto.ReviewDto;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ConsultantService;
import tz.go.tirdo.ciap.domain.consultant.ExpertCategory;
import tz.go.tirdo.ciap.domain.consultant.dto.CreateConsultantRequest;
import tz.go.tirdo.ciap.domain.project.ProjectStatus;
import tz.go.tirdo.ciap.domain.project.ProjectService;
import tz.go.tirdo.ciap.domain.project.TaskStatus;
import tz.go.tirdo.ciap.domain.project.dto.CreateProjectRequest;
import tz.go.tirdo.ciap.domain.project.dto.CreateTaskRequest;
import tz.go.tirdo.ciap.domain.project.dto.ProjectResponse;
import tz.go.tirdo.ciap.domain.project.dto.TaskResponse;
import tz.go.tirdo.ciap.domain.reporting.ReportingService;
import tz.go.tirdo.ciap.domain.reporting.dto.ExecutiveSummary;
import tz.go.tirdo.ciap.domain.reporting.dto.RevenueDashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Walks the full revenue pipeline through the real service layer against H2:
 * client -> request -> review -> quote -> bill -> pay -> assign -> project -> tasks -> report.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("End-to-end pipeline")
class PipelineIntegrationTest {

    @Autowired ClientService clientService;
    @Autowired ConsultantService consultantService;
    @Autowired ConsultancyRequestService requestService;
    @Autowired QuotationService quotationService;
    @Autowired BillingService billingService;
    @Autowired ProjectService projectService;
    @Autowired ReportingService reportingService;

    private Client registerClient(String email, String tin) {
        return clientService.register(new ClientRegistrationRequest(
                "Acme Industries Ltd", tin, ClientType.INDUSTRY, IndustryCategory.MANUFACTURING,
                "Jane Mushi", "+255700000000", email, "Client@12345", "Dar es Salaam", "Plot 1", "Tanzania"));
    }

    private Consultant createConsultant() {
        return consultantService.create(new CreateConsultantRequest(
                "Dr. Asha Kimaro", ExpertCategory.INDUSTRIAL_TECHNOLOGIST,
                "process optimization", 12, new BigDecimal("150000"), null));
    }

    @Test
    @DisplayName("full happy path produces a paid request, a delivery project, and reportable revenue")
    void fullPipeline() {
        Client client = registerClient("jane@acme1.co.tz", "100000001");
        Consultant consultant = createConsultant();

        // Submit
        RequestResponse req = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.INDUSTRIAL, "Bottling line optimization",
                "Improve throughput on line 2", LocalDate.now().plusMonths(1)));
        assertThat(req.status()).isEqualTo(RequestStatus.SUBMITTED);
        assertThat(req.referenceNo()).startsWith("CIAP-");
        Long requestId = req.id();

        // Review -> QUOTED
        requestService.startReview(requestId);
        RequestResponse reviewed = requestService.review(requestId, new ReviewDto(true, "Scope is clear"));
        assertThat(reviewed.status()).isEqualTo(RequestStatus.QUOTED);

        // Quote: 5 x 300,000 + 1 x 800,000 = 2,300,000 subtotal; +18% VAT = 2,714,000
        QuotationResponse quote = quotationService.create(new CreateQuotationRequest(
                requestId, new BigDecimal("0.18"), LocalDate.now().plusMonths(2), "Phase 1",
                List.of(
                        new QuotationItemDto("Process audit", new BigDecimal("5"), new BigDecimal("300000")),
                        new QuotationItemDto("Final report", new BigDecimal("1"), new BigDecimal("800000")))));
        assertThat(quote.subtotal()).isEqualByComparingTo("2300000");
        assertThat(quote.vatAmount()).isEqualByComparingTo("414000");
        assertThat(quote.total()).isEqualByComparingTo("2714000");
        assertThat(quote.versionNo()).isEqualTo(1);

        quotationService.send(quote.id());

        // Bill: issue control number -> request AWAITING_PAYMENT
        PaymentResponse payment = billingService.issueControlNumber(requestId);
        assertThat(payment.controlNumber()).hasSize(12);
        assertThat(payment.amount()).isEqualByComparingTo("2714000");
        assertThat(requestService.get(requestId).status()).isEqualTo(RequestStatus.AWAITING_PAYMENT);

        // GePG callback -> PAID
        PaymentResponse settled = billingService.handleCallback(payment.controlNumber(), "GEPG-TEST-1");
        assertThat(settled.paidAt()).isNotNull();
        assertThat(requestService.get(requestId).status()).isEqualTo(RequestStatus.PAID);
        PaymentResponse repeated = billingService.handleCallback(payment.controlNumber(), "GEPG-TEST-1");
        assertThat(repeated.id()).isEqualTo(settled.id());
        assertThat(repeated.status()).isEqualTo(settled.status());

        // Assign
        RequestResponse assigned = requestService.assign(requestId, new AssignDto(consultant.getId()));
        assertThat(assigned.status()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(assigned.assignedConsultantName()).isEqualTo("Dr. Asha Kimaro");

        // Spawn project (lead auto-filled from assignment)
        ProjectResponse project = projectService.createForRequest(requestId, new CreateProjectRequest(
                "Bottling line delivery", "Execution", LocalDate.now(), LocalDate.now().plusMonths(3), null));
        assertThat(project.status()).isEqualTo(ProjectStatus.PLANNING);
        assertThat(project.progressPercent()).isZero();
        assertThat(project.leadConsultantName()).isEqualTo("Dr. Asha Kimaro");

        projectService.start(project.id());

        // Two tasks; completing one -> 50%
        TaskResponse t1 = projectService.addTask(project.id(),
                new CreateTaskRequest("Baseline audit", null, consultant.getId(), LocalDate.now().plusWeeks(2)));
        projectService.addTask(project.id(),
                new CreateTaskRequest("Implement changes", null, null, null));

        ProjectResponse afterDone = projectService.updateTaskStatus(project.id(), t1.id(), TaskStatus.DONE);
        assertThat(afterDone.progressPercent()).isEqualTo(50);

        // Reporting reflects the collected revenue and active project.
        // Use >= because other test methods in this class may also settle payments
        // into the same shared H2 context (no per-method rollback on @SpringBootTest).
        ExecutiveSummary summary = reportingService.executiveSummary();
        assertThat(summary.totalRevenueCollected()).isGreaterThanOrEqualTo(new BigDecimal("2714000"));
        assertThat(summary.activeProjects()).isGreaterThanOrEqualTo(1);

        RevenueDashboard revenue = reportingService.revenue();
        assertThat(revenue.totalCollected()).isGreaterThanOrEqualTo(new BigDecimal("2714000"));
        assertThat(revenue.paidCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("issuing a control number before QUOTED is rejected")
    void cannotBillBeforeQuoted() {
        Client client = registerClient("jane@acme2.co.tz", "100000002");
        RequestResponse req = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.ICT, "ERP rollout", "desc", null));

        assertThatThrownBy(() -> billingService.issueControlNumber(req.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("illegal workflow transition is rejected")
    void illegalTransitionRejected() {
        Client client = registerClient("jane@acme3.co.tz", "100000003");
        RequestResponse req = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.ENGINEERING, "Plant design", "desc", null));

        // SUBMITTED -> close is not allowed
        assertThatThrownBy(() -> requestService.close(req.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("GePG callback is idempotent")
    void callbackIdempotent() {
        Client client = registerClient("jane@acme4.co.tz", "100000004");
        RequestResponse req = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.ENVIRONMENTAL, "Waste audit", "desc", null));
        requestService.startReview(req.id());
        requestService.review(req.id(), new ReviewDto(true, "ok"));
        QuotationResponse q = quotationService.create(new CreateQuotationRequest(
                req.id(), null, null, null,
                List.of(new QuotationItemDto("Audit", BigDecimal.ONE, new BigDecimal("500000")))));
        quotationService.send(q.id());
        PaymentResponse p = billingService.issueControlNumber(req.id());

        billingService.handleCallback(p.controlNumber(), "GEPG-X");
        // Second callback for the same control number must not throw or double-count
        PaymentResponse again = billingService.handleCallback(p.controlNumber(), "GEPG-X");
        assertThat(again.controlNumber()).isEqualTo(p.controlNumber());
        assertThat(requestService.get(req.id()).status()).isEqualTo(RequestStatus.PAID);
    }

    @Test
    @DisplayName("a newly sent quotation expires the previous one and a second acceptance is rejected")
    void quotationVersionsSupersede() {
        Client client = registerClient("versions@acme.co.tz", "100000009");
        RequestResponse req = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.INDUSTRIAL, "Versioned quote", "desc", null));
        requestService.startReview(req.id());
        requestService.review(req.id(), new ReviewDto(true, "ok"));

        QuotationResponse first = quotationService.create(new CreateQuotationRequest(
                req.id(), null, null, null,
                List.of(new QuotationItemDto("First", BigDecimal.ONE, new BigDecimal("1000")))));
        quotationService.send(first.id());
        QuotationResponse second = quotationService.create(new CreateQuotationRequest(
                req.id(), null, null, null,
                List.of(new QuotationItemDto("Second", BigDecimal.ONE, new BigDecimal("2000")))));
        quotationService.send(second.id());

        assertThat(first.versionNo()).isEqualTo(1);
        assertThat(second.versionNo()).isEqualTo(2);
        List<QuotationResponse> quotes = quotationService.listForRequest(req.id());
        assertThat(quotes).anyMatch(q -> q.id().equals(first.id()) && q.status() == QuotationStatus.EXPIRED);
        assertThat(quotes).anyMatch(q -> q.id().equals(second.id()) && q.status() == QuotationStatus.SENT);

        quotationService.accept(second.id());
        QuotationResponse third = quotationService.create(new CreateQuotationRequest(
                req.id(), null, null, null,
                List.of(new QuotationItemDto("Third", BigDecimal.ONE, new BigDecimal("3000")))));
        quotationService.send(third.id());
        assertThatThrownBy(() -> quotationService.accept(third.id()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("payment confirmation follows the payment record and does not close the request")
    void financeConfirmationFollowsThePaymentRecord() {
        Client client = registerClient("finance-gate@acme.co.tz", "100000010");
        RequestResponse req = requestService.submit(new CreateRequestDto(
                client.getId(), ConsultancyType.INDUSTRIAL, "Finance gate", "desc", null));
        requestService.startReview(req.id());
        requestService.review(req.id(), new ReviewDto(true, "ok"));
        QuotationResponse quote = quotationService.create(new CreateQuotationRequest(
                req.id(), null, null, null,
                List.of(new QuotationItemDto("Inspection", BigDecimal.ONE, new BigDecimal("1000")))));
        quotationService.send(quote.id());

        assertThatThrownBy(() -> requestService.markAwaitingPayment(req.id()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> requestService.confirmPayment(req.id()))
                .isInstanceOf(BusinessException.class);

        PaymentResponse first = billingService.issueControlNumber(req.id());
        assertThat(requestService.get(req.id()).status()).isEqualTo(RequestStatus.AWAITING_PAYMENT);
        billingService.flag(first.id(), "The slip amount does not match the quotation");
        assertThat(requestService.get(req.id()).status()).isEqualTo(RequestStatus.AWAITING_PAYMENT);

        PaymentResponse replacement = billingService.issueControlNumber(req.id());
        billingService.handleCallback(replacement.controlNumber(), "GEPG-GATE-1", "Slip matches the quotation");
        assertThat(requestService.get(req.id()).status()).isEqualTo(RequestStatus.PAID);

        PaymentResponse repeat = billingService.handleCallback(replacement.controlNumber(), "GEPG-GATE-1");
        assertThat(repeat.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(requestService.get(req.id()).status()).isEqualTo(RequestStatus.PAID);

        Client other = registerClient("finance-dup@acme.co.tz", "100000011");
        RequestResponse secondRequest = requestService.submit(new CreateRequestDto(
                other.getId(), ConsultancyType.ENGINEERING, "Duplicate reference", "desc", null));
        requestService.startReview(secondRequest.id());
        requestService.review(secondRequest.id(), new ReviewDto(true, "ok"));
        quotationService.send(quotationService.create(new CreateQuotationRequest(
                secondRequest.id(), null, null, null,
                List.of(new QuotationItemDto("Inspection", BigDecimal.ONE, new BigDecimal("1000"))))).id());
        PaymentResponse secondPayment = billingService.issueControlNumber(secondRequest.id());
        assertThatThrownBy(() -> billingService.handleCallback(
                secondPayment.controlNumber(), "GEPG-GATE-1", "same slip"))
                .isInstanceOf(BusinessException.class);
        assertThat(requestService.get(secondRequest.id()).status()).isEqualTo(RequestStatus.AWAITING_PAYMENT);
    }
}
