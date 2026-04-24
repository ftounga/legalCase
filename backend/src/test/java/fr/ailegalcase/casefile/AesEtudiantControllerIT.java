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
class AesEtudiantControllerIT {

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

    /** 4 ans avant aujourd'hui — satisfait condition présence 3 ans. */
    private final LocalDate entreeIlYa4Ans = LocalDate.now().minusYears(4);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uFr = save(new User(), u -> { u.setEmail("aesetu-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-aesetu-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-aesetu-fr-" + ts, "aesetu-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("aesetu-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-aesetu-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-aesetu-be-" + ts, "aesetu-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("aesetu-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-aesetu-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-aesetu-dt-" + ts, "aesetu-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate entree,
                                     Integer dureeMois,
                                     Integer anneesScolarite,
                                     String niveau,
                                     String resultats,
                                     Boolean inscription,
                                     Boolean moyens,
                                     Boolean menace,
                                     Boolean parcours,
                                     LocalDate depot) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateEntreeFrance", entree != null ? entree.toString() : null);
        m.put("dureePresenceMois", dureeMois);
        m.put("anneesScolariteEnFranceConsecutives", anneesScolarite);
        m.put("niveauEtudesActuel", niveau);
        m.put("resultatsAcademiques", resultats);
        m.put("inscriptionEtablissementReconnu", inscription);
        m.put("moyensSubsistance", moyens);
        m.put("menaceOrdrePublic", menace);
        m.put("parcoursCoherent", parcours);
        m.put("dateDepotDemande", depot != null ? depot.toString() : null);
        return m;
    }

    private Map<String, Object> bodyNominal() {
        return body(entreeIlYa4Ans, 48, 3, "BAC_PLUS_1_2", "EXCELLENT",
                true, true, false, true, null);
    }

    @Test
    void POST_fr_nominal_verdictEleveeScore100() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.presence3AnsOk").value(true))
                .andExpect(jsonPath("$.scolarite2AnsConsecutivesOk").value(true))
                .andExpect(jsonPath("$.resultatsAcceptables").value(true))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("Valls")));
    }

    @Test
    void POST_fr_missingConditions_returnsCriteresNonRemplisAndVerdictFaible() throws Exception {
        Map<String, Object> body = body(LocalDate.now().minusYears(1),
                12, 1, "BAC_PLUS_1_2", "DIFFICULTES_REPETEES",
                false, false, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"))
                .andExpect(jsonPath("$.criteresNonRemplis.length()")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }

    @Test
    void POST_fr_niveauBacPlus3_includesVlsTsHint() throws Exception {
        Map<String, Object> body = body(entreeIlYa4Ans, 48, 3, "BAC_PLUS_3_4",
                "EXCELLENT", true, true, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages")
                        .value(org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.containsString("VLS-TS"))));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aes-etudiant")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authDt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_futureDateEntree_returns400() throws Exception {
        Map<String, Object> body = body(LocalDate.now().plusDays(1),
                48, 3, "BAC_PLUS_1_2", "EXCELLENT",
                true, true, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_niveauInvalide_returns400() throws Exception {
        Map<String, Object> body = body(entreeIlYa4Ans, 48, 3, "POSTDOC",
                "EXCELLENT", true, true, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_resultatsInvalide_returns400() throws Exception {
        Map<String, Object> body = body(entreeIlYa4Ans, 48, 3, "BAC_PLUS_1_2",
                "INCONNU", true, true, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));

        Map<String, Object> next = body(entreeIlYa4Ans, 48, 1,
                "BAC_PLUS_1_2", "DIFFICULTES_REPETEES",
                false, false, false, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.niveauEtudesActuel").value("BAC_PLUS_1_2"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/aes-etudiant")
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
