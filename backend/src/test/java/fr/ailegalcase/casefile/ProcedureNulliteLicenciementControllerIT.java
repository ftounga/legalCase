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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-DT-36-01 : tests d'intégration des endpoints d'analyse des nullités de
 * procédure de licenciement (FRANCE).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ProcedureNulliteLicenciementControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailFrCf;
    private CaseFile immCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace A — FR / droit du travail
        User u1 = new User(); u1.setEmail("pnl-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-pnl-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("PNL FR " + ts); ws1.setSlug("ws-pnl-fr-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("PNL FR " + ts); travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN"); travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-pnl-fr-" + ts, "pnl-fr-" + ts + "@ex.com");

        // Workspace B — FR / immigration (autre workspace + autre domaine)
        User u3 = new User(); u3.setEmail("pnl-o-" + ts + "@ex.com"); u3.setStatus("ACTIVE"); u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE"); a3.setProviderUserId("g-pnl-o-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("PNLO " + ts); ws3.setSlug("ws-pnl-o-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE"); ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE"); ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember(); m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true); workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("PNLO " + ts); immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-pnl-o-" + ts, "pnl-o-" + ts + "@ex.com");
    }

    @Test
    void POST_procedureReguliere_returns200() throws Exception {
        Map<String, Object> body = procedureReguliere();
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PROCEDURE_REGULIERE"))
                .andExpect(jsonPath("$.scoreNullite").value(0))
                .andExpect(jsonPath("$.vicesDetectes").isEmpty())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    void POST_vicesGraves_returns200_nulliteAveree() throws Exception {
        Map<String, Object> body = procedureReguliere();
        body.put("lettreLicenciementEcrite", false);
        body.put("lettreMotivee", false);
        body.put("motivationSuffisante", null);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NULLITE_AVEREE"))
                .andExpect(jsonPath("$.scoreNullite").value(60))
                .andExpect(jsonPath("$.vicesDetectes.length()").value(2));
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        Map<String, Object> body1 = procedureReguliere();
        body1.put("convocationEnvoyee", false);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreNullite").value(30));

        Map<String, Object> body2 = procedureReguliere();
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreNullite").value(0))
                .andExpect(jsonPath("$.verdict").value("PROCEDURE_REGULIERE"));

        mockMvc.perform(get(url(travailFrCf.getId())).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreNullite").value(0));
    }

    @Test
    void GET_apresPost_returnsSnapshot() throws Exception {
        Map<String, Object> body = procedureReguliere();
        body.put("entretienTenu", false);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(travailFrCf.getId())).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entretienTenu").value(false))
                .andExpect(jsonPath("$.vicesDetectes[0].code").value("ABSENCE_ENTRETIEN"))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(travailFrCf.getId())).with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierInexistant_returns404() throws Exception {
        mockMvc.perform(post(url(UUID.randomUUID()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureReguliere())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        // authOther n'appartient pas au workspace du dossier travailFrCf
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureReguliere())))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierImmigration_returns422() throws Exception {
        mockMvc.perform(post(url(immCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureReguliere())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dateMalFormee_returns400() throws Exception {
        Map<String, Object> body = procedureReguliere();
        body.put("dateConvocationPresentee", "06-01-2025"); // format invalide
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureReguliere())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(travailFrCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/procedure-nullite-licenciement";
    }

    /** Procédure régulière de référence (cf. test unitaire). */
    private Map<String, Object> procedureReguliere() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("convocationEnvoyee", true);
        m.put("dateConvocationPresentee", "2025-01-06");
        m.put("dateEntretienPrealable", "2025-01-13");
        m.put("entretienTenu", true);
        m.put("dateNotificationLicenciement", "2025-01-17");
        m.put("lettreLicenciementEcrite", true);
        m.put("lettreMotivee", true);
        m.put("motivationSuffisante", true);
        m.put("licenciementPourMotifGrave", false);
        m.put("licenciementCollectif", false);
        m.put("conventionCollectiveApplicable", false);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
