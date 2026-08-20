package tz.go.tirdo.ciap.integration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tz.go.tirdo.ciap.domain.user.Role;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.RoleRepository;

/**
 * Under the {@code test} profile Flyway is disabled and Hibernate builds the
 * schema, so {@code V2__seed_roles.sql} never runs and the roles table starts
 * empty. {@code DataInitializer} then fails looking up the ADMIN role.
 *
 * This seeder populates the five system roles and is ordered ahead of
 * {@code DataInitializer} (which has no explicit order, so it runs at the
 * default LOWEST_PRECEDENCE) so the admin bootstrap finds its role.
 *
 * Scoped to the {@code test} profile: it never affects production, where
 * Flyway owns this seed.
 */
@Component
@Profile("test")
@Order(0)
public class TestRoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public TestRoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return;
        }
        for (RoleName name : RoleName.values()) {
            roleRepository.save(Role.builder()
                    .name(name)
                    .description(name + " (seeded for tests)")
                    .build());
        }
    }
}
