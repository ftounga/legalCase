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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * F-147 SF-147-02 : force FAILED sur tous les jobs PENDING/PROCESSING d'un
     * case file. Utilisé pour débloquer un dossier coincé après un incident IA
     * qui n'aurait pas été proprement finalizé (ex: 400/429/5xx Anthropic avec
     * ancien code pré-SF-147-01). Retourne le nombre de jobs modifiés.
     */
    @Transactional
    public int resetCaseFilePipeline(OidcUser oidcUser, String provider, UUID caseFileId) {
        assertSuperAdmin(oidcUser, provider);
        int updated = analysisJobRepository.forceFailActiveJobsForCaseFile(
                caseFileId, "Super-admin force reset (F-147-02)", Instant.now());
        log.warn("Super-admin reset pipeline for case file {} — {} job(s) forced to FAILED",
                caseFileId, updated);
        return updated;
    }

    /** Resolves the caller and throws 403 if not super-admin. Reusable by controller-level guards. */
    @Transactional(readOnly = true)
    public User assertSuperAdmin(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();
        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Page<SuperAdminWorkspaceResponse> listAllWorkspaces(OidcUser oidcUser, String provider, Pageable pageable) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        return workspaceRepository.findAll(pageable).map(this::toResponse);
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

    @Transactional(readOnly = true)
    public Page<SuperAdminUserResponse> listAllUsers(OidcUser oidcUser, String provider, Pageable pageable) {
        User caller = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!caller.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        return userRepository.findAll(pageable).map(u -> new SuperAdminUserResponse(
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                workspaceMemberRepository.findByUser(u).size()));
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

    @Transactional(readOnly = true)
    public SuperAdminMetricsResponse getMetrics(OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        if (!user.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super-admin access required");
        }

        Instant now = Instant.now();
        Instant since30d = now.minus(30, ChronoUnit.DAYS);
        Instant since7d = now.minus(7, ChronoUnit.DAYS);

        long totalWorkspaces = workspaceRepository.count();
        long newWorkspacesLast30Days = workspaceRepository.countByCreatedAtAfter(since30d);

        long trialWorkspaces = workspaceRepository.countByPlanCode("FREE");
        long paidWorkspaces = totalWorkspaces - trialWorkspaces;
        double conversionRatePct = totalWorkspaces == 0 ? 0.0 : paidWorkspaces * 100.0 / totalWorkspaces;

        long analysesLast7Days = caseAnalysisRepository.countDoneCreatedAfter(since7d);
        long analysesLast30Days = caseAnalysisRepository.countDoneCreatedAfter(since30d);

        Set<UUID> activeWorkspaceIds = caseAnalysisRepository.findDistinctWorkspaceIdsWithDoneAnalysisSince(since30d);
        long activeWorkspaces30d = activeWorkspaceIds.size();
        long inactiveWorkspaces30d = totalWorkspaces - activeWorkspaces30d;

        return new SuperAdminMetricsResponse(
                totalWorkspaces,
                activeWorkspaces30d,
                inactiveWorkspaces30d,
                trialWorkspaces,
                paidWorkspaces,
                conversionRatePct,
                analysesLast7Days,
                analysesLast30Days,
                newWorkspacesLast30Days
        );
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
