package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DelegationSyndicaleRepository
        extends JpaRepository<DelegationSyndicaleAnalysis, UUID> {

    Optional<DelegationSyndicaleAnalysis> findByCaseFileId(UUID caseFileId);
}
