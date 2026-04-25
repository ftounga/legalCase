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

import java.math.BigDecimal;
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

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ReferePrudhomalControllerIT {

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
    private OAuth2AuthenticationToken authIm;
    private CaseFile dtFrCf;
    private CaseFile dtBeCf;
    private CaseFile imFrCf;

    private final LocalDate medRecente = LocalDate.now().minusDays(20);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_DU_TRAVAIL
        User uFr = save(new User(), u -> { u.setEmail("rp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSRPFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        dtFrCf = saveCf(uFr, wsFr, "CRPFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rp-fr-" + ts, "rp-fr-" + ts + "@ex.com");

        // BE workspace DROIT_DU_TRAVAIL (gate country FRANCE → rejet)
        User uBe = save(new User(), u -> { u.setEmail("rp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSRPBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        dtBeCf = saveCf(uBe, wsBe, "CRPBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-rp-be-" + ts, "rp-be-" + ts + "@ex.com");

        // FR workspace DROIT_IMMIGRATION (gate legal_domain → rejet)
        User uIm = save(new User(), u -> { u.setEmail("rp-im-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uIm, "g-rp-im-" + ts);
        Workspace wsIm = saveWs(uIm, "WSRPIM " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uIm, wsIm);
        imFrCf = saveCf(uIm, wsIm, "CRPIM " + ts, "DROIT_IMMIGRATION");
        authIm = buildAuth("g-rp-im-" + ts, "rp-im-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String typeRefere, String natureCreance,
                                     BigDecimal montant,
                                     boolean absenceContestation, List<String> preuves,
                                     boolean dommage, boolean tresorerie,
                                     LocalDate med, Integer anciennete) {
        Map<String, Object> m = new HashMap<>();
        m.put("typeRefere", typeRefere);
        m.put("natureCreance", natureCreance);
        m.put("montantProvisionDemandeeEur", montant);
        m.put("absenceContestationSerieuse", absenceContestation);
        m.put("preuvesUrgenceProduites", preuves);
        m.put("dommageImmediatCarac", dommage);
        m.put("tresorerieEmployeurDouteuse", tresorerie);
        m.put("dateMiseEnDemeure", med == null ? null : med.toString());
        m.put("ancienneteContratMois", anciennete);
        return m;
    }

    @Test
    void POST_fr_provisionFavorable_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                                        new BigDecimal("5000.00"),
                                        true,
                                        List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                                        true, false, medRecente, 18))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("PROVISION_SALAIRES"))
                .andExpect(jsonPath("$.natureCreance").value("SALAIRES_NON_VERSES"))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreSuccess").exists())
                .andExpect(jsonPath("$.verdictRecommandation").exists())
                .andExpect(jsonPath("$.delaiAudienceJoursPrevisionnel").value(15))
                .andExpect(jsonPath("$.delaiOrdonnanceJoursPrevisionnel").value(8))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("R.1454-1")));
    }

    @Test
    void POST_fr_expertiseMedicale_returnsExpertiseRecommandee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("EXPERTISE_MEDICALE", "AUTRE",
                                        null, false, List.of(),
                                        false, false, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("EXPERTISE_MEDICALE"))
                .andExpect(jsonPath("$.verdictRecommandation").value("EXPERTISE_RECOMMANDEE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/refere-prudhomal")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                                        null, true, List.of(),
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dossierImmigration_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + imFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authIm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                                        null, true, List.of(),
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                                        null, true, List.of(),
                                        false, false, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_typeRefereInvalide_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("BIDON", "SALAIRES_NON_VERSES",
                                        null, true, List.of(),
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_natureCreanceInvalide_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "INCONNUE",
                                        null, true, List.of(),
                                        false, false, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                                        new BigDecimal("3000.00"),
                                        true, List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                                        false, false, medRecente, 12))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("PROVISION_SALAIRES"));

        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("EXPERTISE_TECHNIQUE", "HEURES_SUPPLEMENTAIRES",
                                        null, false, List.of(),
                                        false, false, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("EXPERTISE_TECHNIQUE"))
                .andExpect(jsonPath("$.natureCreance").value("HEURES_SUPPLEMENTAIRES"))
                .andExpect(jsonPath("$.verdictRecommandation").value("EXPERTISE_RECOMMANDEE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                                        new BigDecimal("4500.00"),
                                        true, List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                                        true, false, medRecente, 24))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("PROVISION_SALAIRES"))
                .andExpect(jsonPath("$.natureCreance").value("SALAIRES_NON_VERSES"))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateMiseEnDemeureFuture_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/refere-prudhomal")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                                        null, true, List.of(),
                                        false, false,
                                        LocalDate.now().plusDays(7),
                                        null))))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers ----

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

    private CaseFile saveCf(User user, Workspace ws, String title, String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title);
        cf.setWorkspace(ws);
        cf.setCreatedBy(user);
        cf.setLegalDomain(domain);
        cf.setStatus("OPEN");
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
