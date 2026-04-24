package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Annexe13BeRepository extends JpaRepository<Annexe13BeAnalysis, UUID> {

    Optional<Annexe13BeAnalysis> findByCaseFileId(UUID caseFileId);
}
