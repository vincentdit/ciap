package tz.go.tirdo.ciap.domain.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tz.go.tirdo.ciap.domain.client.ClientType;
import tz.go.tirdo.ciap.domain.client.IndustryCategory;

/** Public self-registration: creates both a Client profile and a CLIENT login. */
public record ClientRegistrationRequest(
        @NotBlank String companyName,
        String tin,
        @NotNull ClientType clientType,
        @NotNull IndustryCategory industryCategory,
        @NotBlank String contactPerson,
        @NotBlank String phone,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String region,
        String physicalAddress,
        @Size(max = 80) String country
) {}
