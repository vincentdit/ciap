package tz.go.tirdo.ciap.domain.department;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.user.User;

@Entity
@Table(name = "departments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** The one active Head of Department. Null until an administrator appoints one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hod_user_id", unique = true)
    private User hodUser;
}
