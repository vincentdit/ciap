package tz.go.tirdo.ciap.domain.project;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;

import java.time.LocalDate;

@Entity
@Table(name = "project_milestones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMilestone extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean achieved = false;

    @Column(name = "achieved_date")
    private LocalDate achievedDate;
}
