package tz.go.tirdo.ciap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.domain.user.UserService;

import java.util.Set;

/**
 * Provisions a default ADMIN on first startup (only if no users exist),
 * using the real BCrypt encoder. Credentials are configurable and MUST be
 * changed in production.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${ciap.bootstrap.admin-email:admin@tirdo.go.tz}")
    private String adminEmail;

    @Value("${ciap.bootstrap.admin-password:Admin@12345}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userService.createUser(adminEmail, adminPassword, "System Administrator", null,
                Set.of(RoleName.ADMIN));
        log.warn("=================================================================");
        log.warn(" Default ADMIN created: {} / {}", adminEmail, adminPassword);
        log.warn(" CHANGE THIS PASSWORD IMMEDIATELY IN PRODUCTION.");
        log.warn("=================================================================");
    }
}
