package tz.go.tirdo.ciap.common.sequence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReferenceSequenceRepository extends JpaRepository<ReferenceSequence, String> {

    /**
     * Pessimistic write lock so concurrent allocations serialise on the row
     * rather than racing — gap-free, no duplicate reference numbers.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ReferenceSequence s where s.seqKey = :key")
    Optional<ReferenceSequence> findByKeyForUpdate(@Param("key") String key);
}
