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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-213-01 : tests d'intégration end-to-end de l'outil
 * clause-non-concurrence-be — gate BE-only strict, isolation workspace,
 * persistance et upsert, validation Bean.
 *
 * <p>Pattern mirroré de {@link MotifGraveBeControllerIT} (SF-DT-27-01).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ClauseNonConcurrenceBeControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailBeCf;
    private CaseFile travailFrCf;
    private CaseFile immBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // BE workspace DROIT_DU_TRAVAIL
        User uBe = save(new User(), u -> { u.setEmail("cnc-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cnc-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-cnc-be-" + ts, "cnc-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(), u -> { u.setEmail("cnc-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cnc-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-cnc-fr-" + ts, "cnc-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(), u -> { u.setEmail("cnc-o-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-cnc-o-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-cnc-o-" + ts, "cnc-o-" + ts + "@ex.com");
    }

    @Test
    void POST_be_nominal_valide_returns200() throws Exception {
        // 100 000 €/an × 6 mois → VALIDE + indemnité = 25 000 €
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 100000.00);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");
        body.put("activiteInternationaleProuvee", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.raisonNullite").doesNotExist())
                .andExpect(jsonPath("$.indemniteLegale").value(25000.00))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Loi 03/07/1978")))
                .andExpect(jsonPath("$.indemniteLegaleFormule").value(
                        org.hamcrest.Matchers.containsString("25000.00")));
    }

    @Test
    void POST_be_remunerationSousSeuil_returnsNulle() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 50000.00);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NULLE"))
                .andExpect(jsonPath("$.raisonNullite").value("REMUNERATION_INSUFFISANTE"));
    }

    @Test
    void POST_be_zoneInternationaleSansActivite_returnsPartiellementNulle() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 100000.00);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_ET_ETRANGER");
        body.put("activiteInternationaleProuvee", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PARTIELLEMENT_NULLE"))
                .andExpect(jsonPath("$.raisonNullite").value("ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE"));
    }

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 100000.00);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 100000.00);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation workspace)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 100000.00);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_remunerationNegative_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", -1);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeMoisManquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 100000.00);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");
        // dureeMois omise → Bean Validation @NotNull

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("remunerationAnnuelleBrute", 100000.00);
        first.put("dureeMois", 6);
        first.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeMois").value(6));

        // Deuxième POST avec autres inputs : upsert → on remplace
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("remunerationAnnuelleBrute", 80000.00);
        second.put("dureeMois", 12);
        second.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeMois").value(12))
                .andExpect(jsonPath("$.remunerationAnnuelleBrute").value(80000.00));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("remunerationAnnuelleBrute", 100000.00);
        body.put("dureeMois", 6);
        body.put("zoneGeographique", "BELGIQUE_UNIQUEMENT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.indemniteLegale").value(25000.00));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404_isolationBE() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId()
                                + "/decision-tools/clause-non-concurrence-be")
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
