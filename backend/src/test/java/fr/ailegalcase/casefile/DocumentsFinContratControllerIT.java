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

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DocumentsFinContratControllerIT {

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

        // FR — droit du travail
        User u1 = new User(); u1.setEmail("dfc-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-dfc-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("DFC FR " + ts); ws1.setSlug("ws-dfc-fr-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("DFC FR " + ts); travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN"); travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-dfc-fr-" + ts, "dfc-fr-" + ts + "@ex.com");

        // BE — droit du travail (doit refuser, FR seulement)
        User u2 = new User(); u2.setEmail("dfc-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-dfc-be-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("DFC BE " + ts); ws2.setSlug("ws-dfc-be-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("BELGIQUE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("DFC BE " + ts); travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-dfc-be-" + ts, "dfc-be-" + ts + "@ex.com");

        // FR — immigration (gate domaine)
        User u3 = new User(); u3.setEmail("dfc-o-" + ts + "@ex.com"); u3.setStatus("ACTIVE"); u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE"); a3.setProviderUserId("g-dfc-o-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("DFCO " + ts); ws3.setSlug("ws-dfc-o-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE"); ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE"); ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember(); m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true); workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("DFCO " + ts); immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-dfc-o-" + ts, "dfc-o-" + ts + "@ex.com");
    }

    @Test
    void POST_nominalFR_documentsParfaits_returns200_score100Faible() throws Exception {
        Map<String, Object> body = bodyComplet();
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreConformiteEmployeur").value(100))
                .andExpect(jsonPath("$.verdictRisqueContentieux").value("FAIBLE"))
                .andExpect(jsonPath("$.certificatRemisDansDelai").value(true))
                .andExpect(jsonPath("$.attestationRemiseDansDelai").value(true))
                .andExpect(jsonPath("$.souldeToutCompteValide").value(true))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("L.1234-19")));
    }

    @Test
    void POST_nominalFR_partiel_returns200_scoreReduit() throws Exception {
        // Certificat OK, attestation absente, STC absent
        Map<String, Object> body = bodyMinimal();
        body.put("certificatTravailRemis", true);
        body.put("dateCertificatTravail", "2026-04-15");
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                // Cert 35 + STC absent +30 = 65
                .andExpect(jsonPath("$.scoreConformiteEmployeur").value(65))
                .andExpect(jsonPath("$.verdictRisqueContentieux").value("MOYEN"));
    }

    @Test
    void POST_aucunDocument_returns200_score30Eleve() throws Exception {
        Map<String, Object> body = bodyMinimal();
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreConformiteEmployeur").value(30))
                .andExpect(jsonPath("$.verdictRisqueContentieux").value("ELEVE"))
                .andExpect(jsonPath("$.totalSanctionsCumulables").value(2));
    }

    @Test
    void POST_workspaceBE_returns400() throws Exception {
        Map<String, Object> body = bodyMinimal();
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dossierImmigration_returns400() throws Exception {
        Map<String, Object> body = bodyMinimal();
        mockMvc.perform(post("/api/v1/case-files/" + immCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = bodyMinimal();
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_salaireZero_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateFinContrat", "2026-04-15");
        body.put("salaireMensuelBrutEur", 0);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFinContratManquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("salaireMensuelBrutEur", 3000);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsSnapshot() throws Exception {
        Map<String, Object> body = bodyComplet();
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salaireMensuelBrutEur").value(3000.00))
                .andExpect(jsonPath("$.scoreConformiteEmployeur").value(100))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_remplaceAnalyse() throws Exception {
        Map<String, Object> body1 = bodyMinimal();
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreConformiteEmployeur").value(30));

        Map<String, Object> body2 = bodyComplet();
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/documents-fin-contrat")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreConformiteEmployeur").value(100));
    }

    private Map<String, Object> bodyMinimal() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dateFinContrat", "2026-04-15");
        m.put("salaireMensuelBrutEur", 3000);
        return m;
    }

    private Map<String, Object> bodyComplet() {
        Map<String, Object> m = bodyMinimal();
        m.put("certificatTravailRemis", true);
        m.put("dateCertificatTravail", "2026-04-17");
        m.put("attestationFranceTravailRemise", true);
        m.put("dateAttestationFranceTravail", "2026-04-18");
        m.put("souldeToutCompteSigne", true);
        m.put("dateSouldeToutCompte", "2026-04-20");
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
