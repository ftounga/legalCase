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
 * SF-207-08 : tests d'intégration de l'endpoint outplacement obligatoire
 * 45+ BE (CCT 82 ; CCT 82 bis ; Loi 05/09/2001 art. 13 ; AR 30/05/2018).
 *
 * <p>Couvre : POST BE 200 (5 verdicts), POST FR 404 (gate BE strict), POST
 * caseFile autre workspace 404 (isolation workspace), GET 404 sans POST,
 * GET retour persisted, validations Bean Validation 400 (champs requis),
 * validations conditionnelles 400 (offre reçue sans date / sans conformité),
 * date naissance future 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class OutplacementBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/outplacement-be-obligatoire-45";

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
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("outpl-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-outpl-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE OUTPL " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE OUTPL " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-outpl-be-" + ts, "outpl-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("outpl-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-outpl-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR OUTPL " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR OUTPL " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-outpl-fr-" + ts, "outpl-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("outpl-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-outpl-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 OUTPL " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 OUTPL " + ts, "DROIT_DU_TRAVAIL");
    }

    /**
     * Body nominal — salarié 54 ans, ancienneté 12,5 ans, licenciement
     * économique, contrat temps plein, offre conforme dans le délai et
     * acceptée → OFFRE_CONFORME_RESPECTEE.
     */
    private Map<String, Object> bodyNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateLicenciement", "2026-04-15");
        m.put("dateNaissanceSalarie", "1972-03-10");
        m.put("ancienneteAnnees", 12.5);
        m.put("motifLicenciement", "LICENCIEMENT_ECONOMIQUE");
        m.put("contratTempsPlein", true);
        m.put("offreOutplacementRecue", true);
        m.put("dateOffreOutplacement", "2026-04-20");
        m.put("offreConformeCCT82", true);
        m.put("salarieAcceptantOffre", true);
        return m;
    }

    @Test
    void POST_workspaceBe_offreConforme_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("OFFRE_CONFORME_RESPECTEE"))
                .andExpect(jsonPath("$.ageALaDateLicenciement").value(54))
                .andExpect(jsonPath("$.obligationEmployeurApplicable").value(true))
                .andExpect(jsonPath("$.sanctionEmployeurEuros").value(0.00))
                .andExpect(jsonPath("$.dateLimiteOffre").value("2026-04-30"))
                .andExpect(jsonPath("$.delaiOffreRespecte").value(true))
                .andExpect(jsonPath("$.etapeSuivante").value("AUCUNE"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("CCT 82")));
    }

    @Test
    void POST_workspaceBe_offreNonFaite_sanction1800() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("offreOutplacementRecue", false);
        body.remove("dateOffreOutplacement");
        body.remove("offreConformeCCT82");
        body.remove("salarieAcceptantOffre");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OFFRE_NON_FAITE_SANCTION_EMPLOYEUR"))
                .andExpect(jsonPath("$.sanctionEmployeurEuros").value(1800.00))
                .andExpect(jsonPath("$.etapeSuivante").value("PLAINTE_INSPECTION_LOIS_SOCIALES"));
    }

    @Test
    void POST_workspaceBe_ageInferieur45_outplacementNonDu() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dateNaissanceSalarie", "1990-01-01");                    // 36 ans

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OUTPLACEMENT_NON_DU"))
                .andExpect(jsonPath("$.raisonNonObligation").value("AGE_INFERIEUR_45"))
                .andExpect(jsonPath("$.sanctionEmployeurEuros").value(0.00));
    }

    @Test
    void POST_workspaceBe_offreRefusee_sanctionSalarie4_52() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("salarieAcceptantOffre", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OFFRE_REFUSEE_SANCTION_SALARIE"))
                .andExpect(jsonPath("$.sanctionSalarieRange.minSemaines").value(4))
                .andExpect(jsonPath("$.sanctionSalarieRange.maxSemaines").value(52))
                .andExpect(jsonPath("$.etapeSuivante").value("PRESENTATION_C4_ONEM"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("OFFRE_CONFORME_RESPECTEE"))
                .andExpect(jsonPath("$.dateNaissanceSalarie").value("1972-03-10"))
                .andExpect(jsonPath("$.dateLicenciement").value("2026-04-15"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateLicenciementManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.remove("dateLicenciement");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ancienneteNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("ancienneteAnnees", -1.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_offreRecue_sansDate_returns400_validationConditionnelle() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("offreOutplacementRecue", true);
        body.remove("dateOffreOutplacement");
        body.put("offreConformeCCT82", true);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_offreRecue_sansConformite_returns400_validationConditionnelle() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("offreOutplacementRecue", true);
        body.put("dateOffreOutplacement", "2026-04-20");
        body.remove("offreConformeCCT82");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNaissanceFuture_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dateNaissanceSalarie", "2099-01-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur RccBeIndemniteControllerIT) ----

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
