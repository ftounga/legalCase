package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PacteSuccessoralBe2018Repository
        extends JpaRepository<PacteSuccessoralBe2018Analysis, UUID> {

    Optional<PacteSuccessoralBe2018Analysis> findByCaseFileId(UUID caseFileId);
}
