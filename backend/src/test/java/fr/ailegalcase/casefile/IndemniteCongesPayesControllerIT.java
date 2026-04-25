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
 * SF-DT-26-01 : tests d'integration de l'endpoint Indemnite compensatrice conges
 * payes (art. L.3141-26 et L.3141-28 Code du travail).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class IndemniteCongesPayesControllerIT {

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
    private OAuth2AuthenticationToken authImm;
    private CaseFile travailFrCf;
    private CaseFile travailBeCf;
    private CaseFile immCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u1 = new User(); u1.setEmail("cp-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE");
        u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE");
        a1.setProviderUserId("g-cp-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("CP FR " + ts); ws1.setSlug("ws-cp-fr-" + ts);
        ws1.setOwner(u1); ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE");
        ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE");
        ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember();
        m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true);
        workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("CP FR " + ts);
        travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN");
        travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-cp-fr-" + ts, "cp-fr-" + ts + "@ex.com");

        User u2 = new User(); u2.setEmail("cp-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE");
        u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE");
        a2.setProviderUserId("g-cp-be-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("CP BE " + ts); ws2.setSlug("ws-cp-be-" + ts);
        ws2.setOwner(u2); ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("BELGIQUE");
        ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE");
        ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true);
        workspaceMemberRepository.save(m2);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("CP BE " + ts);
        travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN");
        travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-cp-be-" + ts, "cp-be-" + ts + "@ex.com");

        User u3 = new User(); u3.setEmail("cp-imm-" + ts + "@ex.com"); u3.setStatus("ACTIVE");
        u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE");
        a3.setProviderUserId("g-cp-imm-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("CP IMM " + ts);
        ws3.setSlug("ws-cp-imm-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE");
        ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE");
        ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember();
        m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true);
        workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("CP IMM " + ts);
        immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN");
        immCf = caseFileRepository.save(immCf);
        authImm = buildAuth("g-cp-imm-" + ts, "cp-imm-" + ts + "@ex.com");
    }

    @Test
    void POST_nominal_persists_and_returns200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("totalRemunerationPeriodeEur", 30000.00);
        body.put("joursAcquisAnnee", 25);
        body.put("joursPris", 5);
        body.put("salaireMensuelBrutEur", 2500.00);
        body.put("dateRupture", "2026-04-30");
        body.put("methodeForcee", null);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursDus").value(20))
                .andExpect(jsonPath("$.montantMethodeDixPourcentEur").value(3000.00))
                .andExpect(jsonPath("$.montantMethodeMaintienEur").value(1666.67))
                .andExpect(jsonPath("$.methodeRetenue").value("DIX_POURCENT"))
                .andExpect(jsonPath("$.montantIndemniteEur").value(3000.00))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value("Art. L.3141-26 et L.3141-28 Code du travail"));
    }

    @Test
    void POST_methodeForceeMaintien_appliqueChoix() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 30000.00,
                                "joursAcquisAnnee", 25,
                                "joursPris", 5,
                                "salaireMensuelBrutEur", 2500.00,
                                "methodeForcee", "MAINTIEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methodeRetenue").value("MAINTIEN"))
                .andExpect(jsonPath("$.montantIndemniteEur").value(1666.67));
    }

    @Test
    void POST_upsertReplacesExistingAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 10000.00,
                                "joursAcquisAnnee", 10,
                                "joursPris", 0,
                                "salaireMensuelBrutEur", 2000.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantMethodeDixPourcentEur").value(1000.00));

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 30000.00,
                                "joursAcquisAnnee", 25,
                                "joursPris", 5,
                                "salaireMensuelBrutEur", 2500.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantMethodeDixPourcentEur").value(3000.00));
    }

    @Test
    void POST_totalZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 0,
                                "joursAcquisAnnee", 25,
                                "joursPris", 5,
                                "salaireMensuelBrutEur", 2500.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_joursPrisSuperieurAcquis_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 30000.00,
                                "joursAcquisAnnee", 5,
                                "joursPris", 10,
                                "salaireMensuelBrutEur", 2500.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBE_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 30000.00,
                                "joursAcquisAnnee", 25,
                                "joursPris", 5,
                                "salaireMensuelBrutEur", 2500.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 30000.00,
                                "joursAcquisAnnee", 25,
                                "joursPris", 5,
                                "salaireMensuelBrutEur", 2500.00))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 30000.00,
                                "joursAcquisAnnee", 25,
                                "joursPris", 5,
                                "salaireMensuelBrutEur", 2500.00))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersistedAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totalRemunerationPeriodeEur", 30000.00,
                                "joursAcquisAnnee", 25,
                                "joursPris", 5,
                                "salaireMensuelBrutEur", 2500.00))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRemunerationPeriodeEur").value(30000.00))
                .andExpect(jsonPath("$.montantIndemniteEur").value(3000.00))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/conges-payes-indemnite")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
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
