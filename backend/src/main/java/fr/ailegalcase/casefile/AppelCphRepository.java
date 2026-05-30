package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppelCphRepository extends JpaRepository<AppelCphAnalysis, UUID> {

    Optional<AppelCphAnalysis> findByCaseFileId(UUID caseFileId);
}
