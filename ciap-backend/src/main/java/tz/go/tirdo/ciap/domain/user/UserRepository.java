package tz.go.tirdo.ciap.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u.email from User u join u.roles r where r.name = :role and u.enabled = true")
    List<String> findEnabledEmailsByRole(@Param("role") RoleName role);

    @Query("""
            select u from User u
            where :q is null
               or lower(u.fullName) like :q
               or lower(u.email) like :q
               or lower(coalesce(u.phone, '')) like :q
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}
