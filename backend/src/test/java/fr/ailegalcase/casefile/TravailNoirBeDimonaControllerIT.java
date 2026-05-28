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
 * SF-219-26 : tests d'intégration de l'endpoint <i>Travail noir BE —
 * DIMONA</i> (Loi-programme du 24/12/2002 + AR du 05/11/2002 + Code
 * pénal social art. 181 niveau 4).
 *
 * <p>Couvre : POST BE 200 (DIMONA_CONFORME / DIMONA_TARDIVE_REGULARISABLE
 * / ABSENCE_DIMONA_SANCTION_NIVEAU_4 / INDEPENDANT_REQUALIFIE_NON_DECLARE
 * / A_QUALIFIER), POST FR 404 (gate BE strict), POST caseFile autre
 * workspace 404, POST dossier non DROIT_DU_TRAVAIL 400, GET 404 sans
 * POST, GET retour persisté, validations Bean Validation 400,
 * majoration personne morale + multiplication travailleurs + récidive,
 * calcul cotisations ONSS rétroactives.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class TravailNoirBeDimonaControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/travail-noir-be-dimona";

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
        User uBe = save(new User(), u -> { u.setEmail("tndbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-tndbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE TND " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE TND " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-tndbe-" + ts, "tndbe-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("tndfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-tndfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR TND " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR TND " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-tndfr-" + ts, "tndfr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("tndbe2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-tndbe2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 TND " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 TND " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — DIMONA ABSENTE + subordination OUI (niveau 4). */
    private Map<String, Object> bodyAbsenteNiveau4() {
        Map<String, Object> m = new HashMap<>();
        m.put("statutDimona", "ABSENTE");
        m.put("dateDebutOccupation", "2024-01-01");
        m.put("dateDimonaEffective", null);
        m.put("dateControle", "2024-07-01");
        m.put("salaireBrutMensuel", "3000.00");
        m.put("nombreTravailleursConcernes", 1);
        m.put("personneMorale", false);
        m.put("recidiveDansLAn", false);
        m.put("elementsSubordination", "OUI");
        return m;
    }

    @Test
    void POST_workspaceBe_absenteNiveau4_returnsSanctionFerme() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAbsenteNiveau4())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("ABSENCE_DIMONA_SANCTION_NIVEAU_4"))
                .andExpect(jsonPath("$.sanctionPenaleNiveau4Applicable").value(true))
                .andExpect(jsonPath("$.amendePenaleAdminMin").value(300))
                .andExpect(jsonPath("$.amendePenaleAdminMax").value(3000))
                .andExpect(jsonPath("$.amendePenaleMin").value(600))
                .andExpect(jsonPath("$.amendePenaleMax").value(6000))
                .andExpect(jsonPath("$.emprisonnementApplicable").value(true))
                .andExpect(jsonPath("$.emprisonnementMinMois").value(6))
                .andExpect(jsonPath("$.emprisonnementMaxMois").value(36))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi-programme du 24/12/2002")));
    }

    @Test
    void POST_workspaceBe_conforme_returnsAucuneSanction() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("statutDimona", "DECLAREE_AVANT_DEBUT");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DIMONA_CONFORME"))
                .andExpect(jsonPath("$.sanctionPenaleNiveau4Applicable").value(false))
                .andExpect(jsonPath("$.emprisonnementApplicable").value(false))
                .andExpect(jsonPath("$.cotisationsOnssTotal").value(0));
    }

    @Test
    void POST_workspaceBe_tardive_returnsRegularisable() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("statutDimona", "DECLAREE_TARDIVE");
        body.put("dateDimonaEffective", "2024-04-01");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("DIMONA_TARDIVE_REGULARISABLE"))
                .andExpect(jsonPath("$.sanctionPenaleNiveau4Applicable").value(false))
                .andExpect(jsonPath("$.emprisonnementApplicable").value(false));
    }

    @Test
    void POST_workspaceBe_independantFaux_subordOui_returnsRequalifie() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("statutDimona", "INDEPENDANT_FAUSSEMENT_QUALIFIE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INDEPENDANT_REQUALIFIE_NON_DECLARE"))
                .andExpect(jsonPath("$.sanctionPenaleNiveau4Applicable").value(true));
    }

    @Test
    void POST_workspaceBe_subordIndetermine_returnsAQualifier() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("elementsSubordination", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_QUALIFIER"))
                .andExpect(jsonPath("$.sanctionPenaleNiveau4Applicable").value(false));
    }

    @Test
    void POST_workspaceBe_personneMorale_drapeauActif() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("personneMorale", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.majorationPersonneMorale").value(true));
    }

    @Test
    void POST_workspaceBe_multTravailleurs_drapeauActif() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("nombreTravailleursConcernes", 8);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multiplicationParTravailleur").value(true))
                .andExpect(jsonPath("$.nombreTravailleursConcernes").value(8));
    }

    @Test
    void POST_workspaceBe_recidive_drapeauActif() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("recidiveDansLAn", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.majorationRecidive").value(true));
    }

    @Test
    void POST_workspaceBe_calculCotisations_employeur25_travailleur1307() throws Exception {
        // 30 jours × 3000 € → employeur 750 €, travailleur 392.10 €
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("dateDebutOccupation", "2024-01-01");
        body.put("dateControle", "2024-01-31");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeNonDeclareeJours").value(30))
                .andExpect(jsonPath("$.cotisationsOnssEmployeur").value(750.00))
                .andExpect(jsonPath("$.cotisationsOnssTravailleur").value(392.10))
                .andExpect(jsonPath("$.cotisationsOnssTotal").value(1142.10))
                .andExpect(jsonPath("$.amendeOnssForfaitaire3x").value(3426.30));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAbsenteNiveau4())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAbsenteNiveau4())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAbsenteNiveau4())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAbsenteNiveau4())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("ABSENCE_DIMONA_SANCTION_NIVEAU_4"))
                .andExpect(jsonPath("$.statutDimona").value("ABSENTE"));
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
    void POST_statutDimonaManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.remove("statutDimona");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDebutManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.remove("dateDebutOccupation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateControleManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.remove("dateControle");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("salaireBrutMensuel", "-100.00");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nbTravailleursZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("nombreTravailleursConcernes", 0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_personneMoraleManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.remove("personneMorale");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_elementsSubordinationManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.remove("elementsSubordination");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_tardiveSansDateDimona_returns400_serviceValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("statutDimona", "DECLAREE_TARDIVE");
        // dateDimonaEffective volontairement omise → IllegalArgumentException
        body.put("dateDimonaEffective", null);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateControleAvantDebut_returns400_serviceValidation() throws Exception {
        Map<String, Object> body = bodyAbsenteNiveau4();
        body.put("dateDebutOccupation", "2024-06-01");
        body.put("dateControle", "2024-01-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur CodePenalSocialBeControllerIT) ----

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
