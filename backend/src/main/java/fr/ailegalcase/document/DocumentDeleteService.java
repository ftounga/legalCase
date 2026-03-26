package fr.ailegalcase.document;

import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.ChunkAnalysisRepository;
import fr.ailegalcase.analysis.DocumentAnalysisRepository;
import fr.ailegalcase.audit.AuditLog;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentDeleteService {

    private final DocumentRepository documentRepository;
    private final CaseFileRepository caseFileRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final ChunkAnalysisRepository chunkAnalysisRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public DocumentDeleteService(DocumentRepository documentRepository,
                                 CaseFileRepository caseFileRepository,
                                 DocumentExtractionRepository documentExtractionRepository,
                                 DocumentChunkRepository documentChunkRepository,
                                 DocumentAnalysisRepository documentAnalysisRepository,
                                 ChunkAnalysisRepository chunkAnalysisRepository,
                                 AnalysisJobRepository analysisJobRepository,
                                 AuditLogRepository auditLogRepository,
                                 CurrentUserResolver currentUserResolver,
                                 WorkspaceMemberRepository workspaceMemberRepository) {
        this.documentRepository = documentRepository;
        this.caseFileRepository = caseFileRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.auditLogRepository = auditLogRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public void delete(UUID caseFileId, UUID documentId, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        if (analysisJobRepository.existsByCaseFileIdAndStatusIn(caseFileId,
                List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Suppression impossible : une analyse est en cours sur ce dossier.");
        }

        // Cascade deletion: chunk_analyses → document_chunks → document_analyses → document_extractions → document
        List<DocumentExtraction> extractions = documentExtractionRepository.findByDocumentIdIn(List.of(documentId));
        List<UUID> extractionIds = extractions.stream().map(DocumentExtraction::getId).toList();

        if (!extractionIds.isEmpty()) {
            List<UUID> chunkIds = documentChunkRepository.findByExtractionIdIn(extractionIds)
                    .stream().map(DocumentChunk::getId).toList();
            if (!chunkIds.isEmpty()) {
                chunkAnalysisRepository.deleteByChunkIdIn(chunkIds);
            }
            documentAnalysisRepository.deleteByExtractionIdIn(extractionIds);
            documentChunkRepository.deleteByExtractionIdIn(extractionIds);
            documentExtractionRepository.deleteByDocumentIdIn(List.of(documentId));
        }

        documentRepository.deleteById(documentId);

        caseFile.setLastDocumentDeletedAt(Instant.now());
        caseFileRepository.save(caseFile);

        AuditLog log = new AuditLog();
        log.setWorkspaceId(workspace.getId());
        log.setUserId(user.getId());
        log.setCaseFileId(caseFileId);
        log.setAction("DOCUMENT_DELETED");
        log.setMetadata("""
                {"documentId":"%s","documentName":"%s","caseFileId":"%s","caseFileTitle":"%s"}"""
                .formatted(documentId, escape(document.getOriginalFilename()),
                        caseFileId, escape(caseFile.getTitle())));
        auditLogRepository.save(log);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
