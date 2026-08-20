package tz.go.tirdo.ciap.domain.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestRepository;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ConsultantService;
import tz.go.tirdo.ciap.domain.project.dto.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * Project Management (Module 6). A project is spawned from a consultancy request that
 * has been billed/assigned, and carries tasks and milestones. Progress is computed from
 * completed tasks.
 */
@Service
public class ProjectService {

    /** A project can only be created once the request's work has been authorized. */
    private static final Set<RequestStatus> CREATABLE_FROM =
            EnumSet.of(RequestStatus.PAID, RequestStatus.ASSIGNED, RequestStatus.IN_EXECUTION);

    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final ConsultantService consultantService;
    private final tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectTaskRepository taskRepository,
                          ProjectMilestoneRepository milestoneRepository,
                          ConsultancyRequestRepository requestRepository,
                          ConsultantService consultantService,
                          tz.go.tirdo.ciap.common.sequence.ReferenceNumberService referenceNumbers) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.milestoneRepository = milestoneRepository;
        this.requestRepository = requestRepository;
        this.consultantService = consultantService;
        this.referenceNumbers = referenceNumbers;
    }

    @Transactional
    public ProjectResponse createForRequest(Long requestId, CreateProjectRequest dto) {
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));

        if (!CREATABLE_FROM.contains(request.getStatus())) {
            throw new BusinessException(
                    "A project can only be created from a PAID, ASSIGNED or IN_EXECUTION request (current: "
                            + request.getStatus() + ")");
        }
        if (projectRepository.existsByRequestId(requestId)) {
            throw new DuplicateResourceException("A project already exists for request " + request.getReferenceNo());
        }

        Consultant lead = null;
        if (dto.leadConsultantId() != null) {
            lead = consultantService.getById(dto.leadConsultantId());
        } else if (request.getAssignedConsultant() != null) {
            lead = request.getAssignedConsultant();
        }

        Project project = Project.builder()
                .projectCode(generateProjectCode())
                .request(request)
                .name(dto.name())
                .description(dto.description())
                .status(ProjectStatus.PLANNING)
                .progressPercent(0)
                .leadConsultant(lead)
                .plannedStartDate(dto.plannedStartDate())
                .plannedEndDate(dto.plannedEndDate())
                .build();

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse start(Long projectId) {
        Project p = getEntity(projectId);
        if (p.getStatus() != ProjectStatus.PLANNING && p.getStatus() != ProjectStatus.ON_HOLD) {
            throw new BusinessException("Only a PLANNING or ON_HOLD project can be started (current: " + p.getStatus() + ")");
        }
        p.setStatus(ProjectStatus.ACTIVE);
        if (p.getActualStartDate() == null) {
            p.setActualStartDate(LocalDate.now());
        }
        return ProjectResponse.from(p);
    }

    @Transactional
    public ProjectResponse hold(Long projectId) {
        Project p = getEntity(projectId);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Only an ACTIVE project can be put on hold (current: " + p.getStatus() + ")");
        }
        p.setStatus(ProjectStatus.ON_HOLD);
        return ProjectResponse.from(p);
    }

    @Transactional
    public ProjectResponse complete(Long projectId) {
        Project p = getEntity(projectId);
        if (p.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException("Only an ACTIVE project can be completed (current: " + p.getStatus() + ")");
        }
        p.setStatus(ProjectStatus.COMPLETED);
        p.setActualEndDate(LocalDate.now());
        return ProjectResponse.from(p);
    }

    @Transactional
    public ProjectResponse cancel(Long projectId) {
        Project p = getEntity(projectId);
        if (p.getStatus() == ProjectStatus.COMPLETED) {
            throw new BusinessException("A COMPLETED project cannot be cancelled");
        }
        p.setStatus(ProjectStatus.CANCELLED);
        return ProjectResponse.from(p);
    }

    @Transactional
    public TaskResponse addTask(Long projectId, CreateTaskRequest dto) {
        Project p = getEntity(projectId);
        Consultant assignee = dto.assigneeConsultantId() != null
                ? consultantService.getById(dto.assigneeConsultantId()) : null;

        ProjectTask task = ProjectTask.builder()
                .title(dto.title())
                .description(dto.description())
                .status(TaskStatus.TODO)
                .assignee(assignee)
                .dueDate(dto.dueDate())
                .build();
        p.addTask(task);
        // Persist the task explicitly so it is assigned an ID immediately and is
        // never a transient reference when the project is flushed.
        ProjectTask saved = taskRepository.save(task);
        p.recomputeProgress();
        projectRepository.save(p);
        return TaskResponse.from(saved);
    }

    @Transactional
    public ProjectResponse updateTaskStatus(Long projectId, Long taskId, TaskStatus status) {
        Project p = getEntity(projectId);
        ProjectTask task = taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task in project " + projectId, taskId));

        task.setStatus(status);
        task.setCompletedAt(status == TaskStatus.DONE ? Instant.now() : null);
        p.recomputeProgress();
        return ProjectResponse.from(projectRepository.save(p));
    }

    @Transactional
    public MilestoneResponse addMilestone(Long projectId, CreateMilestoneRequest dto) {
        Project p = getEntity(projectId);
        ProjectMilestone m = ProjectMilestone.builder()
                .title(dto.title())
                .dueDate(dto.dueDate())
                .achieved(false)
                .build();
        p.addMilestone(m);
        ProjectMilestone saved = milestoneRepository.save(m);
        projectRepository.save(p);
        return MilestoneResponse.from(saved);
    }

    @Transactional
    public MilestoneResponse achieveMilestone(Long projectId, Long milestoneId) {
        ProjectMilestone m = milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone in project " + projectId, milestoneId));
        m.setAchieved(true);
        m.setAchievedDate(LocalDate.now());
        return MilestoneResponse.from(milestoneRepository.save(m));
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long projectId) {
        return ProjectResponse.from(getEntity(projectId));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getForRequest(Long requestId) {
        return ProjectResponse.from(projectRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Project for request", requestId)));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> list(ProjectStatus status, Pageable pageable) {
        Page<Project> page = (status == null)
                ? projectRepository.findAll(pageable)
                : projectRepository.findByStatus(status, pageable);
        return page.map(ProjectResponse::from);
    }

    private Project getEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private String generateProjectCode() {
        return referenceNumbers.formatted("PRJ", "PROJECT");
    }
}
