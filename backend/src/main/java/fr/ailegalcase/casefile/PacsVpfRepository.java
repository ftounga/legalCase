package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PacsVpfRepository extends JpaRepository<PacsVpfAnalysis, UUID> {

    Optional<PacsVpfAnalysis> findByCaseFileId(UUID caseFileId);
}
