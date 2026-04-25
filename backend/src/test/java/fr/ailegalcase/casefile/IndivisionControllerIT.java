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
class IndivisionControllerIT {

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

        User uFr = save(new User(), u -> { u.setEmail("ind-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ind-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSINDFR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-ind-fr-" + ts, "ind-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("ind-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ind-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSINDBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-ind-be-" + ts, "ind-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("ind-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-ind-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSINDDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-ind-dt-" + ts, "ind-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate origine,
                                     List<String> natureBiens,
                                     Number valeur,
                                     Integer nbIndivisaires,
                                     List<Number> quotesPart,
                                     List<String> tentatives,
                                     Boolean consentement,
                                     Boolean occupation,
                                     Integer dureeAnnees,
                                     Boolean mesuresConservatoires,
                                     Boolean conflit) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateOrigineIndivision", origine != null ? origine.toString() : null);
        m.put("natureBiens", natureBiens);
        m.put("valeurEstimeeTotaleEur", valeur);
        m.put("nbIndivisaires", nbIndivisaires);
        m.put("quotesPart", quotesPart);
        m.put("tentativesPartageAmiable", tentatives);
        m.put("consentementPartageGlobal", consentement);
        m.put("occupationBienParUnIndivisaire", occupation);
        m.put("indivisionDureeAnnees", dureeAnnees);
        m.put("demandeMesuresConservatoires", mesuresConservatoires);
        m.put("conflitOuvertEntreIndivisaires", conflit);
        return m;
    }

    private Map<String, Object> bodyJudiciaireRecommande() {
        return body(LocalDate.of(2023, 4, 15),
                List.of("IMMOBILIER"),
                250000,
                2,
                List.of(50.0, 50.0),
                List.of("MEDIATION", "PROPOSITION_NOTAIRE"),
                false, true, 3, false, true);
    }

    @Test
    void POST_fr_partageJudiciaireRecommande() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyJudiciaireRecommande())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreEligibilitePartageJudiciaire").value(100))
                .andExpect(jsonPath("$.verdictRecommandation").value("PARTAGE_JUDICIAIRE_RECOMMANDE"))
                .andExpect(jsonPath("$.indemniteOccupationDueEur").value(15000.00))
                .andExpect(jsonPath("$.licitationRecommandee").value(true))
                .andExpect(jsonPath("$.expertiseNotarialeRecommandee").value(true))
                .andExpect(jsonPath("$.delaiProcedurePartageJudiciaireMois").value(24))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("815")));
    }

    @Test
    void POST_fr_partageAmiable_consentement() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2025, 1, 10),
                List.of("MOBILIER"),
                30000, 2,
                List.of(50.0, 50.0),
                List.of("AUCUNE"),
                true, false, 1, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecommandation").value("PARTAGE_AMIABLE_POSSIBLE"))
                .andExpect(jsonPath("$.indemniteOccupationDueEur").value(0.00))
                .andExpect(jsonPath("$.licitationRecommandee").value(false));
    }

    @Test
    void POST_fr_licitationRequise_immobilier_seuilMoyen() throws Exception {
        // 2 tentatives (+30) + occupation (+10) = 40, immobilier → LICITATION
        Map<String, Object> b = body(LocalDate.of(2024, 6, 1),
                List.of("IMMOBILIER"),
                180000, 2,
                List.of(50.0, 50.0),
                List.of("MEDIATION", "EXPERTISE_VALORISATION"),
                true, true, 1, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecommandation").value("LICITATION_REQUISE"))
                .andExpect(jsonPath("$.licitationRecommandee").value(true));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/indivision")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyJudiciaireRecommande())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/indivision")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyJudiciaireRecommande())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyJudiciaireRecommande())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_missingDateOrigine_returns400() throws Exception {
        Map<String, Object> b = body(null, List.of("MOBILIER"), 30000, 2,
                List.of(50.0, 50.0), List.of("AUCUNE"),
                true, false, 1, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_quotesPart_sommeIncorrecte_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2024, 1, 1),
                List.of("MOBILIER"), 30000, 2,
                List.of(60.0, 30.0),     // somme 90, pas 100
                List.of("AUCUNE"),
                true, false, 1, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_valeurNegative_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2024, 1, 1),
                List.of("MOBILIER"), -100, 2,
                List.of(50.0, 50.0), List.of("AUCUNE"),
                true, false, 1, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyJudiciaireRecommande())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecommandation").value("PARTAGE_JUDICIAIRE_RECOMMANDE"));

        Map<String, Object> next = body(LocalDate.of(2025, 1, 10),
                List.of("MOBILIER"), 30000, 2,
                List.of(50.0, 50.0), List.of("AUCUNE"),
                true, false, 1, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecommandation").value("PARTAGE_AMIABLE_POSSIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyJudiciaireRecommande())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdictRecommandation").value("PARTAGE_JUDICIAIRE_RECOMMANDE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/indivision")
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
