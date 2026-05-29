package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItfJudiciaireRepository extends JpaRepository<ItfJudiciaireAnalysis, UUID> {

    Optional<ItfJudiciaireAnalysis> findByCaseFileId(UUID caseFileId);
}
