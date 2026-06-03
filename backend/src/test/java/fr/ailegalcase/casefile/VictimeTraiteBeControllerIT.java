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

/**
 * SF-221-06 — IT titre victime de la traite des êtres humains BE (art. 61/2 et s. Loi
 * 15/12/1980 ; circulaire du 26/09/2008). Couvre 200 nominal (5 verdicts) + gates 400
 * FR / domaine / phase hors whitelist + 404 isolation + GET persistant.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class VictimeTraiteBeControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile immBeCf;
    private CaseFile immFrCf;
    private CaseFile dtBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uBe = save(new User(), u -> { u.setEmail("vt-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-vt-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEVT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEVT " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-vt-be-" + ts, "vt-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("vt-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-vt-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRVT " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRVT " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-vt-fr-" + ts, "vt-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("vt-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-vt-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETVT " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETVT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-vt-dt-" + ts, "vt-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String phase, Boolean rupture, Boolean cooperation,
                                     Boolean accompagnement, String dateDebut) {
        Map<String, Object> m = new HashMap<>();
        m.put("phaseProcedure", phase);
        m.put("ruptureAvecReseau", rupture);
        m.put("cooperationJudiciaire", cooperation);
        m.put("accompagnementCentreSpecialise", accompagnement);
        m.put("dateDebutAccompagnement", dateDebut);
        return m;
    }

    @Test
    void POST_be_eligibleTitreTemporaire_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("DECLARATION_FAITE", true, false, true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_TITRE_TEMPORAIRE"))
                .andExpect(jsonPath("$.etapeProcedure").value("Déclaration faite"));
    }

    @Test
    void POST_be_eligibleSousProcedurePenale_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROCEDURE_PENALE_EN_COURS", true, true, true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_SOUS_PROCEDURE_PENALE"));
    }

    @Test
    void POST_be_delaiReflexion_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("REFLEXION_45J", true, false, true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DELAI_REFLEXION"));
    }

    @Test
    void POST_be_aOrienterCentre_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("AUCUNE", false, false, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ORIENTER_CENTRE"));
    }

    @Test
    void POST_be_conditionsNonReunies_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("DECLARATION_FAITE", false, true, true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONDITIONS_NON_REUNIES"));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("DECLARATION_FAITE", true, false, true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("DECLARATION_FAITE", true, false, true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_phaseHorsWhitelist_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PHASE_INEXISTANTE", true, false, true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_phaseAbsente_returns400() throws Exception {
        Map<String, Object> m = new HashMap<>();
        m.put("ruptureAvecReseau", true);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("DECLARATION_FAITE", true, false, true, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PROCEDURE_PENALE_EN_COURS", true, true, true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_SOUS_PROCEDURE_PENALE"));

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_SOUS_PROCEDURE_PENALE"))
                .andExpect(jsonPath("$.cooperationJudiciaire").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/victime-traite-be-analysis")
                        .with(authentication(authBe)))
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
