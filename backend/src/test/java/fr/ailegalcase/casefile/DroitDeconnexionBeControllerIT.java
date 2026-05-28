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
 * SF-219-19 : tests d'intégration de l'endpoint <i>droit à la
 * déconnexion BE</i> (Loi 03/10/2022 art. 16 + AR 19/02/2023 + CCT
 * n° 149).
 *
 * <p>Couvre : POST BE 200 (verdicts CONFORME_ACCORD_COMPLET /
 * HORS_CHAMP_SEUIL_NON_ATTEINT / NON_CONFORME_CONTENU_INCOMPLET /
 * NON_CONFORME_INSTRUMENT_MANQUANT /
 * MANQUEMENT_GRAVE_AUCUNE_INITIATIVE / A_ANALYSER), POST FR 404
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
class DroitDeconnexionBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/droit-deconnexion-be";

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
        User uBe = save(new User(), u -> { u.setEmail("ddx-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ddx-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE DDX " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE DDX " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-ddx-be-" + ts, "ddx-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("ddx-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ddx-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR DDX " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR DDX " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-ddx-fr-" + ts, "ddx-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("ddx-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-ddx-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 DDX " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 DDX " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — accord déconnexion complet. */
    private Map<String, Object> bodyConforme() {
        Map<String, Object> m = new HashMap<>();
        m.put("effectifEntreprise", 50);
        m.put("statutAccord", "CCT_ENTREPRISE_DEPOSEE");
        m.put("dateEntreeVigueurInstrument", "2024-03-01");
        m.put("modalitesPratiquesDeconnexionDefinies", true);
        m.put("sensibilisationFormationPrevue", true);
        m.put("modalitesOrganisationTravailDefinies", true);
        m.put("consultationOrganeConcertationEffectuee", true);
        m.put("manquementSignaleCbeOuSpf", false);
        return m;
    }

    @Test
    void POST_workspaceBe_conforme_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_ACCORD_COMPLET"))
                .andExpect(jsonPath("$.seuilAtteint").value(true))
                .andExpect(jsonPath("$.instrumentFormalise").value(true))
                .andExpect(jsonPath("$.contenuComplet").value(true))
                .andExpect(jsonPath("$.consultationRespectee").value(true))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 03/10/2022")));
    }

    @Test
    void POST_workspaceBe_effectif15_returnsHorsChamp() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("effectifEntreprise", 15);
        body.put("statutAccord", "AUCUNE_INITIATIVE");
        body.put("modalitesPratiquesDeconnexionDefinies", false);
        body.put("sensibilisationFormationPrevue", false);
        body.put("modalitesOrganisationTravailDefinies", false);
        body.put("consultationOrganeConcertationEffectuee", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HORS_CHAMP_SEUIL_NON_ATTEINT"))
                .andExpect(jsonPath("$.seuilAtteint").value(false));
    }

    @Test
    void POST_workspaceBe_aucuneInitiative_returnsManquementGrave() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutAccord", "AUCUNE_INITIATIVE");
        body.put("modalitesPratiquesDeconnexionDefinies", false);
        body.put("sensibilisationFormationPrevue", false);
        body.put("modalitesOrganisationTravailDefinies", false);
        body.put("consultationOrganeConcertationEffectuee", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MANQUEMENT_GRAVE_AUCUNE_INITIATIVE"));
    }

    @Test
    void POST_workspaceBe_negociationSansInstrument_returnsInstrumentManquant() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutAccord", "NEGOCIATION_EN_COURS");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_INSTRUMENT_MANQUANT"))
                .andExpect(jsonPath("$.instrumentFormalise").value(false));
    }

    @Test
    void POST_workspaceBe_modalitesPratiquesManquantes_returnsContenuIncomplet() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("modalitesPratiquesDeconnexionDefinies", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_CONTENU_INCOMPLET"))
                .andExpect(jsonPath("$.contenuComplet").value(false));
    }

    @Test
    void POST_workspaceBe_reglementTravailComplet_returnsConforme() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutAccord", "REGLEMENT_TRAVAIL_MODIFIE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_ACCORD_COMPLET"));
    }

    @Test
    void POST_workspaceBe_statutIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutAccord", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("STATUT_ACCORD_INDETERMINE"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_ACCORD_COMPLET"))
                .andExpect(jsonPath("$.statutAccord").value("CCT_ENTREPRISE_DEPOSEE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_effectifManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("effectifEntreprise");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_statutAccordManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("statutAccord");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("effectifEntreprise", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_modalitesPratiquesManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("modalitesPratiquesDeconnexionDefinies");
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
