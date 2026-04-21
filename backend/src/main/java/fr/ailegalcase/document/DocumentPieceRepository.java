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
}
