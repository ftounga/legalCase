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
 * SF-219-18 : tests d'intégration de l'endpoint <i>semaine de 4 jours
 * BE</i> (Loi 03/10/2022 art. 5).
 *
 * <p>Couvre : POST BE 200 (verdicts CONFORME_REGIME_4_JOURS_VALIDE /
 * NON_ELIGIBLE_TEMPS_PARTIEL / NON_CONFORME_DEMANDE_ECRITE_MANQUANTE /
 * NON_CONFORME_JOURNEE_DEPASSE_9H30 /
 * NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT /
 * NON_CONFORME_DUREE_DEPASSE_6_MOIS / REFUS_EMPLOYEUR_NON_MOTIVE /
 * LICENCIEMENT_REPRESAILLES_PRESUME / A_ANALYSER), POST FR 404 (gate
 * BE strict), POST caseFile autre workspace 404, POST dossier non
 * DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class Semaine4JoursBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/semaine-4-jours-be";

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
        User uBe = save(new User(), u -> { u.setEmail("s4j-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-s4j-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE S4J " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE S4J " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-s4j-be-" + ts, "s4j-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("s4j-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-s4j-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR S4J " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR S4J " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-s4j-fr-" + ts, "s4j-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("s4j-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-s4j-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 S4J " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 S4J " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — semaine de 4 jours conforme. */
    private Map<String, Object> bodyConforme() {
        Map<String, Object> m = new HashMap<>();
        m.put("statutDemande", "ACCORDE_AVENANT_SIGNE");
        m.put("travailleurTempsPlein", true);
        m.put("demandeEcriteTravailleur", true);
        m.put("dateDemande", "2024-06-01");
        m.put("dureeHebdomadaireHeures", 38.00);
        m.put("journeeMaximaleHeures", 9.50);
        m.put("cctAutorise10h", false);
        m.put("avenantEcritSigne", true);
        m.put("reglementTravailModifie", true);
        m.put("dureeAvenantMois", 6);
        m.put("avenantRenouvele", false);
        m.put("refusMotiveParEcrit", false);
        m.put("motifLicenciementObjectifEtabli", false);
        return m;
    }

    @Test
    void POST_workspaceBe_conforme_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_REGIME_4_JOURS_VALIDE"))
                .andExpect(jsonPath("$.eligibiliteRespectee").value(true))
                .andExpect(jsonPath("$.demandeRespectee").value(true))
                .andExpect(jsonPath("$.journeeRespectee").value(true))
                .andExpect(jsonPath("$.formalisationRespectee").value(true))
                .andExpect(jsonPath("$.dureeRespectee").value(true))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 03/10/2022")));
    }

    @Test
    void POST_workspaceBe_tempsPartiel_returnsNonEligible() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("travailleurTempsPlein", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_ELIGIBLE_TEMPS_PARTIEL"))
                .andExpect(jsonPath("$.eligibiliteRespectee").value(false));
    }

    @Test
    void POST_workspaceBe_pasDemandeEcrite_returnsDemandeManquante() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("demandeEcriteTravailleur", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_DEMANDE_ECRITE_MANQUANTE"))
                .andExpect(jsonPath("$.demandeRespectee").value(false));
    }

    @Test
    void POST_workspaceBe_journeeDepasse10h_returnsJourneeDepasse() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("journeeMaximaleHeures", 10.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_JOURNEE_DEPASSE_9H30"))
                .andExpect(jsonPath("$.journeeRespectee").value(false));
    }

    @Test
    void POST_workspaceBe_journee10hAvecCct_returnsConforme() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("journeeMaximaleHeures", 10.00);
        body.put("cctAutorise10h", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_REGIME_4_JOURS_VALIDE"))
                .andExpect(jsonPath("$.journeeMaximaleAutoriseeHeures").value(10.0));
    }

    @Test
    void POST_workspaceBe_avenantManquant_returnsFormalisationManquante() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("avenantEcritSigne", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT"))
                .andExpect(jsonPath("$.formalisationRespectee").value(false));
    }

    @Test
    void POST_workspaceBe_duree12moisSansRenouvellement_returnsDureeDepasse() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("dureeAvenantMois", 12);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_DUREE_DEPASSE_6_MOIS"))
                .andExpect(jsonPath("$.dureeRespectee").value(false));
    }

    @Test
    void POST_workspaceBe_refusNonMotive_returnsRefusNonMotive() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutDemande", "REFUSE_SANS_MOTIVATION_ECRITE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("REFUS_EMPLOYEUR_NON_MOTIVE"));
    }

    @Test
    void POST_workspaceBe_licenciementSansMotifObjectif_returnsRepresailles() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutDemande", "LICENCIE_APRES_DEMANDE");
        body.put("dateLicenciement", "2024-07-15");
        body.put("motifLicenciementObjectifEtabli", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("LICENCIEMENT_REPRESAILLES_PRESUME"));
    }

    @Test
    void POST_workspaceBe_statutIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutDemande", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("STATUT_DEMANDE_INDETERMINE"));
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
                .andExpect(jsonPath("$.verdict").value("CONFORME_REGIME_4_JOURS_VALIDE"))
                .andExpect(jsonPath("$.statutDemande").value("ACCORDE_AVENANT_SIGNE"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_statutManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("statutDemande");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDemandeManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("dateDemande");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeHebdoZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("dureeHebdomadaireHeures", 0.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_journeeNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("journeeMaximaleHeures", -1.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeAvenantNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("dureeAvenantMois", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur TeletravailBeCct85149ControllerIT) ----

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
