package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CongesEvenementsFamiliauxRepository
        extends JpaRepository<CongesEvenementsFamiliauxAnalysis, UUID> {

    Optional<CongesEvenementsFamiliauxAnalysis> findByCaseFileId(UUID caseFileId);
}
