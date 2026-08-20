package tz.go.tirdo.ciap.domain.consultant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tz.go.tirdo.ciap.domain.consultant.dto.CreateConsultantRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Consultant workload & availability")
class ConsultantWorkloadTest {

    @Autowired
    ConsultantService consultantService;

    private Long newConsultant() {
        return consultantService.create(new CreateConsultantRequest(
                "Eng. Test Person", ExpertCategory.ENGINEER, "skills", 5,
                new BigDecimal("100000"), null)).getId();
    }

    @Test
    @DisplayName("availability moves AVAILABLE -> PARTIALLY_AVAILABLE -> FULLY_BOOKED as load grows")
    void availabilityRecomputesOnLoad() {
        Long id = newConsultant();
        assertThat(consultantService.getById(id).getAvailabilityStatus())
                .isEqualTo(AvailabilityStatus.AVAILABLE);

        consultantService.incrementWorkload(id); // 1
        assertThat(consultantService.getById(id).getAvailabilityStatus())
                .isEqualTo(AvailabilityStatus.PARTIALLY_AVAILABLE);

        for (int i = 0; i < 4; i++) {
            consultantService.incrementWorkload(id); // up to 5
        }
        assertThat(consultantService.getById(id).getActiveAssignments()).isEqualTo(5);
        assertThat(consultantService.getById(id).getAvailabilityStatus())
                .isEqualTo(AvailabilityStatus.FULLY_BOOKED);
    }

    @Test
    @DisplayName("decrement returns to AVAILABLE at zero and never goes negative")
    void decrementFloorsAtZero() {
        Long id = newConsultant();
        consultantService.incrementWorkload(id);
        consultantService.decrementWorkload(id);
        assertThat(consultantService.getById(id).getActiveAssignments()).isZero();
        assertThat(consultantService.getById(id).getAvailabilityStatus())
                .isEqualTo(AvailabilityStatus.AVAILABLE);

        // extra decrement must not underflow
        consultantService.decrementWorkload(id);
        assertThat(consultantService.getById(id).getActiveAssignments()).isZero();
    }
}
