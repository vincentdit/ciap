package tz.go.tirdo.ciap.domain.department;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.domain.consultant.Consultant;

import java.time.Instant;
import java.time.LocalDate;

/** Immutable assignment or reassignment row. Progress notes and documents stay on the request. */
@Entity
@Table(name = "request_assignments", indexes = {
        @Index(name = "idx_assignment_request", columnList = "request_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "department_id")
    private Long departmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultant_id", nullable = false)
    private Consultant consultant;

    @Column(name = "assigned_by", nullable = false, length = 150)
    private String assignedBy;

    @Column(length = 500)
    private String note;

    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_consultant_id")
    private Consultant previousConsultant;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
