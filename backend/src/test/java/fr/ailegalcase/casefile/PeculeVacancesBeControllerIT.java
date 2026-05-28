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
 * SF-219-20 : tests d'intégration de l'endpoint <i>pécule de vacances
 * BE</i> (Lois coord. 28/06/1971 + AR 30/03/1967).
 *
 * <p>Couvre : POST BE 200 (verdicts DU_PECULE_SIMPLE_CALCULE /
 * DU_DOUBLE_PECULE_CALCULE / DU_PECULE_DEPART_CALCULE /
 * NON_DU_OUVRIER_VIA_ONVA / NON_DU_DEJA_PAYE /
 * NON_DU_JOURS_INSUFFISANTS / PRESCRIT / A_ANALYSER), POST FR 404
 * (gate BE strict), POST caseFile autre workspace 404, POST dossier
 * non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class PeculeVacancesBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/pecule-vacances-be";

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
        User uBe = save(new User(), u -> { u.setEmail("pvbe-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-pvbe-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE PVBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE PVBE " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-pvbe-be-" + ts, "pvbe-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("pvbe-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-pvbe-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR PVBE " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR PVBE " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-pvbe-fr-" + ts, "pvbe-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("pvbe-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-pvbe-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 PVBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 PVBE " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — double pécule employé. */
    private Map<String, Object> bodyDoublePeculeEmploye() {
        Map<String, Object> m = new HashMap<>();
        m.put("statut", "EMPLOYE");
        m.put("typeCalcul", "DOUBLE_PECULE");
        m.put("remunerationBruteAnnuelleExerciceEur", 36000.00);
        m.put("remunerationMensuelleBruteEur", 3000.00);
        m.put("joursCongesPris", 24);
        m.put("peculeDejaPaye", false);
        m.put("remunerationBruteAnnuelleExercicePrecedentEur", 34000.00);
        m.put("dateReclamation", "2024-09-01");
        return m;
    }

    @Test
    void POST_workspaceBe_doublePeculeEmploye_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDoublePeculeEmploye())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DU_DOUBLE_PECULE_CALCULE"))
                .andExpect(jsonPath("$.montantDoublePeculeEur").value(2550.00))
                .andExpect(jsonPath("$.montantTotalDuEur").value(2550.00))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("28/06/1971")));
    }

    @Test
    void POST_workspaceBe_peculeSimpleEmploye_calculProratisation() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("typeCalcul", "PECULE_SIMPLE");
        body.put("joursCongesPris", 12);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DU_PECULE_SIMPLE_CALCULE"))
                .andExpect(jsonPath("$.montantPeculeSimpleEur").value(1500.00));
    }

    @Test
    void POST_workspaceBe_peculeDepartEmploye_calculDeuxExercices() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("typeCalcul", "PECULE_DEPART");
        body.put("joursCongesPris", 0);
        body.put("dateSortie", "2024-06-30");
        body.put("dateFinContrat", "2024-06-30");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DU_PECULE_DEPART_CALCULE"))
                .andExpect(jsonPath("$.montantPeculeDepartEur").value(10738.00));
    }

    @Test
    void POST_workspaceBe_ouvrier_returnsNonDuViaOnva() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("statut", "OUVRIER");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_DU_OUVRIER_VIA_ONVA"));
    }

    @Test
    void POST_workspaceBe_dejaPaye_returnsNonDuDejaPaye() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("peculeDejaPaye", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_DU_DEJA_PAYE"));
    }

    @Test
    void POST_workspaceBe_prescrit_returnsPrescrit() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("typeCalcul", "PECULE_DEPART");
        body.put("joursCongesPris", 0);
        body.put("dateFinContrat", "2022-06-30"); // > 1 an
        body.put("dateReclamation", "2024-09-01");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PRESCRIT"))
                .andExpect(jsonPath("$.prescrit").value(true));
    }

    @Test
    void POST_workspaceBe_jeuneTravailleurSansJours_returnsJoursInsuffisants() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("statut", "JEUNE_TRAVAILLEUR");
        body.put("typeCalcul", "PECULE_SIMPLE");
        body.put("joursCongesPris", 0);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_DU_JOURS_INSUFFISANTS"));
    }

    @Test
    void POST_workspaceBe_statutIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("statut", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("STATUT_INDETERMINE"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDoublePeculeEmploye())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDoublePeculeEmploye())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDoublePeculeEmploye())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDoublePeculeEmploye())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DU_DOUBLE_PECULE_CALCULE"))
                .andExpect(jsonPath("$.statut").value("EMPLOYE"))
                .andExpect(jsonPath("$.typeCalcul").value("DOUBLE_PECULE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_statutManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.remove("statut");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_typeCalculManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.remove("typeCalcul");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("remunerationBruteAnnuelleExerciceEur", -1.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_joursNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.put("joursCongesPris", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateReclamationManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyDoublePeculeEmploye();
        body.remove("dateReclamation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur Semaine4JoursBeControllerIT) ----

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
