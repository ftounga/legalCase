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
import java.time.LocalDate;
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
 * SF-217-18 : tests d'intégration des endpoints d'analyse de recevabilité d'une
 * contestation de filiation BE (BELGIQUE).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ContestationFiliationBeControllerIT {

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
    /** Dossier BE / droit de la famille — cas nominal. */
    private CaseFile familleBeCf;
    /** Dossier BE / droit du travail — domaine non famille (422). */
    private CaseFile travailBeCf;
    /** Dossier FR / droit de la famille — pays non Belgique (422). */
    private CaseFile familleFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace A — BELGIQUE / droit de la famille
        User u1 = new User(); u1.setEmail("cfbe-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-cfbe-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("CFBE " + ts); ws1.setSlug("ws-cfbe-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("BELGIQUE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        familleBeCf = new CaseFile(); familleBeCf.setTitle("CFBE famille BE " + ts); familleBeCf.setWorkspace(ws1); familleBeCf.setCreatedBy(u1);
        familleBeCf.setLegalDomain("DROIT_FAMILLE"); familleBeCf.setStatus("OPEN"); familleBeCf = caseFileRepository.save(familleBeCf);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("CFBE travail BE " + ts); travailBeCf.setWorkspace(ws1); travailBeCf.setCreatedBy(u1);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-cfbe-" + ts, "cfbe-" + ts + "@ex.com");

        // Workspace B — FRANCE / droit de la famille (autre workspace + pays non BE)
        User u2 = new User(); u2.setEmail("cffr-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-cffr-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("CFFR " + ts); ws2.setSlug("ws-cffr-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_FAMILLE"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        familleFrCf = new CaseFile(); familleFrCf.setTitle("CFFR famille FR " + ts); familleFrCf.setWorkspace(ws2); familleFrCf.setCreatedBy(u2);
        familleFrCf.setLegalDomain("DROIT_FAMILLE"); familleFrCf.setStatus("OPEN"); familleFrCf = caseFileRepository.save(familleFrCf);
        authOther = buildAuth("g-cffr-" + ts, "cffr-" + ts + "@ex.com");
    }

    @Test
    void POST_casNominal_returns200_actionRecevable() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ACTION_RECEVABLE"))
                .andExpect(jsonPath("$.voieProcedurale").value("REQUETE_TRIBUNAL_FAMILLE"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.calculatedAt").exists())
                .andExpect(jsonPath("$.dateLimiteAction").exists())
                .andExpect(jsonPath("$.basesJuridiques.length()").isNotEmpty());
    }

    @Test
    void POST_possessionEtat6Ans_returns200_irrecevablePossessionEtat() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("possessionEtatConforme", true);
        body.put("dureePossessionEtatAnnees", 6);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("IRRECEVABLE_POSSESSION_ETAT"))
                .andExpect(jsonPath("$.voieProcedurale").value("AUCUNE_ACTION_RECEVABLE"))
                .andExpect(jsonPath("$.motifsIrrecevabilite[?(@.code == 'POSSESSION_ETAT_CONFORME_5_ANS')]")
                        .isNotEmpty());
    }

    @Test
    void POST_connaissanceIlYa2Ans_returns200_irrecevableDelaiDepasse() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateConnaissanceFaitContestation", LocalDate.now().minusYears(2).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("IRRECEVABLE_DELAI_DEPASSE"))
                .andExpect(jsonPath("$.delaiStatut").value("DEPASSE"))
                .andExpect(jsonPath("$.motifsIrrecevabilite[?(@.code == 'DELAI_FORCLUSION')]").isNotEmpty());
    }

    @Test
    void POST_connaissance350jours_returns200_actionRecevableDelaiCritique() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateConnaissanceFaitContestation", LocalDate.now().minusDays(350).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ACTION_RECEVABLE_DELAI_CRITIQUE"))
                .andExpect(jsonPath("$.delaiStatut").value("CRITIQUE"));
    }

    @Test
    void POST_maternite_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("natureActionFiliation", "MATERNITE");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        // 1er calcul : action recevable
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ACTION_RECEVABLE"));

        // Recalcul : possession d'état 6 ans → irrecevable
        Map<String, Object> body2 = baseBody();
        body2.put("possessionEtatConforme", true);
        body2.put("dureePossessionEtatAnnees", 6);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("IRRECEVABLE_POSSESSION_ETAT"));

        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("IRRECEVABLE_POSSESSION_ETAT"));
    }

    @Test
    void GET_apresPost_returnsSnapshotInputsInclus() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("commentaire", "Père présumé constate divergence ADN — action envisagée");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.natureActionFiliation").value("PATERNITE_PRESUMEE_MARI"))
                .andExpect(jsonPath("$.qualiteDemandeur").value("PERE_PRESUME_OU_RECONNAISSANT"))
                .andExpect(jsonPath("$.dateNaissanceEnfant").exists())
                .andExpect(jsonPath("$.commentaire").value(
                        "Père présumé constate divergence ADN — action envisagée"))
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
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    void GET_autreWorkspace_returns403() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authOther)))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierNonFamille_returns422() throws Exception {
        mockMvc.perform(post(url(travailBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_paysNonBelgique_returns422() throws Exception {
        mockMvc.perform(post(url(familleFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dateConnaissanceAvantNaissance_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateConnaissanceFaitContestation", "2020-01-01"); // bien avant la naissance
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateConnaissanceFuture_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateConnaissanceFaitContestation", LocalDate.now().plusDays(1).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNaissanceMalFormee_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateNaissanceEnfant", "12-03-2023"); // format invalide
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_commentaireTropLong_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("commentaire", "x".repeat(1001));
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_corpsAbsent_returns400() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/contestation-filiation-be";
    }

    /**
     * Body de référence : père présumé, enfant né en 2023, connaissance récente
     * (30 j), pas de possession d'état, ADN disponible.
     */
    private Map<String, Object> baseBody() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("natureActionFiliation", "PATERNITE_PRESUMEE_MARI");
        m.put("qualiteDemandeur", "PERE_PRESUME_OU_RECONNAISSANT");
        m.put("dateNaissanceEnfant", "2023-01-15");
        m.put("dateConnaissanceFaitContestation", LocalDate.now().minusDays(30).toString());
        m.put("possessionEtatConforme", false);
        m.put("dureePossessionEtatAnnees", 0);
        m.put("expertiseAdnDisponible", true);
        m.put("demandeExpertiseAdnEnvisagee", false);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
