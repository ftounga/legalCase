package fr.ailegalcase.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentPieceRepository extends JpaRepository<DocumentPiece, UUID> {

    List<DocumentPiece> findByDocument_IdOrderByOrderIndexAsc(UUID documentId);

    @Modifying
    @Query("DELETE FROM DocumentPiece p WHERE p.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    /**
     * F-260 / SF-260-01 : toutes les pièces d'un dossier (tous documents confondus),
     * triées par numéro persistant croissant. Les pièces non encore numérotées
     * (piece_number null, antérieures au backfill) sont rejetées en fin de liste.
     * Source unique de numérotation pour conclusions (F-98) et fiches.
     */
    @Query("""
            SELECT p FROM DocumentPiece p
            WHERE p.document.caseFile.id = :caseFileId
            ORDER BY p.pieceNumber ASC NULLS LAST, p.document.createdAt ASC, p.orderIndex ASC
            """)
    List<DocumentPiece> findByCaseFileIdOrderByPieceNumber(@Param("caseFileId") UUID caseFileId);

    /**
     * F-260 / SF-260-01 : plus grand {@code piece_number} attribué dans le dossier,
     * ou {@code null} si aucune pièce numérotée n'existe encore. Sert à attribuer
     * {@code max+1} à la création (append en fin sans toucher l'existant).
     */
    @Query("SELECT MAX(p.pieceNumber) FROM DocumentPiece p WHERE p.document.caseFile.id = :caseFileId")
    Integer findMaxPieceNumberByCaseFileId(@Param("caseFileId") UUID caseFileId);
}
