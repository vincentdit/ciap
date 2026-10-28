package tz.go.tirdo.ciap.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.SearchTerms;
import tz.go.tirdo.ciap.common.exception.BusinessException;
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

    @Transactional(readOnly = true)
    public Page<User> list(Pageable pageable) {
        return list(null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> list(String q, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("fullName"));
        return userRepository.search(SearchTerms.like(q), sorted);
    }

    /**
     * Replaces the account's role. Each account keeps exactly one role.
     * Existing tokens are revoked so the new role applies at the next sign-in.
     */
    @Transactional
    public User replaceRole(Long id, RoleName roleName, String actorEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (user.getEmail().equalsIgnoreCase(actorEmail) && roleName != RoleName.ADMIN) {
            throw new BusinessException("You cannot remove your own administrator role");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
        user.getRoles().clear();
        user.getRoles().add(role);
        revokeSessions(user);
        return userRepository.save(user);
    }

    /** Disabling an account revokes its tokens. An administrator cannot disable themselves. */
    @Transactional
    public User setEnabled(Long id, boolean enabled, String actorEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!enabled && user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new BusinessException("You cannot disable your own account");
        }
        user.setEnabled(enabled);
        if (!enabled) {
            revokeSessions(user);
        }
        return userRepository.save(user);
    }

    private void revokeSessions(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setRefreshTokenId(null);
    }
}
