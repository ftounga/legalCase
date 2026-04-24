package fr.ailegalcase.casefile;

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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-136-02 : tests d'intégration de {@link TravailProcedureController}.
 * Couvre le cas nominal FR + BE, le calcul de date cible, la validation
 * d'erreurs (typeProcedure invalide, dateDeclencheur mal formée) et
 * l'isolation workspace.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class TravailProcedureControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authBe;
    private CaseFile dtFrCf;
    private CaseFile dtBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_DU_TRAVAIL
        User uFr = save(new User(), u -> { u.setEmail("tp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-tp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        dtFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-tp-fr-" + ts, "tp-fr-" + ts + "@ex.com");

        // BE workspace DROIT_DU_TRAVAIL
        User uBe = save(new User(), u -> { u.setEmail("tp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-tp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        dtBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-tp-be-" + ts, "tp-be-" + ts + "@ex.com");
    }

    @Test
    void GET_nominalFr_returnsJalonsForPrudhommes() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/travail-procedure-jalons")
                        .param("typeProcedure", "PRUDHOMMES_FR")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].code").value("PRUDHOMMES_FR"))
                .andExpect(jsonPath("$[0].offsetDays").value(45))
                .andExpect(jsonPath("$[0].articleRef")
                        .value(org.hamcrest.Matchers.containsString("R.1452-3")))
                .andExpect(jsonPath("$[0].dateCible").doesNotExist());
    }

    @Test
    void GET_nominalBe_returnsJalonsForTribunalTravail() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtBeCf.getId() + "/travail-procedure-jalons")
                        .param("typeProcedure", "TRIBUNAL_TRAVAIL_BE")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].code").value("TRIBUNAL_TRAVAIL_BE"))
                .andExpect(jsonPath("$[0].offsetDays").value(30))
                .andExpect(jsonPath("$[0].articleRef")
                        .value(org.hamcrest.Matchers.containsString("CJ Art. 700")));
    }

    @Test
    void GET_withDateDeclencheur_computesDateCible() throws Exception {
        LocalDate pivot = LocalDate.of(2026, 1, 1);
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/travail-procedure-jalons")
                        .param("typeProcedure", "APPEL_CA_SOCIALE_FR")
                        .param("dateDeclencheur", pivot.toString())
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].offsetDays").value(30))
                .andExpect(jsonPath("$[0].dateCible").value(pivot.plusDays(30).toString()));
    }

    @Test
    void GET_invalidTypeProcedure_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/travail-procedure-jalons")
                        .param("typeProcedure", "BIDON")
                        .with(authentication(authFr)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_invalidDateDeclencheur_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/travail-procedure-jalons")
                        .param("typeProcedure", "PRUDHOMMES_FR")
                        .param("dateDeclencheur", "not-a-date")
                        .with(authentication(authFr)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_otherWorkspace_returns404() throws Exception {
        // authFr tente d'accéder au case file BE (workspace différent) → 404 (isolation).
        mockMvc.perform(get("/api/v1/case-files/" + dtBeCf.getId() + "/travail-procedure-jalons")
                        .param("typeProcedure", "TRIBUNAL_TRAVAIL_BE")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_missingTypeProcedure_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/travail-procedure-jalons")
                        .with(authentication(authFr)))
                .andExpect(status().isBadRequest());
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
