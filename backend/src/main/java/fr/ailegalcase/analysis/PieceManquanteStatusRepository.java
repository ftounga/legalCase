package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-194 SF-194-01 — Repository pour {@link PieceManquanteStatus}.
 */
public interface PieceManquanteStatusRepository extends JpaRepository<PieceManquanteStatus, UUID> {

    /** Recherche par clé d'idempotence {@code (case_file_id, piece_libelle_normalise)}. */
    Optional<PieceManquanteStatus> findByCaseFileIdAndPieceLibelleNormalise(UUID caseFileId,
                                                                            String pieceLibelleNormalise);

    /** Liste tous les statuts pour un dossier — utilisé par matérialisation et collectForEnrichment. */
    List<PieceManquanteStatus> findByCaseFileId(UUID caseFileId);
}
