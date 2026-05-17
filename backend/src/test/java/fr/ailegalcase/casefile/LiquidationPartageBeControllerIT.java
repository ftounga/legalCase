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
 * SF-217-02 : tests d'intégration des endpoints de suivi de la procédure de
 * liquidation-partage belge (notaire commis — BELGIQUE).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class LiquidationPartageBeControllerIT {

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
        User u1 = new User(); u1.setEmail("lpbe-be-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-lpbe-be-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("LPBE BE " + ts); ws1.setSlug("ws-lpbe-be-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("BELGIQUE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        familleBeCf = new CaseFile(); familleBeCf.setTitle("LPBE famille BE " + ts); familleBeCf.setWorkspace(ws1); familleBeCf.setCreatedBy(u1);
        familleBeCf.setLegalDomain("DROIT_FAMILLE"); familleBeCf.setStatus("OPEN"); familleBeCf = caseFileRepository.save(familleBeCf);
        // Dossier droit du travail dans le même workspace BE → domaine non famille
        travailBeCf = new CaseFile(); travailBeCf.setTitle("LPBE travail BE " + ts); travailBeCf.setWorkspace(ws1); travailBeCf.setCreatedBy(u1);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-lpbe-be-" + ts, "lpbe-be-" + ts + "@ex.com");

        // Workspace B — FRANCE / droit de la famille (autre workspace + pays non BE)
        User u2 = new User(); u2.setEmail("lpbe-fr-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-lpbe-fr-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("LPBE FR " + ts); ws2.setSlug("ws-lpbe-fr-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_FAMILLE"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        familleFrCf = new CaseFile(); familleFrCf.setTitle("LPBE famille FR " + ts); familleFrCf.setWorkspace(ws2); familleFrCf.setCreatedBy(u2);
        familleFrCf.setLegalDomain("DROIT_FAMILLE"); familleFrCf.setStatus("OPEN"); familleFrCf = caseFileRepository.save(familleFrCf);
        authOther = buildAuth("g-lpbe-fr-" + ts, "lpbe-fr-" + ts + "@ex.com");
    }

    @Test
    void POST_notaireNonDesigne_returns200_procedureEngagee() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notaireDesigne", false);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PROCEDURE_NON_ENGAGEE"))
                .andExpect(jsonPath("$.etapes.length()").value(8))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    void POST_notaireDesigne_returns200_enCours() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureEngagee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EN_COURS"))
                .andExpect(jsonPath("$.etapes.length()").value(8))
                .andExpect(jsonPath("$.delais[0].statut").value("NON_DEMARRE"));
    }

    @Test
    void POST_projetNotifieRecemment_returns200_delaiCritique() throws Exception {
        // notification il y a 25 jours → délai de contredits critique
        Map<String, Object> body = procedureEngagee();
        body.put("operationsOuvertes", true);
        body.put("dateOuvertureOperations", "2026-03-01");
        body.put("inventaireEtabli", true);
        body.put("projetLiquidationEtabli", true);
        body.put("dateNotificationProjet", LocalDate.now().minusDays(25).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DELAI_CONTREDITS_CRITIQUE"))
                .andExpect(jsonPath("$.delais[0].statut").value("CRITIQUE"));
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        // 1er calcul : notaire désigné seulement → procédure engagée, en cours
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureEngagee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EN_COURS"));

        // recalcul : procédure clôturée
        Map<String, Object> body2 = procedureEngagee();
        body2.put("operationsOuvertes", true);
        body2.put("dateOuvertureOperations", "2026-02-05");
        body2.put("inventaireEtabli", true);
        body2.put("projetLiquidationEtabli", true);
        body2.put("dateNotificationProjet", "2026-02-20");
        body2.put("contreditsDeposes", true);
        body2.put("procesVerbalDiresEtabli", true);
        body2.put("homologationDemandee", true);
        body2.put("dateHomologation", "2026-04-10");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CLOTUREE"));

        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CLOTUREE"));
    }

    @Test
    void GET_apresPost_returnsSnapshotInputsInclus() throws Exception {
        Map<String, Object> body = procedureEngagee();
        body.put("commentaire", "Notaire commis Me Dupont");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaireDesigne").value(true))
                .andExpect(jsonPath("$.dateDesignationNotaire").value("2026-01-10"))
                .andExpect(jsonPath("$.commentaire").value("Notaire commis Me Dupont"))
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
                        .content(objectMapper.writeValueAsString(procedureEngagee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_autreWorkspace_returns403() throws Exception {
        // authOther n'appartient pas au workspace du dossier familleBeCf
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureEngagee())))
                .andExpect(status().isForbidden());
    }

    @Test
    void POST_dossierNonFamille_returns422() throws Exception {
        mockMvc.perform(post(url(travailBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureEngagee())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_paysNonBelgique_returns422() throws Exception {
        mockMvc.perform(post(url(familleFrCf.getId()))
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureEngagee())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void POST_dateMalFormee_returns400() throws Exception {
        Map<String, Object> body = procedureEngagee();
        body.put("dateDesignationNotaire", "10-01-2026"); // format invalide
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateIncoherente_returns400() throws Exception {
        // notification du projet antérieure à la désignation du notaire
        Map<String, Object> body = procedureEngagee();
        body.put("operationsOuvertes", true);
        body.put("dateOuvertureOperations", "2026-01-20");
        body.put("projetLiquidationEtabli", true);
        body.put("dateNotificationProjet", "2025-12-01");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateManquantePourEtapeFranchie_returns400() throws Exception {
        // notaire désigné mais date de désignation absente
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notaireDesigne", true);
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(procedureEngagee())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void GET_nonAuthentifie_returns401() throws Exception {
        mockMvc.perform(get(url(familleBeCf.getId())))
                .andExpect(status().isUnauthorized());
    }

    private String url(UUID caseFileId) {
        return "/api/v1/case-files/" + caseFileId + "/liquidation-partage-be";
    }

    /** Procédure engagée de référence : notaire désigné, étapes suivantes non franchies. */
    private Map<String, Object> procedureEngagee() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("notaireDesigne", true);
        m.put("dateDesignationNotaire", "2026-01-10");
        m.put("operationsOuvertes", false);
        m.put("inventaireEtabli", false);
        m.put("projetLiquidationEtabli", false);
        m.put("contreditsDeposes", false);
        m.put("procesVerbalDiresEtabli", false);
        m.put("homologationDemandee", false);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
