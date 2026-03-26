package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnalysisDocumentSnapshotService {

    private final DocumentRepository documentRepository;
    private final AnalysisDocumentRepository analysisDocumentRepository;

    public AnalysisDocumentSnapshotService(DocumentRepository documentRepository,
                                           AnalysisDocumentRepository analysisDocumentRepository) {
        this.documentRepository = documentRepository;
        this.analysisDocumentRepository = analysisDocumentRepository;
    }

    public void snapshot(UUID analysisId, CaseFile caseFile) {
        List<Document> documents = documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile);
        List<AnalysisDocument> snapshots = documents.stream()
                .map(doc -> {
                    AnalysisDocument snap = new AnalysisDocument();
                    snap.setAnalysisId(analysisId);
                    snap.setDocumentId(doc.getId());
                    snap.setDocumentName(doc.getOriginalFilename());
                    return snap;
                })
                .toList();
        analysisDocumentRepository.saveAll(snapshots);
    }
}
