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
 * SF-219-24 : tests d'intégration de l'endpoint <i>Code pénal social
 * BE — qualification d'infraction + niveau de sanction</i> (Loi du
 * 06/06/2010 art. 101-110).
 *
 * <p>Couvre : POST BE 200 (verdicts SANCTION_NIVEAU_1 / 2 / 3 / 4 /
 * A_QUALIFIER), POST FR 404 (gate BE strict), POST caseFile autre
 * workspace 404, POST dossier non DROIT_DU_TRAVAIL 400, GET 404 sans
 * POST, GET retour persisté, validations Bean Validation 400,
 * majoration personne morale + multiplication travailleurs + récidive,
 * niveauPropose prime sur défaut.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CodePenalSocialBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/code-penal-social-be";

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
        User uBe = save(new User(), u -> { u.setEmail("cpsbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cpsbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE CPS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE CPS " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-cpsbe-" + ts, "cpsbe-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("cpsfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cpsfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR CPS " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR CPS " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-cpsfr-" + ts, "cpsfr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("cpsbe2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-cpsbe2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 CPS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 CPS " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — travail non déclaré (niveau 4), pas de majoration. */
    private Map<String, Object> bodyTravailNonDeclare() {
        Map<String, Object> m = new HashMap<>();
        m.put("typeInfraction", "TRAVAIL_NON_DECLARE");
        m.put("niveauPropose", null);
        m.put("dateFaits", "2024-03-15");
        m.put("nombreTravailleursConcernes", 0);
        m.put("personneMorale", false);
        m.put("recidiveDansLAn", false);
        m.put("prevenuPreposeOuMandataire", false);
        m.put("elementMoralIntentionnel", false);
        return m;
    }

    @Test
    void POST_workspaceBe_travailNonDeclare_returnsNiveau4() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyTravailNonDeclare())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_4"))
                .andExpect(jsonPath("$.niveauSanction").value(4))
                .andExpect(jsonPath("$.amendeAdminMin").value(300))
                .andExpect(jsonPath("$.amendeAdminMax").value(3000))
                .andExpect(jsonPath("$.amendePenaleMin").value(600))
                .andExpect(jsonPath("$.amendePenaleMax").value(6000))
                .andExpect(jsonPath("$.emprisonnementApplicable").value(true))
                .andExpect(jsonPath("$.emprisonnementMinMois").value(6))
                .andExpect(jsonPath("$.emprisonnementMaxMois").value(36))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 06/06/2010")));
    }

    @Test
    void POST_workspaceBe_nonPaiementRemuneration_returnsNiveau2() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("typeInfraction", "NON_PAIEMENT_REMUNERATION");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_2"))
                .andExpect(jsonPath("$.niveauSanction").value(2))
                .andExpect(jsonPath("$.amendeAdminMin").value(50))
                .andExpect(jsonPath("$.amendeAdminMax").value(500))
                .andExpect(jsonPath("$.emprisonnementApplicable").value(false));
    }

    @Test
    void POST_workspaceBe_depassementTempsTravail_returnsNiveau3() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("typeInfraction", "DEPASSEMENT_TEMPS_TRAVAIL");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_3"))
                .andExpect(jsonPath("$.amendeAdminMax").value(1000))
                .andExpect(jsonPath("$.emprisonnementApplicable").value(false));
    }

    @Test
    void POST_workspaceBe_absenceDmfaIntentionnel_returnsNiveau4() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("typeInfraction", "ABSENCE_DMFA");
        body.put("elementMoralIntentionnel", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_4"));
    }

    @Test
    void POST_workspaceBe_absenceDmfaNonIntentionnel_returnsNiveau2() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("typeInfraction", "ABSENCE_DMFA");
        body.put("elementMoralIntentionnel", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_2"));
    }

    @Test
    void POST_workspaceBe_autreQualificationSansNiveau_returnsAQualifier() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("typeInfraction", "AUTRE_QUALIFICATION");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_QUALIFIER"))
                .andExpect(jsonPath("$.niveauSanction").value(0))
                .andExpect(jsonPath("$.raison").value("QUALIFICATION_OUVERTE"));
    }

    @Test
    void POST_workspaceBe_autreQualificationAvecNiveau1_returnsNiveau1() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("typeInfraction", "AUTRE_QUALIFICATION");
        body.put("niveauPropose", 1);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_1"))
                .andExpect(jsonPath("$.amendeAdminMin").value(10))
                .andExpect(jsonPath("$.amendeAdminMax").value(100))
                .andExpect(jsonPath("$.amendePenaleMin").value(0))
                .andExpect(jsonPath("$.amendePenaleMax").value(0));
    }

    @Test
    void POST_workspaceBe_niveauProposePrimeSurDefaut() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        // TRAVAIL_NON_DECLARE par défaut = niveau 4 ; on force niveau 2.
        body.put("niveauPropose", 2);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_2"))
                .andExpect(jsonPath("$.niveauSanction").value(2))
                .andExpect(jsonPath("$.emprisonnementApplicable").value(false));
    }

    @Test
    void POST_workspaceBe_personneMorale_drapeauActif() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
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
        Map<String, Object> body = bodyTravailNonDeclare();
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
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("recidiveDansLAn", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.majorationRecidive").value(true));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyTravailNonDeclare())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyTravailNonDeclare())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyTravailNonDeclare())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyTravailNonDeclare())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SANCTION_NIVEAU_4"))
                .andExpect(jsonPath("$.typeInfraction").value("TRAVAIL_NON_DECLARE"));
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
    void POST_typeInfractionManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.remove("typeInfraction");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_niveauProposeHorsBornes_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("niveauPropose", 5);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nbTravailleursNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.put("nombreTravailleursConcernes", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_personneMoraleManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.remove("personneMorale");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_elementMoralIntentionnelManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyTravailNonDeclare();
        body.remove("elementMoralIntentionnel");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur EgaliteFemmesHommesBeControllerIT) ----

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
