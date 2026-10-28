package tz.go.tirdo.ciap.domain.client;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.user.User;

@Entity
@Table(name = "clients", indexes = {
        @Index(name = "idx_clients_tin", columnList = "tin", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    /** Tax Identification Number (TRA). */
    @Column(length = 20, unique = true)
    private String tin;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 30)
    private ClientType clientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "industry_category", nullable = false, length = 40)
    private IndustryCategory industryCategory;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 100)
    private String region;

    @Column(length = 300)
    private String physicalAddress;

    @Column(length = 80)
    private String country;

    /** Linked login account (role CLIENT). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}
