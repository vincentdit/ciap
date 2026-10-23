package tz.go.tirdo.ciap.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.security.JwtProperties;

/**
 * Stops the docker profile from booting with the JWT fallback or the built-in
 * administrator password that ship in source for local development.
 */
@Component
@Profile("docker")
@Order(0)
public class DockerSecretGuard implements ApplicationRunner {

    static final String KNOWN_DEV_JWT_SECRET =
            "Y2lhcC10aXJkby1kZXYtc2VjcmV0LWtleS1jaGFuZ2UtbWUtaW4tcHJvZHVjdGlvbg==";

    /** Historical development bootstrap password. Compared, never logged. */
    static final String KNOWN_DEV_ADMIN_PASSWORD = "Admin@12345";

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final String adminPassword;

    public DockerSecretGuard(JwtProperties jwtProperties,
                              UserRepository userRepository,
                              @Value("${ciap.bootstrap.admin-password:}") String adminPassword) {
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        byte[] key;
        try {
            key = Decoders.BASE64.decode(jwtProperties.secret());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "CIAP_JWT_SECRET must be base64. Refusing to start the docker profile.");
        }
        if (key.length < 32 || KNOWN_DEV_JWT_SECRET.equals(jwtProperties.secret())) {
            throw new IllegalStateException(
                    "CIAP_JWT_SECRET is missing, shorter than 32 bytes, or a known development value. "
                            + "Set a unique secret before starting the docker profile.");
        }
        if (userRepository.count() == 0 && KNOWN_DEV_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "Refusing to create the first administrator with the built-in development password. "
                            + "Set CIAP_ADMIN_PASSWORD before first startup.");
        }
    }
}
