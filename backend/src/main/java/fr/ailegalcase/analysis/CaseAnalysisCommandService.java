package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public class CaseAnalysisCommandService {

    private final CaseFileRepository caseFileRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PlanLimitService planLimitService;

    public CaseAnalysisCommandService(CaseFileRepository caseFileRepository,
                                      CaseAnalysisRepository caseAnalysisRepository,
                                      DocumentAnalysisRepository documentAnalysisRepository,
                                      AnalysisJobRepository analysisJobRepository,
                                      CurrentUserResolver currentUserResolver,
                                      WorkspaceMemberRepository workspaceMemberRepository,
                                      RabbitTemplate rabbitTemplate,
                                      PlanLimitService planLimitService) {
        this.caseFileRepository = caseFileRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.planLimitService = planLimitService;
    }

    @Transactional
    public void triggerCaseAnalysis(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findById(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        boolean alreadyRunning = caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(
                caseFileId, List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING));
        if (alreadyRunning) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une analyse est déjà en cours pour ce dossier.");
        }

        long doneDocs = documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(
                caseFileId, AnalysisStatus.DONE);
        if (doneDocs == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Aucun document analysé disponible pour ce dossier.");
        }

        if (planLimitService.isCaseAnalysisLimitReached(caseFileId, workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Limite d'analyses atteinte pour ce dossier.");
        }

        if (planLimitService.isMonthlyTokenBudgetExceeded(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Budget tokens mensuel dépassé.");
        }

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CASE_ANALYSIS_EXCHANGE,
                RabbitMQConfig.CASE_ANALYSIS_ROUTING_KEY,
                new CaseAnalysisMessage(caseFileId));
    }
}
