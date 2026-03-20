package fr.ailegalcase.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByExtractionOrderByChunkIndex(DocumentExtraction extraction);

    long countByExtractionId(UUID extractionId);

    List<DocumentChunk> findByExtractionIdIn(Collection<UUID> extractionIds);

    void deleteByExtractionIdIn(Collection<UUID> extractionIds);
}
