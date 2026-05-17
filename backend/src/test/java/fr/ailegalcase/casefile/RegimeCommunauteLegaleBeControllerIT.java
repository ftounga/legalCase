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
import java.util.ArrayList;
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
 * SF-217-01 : tests d'intégration des endpoints d'analyse du régime de communauté
 * légale belge (BELGIQUE / droit de la famille).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RegimeCommunauteLegaleBeControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authOther;
    private CaseFile familleBeCf;     // workspace A — BE / droit famille
    private CaseFile familleFrCf;     // workspace B — FR / droit famille (gate pays)
    private CaseFile travailBeCf;     // workspace B — BE / droit du travail (gate domaine)

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace A — BE / droit de la famille
        User u1 = new User(); u1.setEmail("rcl-be-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-rcl-be-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("RCL BE " + ts); ws1.setSlug("ws-rcl-be-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("BELGIQUE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        familleBeCf = new CaseFile(); familleBeCf.setTitle("RCL BE " + ts); familleBeCf.setWorkspace(ws1); familleBeCf.setCreatedBy(u1);
        familleBeCf.setLegalDomain("DROIT_FAMILLE"); familleBeCf.setStatus("OPEN"); familleBeCf = caseFileRepository.save(familleBeCf);
        authBe = buildAuth("g-rcl-be-" + ts, "rcl-be-" + ts + "@ex.com");

        // Workspace B — FR / droit famille + BE / droit du travail (autre workspace)
        User u2 = new User(); u2.setEmail("rcl-o-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-rcl-o-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("RCLO " + ts); ws2.setSlug("ws-rcl-o-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_FAMILLE"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        familleFrCf = new CaseFile(); familleFrCf.setTitle("RCL FR " + ts); familleFrCf.setWorkspace(ws2); familleFrCf.setCreatedBy(u2);
        familleFrCf.setLegalDomain("DROIT_FAMILLE"); familleFrCf.setStatus("OPEN"); familleFrCf = caseFileRepository.save(familleFrCf);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("RCL Travail " + ts); travailBeCf.setWorkspace(ws2); travailBeCf.setCreatedBy(u2);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authOther = buildAuth("g-rcl-o-" + ts, "rcl-o-" + ts + "@ex.com");
    }

    @Test
    void POST_communauteLegale_returns200() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("COMMUNAUTE_LEGALE_APPLICABLE"))
                .andExpect(jsonPath("$.biensQualifies[0].qualification").value("COMMUN"))
                .andExpect(jsonPath("$.biensQualifies[0].modeGestion").value("COGESTION"))
                .andExpect(jsonPath("$.dettesQualifiees[0].qualification").value("DETTE_COMMUNE"))
                .andExpect(jsonPath("$.syntheseComposition.nbBiensCommuns").value(1))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    void POST_contratDeMariageSigne_returns200_regimeConventionnel() throws Exception {
        Map<String, Object> body = communauteSimple();
        body.put("contratMariageSigne", true);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("REGIME_CONVENTIONNEL_DETECTE"));
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        Map<String, Object> body1 = communauteSimple();
        body1.put("contratMariageSigne", true);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("REGIME_CONVENTIONNEL_DETECTE"));

        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("COMMUNAUTE_LEGALE_APPLICABLE"));

        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("COMMUNAUTE_LEGALE_APPLICABLE"));
    }

    @Test
    void GET_apresPost_returnsSnapshot() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateMariage").value("2015-06-20"))
                .andExpect(jsonPath("$.contratMariageSigne").value(false))
                .andExpect(jsonPath("$.biens[0].libelle").value("Appartement Schaerbeek"))
                .andExpect(jsonPath("$.biensQualifies[0].qualification").value("COMMUN"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierInexistant_returns404() throws Exception {
        mockMvc.perform(post(url(UUID.randomUUID()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        // authOther n'appartient pas au workspace du dossier familleBeCf
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierNonFamille_returns422() throws Exception {
        mockMvc.perform(post(url(travailBeCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dossierPaysNonBelgique_returns422() throws Exception {
        mockMvc.perform(post(url(familleFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_listeBiensVide_returns400() throws Exception {
        Map<String, Object> body = communauteSimple();
        body.put("biens", new ArrayList<>());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateMalFormee_returns400() throws Exception {
        Map<String, Object> body = communauteSimple();
        body.put("dateMariage", "20-06-2015"); // format invalide
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(communauteSimple())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/regime-mat-be-communaute-legale";
    }

    /** Patrimoine de référence : un appartement acquêt + un emprunt hypothécaire. */
    private Map<String, Object> communauteSimple() {
        Map<String, Object> bien = new LinkedHashMap<>();
        bien.put("libelle", "Appartement Schaerbeek");
        bien.put("categorie", "IMMOBILIER");
        bien.put("acquisAvantMariage", false);
        bien.put("acquisParDonationOuSuccession", false);
        bien.put("financeParFondsPropres", false);
        bien.put("affectationProfessionnelle", false);
        bien.put("outilDeTravailPersonnel", false);

        Map<String, Object> dette = new LinkedHashMap<>();
        dette.put("libelle", "Emprunt hypothécaire appartement");
        dette.put("anterieureAuMariage", false);
        dette.put("contracteeDansLInteretDuMenage", true);
        dette.put("contracteeParUnSeulEpoux", false);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dateMariage", "2015-06-20");
        m.put("contratMariageSigne", false);
        m.put("biens", new ArrayList<>(List.of(bien)));
        m.put("dettes", new ArrayList<>(List.of(dette)));
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
