package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ReAnalysisCommandService {

    private final CaseFileRepository caseFileRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RabbitTemplate rabbitTemplate;

    public ReAnalysisCommandService(CaseFileRepository caseFileRepository,
                                    AnalysisJobRepository analysisJobRepository,
                                    AuthAccountRepository authAccountRepository,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    RabbitTemplate rabbitTemplate) {
        this.caseFileRepository = caseFileRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.authAccountRepository = authAccountRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void triggerReAnalysis(UUID caseFileId, OidcUser oidcUser, String provider) {
        User user = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUser();

        Workspace workspace = workspaceMemberRepository
                .findFirstByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findById(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
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
