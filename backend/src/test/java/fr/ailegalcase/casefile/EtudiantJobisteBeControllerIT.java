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
 * SF-219-13 : tests d'intégration de l'endpoint <i>étudiant jobiste
 * BE</i> (Loi 03/07/1978 + Loi-programme 24/12/2002 + AR 14/07/1995
 * + Loi-programme 22/12/2023 — quota 600h/an).
 *
 * <p>Couvre : POST BE 200 (verdicts ELIGIBLE / INELIGIBLE_STATUT /
 * INELIGIBLE_QUOTA / FRAGILE_CONTRAT / FRAGILE_COTISATIONS /
 * A_ANALYSER), POST FR 404 (gate BE strict), POST caseFile autre
 * workspace 404, POST dossier non DROIT_DU_TRAVAIL 400, GET 404 sans
 * POST, GET retour persisté, validations Bean Validation 400 (champs
 * requis et bornes).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class EtudiantJobisteBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/etudiant-jobiste-be";

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
        User uBe = save(new User(), u -> { u.setEmail("stu-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-stu-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE STU " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE STU " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-stu-be-" + ts, "stu-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("stu-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-stu-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR STU " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR STU " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-stu-fr-" + ts, "stu-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("stu-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-stu-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 STU " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 STU " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — étudiant éligible (statut principal, 600h quota OK, formalisme + cotisations OK). */
    private Map<String, Object> bodyEligible() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutOccupation", "2024-07-01");
        m.put("statutEtudiant", "ETUDIANT_PRINCIPAL");
        m.put("heuresDejaPresteesDansAnnee", 100);
        m.put("heuresContratEnCours", 200);
        m.put("quotaAnnuelHeures", 600);
        m.put("contratEcritSigne", true);
        m.put("dimonaStuDeclaree", true);
        m.put("cotisationsReduitesAppliquees", true);
        m.put("remunerationBruteHoraire", 12.50);
        return m;
    }

    @Test
    void POST_workspaceBe_eligible_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.statutEligible").value(true))
                .andExpect(jsonPath("$.quotaRespecte").value(true))
                .andExpect(jsonPath("$.formalismeRespecte").value(true))
                .andExpect(jsonPath("$.cotisationsConformes").value(true))
                .andExpect(jsonPath("$.heuresRestantesAvantContrat").value(500))
                .andExpect(jsonPath("$.heuresHorsQuota").value(0))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 03/07/1978")));
    }

    @Test
    void POST_workspaceBe_nonEtudiant_returnsStatutNonEtudiant() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("statutEtudiant", "NON_ETUDIANT");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_STATUT_NON_ETUDIANT"))
                .andExpect(jsonPath("$.statutEligible").value(false))
                .andExpect(jsonPath("$.raison").value("STATUT_NON_ETUDIANT"));
    }

    @Test
    void POST_workspaceBe_videPedagogique_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("statutEtudiant", "VIDE_PEDAGOGIQUE_PROLONGE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("VIDE_PEDAGOGIQUE_ZONE_GRISE"));
    }

    @Test
    void POST_workspaceBe_quotaDepasse_returnsQuotaDepasse() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("heuresDejaPresteesDansAnnee", 500);
        body.put("heuresContratEnCours", 200); // 700 > 600 → 100 h hors quota

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_QUOTA_DEPASSE"))
                .andExpect(jsonPath("$.quotaRespecte").value(false))
                .andExpect(jsonPath("$.heuresHorsQuota").value(100))
                .andExpect(jsonPath("$.montantRedressementEstime").value(523.38));
    }

    @Test
    void POST_workspaceBe_dimonaManquante_returnsFragileContrat() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dimonaStuDeclaree", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FRAGILE_CONTRAT_OU_DIMONA_MANQUANT"))
                .andExpect(jsonPath("$.formalismeRespecte").value(false));
    }

    @Test
    void POST_workspaceBe_cotisationsNonReduites_returnsFragileCotisations() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("cotisationsReduitesAppliquees", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FRAGILE_COTISATIONS_NON_REDUITES"))
                .andExpect(jsonPath("$.cotisationsConformes").value(false));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.statutEtudiant").value("ETUDIANT_PRINCIPAL"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.remove("dateDebutOccupation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_statutManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.remove("statutEtudiant");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_heuresContratZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("heuresContratEnCours", 0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_heuresDejaNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("heuresDejaPresteesDansAnnee", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_quotaZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("quotaAnnuelHeures", 0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("remunerationBruteHoraire", -1.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur FlexiJobBeControllerIT) ----

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
