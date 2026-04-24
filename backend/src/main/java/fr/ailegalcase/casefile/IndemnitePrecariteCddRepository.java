package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndemnitePrecariteCddRepository extends JpaRepository<IndemnitePrecariteCddAnalysis, UUID> {

    Optional<IndemnitePrecariteCddAnalysis> findByCaseFileId(UUID caseFileId);
}
