package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndemniteCongesPayesRepository
        extends JpaRepository<IndemniteCongesPayesAnalysis, UUID> {

    Optional<IndemniteCongesPayesAnalysis> findByCaseFileId(UUID caseFileId);
}
