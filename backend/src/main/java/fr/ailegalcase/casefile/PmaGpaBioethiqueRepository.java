package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PmaGpaBioethiqueRepository extends JpaRepository<PmaGpaBioethiqueAnalysis, UUID> {

    Optional<PmaGpaBioethiqueAnalysis> findByCaseFileId(UUID caseFileId);
}
