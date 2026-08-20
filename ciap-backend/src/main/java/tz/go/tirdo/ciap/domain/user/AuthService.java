package tz.go.tirdo.ciap.domain.user;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.user.dto.AuthResponse;
import tz.go.tirdo.ciap.domain.user.dto.LoginRequest;
import tz.go.tirdo.ciap.domain.user.dto.RefreshTokenRequest;
import tz.go.tirdo.ciap.domain.user.dto.RegisterUserRequest;
import tz.go.tirdo.ciap.domain.user.dto.UserResponse;
import tz.go.tirdo.ciap.security.CustomUserDetailsService;
import tz.go.tirdo.ciap.security.JwtService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtService jwtService,
                       UserService userService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

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
            throw new BadCredentialsException("Provided token is not a refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isValid(token, userDetails)) {
            throw new BadCredentialsException("Refresh token is expired or invalid");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        return buildAuthResponse(userDetails, user);
    }

    private AuthResponse buildAuthResponse(UserDetails userDetails, User user) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        String access = jwtService.generateAccessToken(userDetails, roles);
        String refresh = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(
                access, refresh, "Bearer",
                jwtService.getAccessTokenTtlMinutes() * 60,
                user.getEmail(), user.getFullName(),
                user.getRoles().stream().map(r -> r.getName().name()).toList()
        );
    }

    @Transactional
    public UserResponse registerStaff(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        User created = userService.createUser(
                request.email(), request.password(), request.fullName(),
                request.phone(), request.roles());
        return UserResponse.from(created);
    }
}
