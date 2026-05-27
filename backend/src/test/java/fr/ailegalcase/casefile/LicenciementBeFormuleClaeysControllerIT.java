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
 * SF-213-04 : tests d'intégration end-to-end de l'outil
 * licenciement-be-formule-claeys — gate BE-only strict, isolation
 * workspace, persistance et upsert, validation Bean, clause de
 * sauvegarde (art. 67 loi 26/12/2013).
 *
 * <p>Pattern mirroré de {@link LicenciementBeStatutUniquePreavisControllerIT}
 * (SF-213-03).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class LicenciementBeFormuleClaeysControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/licenciement-be-formule-claeys";

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
        User uBe = save(new User(), u -> { u.setEmail("claeys-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-claeys-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-claeys-be-" + ts, "claeys-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(), u -> { u.setEmail("claeys-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-claeys-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-claeys-fr-" + ts, "claeys-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(), u -> { u.setEmail("claeys-o-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-claeys-o-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-claeys-o-" + ts, "claeys-o-" + ts + "@ex.com");
    }

    @Test
    void POST_be_nominalSansSauvegarde_returns200() throws Exception {
        // 12 ans, 60 K€, sans sauvegarde → 50 sem Claeys
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("ancienneteMoisPreStatutUnique", 0);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preavisClaeysMois").value(11.52))
                .andExpect(jsonPath("$.preavisClaeysSemaines").value(50))
                .andExpect(jsonPath("$.preavisStatutUniquesSemaines").value(0))
                .andExpect(jsonPath("$.preavisTotalSemaines").value(50))
                .andExpect(jsonPath("$.appliquerClauseSauvegarde").value(false))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("Formule Claeys")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Loi 03/07/1978")));
    }

    @Test
    void POST_be_avecClauseSauvegarde_cumuleSemaines() throws Exception {
        // 12 ans 60 K€ → 50 Claeys ; 8 ans post-2014 → 24 sem ; total 74
        // salaireHebdo 1000 → indemnité Claeys 50 000 ; totale 74 000
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("ancienneteMoisPreStatutUnique", 0);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", true);
        body.put("ancienneteAnneesPostStatutUnique", 8);
        body.put("salaireHebdomadaireBrut", 1000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preavisClaeysSemaines").value(50))
                .andExpect(jsonPath("$.preavisStatutUniquesSemaines").value(24))
                .andExpect(jsonPath("$.preavisTotalSemaines").value(74))
                .andExpect(jsonPath("$.indemniteClaeysBrute").value(50000.00))
                .andExpect(jsonPath("$.indemniteTotaleBrute").value(74000.00))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("art. 67")));
    }

    @Test
    void POST_be_sauvegardeTrue_sansAnneesPost_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", true);
        body.put("salaireHebdomadaireBrut", 1000.00);
        // ancienneteAnneesPostStatutUnique manquant → 400

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_be_sauvegardeTrue_sansSalaireHebdo_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", true);
        body.put("ancienneteAnneesPostStatutUnique", 8);
        // salaireHebdomadaireBrut manquant → 400

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_ancienneteNegative_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", -1);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationZero_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("remunerationAnnuelleBruteEnMilliers", 0);
        body.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationManquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        // remunerationAnnuelleBruteEnMilliers omise → @NotNull
        body.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("ancienneteAnneesPreStatutUnique", 12);
        first.put("remunerationAnnuelleBruteEnMilliers", 60);
        first.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienneteAnneesPreStatutUnique").value(12));

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("ancienneteAnneesPreStatutUnique", 20);
        second.put("remunerationAnnuelleBruteEnMilliers", 200);
        second.put("appliquerClauseSauvegarde", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienneteAnneesPreStatutUnique").value(20))
                .andExpect(jsonPath("$.preavisClaeysSemaines").value(97));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnneesPreStatutUnique", 12);
        body.put("remunerationAnnuelleBruteEnMilliers", 60);
        body.put("appliquerClauseSauvegarde", true);
        body.put("ancienneteAnneesPostStatutUnique", 8);
        body.put("salaireHebdomadaireBrut", 1000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preavisClaeysSemaines").value(50))
                .andExpect(jsonPath("$.preavisStatutUniquesSemaines").value(24))
                .andExpect(jsonPath("$.preavisTotalSemaines").value(74))
                .andExpect(jsonPath("$.indemniteTotaleBrute").value(74000.00));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404_isolationBE() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
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
