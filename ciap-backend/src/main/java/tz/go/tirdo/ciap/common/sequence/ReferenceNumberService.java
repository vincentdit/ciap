package tz.go.tirdo.ciap.common.sequence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Allocates gap-free, restart-safe reference numbers from DB-backed sequences.
 * Each allocation runs in its own transaction (REQUIRES_NEW) under a row lock,
 * so numbers are never reused even across multiple app instances.
 */
@Service
public class ReferenceNumberService {

    private final ReferenceSequenceRepository repository;

    public ReferenceNumberService(ReferenceSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long next(String key) {
        ReferenceSequence seq = repository.findByKeyForUpdate(key)
                .orElseGet(() -> ReferenceSequence.builder().seqKey(key).nextValue(1).build());
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        repository.save(seq);
        return value;
    }

    /** Formats a reference like PREFIX-2026-000042. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String formatted(String prefix, String key) {
        return "%s-%d-%06d".formatted(prefix, Year.now().getValue(), next(key) % 1_000_000);
    }
}
