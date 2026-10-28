package tz.go.tirdo.ciap.domain.department;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

@Entity
@Table(name = "service_routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceRoute extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultancy_type", nullable = false, unique = true, length = 30)
    private ConsultancyType consultancyType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
