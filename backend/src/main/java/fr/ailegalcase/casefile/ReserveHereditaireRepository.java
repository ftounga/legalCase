package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReserveHereditaireRepository extends JpaRepository<ReserveHereditaireAnalysis, UUID> {

    Optional<ReserveHereditaireAnalysis> findByCaseFileId(UUID caseFileId);
}
