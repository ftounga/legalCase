package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RuptureAnticipeeCddRepository
        extends JpaRepository<RuptureAnticipeeCddAnalysis, UUID> {

    Optional<RuptureAnticipeeCddAnalysis> findByCaseFileId(UUID caseFileId);
}
