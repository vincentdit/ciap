package tz.go.tirdo.ciap.domain.client.dto;

import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.client.ClientType;
import tz.go.tirdo.ciap.domain.client.IndustryCategory;

public record ClientResponse(
        Long id,
        String companyName,
        String tin,
        ClientType clientType,
        IndustryCategory industryCategory,
        String contactPerson,
        String phone,
        String email,
        String region,
        String physicalAddress,
        String country
) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(
                c.getId(), c.getCompanyName(), c.getTin(), c.getClientType(),
                c.getIndustryCategory(), c.getContactPerson(), c.getPhone(),
                c.getEmail(), c.getRegion(), c.getPhysicalAddress(), c.getCountry());
    }
}
