package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KafalaBeRepository
        extends JpaRepository<KafalaBeAnalysis, UUID> {

    Optional<KafalaBeAnalysis> findByCaseFileId(UUID caseFileId);
}
