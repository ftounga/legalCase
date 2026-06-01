package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-218-31 : tests d'intégration de {@link AccordEntrepriseValiditeController}
 * (F-DT-67, outil FRANCE uniquement). Couvre POST/GET nominaux, conditions de
 * majorité, référendum, révision, dénonciation (fin de survie +15 mois), gate
 * country (BE → 400) + domaine (DROIT_IMMIGRATION → 400), validations (400),
 * isolation workspace (404), GET sans POST (404) et upsert.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AccordEntrepriseValiditeControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authImm;
    private CaseFile dtFrCf;
    private CaseFile dtBeCf;
    private CaseFile immFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uFr = save(new User(), u -> { u.setEmail("aev-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-aev-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRT-AEV " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        dtFrCf = saveCf(uFr, wsFr, "CFRT-AEV " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-aev-fr-" + ts, "aev-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("aev-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-aev-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBET-AEV " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        dtBeCf = saveCf(uBe, wsBe, "CBET-AEV " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-aev-be-" + ts, "aev-be-" + ts + "@ex.com");

        User uImm = save(new User(), u -> { u.setEmail("aev-imm-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uImm, "g-aev-imm-" + ts);
        Workspace wsImm = saveWs(uImm, "WSFRI-AEV " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uImm, wsImm);
        immFrCf = saveCf(uImm, wsImm, "CFRI-AEV " + ts, "DROIT_IMMIGRATION");
        authImm = buildAuth("g-aev-imm-" + ts, "aev-imm-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    /** Conclusion avec signataires > 50 % → MAJORITE_50, VALIDE. */
    private Map<String, Object> bodyConclusionMajoritaire() {
        Map<String, Object> m = new HashMap<>();
        m.put("pourcentageSuffragesSignataires", 55);
        m.put("referendumOrganise", false);
        m.put("referendumApprouve", false);
        m.put("typeOperation", "CONCLUSION");
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_majorite50_retourne200_valide() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConclusionMajoritaire())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.conditionMajorite").value("MAJORITE_50"))
                .andExpect(jsonPath("$.statut").value("VALIDE"))
                .andExpect(jsonPath("$.itemsNonConformes").value(0))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("L.2232-12")));
    }

    @Test
    void POST_fr_referendum30_valideSousReserve() throws Exception {
        Map<String, Object> body = bodyConclusionMajoritaire();
        body.put("pourcentageSuffragesSignataires", 35);
        body.put("referendumOrganise", true);
        body.put("referendumApprouve", true);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionMajorite").value("REFERENDUM_30"))
                .andExpect(jsonPath("$.statut").value("VALIDE_SOUS_RESERVE"));
    }

    @Test
    void POST_fr_signataires35SansReferendum_insuffisante_nonValide() throws Exception {
        Map<String, Object> body = bodyConclusionMajoritaire();
        body.put("pourcentageSuffragesSignataires", 35);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionMajorite").value("INSUFFISANTE"))
                .andExpect(jsonPath("$.statut").value("NON_VALIDE"));
    }

    @Test
    void POST_fr_denonciation_dateFinSurviePlus15Mois() throws Exception {
        Map<String, Object> body = bodyConclusionMajoritaire();
        body.put("pourcentageSuffragesSignataires", 60);
        body.put("typeOperation", "DENONCIATION");
        body.put("preavisDenonciationRespecte", true);
        body.put("dateDenonciation", "2025-01-15");
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"))
                .andExpect(jsonPath("$.dateFinSurvie").value("2026-04-15"));
    }

    // ── Validations / gates ─────────────────────────────────────────────

    @Test
    void POST_pourcentageHorsBorne_returns400() throws Exception {
        Map<String, Object> body = bodyConclusionMajoritaire();
        body.put("pourcentageSuffragesSignataires", 120);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_typeOperationInvalide_returns400() throws Exception {
        Map<String, Object> body = bodyConclusionMajoritaire();
        body.put("typeOperation", "FUSION");
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_revisionSansSignePartiesHabilitees_returns400() throws Exception {
        Map<String, Object> body = bodyConclusionMajoritaire();
        body.put("typeOperation", "REVISION");
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post(url(dtBeCf)).with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConclusionMajoritaire())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitImmigration_returns400() throws Exception {
        mockMvc.perform(post(url(immFrCf)).with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConclusionMajoritaire())))
                .andExpect(status().isBadRequest());
    }

    // ── Isolation workspace ─────────────────────────────────────────────

    @Test
    void POST_autreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(url(dtBeCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConclusionMajoritaire())))
                .andExpect(status().isNotFound());
    }

    // ── GET / upsert ────────────────────────────────────────────────────

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(dtFrCf)).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_deuxFois_puisGET_upsertRemplaceAnalyse() throws Exception {
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConclusionMajoritaire())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"));

        Map<String, Object> nonValide = bodyConclusionMajoritaire();
        nonValide.put("pourcentageSuffragesSignataires", 25);
        mockMvc.perform(post(url(dtFrCf)).with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonValide)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("NON_VALIDE"));

        mockMvc.perform(get(url(dtFrCf)).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("NON_VALIDE"))
                .andExpect(jsonPath("$.conditionMajorite").value("INSUFFISANTE"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String url(CaseFile cf) {
        return "/api/v1/case-files/" + cf.getId() + "/accord-entreprise-validite-analysis";
    }

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user); a.setProvider("GOOGLE"); a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name, String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setName(name); ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner); ws.setLegalDomain(legalDomain); ws.setCountry(country);
        ws.setPlanCode("STARTER"); ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws); m.setUser(user); m.setMemberRole("OWNER"); m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private CaseFile saveCf(User user, Workspace ws, String title, String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title); cf.setWorkspace(ws); cf.setCreatedBy(user);
        cf.setLegalDomain(domain); cf.setStatus("OPEN");
        return caseFileRepository.save(cf);
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
