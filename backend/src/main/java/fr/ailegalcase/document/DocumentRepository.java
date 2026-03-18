package fr.ailegalcase.document;

import fr.ailegalcase.casefile.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCaseFileOrderByCreatedAtDesc(CaseFile caseFile);

    long countByCaseFileId(UUID caseFileId);
}
