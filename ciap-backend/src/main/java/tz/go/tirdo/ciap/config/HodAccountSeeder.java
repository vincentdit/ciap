package tz.go.tirdo.ciap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.domain.department.Department;
import tz.go.tirdo.ciap.domain.department.DepartmentRepository;
import tz.go.tirdo.ciap.domain.department.DepartmentService;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.User;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.domain.user.UserService;

import java.util.Set;

/**
 * Appoints one Head of Department for each service line and enrols consultants
 * who already exist. Safe to run again: an existing head is left in place.
 */
@Component
@Profile("docker")
@Order(50)
public class HodAccountSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HodAccountSeeder.class);
    private static final String PHONE = "+255222666034";

    private final UserRepository userRepository;
    private final UserService userService;
    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;

    public HodAccountSeeder(UserRepository userRepository,
                            UserService userService,
                            DepartmentRepository departmentRepository,
                            DepartmentService departmentService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.departmentRepository = departmentRepository;
        this.departmentService = departmentService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        appoint("INDUSTRIAL", "hod.industrial@tirdo.go.tz", "Amina Selemani");
        appoint("ENGINEERING", "hod.engineering@tirdo.go.tz", "Peter Mwakalinga");
        appoint("ICT", "hod.ict@tirdo.go.tz", "Grace Mushi");
        appoint("ENVIRONMENTAL", "hod.environment@tirdo.go.tz", "Frank Komba");
        departmentService.enrollConsultants();
        log.info("Department heads are in place for the four consultancy service lines.");
    }

    private void appoint(String code, String email, String name) {
        Department department = departmentRepository.findByCode(code).orElse(null);
        if (department == null) {
            return;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userService.createUser(email, DemoDataSeeder.DEMO_PASSWORD, name, PHONE, Set.of(RoleName.HOD));
        }
        if (department.getHodUser() != null && department.getHodUser().getId().equals(user.getId())) {
            return;
        }
        departmentService.appointHod(department.getId(), user.getId());
    }
}
