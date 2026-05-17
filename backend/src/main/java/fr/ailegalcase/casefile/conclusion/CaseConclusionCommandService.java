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
import java.util.List;
import java.util.UUID;

/**
 * F-98 / SF-98-01 + SF-98-52 — déclenchement de la génération de conclusions et
 * gestion des versions.
 *
 * <p>SF-98-52 : {@code generate} crée une <strong>nouvelle version</strong>
 * (relation 1:N) au lieu d'écraser la ligne existante. Le service expose en plus
 * la liste des versions, le détail d'une version et la mutation de son cycle de vie.</p>
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

    /** Statuts de génération en cours — bloquent un nouveau déclenchement (garde ALREADY_GENERATING). */
    private static final EnumSet<CaseConclusionStatus> IN_PROGRESS_STATUSES =
            EnumSet.of(CaseConclusionStatus.PENDING, CaseConclusionStatus.PROCESSING);

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
     * Déclenche la génération asynchrone d'une <strong>nouvelle version</strong> de
     * conclusions pour un dossier (SF-98-52).
     *
     * <p>La nouvelle version porte {@code version_number = max(dossier) + 1},
     * {@code status = PENDING}, {@code lifecycle_status = DRAFT}. Les versions
     * précédentes ne sont pas touchées.</p>
     *
     * @return la réponse {@code 202} {@code {"status":"PENDING","versionNumber":N}}
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

        // Garde 4 — pas de génération déjà en cours sur une version quelconque du dossier.
        if (caseConclusionRepository.existsByCaseFileIdAndStatusIn(caseFileId, IN_PROGRESS_STATUSES)) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.ALREADY_GENERATING);
        }

        // SF-98-52 — création d'une NOUVELLE version (version_number = max + 1).
        int nextVersion = caseConclusionRepository
                .findFirstByCaseFileIdOrderByVersionNumberDesc(caseFileId)
                .map(c -> c.getVersionNumber() + 1)
                .orElse(1);

        CaseConclusion conclusion = new CaseConclusion();
        conclusion.setCaseFile(caseFile);
        conclusion.setWorkspace(workspace);
        conclusion.setVersionNumber(nextVersion);
        conclusion.setStatus(CaseConclusionStatus.PENDING);
        conclusion.setLifecycleStatus(ConclusionLifecycleStatus.DRAFT);
        conclusion.setJurisdictionCode(jurisdiction);
        conclusion.setStageCode(stage);
        conclusion.setPositionCode(position);
        conclusion = caseConclusionRepository.save(conclusion);

        UUID conclusionId = conclusion.getId();
        rabbitTemplate.convertAndSend(
                CaseConclusionRabbitMQConfig.CASE_CONCLUSION_EXCHANGE,
                CaseConclusionRabbitMQConfig.CASE_CONCLUSION_ROUTING_KEY,
                new CaseConclusionMessage(conclusionId));
        log.info("Conclusion generation triggered — caseFile={}, conclusion={}, version={}",
                caseFileId, conclusionId, nextVersion);

        return ConclusionGenerationResponse.pending(nextVersion);
    }

    /**
     * Lit la version la plus récente des conclusions d'un dossier.
     *
     * @return {@code NOT_GENERATED} si aucune version, sinon la version au
     *         {@code version_number} le plus élevé
     * @throws ResponseStatusException {@code 404} si le dossier est inconnu
     *                                 ou appartient à un autre workspace
     */
    @Transactional(readOnly = true)
    public ConclusionResponse getConclusion(UUID caseFileId, OidcUser oidcUser,
                                            String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        CaseFile caseFile = resolveCaseFileInWorkspace(caseFileId, workspace);

        return caseConclusionRepository.findFirstByCaseFileIdOrderByVersionNumberDesc(caseFileId)
                .map(c -> ConclusionResponse.fromSafe(c, caseFile, workspace.getCountry()))
                .orElseGet(() -> ConclusionResponse.notGenerated(caseFileId));
    }

    /**
     * Liste les versions de conclusions d'un dossier, triées version décroissante.
     *
     * @throws ResponseStatusException {@code 404} si le dossier est inconnu
     *                                 ou appartient à un autre workspace
     */
    @Transactional(readOnly = true)
    public List<ConclusionVersionSummary> listVersions(UUID caseFileId, OidcUser oidcUser,
                                                       String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        resolveCaseFileInWorkspace(caseFileId, workspace);

        return caseConclusionRepository.findByCaseFileIdOrderByVersionNumberDesc(caseFileId)
                .stream()
                .map(ConclusionVersionSummary::from)
                .toList();
    }

    /**
     * Lit le détail d'une version donnée d'un dossier.
     *
     * @throws ResponseStatusException {@code 404} si le dossier ou la version est
     *                                 inconnu, ou appartient à un autre workspace
     */
    @Transactional(readOnly = true)
    public ConclusionResponse getVersion(UUID caseFileId, UUID versionId, OidcUser oidcUser,
                                         String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        CaseFile caseFile = resolveCaseFileInWorkspace(caseFileId, workspace);
        CaseConclusion version = resolveVersion(caseFileId, versionId);
        return ConclusionResponse.fromSafe(version, caseFile, workspace.getCountry());
    }

    /**
     * Fait évoluer le cycle de vie d'une version (SF-98-52).
     *
     * @throws ResponseStatusException      {@code 400} si {@code newLifecycle} n'est pas
     *                                      une valeur connue ; {@code 404} si dossier /
     *                                      version inconnu ou autre workspace
     * @throws CaseConclusionGuardException {@code 409} si on vise {@code VALIDATED}/
     *                                      {@code DEPOSITED} sur une version non {@code DONE}
     */
    @Transactional
    public ConclusionResponse updateLifecycle(UUID caseFileId, UUID versionId, String newLifecycle,
                                              OidcUser oidcUser, String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        CaseFile caseFile = resolveCaseFileInWorkspace(caseFileId, workspace);
        CaseConclusion version = resolveVersion(caseFileId, versionId);

        ConclusionLifecycleStatus target = parseLifecycle(newLifecycle);

        // Garde — VALIDATED/DEPOSITED exige une génération DONE.
        boolean requiresDone = target == ConclusionLifecycleStatus.VALIDATED
                || target == ConclusionLifecycleStatus.DEPOSITED;
        if (requiresDone && version.getStatus() != CaseConclusionStatus.DONE) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.LIFECYCLE_REQUIRES_DONE);
        }

        version.setLifecycleStatus(target);
        version = caseConclusionRepository.save(version);
        log.info("Conclusion lifecycle updated — conclusion={}, version={}, lifecycle={}",
                versionId, version.getVersionNumber(), target);
        return ConclusionResponse.fromSafe(version, caseFile, workspace.getCountry());
    }

    /** Convertit la valeur de cycle de vie reçue, ou {@code 400} si inconnue / nulle. */
    private static ConclusionLifecycleStatus parseLifecycle(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ lifecycleStatus est requis.");
        }
        try {
            return ConclusionLifecycleStatus.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valeur de cycle de vie inconnue : " + value);
        }
    }

    private Workspace resolveWorkspace(OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        return resolvePrimaryWorkspace(user);
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

    /**
     * Charge une version par id en exigeant son rattachement au dossier — 404 sinon.
     * Le dossier ayant déjà été contrôlé pour le workspace, l'isolation est garantie.
     */
    private CaseConclusion resolveVersion(UUID caseFileId, UUID versionId) {
        return caseConclusionRepository.findByIdAndCaseFileId(versionId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conclusion version not found"));
    }
}
