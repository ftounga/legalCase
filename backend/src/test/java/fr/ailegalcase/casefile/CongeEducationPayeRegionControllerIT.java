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
 * SF-219-11 : tests d'intégration de l'endpoint <i>congé-éducation payé
 * régionalisé</i> (Loi 22/01/1985 régionalisée 2014 — WBR / FLA / BXL).
 *
 * <p>Couvre : POST BE 200 par région (verdicts PLEIN_DROIT / PRORATA /
 * HORS_FORMATION_AGREEE / OCCUPATION_INSUFFISANTE), POST FR 404 (gate
 * BE strict), POST caseFile autre workspace 404 (isolation workspace),
 * POST dossier non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour
 * persisté, validations Bean Validation 400 (champs requis).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CongeEducationPayeRegionControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/conge-education-paye-region";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authFr;
    private CaseFile beCaseFile;
    private CaseFile beFamilleCaseFile;
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("cep-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cep-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE CEP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE CEP " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM CEP " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-cep-be-" + ts, "cep-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("cep-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cep-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR CEP " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR CEP " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-cep-fr-" + ts, "cep-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("cep-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-cep-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 CEP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 CEP " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — Wallonie temps plein, formation pro qualifiante, 150 h agréée. */
    private Map<String, Object> bodyWalloniePleinDroit() {
        Map<String, Object> m = new HashMap<>();
        m.put("region", "WALLONIE");
        m.put("typeFormation", "PROFESSIONNELLE_QUALIFIANTE");
        m.put("heuresFormationAnnuelles", 150);
        m.put("tauxOccupation", 1.0);
        m.put("publicFragilise", false);
        m.put("formationAgreeeRegion", true);
        return m;
    }

    @Test
    void POST_workspaceBe_walloniePleinDroit_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWalloniePleinDroit())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PLEIN_DROIT"))
                .andExpect(jsonPath("$.region").value("WALLONIE"))
                .andExpect(jsonPath("$.plafondRegionalHeures").value(180))
                .andExpect(jsonPath("$.heuresCongeAllouees").value(150))
                .andExpect(jsonPath("$.regimeApplicable").value("Wallonie"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 22/01/1985")));
    }

    @Test
    void POST_workspaceBe_flandreVov_returnsPleinDroit125h() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("region", "FLANDRE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PLEIN_DROIT"))
                .andExpect(jsonPath("$.region").value("FLANDRE"))
                .andExpect(jsonPath("$.plafondRegionalHeures").value(125))
                .andExpect(jsonPath("$.heuresCongeAllouees").value(125))
                .andExpect(jsonPath("$.regimeApplicable").value("Flandre"));
    }

    @Test
    void POST_workspaceBe_flandreEnsSupAlternance_returnsPleinDroit250h() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("region", "FLANDRE");
        body.put("typeFormation", "ENSEIGNEMENT_SUPERIEUR_ALTERNANCE");
        body.put("heuresFormationAnnuelles", 240);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plafondRegionalHeures").value(250))
                .andExpect(jsonPath("$.heuresCongeAllouees").value(240));
    }

    @Test
    void POST_workspaceBe_bruxelles_returnsPleinDroit100h() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("region", "BRUXELLES");
        body.put("heuresFormationAnnuelles", 80);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PLEIN_DROIT"))
                .andExpect(jsonPath("$.plafondRegionalHeures").value(100))
                .andExpect(jsonPath("$.heuresCongeAllouees").value(80))
                .andExpect(jsonPath("$.regimeApplicable").value("Bruxelles-Capitale"));
    }

    @Test
    void POST_workspaceBe_mitemps_returnsProrata90h() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("tauxOccupation", 0.5);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PRORATA"))
                .andExpect(jsonPath("$.heuresCongeAllouees").value(90));
    }

    @Test
    void POST_workspaceBe_formationHorsListeAgreee_returnsIneligible() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("formationAgreeeRegion", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_HORS_FORMATION_AGREEE"))
                .andExpect(jsonPath("$.heuresCongeAllouees").value(0))
                .andExpect(jsonPath("$.raison").value("FORMATION_HORS_LISTE_AGREEE"));
    }

    @Test
    void POST_workspaceBe_occupationSousMitemps_returnsOccupationInsuffisante() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("tauxOccupation", 0.3);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_OCCUPATION_INSUFFISANTE"))
                .andExpect(jsonPath("$.raison").value("OCCUPATION_INSUFFISANTE_SEUIL_REGIONAL"));
    }

    @Test
    void POST_workspaceBe_flandrePublicFragilise_derogationApplique() throws Exception {
        // FLA + public fragilisé + 30 % temps → éligible prorata (dérogation 1/5 temps)
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("region", "FLANDRE");
        body.put("typeFormation", "RECONVERSION_PUBLIC_FRAGILISE");
        body.put("tauxOccupation", 0.3);
        body.put("publicFragilise", true);
        body.put("heuresFormationAnnuelles", 60);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PRORATA"))
                .andExpect(jsonPath("$.plafondRegionalHeures").value(180));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWalloniePleinDroit())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWalloniePleinDroit())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWalloniePleinDroit())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyWalloniePleinDroit())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_PLEIN_DROIT"))
                .andExpect(jsonPath("$.region").value("WALLONIE"))
                .andExpect(jsonPath("$.heuresFormationAnnuelles").value(150));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_regionManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.remove("region");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_typeFormationManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.remove("typeFormation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_heuresManquantes_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.remove("heuresFormationAnnuelles");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_tauxOccupationManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.remove("tauxOccupation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_heuresNegatives_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("heuresFormationAnnuelles", -10);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_tauxOccupationSupOne_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyWalloniePleinDroit();
        body.put("tauxOccupation", 1.5);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur DelegueSyndicalCct5ControllerIT) ----

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user); a.setProvider("GOOGLE"); a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name, String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setName(name); ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner); ws.setLegalDomain(legalDomain); ws.setCountry(country);
        ws.setPlanCode("STARTER"); ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws); m.setUser(user); m.setMemberRole("OWNER"); m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private CaseFile saveCf(User user, Workspace ws, String title, String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title); cf.setWorkspace(ws); cf.setCreatedBy(user);
        cf.setLegalDomain(domain); cf.setStatus("OPEN");
        return caseFileRepository.save(cf);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
