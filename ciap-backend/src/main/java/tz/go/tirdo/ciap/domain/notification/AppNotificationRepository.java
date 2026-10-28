package tz.go.tirdo.ciap.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findTop50ByRecipientEmailIgnoreCaseOrderByIdDesc(String recipientEmail);

    @Query("""
            select n from AppNotification n
            where lower(n.recipientEmail) = lower(:email)
              and (:q is null or lower(n.message) like :q)
            """)
    Page<AppNotification> searchFor(@Param("email") String email, @Param("q") String q, Pageable pageable);
}
