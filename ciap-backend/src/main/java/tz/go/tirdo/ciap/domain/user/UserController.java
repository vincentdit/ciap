package tz.go.tirdo.ciap.domain.user;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.user.dto.SetEnabledRequest;
import tz.go.tirdo.ciap.domain.user.dto.UpdateRoleRequest;
import tz.go.tirdo.ciap.domain.user.dto.UserResponse;

/** Role assignment for existing accounts. Administrators only. */
@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(Pageable pageable) {
        var page = userService.list(pageable).map(UserResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateRoleRequest request,
                                                                Authentication authentication) {
        User updated = userService.replaceRole(id, request.role(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Role updated", UserResponse.from(updated)));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<ApiResponse<UserResponse>> setEnabled(@PathVariable Long id,
                                                                @Valid @RequestBody SetEnabledRequest request,
                                                                Authentication authentication) {
        User updated = userService.setEnabled(id, request.enabled(), authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(
                updated.isEnabled() ? "Account enabled" : "Account disabled",
                UserResponse.from(updated)));
    }
}
