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
 * SF-219-32 : tests d'intégration de l'endpoint <i>Interruption de
 * carrière pour congé parental BE</i> (Loi du 22/01/1985 art. 99 à
 * 107quater + AR du 29/10/1997 + CCT n° 64).
 *
 * <p>Couvre : POST BE 200 sur l'ensemble des verdicts
 * (ELIGIBLE_COMPLET, ELIGIBLE_AVEC_RESERVES, INELIGIBLE_ANCIENNETE,
 * INELIGIBLE_AGE_ENFANT, INELIGIBLE_SOLDE_INSUFFISANT,
 * INELIGIBLE_FORMALISME, DIFFERE_EMPLOYEUR), POST FR 404 (gate BE
 * strict), POST caseFile autre workspace 404, POST dossier non
 * DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400, upsert.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class InterruptionCarriereSoinsParentalControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/interruption-carriere-soins-parental";

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

        User uBe = save(new User(),
                u -> { u.setEmail("ics-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ics-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE ICS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE ICS " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE ICS FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-ics-be-" + ts, "ics-be-" + ts + "@ex.com");

        User uFr = save(new User(),
                u -> { u.setEmail("ics-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ics-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR ICS " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR ICS " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-ics-fr-" + ts, "ics-fr-" + ts + "@ex.com");

        User uBe2 = save(new User(),
                u -> { u.setEmail("ics-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-ics-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 ICS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile =
                saveCf(uBe2, wsBe2, "CFBE2 ICS " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — éligible complet (temps plein). */
    private Map<String, Object> bodyEligibleComplet() {
        Map<String, Object> m = new HashMap<>();
        m.put("forme", "TEMPS_PLEIN");
        m.put("ancienneteMois", 24);
        m.put("ageEnfantMois", 36);
        m.put("enfantHandicap", false);
        m.put("soldeRestantMoisEtp", 4);
        m.put("modeNotification", "LETTRE_RECOMMANDEE");
        m.put("employeurAccepte", true);
        m.put("employeurAdiffere", false);
        m.put("cumulAllocationOnemDemande", true);
        m.put("dateDebutInterruption", "2026-06-15");
        m.put("dateNotification", "2026-04-01");
        m.put("remunerationMensuelleBrute", 3500);
        return m;
    }

    @Test
    void POST_workspaceBe_eligibleComplet_returnsVerdict() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleComplet())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_COMPLET"))
                .andExpect(jsonPath("$.dureeInterruptionMois").value(4))
                .andExpect(jsonPath("$.allocationOnemMensuelle").value(250.00))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi de redressement du 22/01/1985")))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("AR du 29/10/1997")));
    }

    @Test
    void POST_workspaceBe_eligibleAvecReserves() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.put("employeurAccepte", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_AVEC_RESERVES"));
    }

    @Test
    void POST_workspaceBe_ineligibleAnciennete() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.put("ancienneteMois", 6);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_ANCIENNETE"))
                .andExpect(jsonPath("$.ancienneteOk").value(false));
    }

    @Test
    void POST_workspaceBe_ineligibleAgeEnfant() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.put("ageEnfantMois", 150); // 12.5 ans

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_AGE_ENFANT"));
    }

    @Test
    void POST_workspaceBe_ineligibleSoldeInsuffisant() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.put("soldeRestantMoisEtp", 2);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_SOLDE_INSUFFISANT"));
    }

    @Test
    void POST_workspaceBe_ineligibleFormalisme_modeAutre() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.put("modeNotification", "AUTRE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_FORMALISME"));
    }

    @Test
    void POST_workspaceBe_differeEmployeur() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.put("employeurAdiffere", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DIFFERE_EMPLOYEUR"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleComplet())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleComplet())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleComplet())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleComplet())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_COMPLET"))
                .andExpect(jsonPath("$.forme").value("TEMPS_PLEIN"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_formeManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("forme");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ancienneteManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("ancienneteMois");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ancienneteNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.put("ancienneteMois", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ageEnfantManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("ageEnfantMois");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_enfantHandicapManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("enfantHandicap");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_soldeManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("soldeRestantMoisEtp");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_modeNotificationManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("modeNotification");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_employeurAccepteManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("employeurAccepte");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_employeurAdiffereManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("employeurAdiffere");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_cumulOnemManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleComplet();
        body.remove("cumulAllocationOnemDemande");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_meme_dossier_deuxFois_upsert() throws Exception {
        // 1er POST — éligible complet temps plein
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleComplet())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_COMPLET"))
                .andExpect(jsonPath("$.forme").value("TEMPS_PLEIN"));

        // 2e POST avec forme différente → mise à jour
        Map<String, Object> body2 = bodyEligibleComplet();
        body2.put("forme", "CINQUIEME_TEMPS");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forme").value("CINQUIEME_TEMPS"))
                .andExpect(jsonPath("$.dureeInterruptionMois").value(20));

        // GET retourne le 2nd verdict (upsert)
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forme").value("CINQUIEME_TEMPS"));
    }

    // ---- helpers ----

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
        Map<String, Object> claims =
                Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token",
                Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
