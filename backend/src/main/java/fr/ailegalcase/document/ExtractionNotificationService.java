package fr.ailegalcase.document;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.notification.InAppNotificationService;
import fr.ailegalcase.notification.NotificationType;
import fr.ailegalcase.workspace.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * SF-121-02 : écoute les événements d'extraction échouée et notifie le créateur du dossier :
 *   - notification in-app (cloche F-113)
 *   - email au créateur (fail-open)
 *
 * Isolé dans son propre service pour découpler le pipeline d'extraction des canaux de notification
 * et permettre un fail-open indépendant sur chaque canal.
 */
@Service
public class ExtractionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionNotificationService.class);

    private final DocumentRepository documentRepository;
    private final CaseFileRepository caseFileRepository;
    private final EmailService emailService;
    private final InAppNotificationService inAppNotificationService;

    public ExtractionNotificationService(DocumentRepository documentRepository,
                                          CaseFileRepository caseFileRepository,
                                          EmailService emailService,
                                          InAppNotificationService inAppNotificationService) {
        this.documentRepository = documentRepository;
        this.caseFileRepository = caseFileRepository;
        this.emailService = emailService;
        this.inAppNotificationService = inAppNotificationService;
    }

    @EventListener
    public void onExtractionFailed(ExtractionFailedEvent event) {
        UUID documentId = event.documentId();
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("ExtractionNotification: document {} not found — notification skipped", documentId);
            return;
        }

        UUID caseFileId = document.getCaseFile().getId();
        String caseFileTitle = caseFileRepository.findTitleById(caseFileId).orElse("(sans titre)");
        String reasonLabel = humanLabel(event.reason());

        // 1. Notification in-app (cloche F-113) — fail-open
        try {
            UUID userId = caseFileRepository.findCreatedByUserIdById(caseFileId).orElse(null);
            UUID workspaceId = caseFileRepository.findWorkspaceIdById(caseFileId).orElse(null);
            if (userId != null && workspaceId != null) {
                inAppNotificationService.create(
                        userId, workspaceId, NotificationType.EXTRACTION_FAILED,
                        "Document non analysable — " + event.filename(),
                        reasonLabel + " — dossier « " + caseFileTitle + " »",
                        "/case-files/" + caseFileId);
            } else {
                log.warn("ExtractionNotification: cannot resolve user/workspace for caseFile {} — in-app skipped",
                        caseFileId);
            }
        } catch (Exception e) {
            log.warn("ExtractionNotification: in-app notification failed for document {} — {}",
                    documentId, e.getMessage());
        }

        // 2. Email au créateur — fail-open
        try {
            String email = caseFileRepository.findCreatorEmailById(caseFileId).orElse(null);
            if (email != null) {
                emailService.sendExtractionFailed(email, caseFileId, caseFileTitle,
                        event.filename(), reasonLabel);
            } else {
                log.warn("ExtractionNotification: creator email not found for caseFile {} — email skipped",
                        caseFileId);
            }
        } catch (Exception e) {
            log.warn("ExtractionNotification: email send failed for document {} — {}",
                    documentId, e.getMessage());
        }
    }

    /** Libellé humain du motif d'échec pour affichage dans les notifications. */
    static String humanLabel(ExtractionFailureReason reason) {
        if (reason == null) return "Extraction impossible";
        return switch (reason) {
            case EMPTY_TEXT -> "Document illisible (scan sans texte ou image non-textuelle)";
            case UNSUPPORTED_FORMAT -> "Format de fichier non pris en charge";
            case CORRUPTED -> "Document corrompu ou illisible";
            case EXTRACTION_EXCEPTION -> "Erreur technique lors de l'analyse du document";
            case OCR_FAILED -> "Reconnaissance OCR impossible — réessayer ou envoyer un autre document";
            case OCR_UNSUPPORTED_SIZE -> "Document trop volumineux pour l'OCR (max 5 Mo / 11 pages)";
        };
    }
}
