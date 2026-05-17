package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-98 / SF-98-01 — tests d'intégration de l'API du générateur de conclusions.
 *
 * <p>Couvre : déclenchement 202 + ligne PENDING, les 4 gardes 409, GET avant/après
 * déclenchement, isolation workspace 404, rejet 401 sans authentification.</p>
 *
 * <p>Le worker {@code CaseConclusionService} est {@code @Profile({"local","prod"})} :
 * en profil de test il n'est pas chargé, donc après un POST la ligne reste
 * {@code PENDING} (le message RabbitMQ part dans le vide). C'est l'état attendu ici.</p>
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
    private OAuth2AuthenticationToken authB;

    /** Dossier travail FR / CPH / FOND / DEMANDEUR avec analyse DONE — workspace A. */
    private CaseFile supportedCf;
    /** Dossier sans stade procédural — workspace A. */
    private CaseFile noStageCf;
    /** Dossier travail FR / CPH / FOND / DEMANDEUR sans analyse DONE — workspace A. */
    private CaseFile noAnalysisCf;
    /** Dossier BE (combinaison non supportée) — workspace A. */
    private CaseFile unsupportedCf;
    /** Dossier du workspace B (isolation). */
    private CaseFile otherWorkspaceCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // ── Workspace A — FRANCE / DROIT_DU_TRAVAIL ──────────────────────────
        User uA = save(new User(), u -> { u.setEmail("ccl-a-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uA, "g-ccl-a-" + ts);
        Workspace wsA = saveWs(uA, "WSA " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uA, wsA);
        authA = buildAuth("g-ccl-a-" + ts, "ccl-a-" + ts + "@ex.com");

        supportedCf = saveCf(uA, wsA, "CF supported " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "FOND", "DEMANDEUR");
        saveDoneAnalysis(supportedCf);

        noStageCf = saveCf(uA, wsA, "CF nostage " + ts, "DROIT_DU_TRAVAIL",
                null, null, null);
        saveDoneAnalysis(noStageCf);

        noAnalysisCf = saveCf(uA, wsA, "CF noanalysis " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "FOND", "DEMANDEUR");
        // pas d'analyse DONE

        // ── Workspace A — combinaison BE non supportée ───────────────────────
        // Dossier travail dans workspace FR mais combinaison procédurale hors V1
        // (stade REFERE). Le pays vient du workspace → on teste le stade ≠ FOND.
        unsupportedCf = saveCf(uA, wsA, "CF unsupported " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "REFERE", "DEMANDEUR");
        saveDoneAnalysis(unsupportedCf);

        // ── Workspace B (isolation) ──────────────────────────────────────────
        User uB = save(new User(), u -> { u.setEmail("ccl-b-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uB, "g-ccl-b-" + ts);
        Workspace wsB = saveWs(uB, "WSB " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uB, wsB);
        authB = buildAuth("g-ccl-b-" + ts, "ccl-b-" + ts + "@ex.com");
        otherWorkspaceCf = saveCf(uB, wsB, "CF other " + ts, "DROIT_DU_TRAVAIL",
                "CPH", "FOND", "DEMANDEUR");
        saveDoneAnalysis(otherWorkspaceCf);
    }

    // ── POST nominal ─────────────────────────────────────────────────────────

    @Test
    void POST_generate_nominal_returns202AndCreatesPendingRow() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        CaseConclusion row = caseConclusionRepository.findByCaseFileId(supportedCf.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(row.getStatus()).isEqualTo(CaseConclusionStatus.PENDING);
        org.assertj.core.api.Assertions.assertThat(row.getJurisdictionCode()).isEqualTo("CPH");
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
        // 1er déclenchement → ligne PENDING
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted());
        // 2e déclenchement (worker inactif en test → toujours PENDING) → 409
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_GENERATING"));
    }

    // ── GET ──────────────────────────────────────────────────────────────────

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
    void GET_conclusions_afterGenerate_returnsPendingWithLabels() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + supportedCf.getId() + "/conclusions/generate")
                        .with(authentication(authA)))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/case-files/" + supportedCf.getId() + "/conclusions")
                        .with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.jurisdictionLabel").value("Conseil de prud'hommes"))
                .andExpect(jsonPath("$.stageLabel").value("Bureau de jugement (fond)"))
                .andExpect(jsonPath("$.positionLabel").value("Demandeur (salarié)"));
    }

    // ── isolation workspace ──────────────────────────────────────────────────

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

    // ── authentification ─────────────────────────────────────────────────────

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
