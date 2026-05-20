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
 * SF-217-12 : tests d'intégration des endpoints d'analyse d'option successorale BE
 * (acceptation pure / sous bénéfice d'inventaire / renonciation — BELGIQUE).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class SuccessionBeAcceptationRenonciationControllerIT {

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
        User u1 = new User(); u1.setEmail("sbar-be-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-sbar-be-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("SBAR BE " + ts); ws1.setSlug("ws-sbar-be-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("BELGIQUE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        familleBeCf = new CaseFile(); familleBeCf.setTitle("SBAR famille BE " + ts); familleBeCf.setWorkspace(ws1); familleBeCf.setCreatedBy(u1);
        familleBeCf.setLegalDomain("DROIT_FAMILLE"); familleBeCf.setStatus("OPEN"); familleBeCf = caseFileRepository.save(familleBeCf);
        travailBeCf = new CaseFile(); travailBeCf.setTitle("SBAR travail BE " + ts); travailBeCf.setWorkspace(ws1); travailBeCf.setCreatedBy(u1);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailBeCf.setStatus("OPEN"); travailBeCf = caseFileRepository.save(travailBeCf);
        authBe = buildAuth("g-sbar-be-" + ts, "sbar-be-" + ts + "@ex.com");

        // Workspace B — FRANCE / droit de la famille (autre workspace + pays non BE)
        User u2 = new User(); u2.setEmail("sbar-fr-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-sbar-fr-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("SBAR FR " + ts); ws2.setSlug("ws-sbar-fr-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_FAMILLE"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        familleFrCf = new CaseFile(); familleFrCf.setTitle("SBAR famille FR " + ts); familleFrCf.setWorkspace(ws2); familleFrCf.setCreatedBy(u2);
        familleFrCf.setLegalDomain("DROIT_FAMILLE"); familleFrCf.setStatus("OPEN"); familleFrCf = caseFileRepository.save(familleFrCf);
        authOther = buildAuth("g-sbar-fr-" + ts, "sbar-fr-" + ts + "@ex.com");
    }

    @Test
    void POST_casNominal_returns200_optionLibreDelaiOk() throws Exception {
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OPTION_LIBRE_DELAI_OK"))
                .andExpect(jsonPath("$.optionRecommandee").value("ACCEPTER"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.calculatedAt").exists())
                .andExpect(jsonPath("$.dateLimiteOption").exists())
                .andExpect(jsonPath("$.basesJuridiques.length()").isNotEmpty());
    }

    @Test
    void POST_patrimoineDouteux_returns200_optionBeneficeInventaire() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("etatPatrimoineSuccessoral", "DOUTEUX");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE"))
                .andExpect(jsonPath("$.optionRecommandee").value("ACCEPTER_BENEFICE_INVENTAIRE"));
    }

    @Test
    void POST_patrimoineInsolvable_returns200_optionRenonciation() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("etatPatrimoineSuccessoral", "INSOLVABLE");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OPTION_RECOMMANDEE_RENONCIATION"))
                .andExpect(jsonPath("$.optionRecommandee").value("RENONCER"));
    }

    @Test
    void POST_acteHeritierAccompli_returns200_acceptationTaciteProbable() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("actesAccomplis", List.of("ENCAISSEMENT_CREANCE_DEFUNT"));
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ACCEPTATION_TACITE_PROBABLE"))
                .andExpect(jsonPath("$.risques[?(@.code == 'ACTE_HERITIER_VALANT_ACCEPTATION_TACITE')]")
                        .isNotEmpty());
    }

    @Test
    void POST_decesIlYa150Jours_returns200_delaiDepasse() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateDeces", LocalDate.now().minusDays(150).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DELAI_DEPASSE"))
                .andExpect(jsonPath("$.delaiStatut").value("DEPASSE"))
                .andExpect(jsonPath("$.risques[?(@.code == 'DEVOLUTION_FORCEE_RISQUE')]")
                        .isNotEmpty());
    }

    @Test
    void POST_recalcul_ecraseResultatPrecedent() throws Exception {
        // 1er calcul : patrimoine solvable
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OPTION_LIBRE_DELAI_OK"));

        // Recalcul : patrimoine insolvable
        Map<String, Object> body2 = baseBody();
        body2.put("etatPatrimoineSuccessoral", "INSOLVABLE");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OPTION_RECOMMANDEE_RENONCIATION"));

        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OPTION_RECOMMANDEE_RENONCIATION"));
    }

    @Test
    void GET_apresPost_returnsSnapshotInputsInclus() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("commentaire", "Héritier en réflexion — pas d'acte d'héritier accompli");
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(familleBeCf.getId())).with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateDeces").exists())
                .andExpect(jsonPath("$.qualiteHeritier").value("ENFANT"))
                .andExpect(jsonPath("$.etatPatrimoineSuccessoral").value("SOLVABLE"))
                .andExpect(jsonPath("$.volonteClient").value("INDECIS"))
                .andExpect(jsonPath("$.commentaire").value(
                        "Héritier en réflexion — pas d'acte d'héritier accompli"))
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
        // authOther (workspace FR) tente d'accéder au dossier BE → isolation workspace
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
    void POST_dateDecesFuture_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateDeces", LocalDate.now().plusDays(1).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateMiseEnDemeureAvantDeces_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("miseEnDemeureCreancier", true);
        body.put("dateMiseEnDemeureCreancier",
                LocalDate.parse(body.get("dateDeces").toString()).minusDays(1).toString());
        mockMvc.perform(post(url(familleBeCf.getId()))
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDecesMalFormee_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateDeces", "10-05-2026"); // format invalide
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
        return "/api/v1/case-files/" + caseFileId + "/succession-be-acceptation-renonciation";
    }

    /** Body de référence : décès récent (10 j), patrimoine solvable, aucun acte, indécis. */
    private Map<String, Object> baseBody() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dateDeces", LocalDate.now().minusDays(10).toString());
        m.put("qualiteHeritier", "ENFANT");
        m.put("etatPatrimoineSuccessoral", "SOLVABLE");
        m.put("actesAccomplis", List.of("AUCUN"));
        m.put("volonteClient", "INDECIS");
        m.put("miseEnDemeureCreancier", false);
        return m;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
