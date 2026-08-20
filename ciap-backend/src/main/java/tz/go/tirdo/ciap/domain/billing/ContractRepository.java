package tz.go.tirdo.ciap.domain.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByQuotationId(Long quotationId);
    boolean existsByContractNo(String contractNo);
}
