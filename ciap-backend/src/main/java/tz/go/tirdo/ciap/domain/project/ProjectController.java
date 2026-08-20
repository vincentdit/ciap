package tz.go.tirdo.ciap.domain.project;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.project.dto.*;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/from-request/{requestId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@PathVariable Long requestId,
                                                               @Valid @RequestBody CreateProjectRequest dto) {
        return ResponseEntity.ok(ApiResponse.ok("Project created", projectService.createForRequest(requestId, dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','EXPERT','FINANCE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.get(id)));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','EXPERT','FINANCE')")
    public ResponseEntity<ApiResponse<ProjectResponse>> getForRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.getForRequest(requestId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> list(
            @RequestParam(required = false) ProjectStatus status, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(projectService.list(status, pageable))));
    }

    // --- lifecycle ---

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> start(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.start(id)));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> hold(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.hold(id)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Project completed", projectService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Project cancelled", projectService.cancel(id)));
    }

    // --- tasks ---

    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','EXPERT')")
    public ResponseEntity<ApiResponse<TaskResponse>> addTask(@PathVariable Long id,
                                                             @Valid @RequestBody CreateTaskRequest dto) {
        return ResponseEntity.ok(ApiResponse.ok("Task added", projectService.addTask(id, dto)));
    }

    @PatchMapping("/{id}/tasks/{taskId}/status")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','EXPERT')")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateTaskStatus(@PathVariable Long id,
                                                                         @PathVariable Long taskId,
                                                                         @Valid @RequestBody UpdateTaskStatusRequest dto) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.updateTaskStatus(id, taskId, dto.status())));
    }

    // --- milestones ---

    @PostMapping("/{id}/milestones")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','EXPERT')")
    public ResponseEntity<ApiResponse<MilestoneResponse>> addMilestone(@PathVariable Long id,
                                                                       @Valid @RequestBody CreateMilestoneRequest dto) {
        return ResponseEntity.ok(ApiResponse.ok("Milestone added", projectService.addMilestone(id, dto)));
    }

    @PostMapping("/{id}/milestones/{milestoneId}/achieve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','EXPERT')")
    public ResponseEntity<ApiResponse<MilestoneResponse>> achieveMilestone(@PathVariable Long id,
                                                                           @PathVariable Long milestoneId) {
        return ResponseEntity.ok(ApiResponse.ok("Milestone achieved", projectService.achieveMilestone(id, milestoneId)));
    }
}
