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

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class MesuresProvisoiresControllerIT {

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
    private CaseFile famFrCf;
    private CaseFile famBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_FAMILLE
        User uFr = save(new User(), u -> { u.setEmail("mp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-mp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSMPFR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-mp-fr-" + ts, "mp-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE (gate country → rejet)
        User uBe = save(new User(), u -> { u.setEmail("mp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-mp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSMPBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-mp-be-" + ts, "mp-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate legal_domain → rejet)
        User uDt = save(new User(), u -> { u.setEmail("mp-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-mp-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSMPDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-mp-dt-" + ts, "mp-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate audience,
                                     Number revenusD,
                                     Number revenusE,
                                     String logementProp,
                                     List<Map<String, Object>> enfants,
                                     String souhait,
                                     Boolean violences,
                                     Boolean patrimoine,
                                     Boolean demandeMC) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateAudienceAOMP", audience != null ? audience.toString() : null);
        m.put("revenusEpouxDemandeurEur", revenusD);
        m.put("revenusEpouxDefendeurEur", revenusE);
        m.put("logementCommunDescription", "Test logement");
        m.put("logementProprietaire", logementProp);
        m.put("enfantsMineurs", enfants);
        m.put("souhaitResidenceEnfants", souhait);
        m.put("violencesAlleguees", violences);
        m.put("patrimoineCommunIsignificatif", patrimoine);
        m.put("demandeMesureConservatoire", demandeMC);
        return m;
    }

    private static Map<String, Object> enfant(String prenom, int age) {
        Map<String, Object> m = new HashMap<>();
        m.put("prenom", prenom);
        m.put("age", age);
        return m;
    }

    private Map<String, Object> bodyNominalElevee() {
        return body(LocalDate.of(2026, 6, 15),
                3500, 2000,
                "EN_INDIVISION",
                List.of(enfant("Léa", 8), enfant("Tom", 12)),
                "ALTERNEE",
                false, false, false);
    }

    @Test
    void POST_fr_nominal_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.differentielRevenus").value(1500.00))
                .andExpect(jsonPath("$.pensionAlimentairePropose").value(750.00))
                .andExpect(jsonPath("$.attributionLogementRecommande").value("DEFENDEUR"))
                .andExpect(jsonPath("$.residenceEnfantsRecommande").value("ALTERNEE"))
                .andExpect(jsonPath("$.contributionCharges").value(1750.00))
                .andExpect(jsonPath("$.scoreCohesionMesures").value(100))
                .andExpect(jsonPath("$.verdictAcceptabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("254")));
    }

    @Test
    void POST_fr_violences_attributionDemandeur() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 6, 15),
                4000, 1500, "PROPRIETE_DEFENDEUR",
                List.of(enfant("Mia", 4)), "ALTERNEE",
                true, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributionLogementRecommande").value("DEMANDEUR"))
                .andExpect(jsonPath("$.residenceEnfantsRecommande").value("EXCLUSIVE_DEMANDEUR"));
    }

    @Test
    void POST_fr_pasEnfants_residenceSansObjet() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 6, 15),
                3000, 2000, "LOCATION_COMMUNE",
                List.of(), "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residenceEnfantsRecommande").value("SANS_OBJET"));
    }

    @Test
    void POST_fr_mesureConservatoire_recommandee() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 6, 15),
                5000, 2500, "EN_INDIVISION",
                List.of(), "EXCLUSIVE_DEMANDEUR",
                false, true, true);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesureConservatoireRecommande").value(true));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/mesures-provisoires")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr essaie d'accéder à dossier BE → 404 isolation
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_missingDateAudience_returns400() throws Exception {
        Map<String, Object> b = body(null, 3000, 2000, "EN_INDIVISION",
                List.of(), "EXCLUSIVE_DEMANDEUR", false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_missingRevenusDemandeur_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 6, 15),
                null, 2000, "EN_INDIVISION",
                List.of(), "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_invalidLogementProprietaire_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 6, 15),
                3000, 2000, "INVALIDE",
                List.of(), "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_revenusNegatifs_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 6, 15),
                -100, 2000, "EN_INDIVISION",
                List.of(), "EXCLUSIVE_DEMANDEUR",
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreCohesionMesures").value(100));

        // 2e appel : revenus égaux → indivision maintenue + souhait diverge
        Map<String, Object> next = body(LocalDate.of(2026, 6, 15),
                3000, 3000, "EN_INDIVISION",
                List.of(enfant("Bébé", 1)),
                "ALTERNEE",
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreCohesionMesures").value(65))
                .andExpect(jsonPath("$.verdictAcceptabilite").value("MOYENNE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreCohesionMesures").value(100))
                .andExpect(jsonPath("$.verdictAcceptabilite").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/mesures-provisoires")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

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
