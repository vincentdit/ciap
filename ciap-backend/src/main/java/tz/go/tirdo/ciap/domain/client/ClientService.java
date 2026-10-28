package tz.go.tirdo.ciap.domain.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.DuplicateResourceException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.client.dto.ClientRegistrationRequest;
import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.User;
import tz.go.tirdo.ciap.domain.user.UserRepository;
import tz.go.tirdo.ciap.domain.user.UserService;

import java.util.Set;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ClientService(ClientRepository clientRepository, UserRepository userRepository, UserService userService) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public Client register(ClientRegistrationRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException(
                    "Registration could not be completed with the details provided");
        }
        if (req.tin() != null && !req.tin().isBlank() && clientRepository.existsByTin(req.tin())) {
            throw new DuplicateResourceException(
                    "Registration could not be completed with the details provided");
        }

        User user = userService.createUser(
                req.email(), req.password(), req.contactPerson(), req.phone(),
                Set.of(RoleName.CLIENT));

        Client client = Client.builder()
                .companyName(req.companyName())
                .tin(req.tin())
                .clientType(req.clientType())
                .industryCategory(req.industryCategory())
                .contactPerson(req.contactPerson())
                .phone(req.phone())
                .email(req.email())
                .region(req.region())
                .physicalAddress(req.physicalAddress())
                .country(blankToNull(req.country()))
                .user(user)
                .build();

        return clientRepository.save(client);
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional(readOnly = true)
    public Client getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", id));
    }

    @Transactional(readOnly = true)
    public Page<Client> list(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Client getByUserId(Long userId) {
        return clientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client profile for user", userId));
    }

    /** Resolve the client profile for the currently authenticated user (by email). */
    @Transactional(readOnly = true)
    public Client getByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client profile for user", user.getId()));
    }
}
