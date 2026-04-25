package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferesAdminRepository extends JpaRepository<ReferesAdminAnalysis, UUID> {

    Optional<ReferesAdminAnalysis> findByCaseFileId(UUID caseFileId);
}
