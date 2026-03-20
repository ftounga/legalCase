package fr.ailegalcase.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, UUID> {

    Optional<DocumentExtraction> findByDocumentId(UUID documentId);

    @Query("SELECT e.document.caseFile.id FROM DocumentExtraction e WHERE e.id = :id")
    Optional<UUID> findCaseFileIdById(@Param("id") UUID id);

    List<DocumentExtraction> findByDocumentIdIn(Collection<UUID> documentIds);

    void deleteByDocumentIdIn(Collection<UUID> documentIds);
}
