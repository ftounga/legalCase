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
 * SF-206-03 : tests d'intégration des endpoints de chiffrage du rappel de
 * congés payés acquis pendant un arrêt maladie (FRANCE).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CongesPayesArretMaladieControllerIT {

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
        User u1 = new User(); u1.setEmail("cpam-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-cpam-fr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("CPAM FR " + ts); ws1.setSlug("ws-cpam-fr-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        travailFrCf = new CaseFile(); travailFrCf.setTitle("CPAM FR " + ts); travailFrCf.setWorkspace(ws1); travailFrCf.setCreatedBy(u1);
        travailFrCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailFrCf.setStatus("OPEN"); travailFrCf = caseFileRepository.save(travailFrCf);
        authFr = buildAuth("g-cpam-fr-" + ts, "cpam-fr-" + ts + "@ex.com");

        // Workspace B — BE / droit du travail (outil FR-only → 422)
        User u2 = new User(); u2.setEmail("cpam-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-cpam-be-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("CPAM BE " + ts); ws2.setSlug("ws-cpam-be-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("BELGIQUE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("CPAM BE " + ts); travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-cpam-be-" + ts, "cpam-be-" + ts + "@ex.com");

        // Workspace C — FR / immigration (autre workspace + autre domaine)
        User u3 = new User(); u3.setEmail("cpam-o-" + ts + "@ex.com"); u3.setStatus("ACTIVE"); u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE"); a3.setProviderUserId("g-cpam-o-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("CPAMO " + ts); ws3.setSlug("ws-cpam-o-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_IMMIGRATION"); ws3.setCountry("FRANCE"); ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE"); ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember(); m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true); workspaceMemberRepository.save(m3);
        immCf = new CaseFile(); immCf.setTitle("CPAMO " + ts); immCf.setWorkspace(ws3); immCf.setCreatedBy(u3);
        immCf.setLegalDomain("DROIT_IMMIGRATION"); immCf.setStatus("OPEN"); immCf = caseFileRepository.save(immCf);
        authOther = buildAuth("g-cpam-o-" + ts, "cpam-o-" + ts + "@ex.com");
    }

    @Test
    void POST_casReelMaladieNonPro_returns200_rappelSignificatif() throws Exception {
        Map<String, Object> body = bodyMaladieNonPro6Mois();
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursCpAcquis").value(12.00))
                .andExpect(jsonPath("$.joursCpRappel").value(12.00))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.calculatedAt").exists())
                .andExpect(jsonPath("$.dateLimiteAction").exists());
    }

    @Test
    void POST_atMp_18mois_returns200_45JoursSansPlafond() throws Exception {
        Map<String, Object> body = bodyMaladieNonPro6Mois();
        body.put("typeArret", "ACCIDENT_TRAVAIL_MALADIE_PRO");
        body.put("nombreMoisArret", 18);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursCpAcquis").value(45.00));
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        Map<String, Object> body1 = bodyMaladieNonPro6Mois();
        body1.put("nombreMoisArret", 3); // 6 j acquis (3 × 2)
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursCpAcquis").value(6.00));

        Map<String, Object> body2 = bodyMaladieNonPro6Mois();
        body2.put("nombreMoisArret", 12); // 24 j (plafond année 1)
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursCpAcquis").value(24.00));

        mockMvc.perform(get(url(travailFrCf.getId())).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursCpAcquis").value(24.00));
    }

    @Test
    void GET_apresPost_returnsSnapshot() throws Exception {
        Map<String, Object> body = bodyMaladieNonPro6Mois();
        body.put("joursCpDejaAccordes", 5);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(travailFrCf.getId())).with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursCpDejaAccordes").value(5))
                .andExpect(jsonPath("$.joursCpRappel").value(7))
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
                        .content(objectMapper.writeValueAsString(bodyMaladieNonPro6Mois())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        // authOther n'appartient pas au workspace du dossier travailFrCf
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMaladieNonPro6Mois())))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierImmigration_returns422() throws Exception {
        mockMvc.perform(post(url(immCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMaladieNonPro6Mois())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dossierTravailBe_returns422() throws Exception {
        // Outil FR-only — workspace BE = 422 (outil hors pays).
        mockMvc.perform(post(url(travailBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMaladieNonPro6Mois())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dateMalFormee_returns400() throws Exception {
        Map<String, Object> body = bodyMaladieNonPro6Mois();
        body.put("salarieEncoreEnPoste", false);
        body.put("dateRuptureContrat", "06-01-2026"); // format invalide
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nombreMoisNul_returns400() throws Exception {
        Map<String, Object> body = bodyMaladieNonPro6Mois();
        body.put("nombreMoisArret", 0);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireNegatif_returns400() throws Exception {
        Map<String, Object> body = bodyMaladieNonPro6Mois();
        body.put("salaireBrutMensuel", -100);
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salarieSorti_sansDateRupture_returns400() throws Exception {
        Map<String, Object> body = bodyMaladieNonPro6Mois();
        body.put("salarieEncoreEnPoste", false);
        body.remove("dateRuptureContrat");
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(travailFrCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMaladieNonPro6Mois())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(travailFrCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/conges-payes-arret-maladie";
    }

    /** Body de référence — maladie non pro 6 mois, salarié en poste, salaire 3000 €. */
    private Map<String, Object> bodyMaladieNonPro6Mois() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("typeArret", "MALADIE_NON_PROFESSIONNELLE");
        m.put("nombreMoisArret", 6);
        m.put("salarieEncoreEnPoste", true);
        m.put("joursCpDejaAccordes", 0);
        m.put("salaireBrutMensuel", 3000);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
