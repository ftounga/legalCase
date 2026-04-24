package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Belgian9terRepository extends JpaRepository<Belgian9terAnalysis, UUID> {

    Optional<Belgian9terAnalysis> findByCaseFileId(UUID caseFileId);
}
