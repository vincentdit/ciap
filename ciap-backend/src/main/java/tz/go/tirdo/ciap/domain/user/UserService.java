package tz.go.tirdo.ciap.domain.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String email, String rawPassword, String fullName, String phone, Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (RoleName rn : roleNames) {
            roles.add(roleRepository.findByName(rn)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", rn)));
        }

        User user = User.builder()
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .phone(phone)
                .enabled(true)
                .roles(roles)
                .build();

        return userRepository.save(user);
    }
}
