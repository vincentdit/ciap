package tz.go.tirdo.ciap.domain.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tz.go.tirdo.ciap.common.SearchTerms;

import java.time.Instant;
import java.util.List;

/**
 * In-app notices. A failed write is logged and does not undo the business
 * transaction, because the write runs after that transaction commits.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AppNotificationRepository repository;

    public NotificationService(AppNotificationRepository repository) {
        this.repository = repository;
    }

    public void schedule(String email, String message) {
        if (email == null || email.isBlank() || message == null || message.isBlank()) {
            return;
        }
        String to = email.trim();
        String body = message.length() > 500 ? message.substring(0, 500) : message;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write(to, body);
                }
            });
            return;
        }
        write(to, body);
    }

    public void write(String email, String message) {
        try {
            repository.save(AppNotification.builder()
                    .recipientEmail(email)
                    .message(message)
                    .readFlag(false)
                    .createdAt(Instant.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn("In-app notice was not stored");
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listFor(String email) {
        if (email == null || email.isBlank()) {
            return List.of();
        }
        return repository.findTop50ByRecipientEmailIgnoreCaseOrderByIdDesc(email.trim())
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> search(String email, String q, Pageable pageable) {
        if (email == null || email.isBlank()) {
            return Page.empty(pageable);
        }
        Pageable ordered = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return repository.searchFor(email.trim(), SearchTerms.like(q), ordered).map(NotificationResponse::from);
    }
}
