package tz.go.tirdo.ciap.domain.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.domain.user.dto.AuthResponse;
import tz.go.tirdo.ciap.domain.user.dto.ChangePasswordRequest;
import tz.go.tirdo.ciap.domain.user.dto.LoginRequest;
import tz.go.tirdo.ciap.domain.user.dto.RefreshTokenRequest;
import tz.go.tirdo.ciap.domain.user.dto.RegisterUserRequest;
import tz.go.tirdo.ciap.domain.user.dto.UserResponse;
import tz.go.tirdo.ciap.security.CustomUserDetailsService;
import tz.go.tirdo.ciap.security.JwtService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtService jwtService,
                       UserService userService,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.warn("Failed login attempt for {}", request.email());
            throw ex;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return buildAuthResponse(userDetails, user);
    }

    /**
     * Exchange a valid refresh token for a new access token (with refresh-token rotation).
     * Rejects access tokens, expired tokens, and tokens for unknown users with 401.
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        final String token = request.refreshToken();

        final String type;
        final String email;
        try {
            type = jwtService.extractTokenType(token);
            email = jwtService.extractUsername(token);
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (!"refresh".equals(type)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!userDetails.isEnabled() || !jwtService.isValid(token, userDetails)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        Integer version = jwtService.extractTokenVersion(token);
        String tokenId = jwtService.extractTokenId(token);
        if (!user.isEnabled()
                || version == null
                || version != user.getTokenVersion()
                || tokenId == null
                || !tokenId.equals(user.getRefreshTokenId())) {
            log.warn("Rejected refresh token for {}", email);
            throw new BadCredentialsException("Invalid refresh token");
        }

        return buildAuthResponse(userDetails, user);
    }

    /**
     * Replaces the caller's password and revokes every existing token.
     * The caller must sign in again with the new password.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Rejected password change for {}", email);
            throw new BadCredentialsException("Invalid credentials");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("Choose a different password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setRefreshTokenId(null);
        userRepository.save(user);
        log.info("Password changed and sessions revoked for {}", email);
    }

    /** Invalidates every token issued for this account, including other devices. */
    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            user.setRefreshTokenId(null);
            userRepository.save(user);
            log.info("Session revoked for {}", email);
        });
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails, User user) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        String refreshId = UUID.randomUUID().toString();
        user.setRefreshTokenId(refreshId);
        userRepository.save(user);

        String access = jwtService.generateAccessToken(userDetails, roles, user.getTokenVersion());
        String refresh = jwtService.generateRefreshToken(userDetails, refreshId, user.getTokenVersion());

        return new AuthResponse(
                access, refresh, "Bearer",
                jwtService.getAccessTokenTtlMinutes() * 60,
                user.getEmail(), user.getFullName(),
                user.getRoles().stream().map(r -> r.getName().name()).toList()
        );
    }

    @Transactional
    public UserResponse registerStaff(RegisterUserRequest request) {
        if (request.roles() == null
                || request.roles().size() != 1
                || request.roles().contains(RoleName.CLIENT)) {
            throw new BusinessException(
                    "Choose one staff role: administrator, manager, head of department, expert, or finance");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "Staff account could not be created with the details provided");
        }
        User created = userService.createUser(
                request.email(), request.password(), request.fullName(),
                request.phone(), request.roles());
        return UserResponse.from(created);
    }
}
