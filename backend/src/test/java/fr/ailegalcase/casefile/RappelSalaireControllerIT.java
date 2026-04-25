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
 * SF-DT-20-01 : tests d'intégration de l'endpoint Rappel de salaire FR
 * (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RappelSalaireControllerIT {

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

        User u1 = new User(); u1.setEmail("rs-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE");
        u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE");
        a1.setProviderUserId("g-rs-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("RS FR " + ts); ws1.setSlug("ws-rs-fr-" + ts);
        ws1.setOwner(u1); ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE");
        ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE");
        ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember();
        m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true);
        workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("RS FR " + ts);
        travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN");
        travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-rs-fr-" + ts, "rs-fr-" + ts + "@ex.com");

        User u2 = new User(); u2.setEmail("rs-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE");
        u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE");
        a2.setProviderUserId("g-rs-be-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("RS BE " + ts); ws2.setSlug("ws-rs-be-" + ts);
        ws2.setOwner(u2); ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("BELGIQUE");
        ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE");
        ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true);
        workspaceMemberRepository.save(m2);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("RS BE " + ts);
        travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN");
        travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-rs-be-" + ts, "rs-be-" + ts + "@ex.com");

        User u3 = new User(); u3.setEmail("rs-imm-" + ts + "@ex.com"); u3.setStatus("ACTIVE");
        u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE");
        a3.setProviderUserId("g-rs-imm-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("RS IMM " + ts);
        ws3.setSlug("ws-rs-imm-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE");
        ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE");
        ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember();
        m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true);
        workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("RS IMM " + ts);
        immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN");
        immCf = caseFileRepository.save(immCf);
        authImm = buildAuth("g-rs-imm-" + ts, "rs-imm-" + ts + "@ex.com");
    }

    private Map<String, Object> nominalBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("periodeDebut", "2023-01-01");
        body.put("periodeFin", "2024-12-31");
        body.put("montantSalaireDuMensuelEur", 2500.00);
        body.put("montantSalairePerVerseMensuelEur", 2200.00);
        body.put("conventionCollectiveCode", null);
        body.put("ancienneteAnneesPrime", 8);
        body.put("indexInseeRevalorise", true);
        body.put("tauxRevalorisationPct", 3.5);
        body.put("methodeCpSurRappel", "DIX_POURCENT");
        return body;
    }

    @Test
    void POST_nominal_persists_and_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbMoisPeriode").value(24))
                .andExpect(jsonPath("$.differentielMensuelEur").value(300.00))
                .andExpect(jsonPath("$.totalRappelBrutHorsRevalorisationEur").value(7200.00))
                .andExpect(jsonPath("$.montantRevalorisationEur").value(252.00))
                .andExpect(jsonPath("$.totalRappelBrutEur").value(7452.00))
                .andExpect(jsonPath("$.congesPayesSurRappelEur").value(745.20))
                .andExpect(jsonPath("$.totalAvecCpEur").value(8197.20))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void POST_indexInseeFalse_pasDeRevalorisation() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("indexInseeRevalorise", false);
        body.put("tauxRevalorisationPct", null);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantRevalorisationEur").value(0.00))
                .andExpect(jsonPath("$.totalRappelBrutEur").value(7200.00))
                .andExpect(jsonPath("$.totalAvecCpEur").value(7920.00));
    }

    @Test
    void POST_methodeAucun_pasDeCp() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("indexInseeRevalorise", false);
        body.put("tauxRevalorisationPct", null);
        body.put("methodeCpSurRappel", "AUCUN");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.congesPayesSurRappelEur").value(0.00))
                .andExpect(jsonPath("$.totalAvecCpEur").value(7200.00));
    }

    @Test
    void POST_upsertReplacesExistingAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk());

        Map<String, Object> body2 = nominalBody();
        body2.put("montantSalaireDuMensuelEur", 3000.00);
        body2.put("indexInseeRevalorise", false);
        body2.put("tauxRevalorisationPct", null);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.differentielMensuelEur").value(800.00))
                .andExpect(jsonPath("$.totalRappelBrutEur").value(19200.00));
    }

    @Test
    void POST_periodeFinAvantDebut_returns400() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("periodeDebut", "2024-12-31");
        body.put("periodeFin", "2023-01-01");
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_duInferieurAVerse_returns400() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("montantSalaireDuMensuelEur", 2000.00);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBE_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/rappel-salaire")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/rappel-salaire")
                        .with(authentication(authImm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersistedAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAvecCpEur").value(8197.20))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/rappel-salaire")
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
