package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-98 / SF-98-01 + SF-98-52 — tests d'intégration de l'API du générateur de
 * conclusions versionné.
 *
 * <p>Couvre : déclenchement 202 versionné, les gardes 409, GET = version la plus
 * récente, liste des versions, détail d'une version, transitions de cycle de vie
 * (409 non-DONE / 400 valeur inconnue), isolation workspace 404, rejet 401.</p>
 *
 * <p>Le worker {@code CaseConclusionService} est {@code @Profile({"local","prod"})} :
 * en profil de test il n'est pas chargé, donc après un POST la version reste
 * {@code PENDING}. Les versions {@code DONE} sont posées directement en base.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CaseConclusionControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired CaseAnalysisRepository caseAnalysisRepository;
    @Autowired CaseConclusionRepository caseConclusionRepository;
    @MockBean AnthropicService anthropicService;
    @MockBean RabbitTemplate rabbitTemplate;

    private OAuth2AuthenticationToken authA;

    /** Dossier travail FR / CPH / FOND / DEMANDEUR avec analyse DONE — workspace A. */
    private CaseFile supportedCf;
    /** Dossier travail FR / CPH / FOND / DEFENDEUR avec analyse DONE — workspace A (SF-98-02). */
    private CaseFile supportedDefendeurCf;
    /** Dossier sans stade procédural — workspace A. */
    private CaseFile noStageCf;
    /** Dossier travail FR / CPH / FOND / DEMANDEUR sans analyse DONE — workspace A. */
    private CaseFile noAnalysisCf;
    /** Dossier dont la combinaison procédurale n'a aucune cellule F-98 — workspace A. */
    private CaseFile unsupportedCf;
    /** Dossier du workspace B (isolation). */
    private CaseFile otherWorkspaceCf;
    /** Workspace A pour poser des versions directement en base. */
    private Workspace wsA;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // ── Workspace A — FRANCE / DROIT_DU_TRAVAIL ──────────────────────────
        User uA = save(new User(), u -> { u.setEmail("ccl-a-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uA, "g-ccl-a-" + ts);
        wsA = saveWs(uA, "WSA " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uA, wsA);
        authA = buildAuth("g-ccl-a-" + ts, "ccl-a-" + ts + "@ex.com");

        supportedCf = saveCf(uA, wsA, "CF supported " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "FOND", "DEMANDEUR");
        saveDoneAnalysis(supportedCf);

        supportedDefendeurCf = saveCf(uA, wsA, "CF defendeur " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "FOND", "DEFENDEUR");
        saveDoneAnalysis(supportedDefendeurCf);

        noStageCf = saveCf(uA, wsA, "CF nostage " + ts, "DROIT_DU_TRAVAIL",
                null, null, null);
        saveDoneAnalysis(noStageCf);

        noAnalysisCf = saveCf(uA, wsA, "CF noanalysis " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "FOND", "DEMANDEUR");

        // Combinaison volontairement hors registre F-98 : stade synthétique qui n'existe
        // dans aucune cellule ni dans le catalogue F-243 — combinaison « non supportée »
        // stable pour ce test. (Le BCO, anciennement utilisé ici, est désormais couvert —
        // SF-98-62 — ; ce test cible donc un stade fictif garanti non couvert.)
        unsupportedCf = saveCf(uA, wsA, "CF unsupported " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "STADE_NON_COUVERT_TEST", "DEMANDEUR");
        saveDoneAnalysis(unsupportedCf);

        // ── Workspace B (isolation) ──────────────────────────────────────────
        User uB = save(new User(), u -> { u.setEmail("ccl-b-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uB, "g-ccl-b-" + ts);
        Workspace wsB = saveWs(uB, "WSB " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uB, wsB);
        otherWorkspaceCf = saveCf(uB, wsB, "CF other " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "FOND", "DEMANDEUR");
        saveDoneAnalysis(otherWorkspaceCf);
    }

    // ── POST nominal versionné (CA2) ─────────────────────────────────────────

    @Test
    void POST_generate_nominal_returns202WithVersionOne() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.versionNumber").value(1));

        List<CaseConclusion> versions = caseConclusionRepository
                .findByCaseFileIdOrderByVersionNumberDesc(supportedCf.getId());
        org.assertj.core.api.Assertions.assertThat(versions).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(versions.get(0).getVersionNumber()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(versions.get(0).getStatus())
                .isEqualTo(CaseConclusionStatus.PENDING);
        org.assertj.core.api.Assertions.assertThat(versions.get(0).getLifecycleStatus())
                .isEqualTo(ConclusionLifecycleStatus.DRAFT);
        // SF-271-02 — sans query param = défaut « Actualiser ».
        org.assertj.core.api.Assertions.assertThat(versions.get(0).isGeneratedFromScratch())
                .isFalse();
    }

    // SF-271-02 — « Régénérer de zéro » : ?fromScratch=true → flag persisté.
    @Test
    void POST_generate_fromScratchTrue_persistsFlag() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .param("fromScratch", "true")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.versionNumber").value(1));

        List<CaseConclusion> versions = caseConclusionRepository
                .findByCaseFileIdOrderByVersionNumberDesc(supportedCf.getId());
        org.assertj.core.api.Assertions.assertThat(versions).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(versions.get(0).isGeneratedFromScratch())
                .isTrue();
    }

    @Test
    void POST_generate_secondVersion_afterPreviousDone_returnsVersionTwo() throws Exception {
        // version 1 posée DONE en base (simule une génération terminée)
        persistVersion(supportedCf, 1, CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.versionNumber").value(2));

        org.assertj.core.api.Assertions.assertThat(caseConclusionRepository
                .findByCaseFileIdOrderByVersionNumberDesc(supportedCf.getId())).hasSize(2);
    }

    @Test
    void POST_generate_defendeurCombination_returns202() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + supportedDefendeurCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.versionNumber").value(1));

        List<CaseConclusion> versions = caseConclusionRepository
                .findByCaseFileIdOrderByVersionNumberDesc(supportedDefendeurCf.getId());
        org.assertj.core.api.Assertions.assertThat(versions).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(versions.get(0).getPositionCode())
                .isEqualTo("DEFENDEUR");
    }

    // ── POST gardes 409 ──────────────────────────────────────────────────────

    @Test
    void POST_generate_stageNotSet_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + noStageCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("STAGE_NOT_SET"));
    }

    @Test
    void POST_generate_analysisNotReady_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + noAnalysisCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ANALYSIS_NOT_READY"));
    }

    @Test
    void POST_generate_combinationNotSupported_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + unsupportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("COMBINATION_NOT_SUPPORTED"));
    }

    @Test
    void POST_generate_alreadyGenerating_returns409() throws Exception {
        // 1er déclenchement → version 1 PENDING (worker inactif en test)
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted());
        // 2e déclenchement → 409 : une version est PENDING
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_GENERATING"));
    }

    // ── GET .../conclusions = version la plus récente (CA3) ──────────────────

    @Test
    void GET_conclusions_beforeGenerate_returnsNotGenerated() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_GENERATED"))
                .andExpect(jsonPath("$.caseFileId").value(supportedCf.getId().toString()))
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    void GET_conclusions_returnsMostRecentVersion() throws Exception {
        persistVersion(supportedCf, 1, CaseConclusionStatus.DONE, ConclusionLifecycleStatus.VALIDATED);
        persistVersion(supportedCf, 2, CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.lifecycleStatus").value("DRAFT"))
                .andExpect(jsonPath("$.jurisdictionLabel").value("Conseil de prud'hommes"));
    }

    // ── SF-98-47 — styleApplied reflété dans ConclusionResponse ──────────────

    @Test
    void GET_conclusions_reflectsStyleAppliedTrue() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        v1.setStyleApplied(true);
        caseConclusionRepository.save(v1);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.styleApplied").value(true));
    }

    @Test
    void GET_conclusions_reflectsStyleAppliedFalseByDefault() throws Exception {
        persistVersion(supportedCf, 1, CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.styleApplied").value(false));
    }

    @Test
    void GET_conclusions_beforeGenerate_styleAppliedFalse() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_GENERATED"))
                .andExpect(jsonPath("$.styleApplied").value(false));
    }

    @Test
    void GET_version_reflectsStyleApplied() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        v1.setStyleApplied(true);
        caseConclusionRepository.save(v1);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId())
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.styleApplied").value(true));
    }

    // ── SF-98-53 — péremption (stale) ────────────────────────────────────────

    @Test
    void GET_conclusions_analysisUpdatedAfterGeneration_staleTrue() throws Exception {
        // version DONE générée il y a 2h
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        v1.setGeneratedAt(Instant.now().minusSeconds(7200));
        caseConclusionRepository.save(v1);
        // une analyse DONE ré-exécutée ensuite (updatedAt = maintenant)
        saveDoneAnalysis(supportedCf);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(true));
    }

    @Test
    void GET_conclusions_noAnalysisAfterGeneration_staleFalse() throws Exception {
        // version DONE générée maintenant ; les analyses du setUp sont antérieures
        persistVersion(supportedCf, 1, CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(false));
    }

    @Test
    void GET_conclusions_nonDoneVersion_staleFalse() throws Exception {
        persistVersion(supportedCf, 1, CaseConclusionStatus.PENDING, ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(false));
    }

    @Test
    void GET_conclusions_beforeGenerate_staleFalse() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_GENERATED"))
                .andExpect(jsonPath("$.stale").value(false));
    }

    @Test
    void GET_version_analysisUpdatedAfterGeneration_staleTrue() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        v1.setGeneratedAt(Instant.now().minusSeconds(7200));
        caseConclusionRepository.save(v1);
        saveDoneAnalysis(supportedCf);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId())
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(true));
    }

    @Test
    void GET_version_noAnalysisAfterGeneration_staleFalse() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId())
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stale").value(false));
    }

    // ── GET .../conclusions/versions (CA4) ───────────────────────────────────

    @Test
    void GET_versions_returnsAllVersionsDescending() throws Exception {
        persistVersion(supportedCf, 1, CaseConclusionStatus.DONE, ConclusionLifecycleStatus.VALIDATED);
        persistVersion(supportedCf, 2, CaseConclusionStatus.PENDING, ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/versions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNumber").value(2))
                .andExpect(jsonPath("$[0].lifecycleStatus").value("DRAFT"))
                .andExpect(jsonPath("$[1].versionNumber").value(1))
                .andExpect(jsonPath("$[1].lifecycleStatus").value("VALIDATED"));
    }

    @Test
    void GET_versions_noVersion_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/versions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET .../conclusions/versions/{versionId} (CA5) ───────────────────────

    @Test
    void GET_version_returnsFullDetail() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId())
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(v1.getId().toString()))
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.lifecycleStatus").value("DRAFT"));
    }

    @Test
    void GET_version_unknownVersion_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + UUID.randomUUID())
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    // ── PATCH .../lifecycle (CA6) ────────────────────────────────────────────

    @Test
    void PATCH_lifecycle_doneVersionToValidated_returns200() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId() + "/lifecycle")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lifecycleStatus\":\"VALIDATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleStatus").value("VALIDATED"));

        org.assertj.core.api.Assertions.assertThat(caseConclusionRepository
                        .findById(v1.getId()).orElseThrow().getLifecycleStatus())
                .isEqualTo(ConclusionLifecycleStatus.VALIDATED);
    }

    @Test
    void PATCH_lifecycle_nonDoneVersionToValidated_returns409() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.PENDING,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId() + "/lifecycle")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lifecycleStatus\":\"VALIDATED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LIFECYCLE_REQUIRES_DONE"));
    }

    @Test
    void PATCH_lifecycle_unknownValue_returns400() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId() + "/lifecycle")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lifecycleStatus\":\"ARCHIVED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PATCH_lifecycle_unknownVersion_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + UUID.randomUUID() + "/lifecycle")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lifecycleStatus\":\"DRAFT\"}"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH .../content (SF-98-49) ─────────────────────────────────────────

    @Test
    void PATCH_content_doneDraftVersion_returns200AndPersists() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId() + "/content")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Texte révisé par l'avocat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Texte révisé par l'avocat"));

        org.assertj.core.api.Assertions.assertThat(caseConclusionRepository
                        .findById(v1.getId()).orElseThrow().getContent())
                .isEqualTo("Texte révisé par l'avocat");
    }

    @Test
    void PATCH_content_nonDoneVersion_returns409() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.PENDING,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId() + "/content")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Texte révisé\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONTENT_REQUIRES_DONE"));
    }

    @Test
    void PATCH_content_validatedVersion_returns409() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.VALIDATED);

        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId() + "/content")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Texte révisé\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONTENT_NOT_EDITABLE"));
    }

    @Test
    void PATCH_content_blankContent_returns400() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + v1.getId() + "/content")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void PATCH_content_unknownVersion_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + UUID.randomUUID() + "/content")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Texte révisé\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_content_otherWorkspace_returns404() throws Exception {
        CaseConclusion otherVersion = persistVersion(otherWorkspaceCf, 1,
                CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);
        mockMvc.perform(patch("/api/v1/case-files/" + otherWorkspaceCf.getId()
                        + "/conclusions/versions/" + otherVersion.getId() + "/content")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Texte révisé\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_content_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + UUID.randomUUID() + "/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Texte révisé\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── isolation workspace 404 (CA7) ────────────────────────────────────────

    @Test
    void POST_generate_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + otherWorkspaceCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_conclusions_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + otherWorkspaceCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_versions_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + otherWorkspaceCf.getId() + "/conclusions/versions")
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_version_otherWorkspaceVersion_returns404() throws Exception {
        // version réelle dans le workspace B
        CaseConclusion otherVersion = persistVersion(otherWorkspaceCf, 1,
                CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);
        // l'avocat A demande cette version sur SON dossier → 404 (version pas rattachée)
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + otherVersion.getId())
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
        // et sur le dossier de B → 404 (dossier hors workspace A)
        mockMvc.perform(get("/api/v1/case-files/" + otherWorkspaceCf.getId()
                        + "/conclusions/versions/" + otherVersion.getId())
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_lifecycle_otherWorkspace_returns404() throws Exception {
        CaseConclusion otherVersion = persistVersion(otherWorkspaceCf, 1,
                CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);
        mockMvc.perform(patch("/api/v1/case-files/" + otherWorkspaceCf.getId()
                        + "/conclusions/versions/" + otherVersion.getId() + "/lifecycle")
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lifecycleStatus\":\"VALIDATED\"}"))
                .andExpect(status().isNotFound());
    }

    // ── authentification 401 ─────────────────────────────────────────────────

    @Test
    void POST_generate_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_conclusions_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_versions_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/versions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void PATCH_lifecycle_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + supportedCf.getId()
                        + "/conclusions/versions/" + UUID.randomUUID() + "/lifecycle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lifecycleStatus\":\"DRAFT\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user);
        a.setProvider("GOOGLE");
        a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name, String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setName(name);
        ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner);
        ws.setLegalDomain(legalDomain);
        ws.setCountry(country);
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws);
        m.setUser(user);
        m.setMemberRole("OWNER");
        m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private CaseFile saveCf(User user, Workspace ws, String title, String domain,
                            String jurisdiction, String stage, String position) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title);
        cf.setWorkspace(ws);
        cf.setCreatedBy(user);
        cf.setLegalDomain(domain);
        cf.setStatus("OPEN");
        cf.setProcedureJurisdiction(jurisdiction);
        cf.setProcedureStage(stage);
        cf.setProcedurePosition(position);
        return caseFileRepository.save(cf);
    }

    private void saveDoneAnalysis(CaseFile cf) {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(cf);
        analysis.setVersion(1);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisResult("{\"faits\": [], \"points_juridiques\": [], \"risques\": []}");
        caseAnalysisRepository.save(analysis);
    }

    /** Pose directement une version en base (le worker n'est pas chargé en test). */
    private CaseConclusion persistVersion(CaseFile cf, int versionNumber,
                                          CaseConclusionStatus status,
                                          ConclusionLifecycleStatus lifecycle) {
        CaseConclusion c = new CaseConclusion();
        c.setCaseFile(cf);
        c.setWorkspace(cf.getWorkspace());
        c.setVersionNumber(versionNumber);
        c.setStatus(status);
        c.setLifecycleStatus(lifecycle);
        c.setJurisdictionCode("CPH");
        c.setStageCode("FOND");
        c.setPositionCode("DEMANDEUR");
        if (status == CaseConclusionStatus.DONE) {
            c.setContent("Conclusions générées — version " + versionNumber);
            c.setGeneratedAt(Instant.now());
        }
        return caseConclusionRepository.save(c);
    }

    // ── POST .../sections/regenerate (F-265 / SF-265-01) ─────────────────────

    private static final String REGEN_URL_TPL =
            "/api/v1/case-files/%s/conclusions/versions/%s/sections/regenerate";

    @Test
    void POST_regenerateSection_doneDraft_returns200WithMarkdown() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        when(anthropicService.analyze(any(), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("## Sur la prescription\n\nSection renforcée.",
                        "claude-test", 100, 50));

        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), v1.getId()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## Sur la prescription\\n\\nTexte.\","
                                + "\"instruction\":\"Renforce la prescription\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regeneratedMarkdown")
                        .value("## Sur la prescription\n\nSection renforcée."));

        // Aucune persistance : le content de la version reste inchangé.
        org.assertj.core.api.Assertions.assertThat(caseConclusionRepository
                        .findById(v1.getId()).orElseThrow().getContent())
                .isEqualTo("Conclusions générées — version 1");
    }

    @Test
    void POST_regenerateSection_blankSection_returns400() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), v1.getId()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"  \",\"instruction\":\"Renforce\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_regenerateSection_blankInstruction_returns400() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), v1.getId()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## X\",\"instruction\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_regenerateSection_nonDoneVersion_returns409() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.PENDING,
                ConclusionLifecycleStatus.DRAFT);
        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), v1.getId()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## X\",\"instruction\":\"Renforce\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONTENT_REQUIRES_DONE"));
    }

    @Test
    void POST_regenerateSection_validatedVersion_returns409() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.VALIDATED);
        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), v1.getId()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## X\",\"instruction\":\"Renforce\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONTENT_NOT_EDITABLE"));
    }

    @Test
    void POST_regenerateSection_otherWorkspace_returns404() throws Exception {
        CaseConclusion otherVersion = persistVersion(otherWorkspaceCf, 1,
                CaseConclusionStatus.DONE, ConclusionLifecycleStatus.DRAFT);
        mockMvc.perform(post(String.format(REGEN_URL_TPL, otherWorkspaceCf.getId(), otherVersion.getId()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## X\",\"instruction\":\"Renforce\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_regenerateSection_unknownVersion_returns404() throws Exception {
        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), UUID.randomUUID()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## X\",\"instruction\":\"Renforce\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_regenerateSection_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## X\",\"instruction\":\"Renforce\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void POST_regenerateSection_emptyAiResult_returns502() throws Exception {
        CaseConclusion v1 = persistVersion(supportedCf, 1, CaseConclusionStatus.DONE,
                ConclusionLifecycleStatus.DRAFT);
        when(anthropicService.analyze(any(), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("   ", "claude-test", 10, 0));

        mockMvc.perform(post(String.format(REGEN_URL_TPL, supportedCf.getId(), v1.getId()))
                        .with(authentication(authA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionMarkdown\":\"## X\",\"instruction\":\"Renforce\"}"))
                .andExpect(status().is(502));
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
