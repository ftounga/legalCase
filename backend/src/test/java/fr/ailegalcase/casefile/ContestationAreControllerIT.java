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
class ContestationAreControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFrDt;
    private OAuth2AuthenticationToken authBeDt;
    private OAuth2AuthenticationToken authFrIm;
    private CaseFile dtFrCf;
    private CaseFile dtBeCf;
    private CaseFile imFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR DROIT_DU_TRAVAIL → cible
        User uFrDt = save(new User(), u -> { u.setEmail("are-fr-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFrDt, "g-are-fr-dt-" + ts);
        Workspace wsFrDt = saveWs(uFrDt, "WSFRDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFrDt, wsFrDt);
        dtFrCf = saveCf(uFrDt, wsFrDt, "CFRDT " + ts, "DROIT_DU_TRAVAIL");
        authFrDt = buildAuth("g-are-fr-dt-" + ts, "are-fr-dt-" + ts + "@ex.com");

        // BE DROIT_DU_TRAVAIL → gate FR → 400
        User uBeDt = save(new User(), u -> { u.setEmail("are-be-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBeDt, "g-are-be-dt-" + ts);
        Workspace wsBeDt = saveWs(uBeDt, "WSBEDT " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBeDt, wsBeDt);
        dtBeCf = saveCf(uBeDt, wsBeDt, "CBEDT " + ts, "DROIT_DU_TRAVAIL");
        authBeDt = buildAuth("g-are-be-dt-" + ts, "are-be-dt-" + ts + "@ex.com");

        // FR DROIT_IMMIGRATION → gate domaine → 400
        User uFrIm = save(new User(), u -> { u.setEmail("are-fr-im-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFrIm, "g-are-fr-im-" + ts);
        Workspace wsFrIm = saveWs(uFrIm, "WSFRIM " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFrIm, wsFrIm);
        imFrCf = saveCf(uFrIm, wsFrIm, "CFRIM " + ts, "DROIT_IMMIGRATION");
        authFrIm = buildAuth("g-are-fr-im-" + ts, "are-fr-im-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String type, String motif,
                                     String dateNotif, String dateRecours,
                                     List<String> preuves, Object montant,
                                     Boolean dejaSaisi, Boolean delaiOk) {
        Map<String, Object> m = new HashMap<>();
        m.put("typeDecisionContestee", type);
        m.put("motifContestation", motif);
        m.put("dateNotificationDecision", dateNotif);
        m.put("dateRecoursHierarchiquePropose", dateRecours);
        m.put("preuvesProduites", preuves);
        m.put("montantContesteEur", montant);
        m.put("demandeurDejaSaisiTribunal", dejaSaisi);
        m.put("delaiContestationRespecte", delaiOk);
        return m;
    }

    @Test
    void POST_fr_nominal_recoursPrioritaire() throws Exception {
        Map<String, Object> body = body(
                "REFUS_OUVERTURE_DROITS", "ERREUR_CALCUL_REMUNERATION_REFERENCE",
                "2026-03-01", "2026-04-01",
                List.of("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                1500.00, false, true);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreSuccessProbable").value(100))
                .andExpect(jsonPath("$.verdictRecommandation").value("RECOURS_HIERARCHIQUE_PRIORITAIRE"))
                .andExpect(jsonPath("$.delaiRecoursHierarchiqueJoursOk").value(true))
                .andExpect(jsonPath("$.delaiRecoursContentieuxTaJoursOk").value(true))
                .andExpect(jsonPath("$.delaiInstructionMoisPrevisionnel").value(4))
                .andExpect(jsonPath("$.expertiseTraitementSalaireRecommandee").value(true))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.5422-1")));
    }

    @Test
    void POST_fr_insuffisammentFonde_pourScoreFaible() throws Exception {
        Map<String, Object> body = body(
                "TROP_PERCU", "REFUS_INJUSTIFIE",
                "2026-03-01", null, List.of("BULLETIN_PAIE"),
                null, true, false);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecommandation").value("INSUFFISAMMENT_FONDE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        Map<String, Object> body = body(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                "2026-03-01", null, List.of(), null, false, true);
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/contestation-are")
                        .with(authentication(authBeDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dossierImmigration_returns400() throws Exception {
        Map<String, Object> body = body(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                "2026-03-01", null, List.of(), null, false, true);
        mockMvc.perform(post("/api/v1/case-files/" + imFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrIm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = body(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                "2026-03-01", null, List.of(), null, false, true);
        // Avocat FR DT essaie d'accéder au dossier de l'avocat BE DT
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_typeInvalide_returns400() throws Exception {
        Map<String, Object> body = body(
                "INVALIDE", "REFUS_INJUSTIFIE",
                "2026-03-01", null, List.of(), null, false, true);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_sansDateNotification_returns400() throws Exception {
        Map<String, Object> body = body(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                null, null, List.of(), null, false, true);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_sansDejaSaisiTribunal_returns400() throws Exception {
        Map<String, Object> body = body(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                "2026-03-01", null, List.of(), null, null, true);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_montantNegatif_returns400() throws Exception {
        Map<String, Object> body = body(
                "REFUS_OUVERTURE_DROITS", "REFUS_INJUSTIFIE",
                "2026-03-01", null, List.of(), -100.00, false, true);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        // 1er POST score élevé
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                "REFUS_OUVERTURE_DROITS", "ERREUR_CALCUL_REMUNERATION_REFERENCE",
                                "2026-03-01", "2026-04-01",
                                List.of("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                                1500.00, false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecommandation").value("RECOURS_HIERARCHIQUE_PRIORITAIRE"));

        // 2e POST score faible
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                "TROP_PERCU", "AUTRE",
                                "2026-03-01", null, List.of(), null, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecommandation").value("INSUFFISAMMENT_FONDE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                "REFUS_OUVERTURE_DROITS", "ERREUR_CALCUL_REMUNERATION_REFERENCE",
                                "2026-03-01", "2026-04-01",
                                List.of("BULLETIN_PAIE", "ATTESTATION_EMPLOYEUR"),
                                1500.00, false, true))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdictRecommandation").value("RECOURS_HIERARCHIQUE_PRIORITAIRE"))
                .andExpect(jsonPath("$.delaiInstructionMoisPrevisionnel").value(4));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/contestation-are")
                        .with(authentication(authFrDt)))
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
