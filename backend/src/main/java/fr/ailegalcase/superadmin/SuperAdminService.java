package fr.ailegalcase.superadmin;

import fr.ailegalcase.analysis.AiQuestionAnswerRepository;
import fr.ailegalcase.analysis.AiQuestionRepository;
import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.ChunkAnalysisRepository;
import fr.ailegalcase.analysis.DocumentAnalysisRepository;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.StripeCustomerService;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceInvitationRepository;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminService.class);

    private final AuthAccountRepository authAccountRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeCustomerService stripeCustomerService;
    private final CaseFileRepository caseFileRepository;
    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ChunkAnalysisRepository chunkAnalysisRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final UsageEventRepository usageEventRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final AnalysisJobRepository analysisJobRepository;

    public SuperAdminService(AuthAccountRepository authAccountRepository,
                             UserRepository userRepository,
                             WorkspaceRepository workspaceRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             WorkspaceInvitationRepository workspaceInvitationRepository,
                             SubscriptionRepository subscriptionRepository,
                             StripeCustomerService stripeCustomerService,
                             CaseFileRepository caseFileRepository,
                             DocumentRepository documentRepository,
                             DocumentExtractionRepository documentExtractionRepository,
                             DocumentChunkRepository documentChunkRepository,
                             ChunkAnalysisRepository chunkAnalysisRepository,
                             DocumentAnalysisRepository documentAnalysisRepository,
                             UsageEventRepository usageEventRepository,
                             AiQuestionRepository aiQuestionRepository,
                             AiQuestionAnswerRepository aiQuestionAnswerRepository,
                             CaseAnalysisRepository caseAnalysisRepository,
                             AnalysisJobRepository analysisJobRepository) {
        this.authAccountRepository = authAccountRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceInvitationRepository = workspaceInvitationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeCustomerService = stripeCustomerService;
        this.caseFileRepository = caseFileRepository;
        this.documentRepository = documentRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.usageEventRepository = usageEventRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.analysisJobRepository = analysisJobRepository;
    }

    @Transactional(readOnly = true)
    public List<SuperAdminWorkspaceResponse> listAllWorkspaces(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        return workspaceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SuperAdminUsageResponse> getUsageByWorkspace(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        record UsageRow(long tokensInput, long tokensOutput, BigDecimal cost) {}

        Map<UUID, UsageRow> usageByWorkspace = usageEventRepository.aggregateByWorkspaceId().stream()
                .collect(Collectors.toMap(
                        row -> toUUID(row[0]),
                        row -> new UsageRow(
                                ((Number) row[1]).longValue(),
                                ((Number) row[2]).longValue(),
                                new BigDecimal(row[3].toString())
                        )
                ));

        return workspaceRepository.findAll().stream()
                .map(ws -> {
                    UsageRow row = usageByWorkspace.getOrDefault(ws.getId(),
                            new UsageRow(0, 0, BigDecimal.ZERO));
                    return new SuperAdminUsageResponse(ws.getId(), ws.getName(),
                            row.tokensInput(), row.tokensOutput(), row.cost());
                })
                .toList();
    }

    @Transactional
    public void deleteWorkspace(OidcUser oidcUser, String provider, UUID workspaceId) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        List<UUID> caseFileIds = caseFileRepository.findByWorkspace_Id(workspaceId)
                .stream().map(CaseFile::getId).toList();

        if (!caseFileIds.isEmpty()) {
            usageEventRepository.deleteByCaseFileIdIn(caseFileIds);

            List<UUID> questionIds = aiQuestionRepository.findByCaseFileIdIn(caseFileIds)
                    .stream().map(fr.ailegalcase.analysis.AiQuestion::getId).toList();
            if (!questionIds.isEmpty()) {
                aiQuestionAnswerRepository.deleteByAiQuestionIdIn(questionIds);
                aiQuestionRepository.deleteByCaseFileIdIn(caseFileIds);
            }

            caseAnalysisRepository.deleteByCaseFileIdIn(caseFileIds);
            analysisJobRepository.deleteByCaseFileIdIn(caseFileIds);

            List<UUID> docIds = documentRepository.findByCaseFileIdIn(caseFileIds)
                    .stream().map(fr.ailegalcase.document.Document::getId).toList();

            if (!docIds.isEmpty()) {
                List<UUID> extractionIds = documentExtractionRepository.findByDocumentIdIn(docIds)
                        .stream().map(fr.ailegalcase.document.DocumentExtraction::getId).toList();

                if (!extractionIds.isEmpty()) {
                    List<UUID> chunkIds = documentChunkRepository.findByExtractionIdIn(extractionIds)
                            .stream().map(fr.ailegalcase.document.DocumentChunk::getId).toList();
                    if (!chunkIds.isEmpty()) {
                        chunkAnalysisRepository.deleteByChunkIdIn(chunkIds);
                        documentChunkRepository.deleteByExtractionIdIn(extractionIds);
                    }
                    documentAnalysisRepository.deleteByExtractionIdIn(extractionIds);
                    documentExtractionRepository.deleteByDocumentIdIn(docIds);
                }
                documentRepository.deleteByCaseFileIdIn(caseFileIds);
            }

            caseFileRepository.deleteAllById(caseFileIds);
        }

        workspaceInvitationRepository.deleteByWorkspaceId(workspaceId);
        workspaceMemberRepository.deleteAll(workspaceMemberRepository.findByWorkspace_Id(workspaceId));

        subscriptionRepository.findByWorkspaceId(workspaceId).ifPresent(sub -> {
            if (sub.getStripeSubscriptionId() != null) {
                try {
                    stripeCustomerService.cancelSubscription(sub.getStripeSubscriptionId());
                } catch (Exception e) {
                    log.warn("Stripe cancellation failed for workspace {} — continuing deletion: {}", workspaceId, e.getMessage());
                }
            }
            subscriptionRepository.delete(sub);
        });

        workspaceRepository.delete(workspace);
    }

    @Transactional
    public void deleteUser(OidcUser oidcUser, String provider, UUID userId) {
        User caller = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!caller.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUser(target);

        for (WorkspaceMember membership : memberships) {
            if ("OWNER".equals(membership.getMemberRole())) {
                UUID workspaceId = membership.getWorkspace().getId();
                long ownerCount = workspaceMemberRepository.findByWorkspace_Id(workspaceId).stream()
                        .filter(m -> "OWNER".equals(m.getMemberRole()))
                        .count();
                if (ownerCount == 1) {
                    deleteWorkspace(oidcUser, provider, workspaceId);
                    continue;
                }
            }
            workspaceMemberRepository.delete(membership);
        }

        workspaceInvitationRepository.deleteByInvitedByUserId(userId);
        authAccountRepository.deleteByUser(target);
        userRepository.delete(target);
    }

    private static UUID toUUID(Object obj) {
        if (obj instanceof UUID u) return u;
        if (obj instanceof byte[] bytes) {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            return new UUID(bb.getLong(), bb.getLong());
        }
        return UUID.fromString(obj.toString());
    }

    private SuperAdminWorkspaceResponse toResponse(Workspace ws) {
        Instant expiresAt = subscriptionRepository.findByWorkspaceId(ws.getId())
                .map(Subscription::getExpiresAt)
                .orElse(null);
        long memberCount = workspaceMemberRepository.findByWorkspace_Id(ws.getId()).size();
        return new SuperAdminWorkspaceResponse(
                ws.getId(), ws.getName(), ws.getSlug(),
                ws.getPlanCode(), ws.getStatus(), expiresAt,
                memberCount, ws.getCreatedAt()
        );
    }
}
