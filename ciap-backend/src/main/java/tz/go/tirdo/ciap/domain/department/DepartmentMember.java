package tz.go.tirdo.ciap.domain.department;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.user.User;

@Entity
@Table(name = "department_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_department_member", columnNames = {"department_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentMember extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "can_receive_work", nullable = false)
    @Builder.Default
    private boolean canReceiveWork = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
