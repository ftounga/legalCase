package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VpfJeuneMajeurRepository extends JpaRepository<VpfJeuneMajeurAnalysis, UUID> {

    Optional<VpfJeuneMajeurAnalysis> findByCaseFileId(UUID caseFileId);
}
