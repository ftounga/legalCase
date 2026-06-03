package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EpargneSalarialeConformiteRepository
        extends JpaRepository<EpargneSalarialeConformiteAnalysis, UUID> {

    Optional<EpargneSalarialeConformiteAnalysis> findByCaseFileId(UUID caseFileId);
}
