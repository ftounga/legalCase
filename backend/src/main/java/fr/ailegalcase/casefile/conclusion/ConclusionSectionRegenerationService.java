package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-265 / SF-265-01 — Co-rédaction au paragraphe : régénération <strong>synchrone</strong>
 * d'une seule section d'une version de conclusions selon une instruction libre de l'avocat.
 *
 * <p>Réutilise les gardes rédactionnelles de la génération complète (F-98) via le
 * {@link CaseConclusionPromptBuilder#buildSystemPrompt(CombinationKey, List)} (anti-jargon,
 * chiffres tracés aux outils, jurisprudence vérifiée, style cabinet). Le grounding est
 * l'<strong>acte courant déjà généré</strong> (le {@code content} de la version), qui
 * intègre déjà faits/pièces/chiffres/jurisprudence — l'instruction oriente le style/l'angle
 * de la section, jamais les faits ni les montants.</p>
 *
 * <p><strong>Aucune persistance</strong> : la réponse rend le markdown régénéré, le frontend
 * l'insère dans l'éditeur (round-trip markdown F-264), l'avocat relit puis enregistre via
 * l'endpoint {@code PATCH .../content} existant. Stockage, versions, export et cycle de vie
 * restent intacts.</p>
 */
@Service
public class ConclusionSectionRegenerationService {

    private static final Logger log = LoggerFactory.getLogger(ConclusionSectionRegenerationService.class);

    /** Budget de tokens de sortie — une section ≈ court (cf. mini-spec SF-265-01). */
    static final int MAX_TOKENS = 2000;

    /** Borne anti-abus sur l'instruction libre. */
    static final int MAX_INSTRUCTION_LENGTH = 2000;

    private final CaseFileRepository caseFileRepository;
    private final CaseConclusionRepository caseConclusionRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CaseConclusionPromptBuilder promptBuilder;
    private final AnthropicService anthropicService;

    public ConclusionSectionRegenerationService(CaseFileRepository caseFileRepository,
                                                CaseConclusionRepository caseConclusionRepository,
                                                CurrentUserResolver currentUserResolver,
                                                WorkspaceMemberRepository workspaceMemberRepository,
                                                CaseConclusionPromptBuilder promptBuilder,
                                                AnthropicService anthropicService) {
        this.caseFileRepository = caseFileRepository;
        this.caseConclusionRepository = caseConclusionRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.promptBuilder = promptBuilder;
        this.anthropicService = anthropicService;
    }

    /**
     * Régénère le markdown d'une section d'une version de conclusions.
     *
     * @throws ResponseStatusException      {@code 400} si {@code sectionMarkdown}/{@code instruction}
     *                                      vide ou {@code instruction} trop longue ; {@code 404} si
     *                                      dossier/version inconnu ou autre workspace ; {@code 502}
     *                                      si la réponse IA est vide
     * @throws CaseConclusionGuardException {@code 409} si la version n'est pas {@code DONE}
     *                                      ({@code CONTENT_REQUIRES_DONE}) ou non {@code DRAFT}
     *                                      ({@code CONTENT_NOT_EDITABLE})
     */
    @Transactional(readOnly = true)
    public SectionRegenerationResponse regenerateSection(UUID caseFileId, UUID versionId,
                                                         String sectionMarkdown, String instruction,
                                                         OidcUser oidcUser, String provider,
                                                         Principal principal) {
        // Garde 400 — entrées requises.
        if (sectionMarkdown == null || sectionMarkdown.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ sectionMarkdown est requis.");
        }
        if (instruction == null || instruction.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ instruction est requis.");
        }
        if (instruction.length() > MAX_INSTRUCTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'instruction est trop longue (max " + MAX_INSTRUCTION_LENGTH + " caractères).");
        }

        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = resolvePrimaryWorkspace(user);
        CaseFile caseFile = resolveCaseFileInWorkspace(caseFileId, workspace);
        CaseConclusion version = resolveVersion(caseFileId, versionId);

        // Gardes 409 — symétrie stricte avec updateContent : DONE + DRAFT only.
        if (version.getStatus() != CaseConclusionStatus.DONE) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.CONTENT_REQUIRES_DONE);
        }
        if (version.getLifecycleStatus() != ConclusionLifecycleStatus.DRAFT) {
            throw new CaseConclusionGuardException(CaseConclusionGuardCode.CONTENT_NOT_EDITABLE);
        }

        // System prompt = mêmes gardes rédactionnelles que la génération complète.
        CombinationKey key = new CombinationKey(caseFile.getLegalDomain(), workspace.getCountry(),
                version.getJurisdictionCode(), version.getStageCode(), version.getPositionCode());
        String systemPrompt = promptBuilder.buildSystemPrompt(key, List.of());

        String userMessage = buildSectionUserMessage(version.getContent(), sectionMarkdown, instruction);

        // Gate coût — user-level : gate token user + record usage_events (workspace/user/case non null).
        AiCallContext ctx = AiCallContext.userLevel(
                workspace.getId(), user.getId(), caseFileId, JobType.CONCLUSION_SECTION_REGEN);

        AnthropicResult result = anthropicService.analyze(ctx, systemPrompt, userMessage, MAX_TOKENS);
        if (result.content() == null || result.content().isBlank()) {
            log.warn("Section regeneration returned empty content — caseFile={}, version={}",
                    caseFileId, versionId);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La régénération de la section n'a produit aucun résultat. Réessayez.");
        }

        log.info("Conclusion section regenerated — caseFile={}, version={}, tokens {}/{}",
                caseFileId, versionId, result.promptTokens(), result.completionTokens());
        return new SectionRegenerationResponse(result.content().trim());
    }

    /**
     * Construit le user message scopé : acte courant en grounding + section à régénérer +
     * instruction, avec consigne stricte « réponds uniquement le markdown de la section ».
     */
    private String buildSectionUserMessage(String currentContent, String sectionMarkdown,
                                           String instruction) {
        String acte = currentContent != null ? currentContent : "";
        return """
                Tu régénères UNE SEULE section d'un acte de conclusions déjà rédigé.

                Respecte STRICTEMENT toutes les consignes du prompt système (chiffres tracés \
                aux outils du dossier, jurisprudence vérifiée, aucun jargon interne, markdown \
                valide). N'invente aucun fait ni aucun montant : l'instruction oriente le style \
                et l'angle de la section, jamais les faits ni les chiffres.

                Ne réécris PAS le reste de l'acte. Ne réintroduis PAS le bordereau de pièces ni \
                de section globale de jurisprudence.

                === ACTE COMPLET EN COURS (contexte, ne pas réécrire) ===
                %s

                === SECTION À RÉGÉNÉRER ===
                %s

                === INSTRUCTION DE L'AVOCAT ===
                %s

                Réponds UNIQUEMENT le markdown régénéré de cette section, sans préambule, sans \
                commentaire, sans bloc de code englobant.""".formatted(acte, sectionMarkdown, instruction);
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

    /** Charge une version par id en exigeant son rattachement au dossier — 404 sinon. */
    private CaseConclusion resolveVersion(UUID caseFileId, UUID versionId) {
        return caseConclusionRepository.findByIdAndCaseFileId(versionId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Conclusion version not found"));
    }
}
