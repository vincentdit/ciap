package tz.go.tirdo.ciap.common.sequence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reference_sequences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferenceSequence {

    @Id
    @Column(name = "seq_key", length = 40)
    private String seqKey;

    @Column(name = "next_value", nullable = false)
    private long nextValue;
}
