package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RuptureConvIndemniteRepository extends JpaRepository<RuptureConvIndemniteAnalysis, UUID> {

    Optional<RuptureConvIndemniteAnalysis> findByCaseFileId(UUID caseFileId);
}
