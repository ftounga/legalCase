package fr.ailegalcase.casefile.strategy;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.StrategicOption;
import fr.ailegalcase.analysis.StrategicOptionRepository;
import fr.ailegalcase.analysis.StrategicOptionStatus;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * F-286 / SF-286-01 — génération et lecture de la stratégie de dossier unifiée.
 *
 * <p><b>Couche LLM de synthèse en LECTURE</b> (décision PO 2026-06-12). Le service lit
 * les <b>verdicts des outils CALCULÉS</b> du dossier (via
 * {@link CaseFileDashboardService#assembleDecisionToolTiles} — exactement la même source
 * que le générateur de conclusions F-98, qui ne retourne QUE les outils calculés) et la
 * <b>synthèse</b> {@code DONE}, puis demande au modèle une recommandation stratégique
 * consolidée. Le résultat est persisté dans {@code case_strategy} (1 ligne courante par
 * dossier, upsert).</p>
 *
 * <p><b>Invariant « 1 outil = 1 situation » garanti par construction</b> : ce service
 * n'injecte AUCUN repository {@code *Analysis} en écriture, et n'écrit jamais sur les
 * tables d'outils ni sur {@code decision_tool_visibility_rules}. Il n'observe les outils
 * qu'en lecture agrégée. La stratégie ne mute, ne réordonne ni ne masque aucun outil.</p>
 *
 * <p>Isolation workspace via le dossier (mirror {@code ContradictoireService}).</p>
 */
@Service
public class CaseStrategyService {

    private static final Logger log = LoggerFactory.getLogger(CaseStrategyService.class);

    /** Plafond de tokens de sortie — la reco est volontairement concise (4 sections). */
    private static final int MAX_TOKENS = 2048;

    private final CaseStrategyRepository strategyRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final CaseFileDashboardService dashboardService;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final StrategicOptionRepository strategicOptionRepository;
    private final CaseStrategyPromptBuilder promptBuilder;
    private final AnthropicService anthropicService;

    public CaseStrategyService(CaseStrategyRepository strategyRepository,
                               CaseFileRepository caseFileRepository,
                               WorkspaceMemberRepository workspaceMemberRepository,
                               CurrentUserResolver currentUserResolver,
                               CaseFileDashboardService dashboardService,
                               CaseAnalysisRepository caseAnalysisRepository,
                               StrategicOptionRepository strategicOptionRepository,
                               CaseStrategyPromptBuilder promptBuilder,
                               AnthropicService anthropicService) {
        this.strategyRepository = strategyRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.dashboardService = dashboardService;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.strategicOptionRepository = strategicOptionRepository;
        this.promptBuilder = promptBuilder;
        this.anthropicService = anthropicService;
    }

    /** Lecture de la stratégie courante du dossier (jamais de génération). */
    @Transactional(readOnly = true)
    public CaseStrategyResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        return strategyRepository.findByCaseFileId(caseFile.getId())
                .map(CaseStrategyResponse::from)
                .orElseGet(CaseStrategyResponse::empty);
    }

    /**
     * Génère (ou régénère) la stratégie du dossier. Appel LLM <b>synchrone</b> borné
     * (1 appel, input plafonné), {@code temperature=0} (déterminisme, câblé dans
     * {@link AnthropicService}). Si aucun outil n'est calculé, retourne
     * {@link CaseStrategyResponse#emptyInput()} <b>sans appeler le modèle</b> — la
     * stratégie n'invente rien (reprise du principe F-258).
     */
    @Transactional
    public CaseStrategyResponse generate(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        List<DashboardTile> tiles = loadCalculatedToolTiles(caseFile.getId());
        if (tiles.isEmpty()) {
            // Honnêteté du vide : aucune donnée calculée → aucune stratégie inventée.
            log.info("F-286 — génération stratégie ignorée (0 outil calculé) — caseFile={}", caseFile.getId());
            return CaseStrategyResponse.emptyInput();
        }

        String synthesisJson = loadLatestSynthesis(caseFile.getId());
        List<String> retained = loadRetainedStrategies(caseFile.getId());

        String userMessage = promptBuilder.buildUserMessage(
                caseFile.getTitle(), tiles, synthesisJson, retained);

        // Gate F-257 : system-level (skip gate token user, record obligatoire userId=null),
        // rattaché au dossier pour la traçabilité du coût Anthropic.
        AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_CASE_STRATEGY, caseFile.getId());
        AnthropicResult result = anthropicService.analyze(
                ctx, CaseStrategyPromptBuilder.SYSTEM_PROMPT, userMessage, MAX_TOKENS);

        CaseStrategy strategy = upsert(caseFile, result, tiles.size());
        log.info("F-286 — stratégie générée — caseFile={}, outils={}, tokens {}/{}",
                caseFile.getId(), tiles.size(), result.promptTokens(), result.completionTokens());
        return CaseStrategyResponse.from(strategy);
    }

    // ── Persistance ──────────────────────────────────────────────────────────

    private CaseStrategy upsert(CaseFile caseFile, AnthropicResult result, int toolsConsidered) {
        CaseStrategy strategy = strategyRepository.findByCaseFileId(caseFile.getId())
                .orElseGet(CaseStrategy::new);
        strategy.setCaseFile(caseFile);
        strategy.setContent(result.content());
        strategy.setToolsConsidered(toolsConsidered);
        strategy.setModelUsed(result.modelUsed());
        strategy.setPromptTokens(result.promptTokens());
        strategy.setCompletionTokens(result.completionTokens());
        strategy.setGeneratedAt(Instant.now());
        return strategyRepository.save(strategy);
    }

    // ── Lecture des intrants (jamais d'écriture sur les outils) ────────────────

    /**
     * Verdicts des outils <b>CALCULÉS</b> du dossier. Réutilise l'agrégation générique
     * déjà exposée (F-98 SF-98-01) qui ne retourne QUE les outils dont une {@code *Analysis}
     * est persistée — les tuiles pré-remplies non cliquées n'y figurent pas (semantique F-258).
     * <strong>Fail-open</strong> : en cas d'échec d'agrégation, liste vide (→ EMPTY_INPUT).
     */
    private List<DashboardTile> loadCalculatedToolTiles(UUID caseFileId) {
        try {
            return dashboardService.assembleDecisionToolTiles(caseFileId);
        } catch (RuntimeException ex) {
            log.warn("F-286 — agrégation des outils décisionnels indisponible pour caseFile {} : {}",
                    caseFileId, ex.getMessage());
            return List.of();
        }
    }

    /** JSON brut de la synthèse {@code DONE} la plus récente, ou {@code null}. */
    private String loadLatestSynthesis(UUID caseFileId) {
        try {
            return caseAnalysisRepository
                    .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                    .map(CaseAnalysis::getAnalysisResult)
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("F-286 — lecture de la synthèse indisponible pour caseFile {} : {}",
                    caseFileId, ex.getMessage());
            return null;
        }
    }

    /** Pistes stratégiques {@code RETAINED} de la synthèse la plus récente (texte). */
    private List<String> loadRetainedStrategies(UUID caseFileId) {
        try {
            CaseAnalysis analysis = caseAnalysisRepository
                    .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                    .orElse(null);
            if (analysis == null) {
                return List.of();
            }
            List<String> retained = new ArrayList<>();
            for (StrategicOption option : strategicOptionRepository
                    .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(analysis.getId(), StrategicOptionStatus.RETAINED)) {
                String texte = option.getTexte();
                if (texte != null && !texte.isBlank()) {
                    retained.add(option.getBaseJuridique() != null && !option.getBaseJuridique().isBlank()
                            ? texte.trim() + " [Base juridique : " + option.getBaseJuridique().trim() + "]"
                            : texte.trim());
                }
            }
            return retained;
        } catch (RuntimeException ex) {
            log.warn("F-286 — lecture des pistes stratégiques indisponible pour caseFile {} : {}",
                    caseFileId, ex.getMessage());
            return List.of();
        }
    }

    // ── Isolation workspace (mirror ContradictoireService) ─────────────────────

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }
}
