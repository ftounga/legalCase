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
import java.util.UUID;

@Service
public class ReAnalysisCommandService {

    private final CaseFileRepository caseFileRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PlanLimitService planLimitService;

    public ReAnalysisCommandService(CaseFileRepository caseFileRepository,
                                    AnalysisJobRepository analysisJobRepository,
                                    CurrentUserResolver currentUserResolver,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    RabbitTemplate rabbitTemplate,
                                    PlanLimitService planLimitService) {
        this.caseFileRepository = caseFileRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.planLimitService = planLimitService;
    }

    @Transactional
    public void triggerReAnalysis(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
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

        if (!planLimitService.isEnrichedAnalysisAllowedForWorkspace(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "La re-analyse enrichie est réservée au plan Pro.");
        }

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RE_ANALYSIS_EXCHANGE,
                RabbitMQConfig.RE_ANALYSIS_ROUTING_KEY,
                new ReAnalysisMessage(caseFileId));
    }
}
