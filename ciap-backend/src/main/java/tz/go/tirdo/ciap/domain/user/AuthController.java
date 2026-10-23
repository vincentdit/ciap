package tz.go.tirdo.ciap.domain.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import org.springframework.security.core.Authentication;
import tz.go.tirdo.ciap.domain.user.dto.AuthResponse;
import tz.go.tirdo.ciap.domain.user.dto.ChangePasswordRequest;
import tz.go.tirdo.ciap.domain.user.dto.LoginRequest;
import tz.go.tirdo.ciap.domain.user.dto.RefreshTokenRequest;
import tz.go.tirdo.ciap.domain.user.dto.RegisterUserRequest;
import tz.go.tirdo.ciap.domain.user.dto.UserResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    /** Replaces the caller's password and revokes existing tokens. */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication authentication,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.message("Password updated. Sign in again."));
    }

    /** Revokes the caller's access and refresh tokens. */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.message("Signed out"));
    }

    /** Internal staff provisioning — ADMIN only. */
    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> registerStaff(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Staff account created", authService.registerStaff(request)));
    }
}
