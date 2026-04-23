package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-149-01 : édition manuelle de l'extrait OCR par l'avocat.
 *
 * <p>Deux opérations :
 * <ul>
 *   <li>{@link #editText} — enregistre une nouvelle version de l'extrait.
 *       Au 1er edit, sauvegarde la version d'origine dans
 *       {@code extracted_text_original}. Met à jour {@code text_edited_at}.</li>
 *   <li>{@link #resetToOriginal} — restaure la version OCR d'origine,
 *       remet à null {@code extracted_text_original} et {@code text_edited_at}.</li>
 * </ul>
 */
@Service
public class DocumentExtractionEditService {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractionEditService.class);

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public DocumentExtractionEditService(DocumentRepository documentRepository,
                                         DocumentExtractionRepository extractionRepository,
                                         CaseFileRepository caseFileRepository,
                                         WorkspaceMemberRepository workspaceMemberRepository,
                                         CurrentUserResolver currentUserResolver) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional
    public void editText(UUID caseFileId, UUID documentId, String newText,
                         OidcUser oidcUser, String provider, Principal principal) {
        if (newText == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "extractedText is required");
        }
        DocumentExtraction extraction = loadAndAuthorize(caseFileId, documentId, oidcUser, provider, principal);

        if (extraction.getExtractionStatus() != ExtractionStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot edit — extraction not DONE (current: " + extraction.getExtractionStatus() + ")");
        }

        // Premier edit : sauvegarde la version d'origine
        if (extraction.getExtractedTextOriginal() == null) {
            extraction.setExtractedTextOriginal(extraction.getExtractedText());
        }

        extraction.setExtractedText(newText);
        extraction.setTextEditedAt(Instant.now());
        extractionRepository.save(extraction);

        log.info("Manual edit of extraction for document {} ({} chars)", documentId, newText.length());
    }

    @Transactional
    public void resetToOriginal(UUID caseFileId, UUID documentId,
                                OidcUser oidcUser, String provider, Principal principal) {
        DocumentExtraction extraction = loadAndAuthorize(caseFileId, documentId, oidcUser, provider, principal);

        if (extraction.getExtractedTextOriginal() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No manual edit recorded — nothing to reset");
        }

        extraction.setExtractedText(extraction.getExtractedTextOriginal());
        extraction.setExtractedTextOriginal(null);
        extraction.setTextEditedAt(null);
        extractionRepository.save(extraction);

        log.info("Reset extraction to original for document {}", documentId);
    }

    private DocumentExtraction loadAndAuthorize(UUID caseFileId, UUID documentId,
                                                OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        Workspace workspace = member.getWorkspace();

        CaseFile caseFile = caseFileRepository.findById(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!document.getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }

        return extractionRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction not found"));
    }
}
