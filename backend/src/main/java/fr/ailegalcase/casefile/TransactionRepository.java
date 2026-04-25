package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<TransactionAnalysis, UUID> {

    Optional<TransactionAnalysis> findByCaseFileId(UUID caseFileId);
}
