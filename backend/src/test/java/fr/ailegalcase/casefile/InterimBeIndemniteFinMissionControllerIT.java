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
 * SF-219-15 : tests d'intégration de l'endpoint <i>indemnité de fin
 * de mission intérim BE</i> (Loi 24/07/1987 + CCT n° 322 + CCT
 * n° 322bis + jurisprudence Cass. BE).
 *
 * <p>Couvre : POST BE 200 (verdicts INDEMNITES_DUES /
 * RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR / AUCUNE_INDEMNITE_DUE /
 * A_ANALYSER_SECTEUR_NON_RECONNU), POST FR 404 (gate BE strict),
 * POST caseFile autre workspace 404, POST dossier non DROIT_DU_TRAVAIL
 * 400, GET 404 sans POST, GET retour persisté, validations Bean
 * Validation 400 (champs requis et bornes).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class InterimBeIndemniteFinMissionControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/interim-be-indemnite-fin-mission";

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
        User uBe = save(new User(), u -> {
            u.setEmail("ifm-be-" + ts + "@ex.com");
            u.setStatus("ACTIVE");
        });
        saveAuth(uBe, "g-ifm-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE IFM " + ts,
                "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE IFM " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts,
                "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-ifm-be-" + ts, "ifm-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> {
            u.setEmail("ifm-fr-" + ts + "@ex.com");
            u.setStatus("ACTIVE");
        });
        saveAuth(uFr, "g-ifm-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR IFM " + ts,
                "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR IFM " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-ifm-fr-" + ts, "ifm-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> {
            u.setEmail("ifm-be2-" + ts + "@ex.com");
            u.setStatus("ACTIVE");
        });
        saveAuth(uBe2, "g-ifm-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 IFM " + ts,
                "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 IFM " + ts,
                "DROIT_DU_TRAVAIL");
    }

    /**
     * Body nominal — mission 90 jours, salaire 16 €/h, 684 h prestées,
     * pas d'heures sup, pécule pas encore versé, CP 200 employés,
     * ancienneté 100 j.
     */
    private Map<String, Object> bodyStandard() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutMission", "2024-06-01");
        m.put("dateFinPrevue", "2024-08-30");
        m.put("dateFinReelle", "2024-08-30");
        m.put("dureeReellePrestationJours", 90);
        m.put("dureePrevueJours", 90);
        m.put("salaireHoraireBrut", 16.00);
        m.put("heuresPrestees", 684.00);
        m.put("heuresSupplementairesSemaine", 0.00);
        m.put("heuresSupplementairesDimancheFerie", 0.00);
        m.put("peculeDejaVerseParFsi", false);
        m.put("ruptureAnticipeeParEtiSansMotifGrave", false);
        m.put("commissionParitaireUtilisateur", "CP_200_AUXILIAIRE_EMPLOYES");
        m.put("ancienneteSectorielleJours", 100);
        return m;
    }

    @Test
    void POST_workspaceBe_standard_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStandard())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INDEMNITES_DUES"))
                .andExpect(jsonPath("$.peculeVacancesInterim").value(1683.19))
                .andExpect(jsonPath("$.primeFinAnneeSectorielle").value(911.64))
                .andExpect(jsonPath("$.primePrecaritePresumee").value(0.00))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("24/07/1987")));
    }

    @Test
    void POST_workspaceBe_ruptureAnticipee_returnsResteACourir() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("dureeReellePrestationJours", 45);
        body.put("dateFinReelle", "2024-07-15");
        body.put("ruptureAnticipeeParEtiSansMotifGrave", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR"))
                .andExpect(jsonPath("$.joursRestantsACourirJusquAuTerme").value(45))
                .andExpect(jsonPath("$.indemniteRuptureAnticipee").value(5472.00))
                .andExpect(jsonPath("$.raison")
                        .value("RUPTURE_ANTICIPEE_RESTE_A_COURIR"));
    }

    @Test
    void POST_workspaceBe_cpNonRecensee_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("commissionParitaireUtilisateur", "AUTRE_NON_RECENSE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("A_ANALYSER_SECTEUR_NON_RECONNU"))
                .andExpect(jsonPath("$.primeFinAnneeSectorielle").value(0.00))
                .andExpect(jsonPath("$.raison").value("SECTEUR_CP_NON_RECENSE"));
    }

    @Test
    void POST_workspaceBe_aucuneIndemDue_returnsAucuneIndem() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("dureeReellePrestationJours", 20);
        body.put("dureePrevueJours", 20);
        body.put("heuresPrestees", 152.00);
        body.put("peculeDejaVerseParFsi", true);
        body.put("ancienneteSectorielleJours", 20);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("AUCUNE_INDEMNITE_DUE"))
                .andExpect(jsonPath("$.totalIndemnitesBrutes").value(0.00))
                .andExpect(jsonPath("$.peculeVacancesInterim").value(0.00));
    }

    @Test
    void POST_workspaceBe_avecHeuresSup_sursalaireCalcule() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("heuresSupplementairesSemaine", 10.00);
        body.put("heuresSupplementairesDimancheFerie", 8.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                // 10 h × 16 × 1,5 + 8 h × 16 × 2,0 = 240 + 256 = 496 €
                .andExpect(jsonPath("$.sursalaireHeuresSupplementaires")
                        .value(496.00));
    }

    @Test
    void POST_workspaceBe_cp124Construction_taux912pct() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("commissionParitaireUtilisateur", "CP_124_CONSTRUCTION");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primeFinAnneeSectorielle").value(998.09))
                .andExpect(jsonPath("$.tauxPrimeFinAnneeApplique").value(0.0912));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStandard())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStandard())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStandard())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStandard())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INDEMNITES_DUES"))
                .andExpect(jsonPath("$.commissionParitaireUtilisateur")
                        .value("CP_200_AUXILIAIRE_EMPLOYES"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateDebutManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.remove("dateDebutMission");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_cpManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.remove("commissionParitaireUtilisateur");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("salaireHoraireBrut", 0.00);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeReelleNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("dureeReellePrestationJours", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureePrevueZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("dureePrevueJours", 0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ancienneteNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStandard();
        body.put("ancienneteSectorielleJours", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur InterimBeCct322ControllerIT) ----

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user);
        a.setProvider("GOOGLE");
        a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name, String legalDomain,
            String country) {
        Workspace ws = new Workspace();
        ws.setName(name);
        ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner);
        ws.setLegalDomain(legalDomain);
        ws.setCountry(country);
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws);
        m.setUser(user);
        m.setMemberRole("OWNER");
        m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private CaseFile saveCf(User user, Workspace ws, String title,
            String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title);
        cf.setWorkspace(ws);
        cf.setCreatedBy(user);
        cf.setLegalDomain(domain);
        cf.setStatus("OPEN");
        return caseFileRepository.save(cf);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub,
                "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token",
                Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser,
                oidcUser.getAuthorities(), "google");
    }
}
