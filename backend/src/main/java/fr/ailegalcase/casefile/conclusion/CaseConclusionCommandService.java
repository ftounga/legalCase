package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.ProcedureStageCatalog;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.EnumSet;
import java.util.UUID;

/**
 * F-98 / SF-98-01 — déclenchement de la génération d'un projet de conclusions.
 *
 * <p>Valide les 4 gardes 409, crée (ou réutilise) la ligne {@code case_conclusions}
 * au statut {@code PENDING} et publie le message RabbitMQ pour le worker
 * {@link CaseConclusionService}.</p>
 */
@Service
public class CaseConclusionCommandService {

    private static final Logger log = LoggerFactory.getLogger(CaseConclusionCommandService.class);

    /** Combinaison procédurale couverte par la V1 (une seule cellule de la matrice F-98). */
    private static final String SUPPORTED_DOMAIN = ProcedureStageCatalog.DROIT_DU_TRAVAIL;
    private static final String SUPPORTED_COUNTRY = ProcedureStageCatalog.FRANCE;
    private static final String SUPPORTED_JURISDICTION = "CPH";
    private static final String SUPPORTED_STAGE = "FOND";
    private static final String SUPPORTED_POSITION = "DEMANDEUR";

    private final CaseFileRepository caseFileRepository;
    private final CaseConclusionRepository caseConclusionRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RabbitTemplate rabbitTemplate;

    public CaseConclusionCommandService(CaseFileRepository caseFileRepository,
                                        CaseConclusionRepository caseConclusionRepository,
                                        CaseAnalysisRepository caseAnalysisRepository,
                                        CurrentUserResolver currentUserResolver,
                                        WorkspaceMemberRepository workspaceMemberRepository,
                                        RabbitTemplate rabbitTemplate) {
        this.caseFileRepository = caseFileRepository;
        this.caseConclusionRepository = caseConclusionRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Déclenche la génération asynchrone du projet de conclusions d'un dossier.
     *
     * @return la réponse {@code 202} {@code {"status":"PENDING"}}
     * @throws ResponseStatusException        {@code 404} si le dossier est inconnu
     *                                        ou appartient à un autre workspace
     * @throws CaseConclusionGuardException   {@code 409} si une garde échoue
     */
    @Transactional
    public ConclusionGenerationResponse triggerGeneration(UUID caseFileId, OidcUser oidcUser,
                                                          String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = resolvePrimaryWorkspace(user);
        CaseFile caseFile = resolveCaseFileInWorkspace(caseFileId, workspace);

        // Garde 1 — stade procédural complet (juridiction + stade + position).
        String jurisdiction = caseFile.getProcedureJurisdiction();
        String stage = caseFile.getProcedureStage();
        String position = caseFile.getProcedurePosition();
        if (jurisdiction == null || stage == null || position == null) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.STAGE_NOT_SET);
        }

        // Garde 2 — combinaison couverte par la V1.
        boolean supported = SUPPORTED_DOMAIN.equals(caseFile.getLegalDomain())
                && SUPPORTED_COUNTRY.equals(workspace.getCountry())
                && SUPPORTED_JURISDICTION.equals(jurisdiction)
                && SUPPORTED_STAGE.equals(stage)
                && SUPPORTED_POSITION.equals(position);
        if (!supported) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.COMBINATION_NOT_SUPPORTED);
        }

        // Garde 3 — au moins une analyse de dossier terminée.
        boolean analysisReady = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .isPresent();
        if (!analysisReady) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.ANALYSIS_NOT_READY);
        }

        // Garde 4 — pas de génération déjà en cours.
        CaseConclusion conclusion = caseConclusionRepository.findByCaseFileId(caseFileId).orElse(null);
        if (conclusion != null && EnumSet.of(CaseConclusionStatus.PENDING, CaseConclusionStatus.PROCESSING)
                .contains(conclusion.getStatus())) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.ALREADY_GENERATING);
        }

        // Création (ou réutilisation 1:1) de la ligne — statut PENDING, snapshot du stade.
        if (conclusion == null) {
            conclusion = new CaseConclusion();
            conclusion.setCaseFile(caseFile);
            conclusion.setWorkspace(workspace);
        }
        conclusion.setStatus(CaseConclusionStatus.PENDING);
        conclusion.setJurisdictionCode(jurisdiction);
        conclusion.setStageCode(stage);
        conclusion.setPositionCode(position);
        // Régénération : on purge le résultat précédent (écrasement, cf. mini-spec).
        conclusion.setContent(null);
        conclusion.setErrorMessage(null);
        conclusion.setGeneratedAt(null);
        conclusion.setModelUsed(null);
        conclusion.setPromptTokens(null);
        conclusion.setCompletionTokens(null);
        conclusion = caseConclusionRepository.save(conclusion);

        UUID conclusionId = conclusion.getId();
        rabbitTemplate.convertAndSend(
                CaseConclusionRabbitMQConfig.CASE_CONCLUSION_EXCHANGE,
                CaseConclusionRabbitMQConfig.CASE_CONCLUSION_ROUTING_KEY,
                new CaseConclusionMessage(conclusionId));
        log.info("Conclusion generation triggered — caseFile={}, conclusion={}", caseFileId, conclusionId);

        return ConclusionGenerationResponse.pending();
    }

    /**
     * Lit l'état courant des conclusions d'un dossier.
     *
     * @return {@code NOT_GENERATED} si aucune ligne, sinon l'état persisté
     * @throws ResponseStatusException {@code 404} si le dossier est inconnu
     *                                 ou appartient à un autre workspace
     */
    @Transactional(readOnly = true)
    public ConclusionResponse getConclusion(UUID caseFileId, OidcUser oidcUser,
                                            String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = resolvePrimaryWorkspace(user);
        CaseFile caseFile = resolveCaseFileInWorkspace(caseFileId, workspace);

        return caseConclusionRepository.findByCaseFileId(caseFileId)
                .map(c -> ConclusionResponse.fromSafe(c, caseFile, workspace.getCountry()))
                .orElseGet(() -> ConclusionResponse.notGenerated(caseFileId));
    }

    private Workspace resolvePrimaryWorkspace(User user) {
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    /** Charge le dossier et vérifie l'isolation workspace — 404 (pas de fuite d'existence). */
    private CaseFile resolveCaseFileInWorkspace(UUID caseFileId, Workspace workspace) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }
}
