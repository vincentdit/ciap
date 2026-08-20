package tz.go.tirdo.ciap.domain.billing.dto;

import tz.go.tirdo.ciap.domain.billing.Contract;
import tz.go.tirdo.ciap.domain.billing.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractResponse(
        Long id,
        String contractNo,
        Long requestId,
        Long quotationId,
        ContractStatus status,
        BigDecimal contractValue,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        boolean signedByClient,
        boolean signedByTirdo
) {
    public static ContractResponse from(Contract c) {
        return new ContractResponse(
                c.getId(), c.getContractNo(), c.getRequest().getId(), c.getQuotation().getId(),
                c.getStatus(), c.getContractValue(), c.getCurrency(),
                c.getStartDate(), c.getEndDate(), c.isSignedByClient(), c.isSignedByTirdo());
    }
}
