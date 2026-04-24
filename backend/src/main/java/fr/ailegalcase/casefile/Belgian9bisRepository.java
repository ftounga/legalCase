package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Belgian9bisRepository extends JpaRepository<Belgian9bisAnalysis, UUID> {

    Optional<Belgian9bisAnalysis> findByCaseFileId(UUID caseFileId);
}
