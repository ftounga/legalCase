package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImmigrationTitleDecisionRepository extends JpaRepository<ImmigrationTitleDecision, UUID> {

    Optional<ImmigrationTitleDecision> findByCaseFileId(UUID caseFileId);
}
