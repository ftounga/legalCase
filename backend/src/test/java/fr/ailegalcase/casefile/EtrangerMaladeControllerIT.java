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
 * SF-214-01 : tests d'intégration de {@link EtrangerMaladeController}.
 * Couvre POST/GET, workspace isolation (country + domain), upsert et 404.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class EtrangerMaladeControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile immFrCf;
    private CaseFile immBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR DROIT_IMMIGRATION (cas nominal)
        User uFr = save(new User(), u -> { u.setEmail("em-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-em-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-EM " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-EM " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-em-fr-" + ts, "em-fr-" + ts + "@ex.com");

        // BE DROIT_IMMIGRATION (gate country FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("em-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-em-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-EM " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-EM " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-em-be-" + ts, "em-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL (gate domaine → 400)
        User uDt = save(new User(), u -> { u.setEmail("em-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-em-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-EM " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-EM " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-em-dt-" + ts, "em-dt-" + ts + "@ex.com");
    }

    // ── Helpers de corps de requête ─────────────────────────────────────

    private Map<String, Object> bodyNominal() {
        return body("Tuberculose multi-résistante", "Mali", false, null, null, null);
    }

    private Map<String, Object> bodyDefavorable() {
        return body("Insuffisance rénale chronique", "Sénégal", false,
                "DEFAVORABLE", LocalDate.now().minusDays(30).toString(), null);
    }

    private Map<String, Object> body(String pathologie, String pays, boolean traitement,
                                      String avis, String dateAvis, String dateDepot) {
        Map<String, Object> m = new HashMap<>();
        m.put("pathologiePrincipale", pathologie);
        m.put("paysOrigine", pays);
        m.put("traitementDisponiblePaysOrigine", traitement);
        if (avis != null) m.put("avisOFII", avis);
        if (dateAvis != null) m.put("dateAvisOFII", dateAvis);
        if (dateDepot != null) m.put("dateDepotDossierOFII", dateDepot);
        return m;
    }

    // ── Tests nominaux ──────────────────────────────────────────────────

    @Test
    void POST_fr_traitementIndisponible_sanAvis_retourneEN_ATTENTE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("EN_ATTENTE_AVIS_OFII"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("L. 425-9")))
                .andExpect(jsonPath("$.chipsCriteresNonRemplis").isArray());
    }

    @Test
    void POST_fr_avisDefavorable_retourneDelaiRecoursTA() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDefavorable())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PROBABLE"))
                .andExpect(jsonPath("$.delaiRecoursTA").isNotEmpty())
                .andExpect(jsonPath("$.motifRecours").isNotEmpty())
                .andExpect(jsonPath("$.chipsCriteresNonRemplis[0]").value("AVIS_OFII_DEFAVORABLE"));
    }

    @Test
    void POST_fr_avisFavorable_traitementDisponible_retourneELIGIBLE_SOUS_RESERVE() throws Exception {
        Map<String, Object> b = body("Dialyse chronique", "Maroc", true,
                "FAVORABLE", null, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_SOUS_RESERVE"));
    }

    // ── Tests d'isolation workspace ─────────────────────────────────────

    @Test
    void POST_workspaceBelgique_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_autreWorkspace_returns404() throws Exception {
        // authFr tente d'accéder au dossier de authBe
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isNotFound());
    }

    // ── Tests upsert et GET ─────────────────────────────────────────────

    @Test
    void POST_deuxFois_upsert_retourneDernierResultat() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EN_ATTENTE_AVIS_OFII"));

        // Second POST avec avis FAVORABLE
        Map<String, Object> b2 = body("Tuberculose multi-résistante", "Mali", false,
                "FAVORABLE", null, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PROBABLE"));
    }

    @Test
    void GET_afterPost_retourneAnalysePersistee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("EN_ATTENTE_AVIS_OFII"))
                .andExpect(jsonPath("$.pathologiePrincipale").value("Tuberculose multi-résistante"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/etranger-malade-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

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
