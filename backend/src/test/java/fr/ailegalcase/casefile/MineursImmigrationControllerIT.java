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
class MineursImmigrationControllerIT {

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

        // FR DROIT_IMMIGRATION
        User uFr = save(new User(), u -> { u.setEmail("min-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-min-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-MN " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-MN " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-min-fr-" + ts, "min-fr-" + ts + "@ex.com");

        // BE DROIT_IMMIGRATION (gate country FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("min-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-min-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-MN " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-MN " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-min-be-" + ts, "min-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL (gate domaine → 400)
        User uDt = save(new User(), u -> { u.setEmail("min-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-min-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-MN " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-MN " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-min-dt-" + ts, "min-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dispositif, LocalDate naissance, LocalDate entree,
                                     Boolean parent, Boolean isolement, Boolean op,
                                     String nationalite) {
        Map<String, Object> m = new HashMap<>();
        m.put("dispositifVise", dispositif);
        if (naissance != null) m.put("dateNaissance", naissance.toString());
        if (entree != null) m.put("dateEntreeFrance", entree.toString());
        m.put("parentRegulier", parent);
        m.put("isolementAvere", isolement);
        m.put("motifOrdrePublic", op);
        m.put("nationalite", nationalite);
        return m;
    }

    private Map<String, Object> bodyMnaNominal() {
        LocalDate naissance = LocalDate.now().minusYears(15);
        return body("MNA_ORDONNANCE_JE", naissance, null, false, true, false, "Côte d'Ivoire");
    }

    @Test
    void POST_fr_mnaMineurIsole_returnsELEVEE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMnaNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositifVise").value("MNA_ORDONNANCE_JE"))
                .andExpect(jsonPath("$.verdictEligibilite").value("ELEVEE"))
                .andExpect(jsonPath("$.documentsRequis").isArray())
                .andExpect(jsonPath("$.delaiInstructionMois").value(4))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("Cciv art. 375")));
    }

    @Test
    void POST_fr_l435_3_conditionsRemplies_returnsELEVEE() throws Exception {
        LocalDate naissance = LocalDate.now().minusYears(5);
        Map<String, Object> b = body("TITRE_SEJOUR_L435_3", naissance, naissance,
                true, false, false, "Mali");
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictEligibilite").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiInstructionMois").value(6));
    }

    @Test
    void POST_fr_dcemOrdrePublic_returnsFAIBLE() throws Exception {
        LocalDate naissance = LocalDate.now().minusYears(10);
        Map<String, Object> b = body("DCEM", naissance, null,
                false, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictEligibilite").value("FAIBLE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMnaNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMnaNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMnaNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dispositifInvalide_returns400() throws Exception {
        LocalDate naissance = LocalDate.now().minusYears(10);
        Map<String, Object> b = body("AUTRE_DISPOSITIF", naissance, null,
                false, false, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNaissanceNull_returns400() throws Exception {
        Map<String, Object> b = body("MNA_ORDONNANCE_JE", null, null,
                false, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMnaNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictEligibilite").value("ELEVEE"));

        // mineur isolé NON → FAIBLE
        LocalDate naissance = LocalDate.now().minusYears(15);
        Map<String, Object> next = body("MNA_ORDONNANCE_JE", naissance, null,
                false, false, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictEligibilite").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMnaNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositifVise").value("MNA_ORDONNANCE_JE"))
                .andExpect(jsonPath("$.verdictEligibilite").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_tirApatride_returnsELEVEE() throws Exception {
        LocalDate naissance = LocalDate.now().minusYears(8);
        Map<String, Object> b = body("TIR", naissance, null,
                false, false, false, "APATRIDE");
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mineurs-immigration-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictEligibilite").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiInstructionMois").value(3));
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
