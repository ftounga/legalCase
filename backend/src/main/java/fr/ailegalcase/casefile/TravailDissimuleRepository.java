package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TravailDissimuleRepository extends JpaRepository<TravailDissimuleAnalysis, UUID> {

    Optional<TravailDissimuleAnalysis> findByCaseFileId(UUID caseFileId);
}
