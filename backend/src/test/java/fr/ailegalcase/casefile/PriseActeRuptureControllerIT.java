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
 * SF-206-05 : tests d'intégration des endpoints d'analyse de prise d'acte de
 * la rupture aux torts de l'employeur (FRANCE).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class PriseActeRuptureControllerIT {

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

        // Workspace A — FR / droit du travail
        User u1 = new User(); u1.setEmail("pa-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-pa-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("PA FR " + ts); ws1.setSlug("ws-pa-fr-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("PA FR " + ts); travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN"); travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-pa-fr-" + ts, "pa-fr-" + ts + "@ex.com");

        // Workspace B — BE / droit du travail (outil FR-only → 422)
        User u2 = new User(); u2.setEmail("pa-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-pa-be-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("PA BE " + ts); ws2.setSlug("ws-pa-be-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("BELGIQUE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("PA BE " + ts); travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-pa-be-" + ts, "pa-be-" + ts + "@ex.com");

        // Workspace C — FR / immigration (autre workspace + autre domaine)
        User u3 = new User(); u3.setEmail("pa-o-" + ts + "@ex.com"); u3.setStatus("ACTIVE"); u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE"); a3.setProviderUserId("g-pa-o-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("PAO " + ts); ws3.setSlug("ws-pa-o-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE"); ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE"); ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember(); m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true); workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("PAO " + ts); immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-pa-o-" + ts, "pa-o-" + ts + "@ex.com");
    }

    @Test
    void POST_aucunGrief_returns200_priseActeDefavorable() throws Exception {
        Map<String, Object> body = aucunGriefBody();
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PRISE_ACTE_DEFAVORABLE"))
                .andExpect(jsonPath("$.scoreSolidite").value(0))
                .andExpect(jsonPath("$.griefsRetenus").isEmpty())
                .andExpect(jsonPath("$.effetProbable").value("DEMISSION"))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    void POST_harcelement_returns200_basculeLicenciementNul() throws Exception {
        Map<String, Object> body = aucunGriefBody();
        body.put("harcelement", true);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effetProbable").value("LICENCIEMENT_NUL"))
                .andExpect(jsonPath("$.griefsRetenus[0].code").value("DT39_HARCELEMENT"));
    }

    @Test
    void POST_discrimination_returns200_basculeLicenciementNul() throws Exception {
        Map<String, Object> body = aucunGriefBody();
        body.put("discrimination", true);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effetProbable").value("LICENCIEMENT_NUL"));
    }

    @Test
    void POST_cumulMajeurs_returns200_priseActeFavorable() throws Exception {
        Map<String, Object> body = aucunGriefBody();
        body.put("defautPaiementSalaire", true);
        body.put("montantImpayesEur", 3000);
        body.put("manquementSecurite", true);
        body.put("griefRendImpossiblePoursuite", true);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PRISE_ACTE_FAVORABLE"))
                .andExpect(jsonPath("$.effetProbable").value("LICENCIEMENT_SANS_CAUSE"));
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        Map<String, Object> body1 = aucunGriefBody();
        body1.put("harcelement", true);
        body1.put("manquementSecurite", true);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PRISE_ACTE_FAVORABLE"));

        Map<String, Object> body2 = aucunGriefBody();
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreSolidite").value(0))
                .andExpect(jsonPath("$.verdict").value("PRISE_ACTE_DEFAVORABLE"));

        mockMvc.perform(get(url(travailFrCf.getId())).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreSolidite").value(0));
    }

    @Test
    void GET_apresPost_returnsSnapshot() throws Exception {
        Map<String, Object> body = aucunGriefBody();
        body.put("modificationUnilateraleContrat", true);
        body.put("griefsCommentaire", "Mutation imposée hors clause de mobilité");
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(travailFrCf.getId())).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modificationUnilateraleContrat").value(true))
                .andExpect(jsonPath("$.griefsCommentaire").value("Mutation imposée hors clause de mobilité"))
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
                        .content(objectMapper.writeValueAsString(aucunGriefBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aucunGriefBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierImmigration_returns422() throws Exception {
        mockMvc.perform(post(url(immCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aucunGriefBody())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dossierTravailBe_returns422() throws Exception {
        mockMvc.perform(post(url(travailBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aucunGriefBody())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_montantImpayesNegatif_returns400() throws Exception {
        Map<String, Object> body = aucunGriefBody();
        body.put("defautPaiementSalaire", true);
        body.put("montantImpayesEur", -500);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aucunGriefBody())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(travailFrCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/prise-acte-rupture";
    }

    /** Body de référence — aucun grief coché. */
    private Map<String, Object> aucunGriefBody() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("defautPaiementSalaire", false);
        m.put("harcelement", false);
        m.put("manquementSecurite", false);
        m.put("modificationUnilateraleContrat", false);
        m.put("declassement", false);
        m.put("discrimination", false);
        m.put("heuresSupNonPayees", false);
        m.put("nonRespectDureesRepos", false);
        m.put("griefsActuelsEtPersistants", true);
        m.put("griefRendImpossiblePoursuite", false);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
