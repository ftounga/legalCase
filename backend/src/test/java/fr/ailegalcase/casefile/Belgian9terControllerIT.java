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
class Belgian9terControllerIT {

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

    private final LocalDate symptomesIlYa1An = LocalDate.now().minusYears(1);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // BE immigration
        User uBe = save(new User(), u -> { u.setEmail("b9ter-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-b9ter-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE9 " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBE9 " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-b9ter-be-" + ts, "b9ter-be-" + ts + "@ex.com");

        // FR immigration — gate country BE → 400
        User uFr = save(new User(), u -> { u.setEmail("b9ter-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-b9ter-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR9 " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFR9 " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-b9ter-fr-" + ts, "b9ter-fr-" + ts + "@ex.com");

        // BE droit du travail — gate legal_domain → 400
        User uDt = save(new User(), u -> { u.setEmail("b9ter-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-b9ter-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBET " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBET " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-b9ter-dt-" + ts, "b9ter-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate dateDebutSymptomes,
                                     Boolean maladieGraveCertifiee,
                                     Boolean soinsNecessairesDisponiblesBe,
                                     Boolean soinsInaccessiblesPaysOrigine,
                                     Boolean menaceOrdrePublic,
                                     LocalDate dateDepotDemande) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutSymptomes", dateDebutSymptomes != null ? dateDebutSymptomes.toString() : null);
        m.put("maladieGraveCertifiee", maladieGraveCertifiee);
        m.put("soinsNecessairesDisponiblesBe", soinsNecessairesDisponiblesBe);
        m.put("soinsInaccessiblesPaysOrigine", soinsInaccessiblesPaysOrigine);
        m.put("menaceOrdrePublic", menaceOrdrePublic);
        m.put("dateDepotDemande", dateDepotDemande != null ? dateDepotDemande.toString() : null);
        return m;
    }

    private Map<String, Object> bodyNominalElevee() {
        return body(symptomesIlYa1An, true, true, true, false, null);
    }

    @Test
    void POST_be_allConditionsMet_verdictEleve() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.certificatMedicalType1Ok").value(true))
                .andExpect(jsonPath("$.soinsRequisOk").value(true))
                .andExpect(jsonPath("$.inaccessibiliteOk").value(true))
                .andExpect(jsonPath("$.pasMenace").value(true))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("art. 9ter")));
    }

    @Test
    void POST_be_soinsInaccessibiliteNon_verdictFaible() throws Exception {
        Map<String, Object> body = body(null, false, false, false, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(25))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void POST_be_menaceOrdrePublic_score75() throws Exception {
        Map<String, Object> body = body(null, true, true, true, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasMenace").value(false))
                .andExpect(jsonPath("$.scoreGlobal").value(75))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/belgian-9ter")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe → dossier FR → isolation 404
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_futureDateDebutSymptomes_returns400() throws Exception {
        Map<String, Object> body = body(LocalDate.now().plusDays(2), true, true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));

        Map<String, Object> next = body(null, false, false, false, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(25))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/belgian-9ter")
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
