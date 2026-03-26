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
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert.
            Réponds aux questions de l'avocat en te basant UNIQUEMENT sur le dossier fourni en contexte.
            Si la réponse ne peut pas être déduite du dossier, dis-le clairement.
            Sois précis, concis et cite les éléments du dossier pour appuyer tes réponses.
            """;

    private final ChatMessageRepository chatMessageRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;
    private final PlanLimitService planLimitService;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       CaseFileRepository caseFileRepository,
                       CaseAnalysisRepository caseAnalysisRepository,
                       WorkspaceMemberRepository workspaceMemberRepository,
                       CurrentUserResolver currentUserResolver,
                       AnthropicService anthropicService,
                       UsageEventService usageEventService,
                       PlanLimitService planLimitService) {
        this.chatMessageRepository = chatMessageRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
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

        String userMessage = "Dossier :\n" + caseAnalysis.getAnalysisResult()
                + "\n\nQuestion : " + request.question();

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
}
