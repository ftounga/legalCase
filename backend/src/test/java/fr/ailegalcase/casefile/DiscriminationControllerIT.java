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
class DiscriminationControllerIT {

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
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailFrCf;
    private CaseFile travailBeCf;
    private CaseFile immCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace
        User uFr = save(new User(), u -> { u.setEmail("disc-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-disc-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-disc-fr-" + ts, "disc-fr-" + ts + "@ex.com");

        // BE workspace
        User uBe = save(new User(), u -> { u.setEmail("disc-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-disc-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-disc-be-" + ts, "disc-be-" + ts + "@ex.com");

        // Autre (IMMIGRATION) workspace
        User uOt = save(new User(), u -> { u.setEmail("disc-o-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-disc-o-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uOt, wsOt);
        immCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-disc-o-" + ts, "disc-o-" + ts + "@ex.com");
    }

    @Test
    void POST_fr_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 3000,
                                "motifDiscrimination", "SEXE_GROSSESSE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.fourchetteMin").value(18000.00))
                .andExpect(jsonPath("$.fourchetteMax").value(36000.00))
                .andExpect(jsonPath("$.baseJuridique").value(org.hamcrest.Matchers.containsString("L.1134-5")));
    }

    @Test
    void POST_be_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 2500,
                                "motifDiscrimination", "DISCRIMINATION_RACIALE_BE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.fourchetteMin").value(15000.00));
    }

    @Test
    void POST_motifFr_sur_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 3000,
                                "motifDiscrimination", "SEXE_GROSSESSE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_motifBe_sur_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 3000,
                                "motifDiscrimination", "DISCRIMINATION_RACIALE_BE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 0,
                                "motifDiscrimination", "SEXE_GROSSESSE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_immigrationCaseFile_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 3000,
                                "motifDiscrimination", "SEXE_GROSSESSE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 3000,
                                "motifDiscrimination", "SEXE_GROSSESSE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 3000,
                                "motifDiscrimination", "SEXE_GROSSESSE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 4000,
                                "motifDiscrimination", "AGE",
                                "contexteActe", "PROMOTION_REFUSEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifDiscrimination").value("AGE"))
                .andExpect(jsonPath("$.contexteActe").value("PROMOTION_REFUSEE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "salaireMensuelReference", 3000,
                                "motifDiscrimination", "SEXE_GROSSESSE",
                                "contexteActe", "LICENCIEMENT"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fourchetteMin").value(18000.00));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/discrimination-dommages-interets")
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
