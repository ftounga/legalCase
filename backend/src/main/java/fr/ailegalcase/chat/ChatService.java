package fr.ailegalcase.chat;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.UsageEventService;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.document.ExtractionStatus;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert.
            Réponds aux questions de l'avocat en te basant UNIQUEMENT sur le dossier fourni en contexte.
            Si la réponse ne peut pas être déduite du dossier, dis-le clairement.
            Sois précis, concis et cite les éléments du dossier pour appuyer tes réponses.
            """;

    /** SF-35-03 : budget max de caractères injectés depuis les documents bruts.
     *  150 000 car. ≈ 37 500 tokens Claude → marge confortable sur les 200K de context. */
    static final int DOCUMENT_TEXT_BUDGET_CHARS = 150_000;

    private final ChatMessageRepository chatMessageRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;
    private final PlanLimitService planLimitService;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       CaseFileRepository caseFileRepository,
                       CaseAnalysisRepository caseAnalysisRepository,
                       DocumentRepository documentRepository,
                       DocumentExtractionRepository documentExtractionRepository,
                       WorkspaceMemberRepository workspaceMemberRepository,
                       CurrentUserResolver currentUserResolver,
                       AnthropicService anthropicService,
                       UsageEventService usageEventService,
                       PlanLimitService planLimitService) {
        this.chatMessageRepository = chatMessageRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.documentRepository = documentRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
        this.planLimitService = planLimitService;
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID caseFileId, ChatMessageRequest request,
                                           OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        var member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        UUID workspaceId = member.getWorkspace().getId();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        if (planLimitService.isChatMessageLimitReached(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Limite de messages chat atteinte.");
        }

        var caseAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY,
                        "Aucune synthèse disponible pour ce dossier."));

        boolean useEnriched = request.useEnriched()
                && planLimitService.isEnrichedAnalysisAllowedForWorkspace(workspaceId);

        String userMessage = buildUserMessage(caseAnalysis.getAnalysisResult(),
                caseFileId, request.question());

        AnthropicResult result = useEnriched
                ? anthropicService.analyze(SYSTEM_PROMPT, userMessage, 2048)
                : anthropicService.analyzeFast(SYSTEM_PROMPT, userMessage, 2048);

        ChatMessage message = new ChatMessage();
        message.setCaseFileId(caseFileId);
        message.setUserId(user.getId());
        message.setQuestion(request.question());
        message.setAnswer(result.content());
        message.setModelUsed(result.modelUsed());
        message.setUseEnriched(useEnriched);
        chatMessageRepository.save(message);

        usageEventService.record(caseFileId, user.getId(), JobType.CHAT_MESSAGE,
                result.promptTokens(), result.completionTokens());

        return ChatMessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(UUID caseFileId,
                                                OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        var member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
        UUID workspaceId = member.getWorkspace().getId();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspaceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        return chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId)
                .stream().map(ChatMessageResponse::from).toList();
    }

    /**
     * SF-35-03 : construit le prompt utilisateur avec synthèse + textes bruts des
     * documents DONE (plus récents d'abord). Permet au chat de répondre sur des
     * détails absents de la synthèse (chiffres, dates, références de pièces).
     */
    String buildUserMessage(String synthesis, UUID caseFileId, String question) {
        List<Document> docs = documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId);
        if (docs.isEmpty()) {
            return "Dossier :\n" + synthesis + "\n\nQuestion : " + question;
        }

        List<UUID> docIds = docs.stream().map(Document::getId).toList();
        Map<UUID, DocumentExtraction> extractionsByDocId = documentExtractionRepository
                .findByDocumentIdIn(docIds).stream()
                .collect(Collectors.toMap(e -> e.getDocument().getId(), e -> e, (a, b) -> a, HashMap::new));

        StringBuilder documentsSection = new StringBuilder();
        int remaining = DOCUMENT_TEXT_BUDGET_CHARS;
        int included = 0;
        int excluded = 0;

        for (Document doc : docs) {
            DocumentExtraction ex = extractionsByDocId.get(doc.getId());
            if (ex == null || ex.getExtractionStatus() != ExtractionStatus.DONE) {
                continue; // ignore PENDING / PROCESSING / FAILED silencieusement
            }
            String text = ex.getExtractedText();
            if (text == null || text.isBlank()) continue;

            if (remaining <= 0) {
                excluded++;
                continue;
            }

            String extractor = detectExtractor(ex.getExtractionMetadata());
            String header = "\n\n=== " + doc.getOriginalFilename() + " (" + extractor + ", "
                    + text.length() + " car.) ===\n";
            String slice = text.length() <= remaining ? text : text.substring(0, remaining) + "\n[… tronqué]";
            documentsSection.append(header).append(slice);
            remaining -= slice.length() + header.length();
            included++;
        }

        StringBuilder prompt = new StringBuilder()
                .append("Synthèse du dossier :\n").append(synthesis);
        if (included > 0) {
            prompt.append("\n\nDocuments du dossier (").append(included).append(" inclus");
            if (excluded > 0) {
                prompt.append(", ").append(excluded).append(" exclus par limite de taille");
            }
            prompt.append(") :").append(documentsSection);
        }
        prompt.append("\n\nQuestion : ").append(question);
        return prompt.toString();
    }

    private static String detectExtractor(String metadataJson) {
        if (metadataJson == null) return "extraction classique";
        String lower = metadataJson.toLowerCase();
        if (lower.contains("textract") || lower.contains("ocr")) return "OCR";
        return "extraction classique";
    }
}
