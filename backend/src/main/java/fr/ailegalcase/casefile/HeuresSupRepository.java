package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HeuresSupRepository extends JpaRepository<HeuresSupAnalysis, UUID> {

    Optional<HeuresSupAnalysis> findByCaseFileId(UUID caseFileId);
}
