package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FauteInexcusableEmployeurRepository
        extends JpaRepository<FauteInexcusableEmployeurAnalysis, UUID> {

    Optional<FauteInexcusableEmployeurAnalysis> findByCaseFileId(UUID caseFileId);
}
