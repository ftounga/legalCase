package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "divorce_checklists") @Getter @Setter
public class DivorceChecklist {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true) private CaseFile caseFile;
    @Column(nullable = false, length = 20) private String country;
    @Column(nullable = false, columnDefinition = "TEXT") private String resultData = "{}";
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @PrePersist void onPrePersist() { Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void onPreUpdate() { updatedAt = Instant.now(); }
}
