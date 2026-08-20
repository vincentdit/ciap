package tz.go.tirdo.ciap.domain.reporting;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.domain.reporting.dto.*;

/**
 * Reporting & Business Intelligence dashboards (Module 12). All endpoints are read-only
 * and restricted to management/finance roles.
 */
@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','FINANCE')")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExecutiveSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.executiveSummary()));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueDashboard>> revenue() {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.revenue()));
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectDashboard>> projects() {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.projects()));
    }

    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<RequestPipelineDashboard>> requests() {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.requestPipeline()));
    }

    @GetMapping("/clients")
    public ResponseEntity<ApiResponse<ClientDashboard>> clients() {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.clients()));
    }

    @GetMapping("/experts")
    public ResponseEntity<ApiResponse<ExpertUtilizationDashboard>> experts() {
        return ResponseEntity.ok(ApiResponse.ok(reportingService.expertUtilization()));
    }
}
