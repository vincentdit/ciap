package tz.go.tirdo.ciap.domain.consultant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.SearchTerms;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultant.dto.CreateConsultantRequest;
import tz.go.tirdo.ciap.domain.user.User;
import tz.go.tirdo.ciap.domain.user.UserRepository;

@Service
public class ConsultantService {

    private static final int WORKLOAD_FULL_THRESHOLD = 5;

    private final ConsultantRepository consultantRepository;
    private final UserRepository userRepository;

    public ConsultantService(ConsultantRepository consultantRepository, UserRepository userRepository) {
        this.consultantRepository = consultantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Consultant create(CreateConsultantRequest req) {
        User user = null;
        if (req.userId() != null) {
            user = userRepository.findById(req.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", req.userId()));
        }
        Consultant consultant = Consultant.builder()
                .fullName(req.fullName())
                .category(req.category())
                .skills(req.skills())
                .yearsExperience(req.yearsExperience())
                .billingRatePerHour(req.billingRatePerHour())
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .user(user)
                .build();
        return consultantRepository.save(consultant);
    }

    @Transactional(readOnly = true)
    public Consultant getById(Long id) {
        return consultantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultant", id));
    }

    @Transactional(readOnly = true)
    public Page<Consultant> list(Pageable pageable) {
        return list(null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Consultant> list(String q, Pageable pageable) {
        return consultantRepository.search(SearchTerms.like(q), pageable);
    }

    /** Increment workload when assigned to a request; recompute availability. */
    @Transactional
    public void incrementWorkload(Long consultantId) {
        Consultant c = getById(consultantId);
        c.setActiveAssignments(c.getActiveAssignments() + 1);
        recomputeAvailability(c);
        consultantRepository.save(c);
    }

    @Transactional
    public void decrementWorkload(Long consultantId) {
        Consultant c = getById(consultantId);
        c.setActiveAssignments(Math.max(0, c.getActiveAssignments() - 1));
        recomputeAvailability(c);
        consultantRepository.save(c);
    }

    private void recomputeAvailability(Consultant c) {
        if (c.getAvailabilityStatus() == AvailabilityStatus.ON_LEAVE) {
            return;
        }
        int load = c.getActiveAssignments();
        if (load == 0) {
            c.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        } else if (load >= WORKLOAD_FULL_THRESHOLD) {
            c.setAvailabilityStatus(AvailabilityStatus.FULLY_BOOKED);
        } else {
            c.setAvailabilityStatus(AvailabilityStatus.PARTIALLY_AVAILABLE);
        }
    }
}
