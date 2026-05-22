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
class IndivisionSuccessoraleControllerIT {

    private static final String ENDPOINT = "/api/v1/case-files/%s/indivision-successorale-analysis";

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

        User uFr = save(new User(), u -> { u.setEmail("isucc-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-isucc-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSISUCCFR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-isucc-fr-" + ts, "isucc-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("isucc-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-isucc-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSISUCCBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-isucc-be-" + ts, "isucc-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("isucc-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-isucc-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSISUCCDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-isucc-dt-" + ts, "isucc-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate ouverture,
                                     String typeIndivision,
                                     Integer nbHeritiers,
                                     Number valeurPatrimoine,
                                     Number valeurBienOccupe,
                                     Boolean consentements,
                                     Boolean occupation,
                                     Boolean actesContestes,
                                     Boolean demandePartage) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateOuvertureSuccession", ouverture != null ? ouverture.toString() : null);
        m.put("typeIndivision", typeIndivision);
        m.put("nbHeritiers", nbHeritiers);
        m.put("valeurPatrimoineIndivisEur", valeurPatrimoine);
        m.put("valeurBienOccupeEur", valeurBienOccupe);
        m.put("consentementsTous", consentements);
        m.put("occupationExclusive", occupation);
        m.put("actesAdministrationContestes", actesContestes);
        m.put("demandePartage", demandePartage);
        return m;
    }

    private Map<String, Object> bodyHarmonieuse() {
        return body(LocalDate.now().minusMonths(18),
                "INDIVISION_LEGALE", 3, 200000, 0,
                true, false, false, false);
    }

    private Map<String, Object> bodyBlocage() {
        return body(LocalDate.now().minusMonths(24),
                "INDIVISION_LEGALE", 4, 500000, 250000,
                false, true, true, true);
    }

    @Test
    void POST_fr_harmonieuse_retourneConvention() throws Exception {
        mockMvc.perform(post(String.format(ENDPOINT, famFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHarmonieuse())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdictGestion").value("HARMONIEUSE"))
                .andExpect(jsonPath("$.dispositifRecommande").value("CONVENTION_INDIVISION_5_ANS"))
                .andExpect(jsonPath("$.indemniteOccupationDue").value(false))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("815")));
    }

    @Test
    void POST_fr_blocage_retournePartageJudiciaire() throws Exception {
        mockMvc.perform(post(String.format(ENDPOINT, famFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyBlocage())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictGestion").value("BLOCAGE"))
                .andExpect(jsonPath("$.dispositifRecommande").value("PARTAGE_JUDICIAIRE"))
                .andExpect(jsonPath("$.indemniteOccupationDue").value(true));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post(String.format(ENDPOINT, famBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHarmonieuse())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(ENDPOINT, dtFrCf.getId()))
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHarmonieuse())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(ENDPOINT, famBeCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHarmonieuse())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateFuture_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.now().plusDays(2),
                "INDIVISION_LEGALE", 3, 200000, 0,
                true, false, false, false);
        mockMvc.perform(post(String.format(ENDPOINT, famFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post(String.format(ENDPOINT, famFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHarmonieuse())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictGestion").value("HARMONIEUSE"));

        mockMvc.perform(post(String.format(ENDPOINT, famFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyBlocage())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictGestion").value("BLOCAGE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(ENDPOINT, famFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHarmonieuse())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(ENDPOINT, famFrCf.getId()))
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdictGestion").value("HARMONIEUSE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(ENDPOINT, famFrCf.getId()))
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
