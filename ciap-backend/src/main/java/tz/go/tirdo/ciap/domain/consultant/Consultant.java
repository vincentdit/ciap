package tz.go.tirdo.ciap.domain.consultant;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.user.User;

@Entity
@Table(name = "consultants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consultant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ExpertCategory category;

    /** Comma-separated skills/specialisations (simple v1; normalise later if needed). */
    @Column(length = 500)
    private String skills;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 30)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    /** Current count of active assignments — used for workload management. */
    @Column(name = "active_assignments", nullable = false)
    @Builder.Default
    private int activeAssignments = 0;

    /** Rolling average performance rating (0–5). */
    @Column(name = "performance_rating")
    @Builder.Default
    private Double performanceRating = 0.0;

    @Column(name = "billing_rate_per_hour", precision = 12, scale = 2)
    private java.math.BigDecimal billingRatePerHour;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}
