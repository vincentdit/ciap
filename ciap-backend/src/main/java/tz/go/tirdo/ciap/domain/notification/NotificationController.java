package tz.go.tirdo.ciap.domain.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> mine(
            @RequestParam(required = false) String q,
            Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                notificationService.search(authentication.getName(), q, pageable))));
    }
}
