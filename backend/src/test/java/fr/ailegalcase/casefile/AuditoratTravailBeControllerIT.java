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
 * SF-219-25 : tests d'intégration de l'endpoint <i>Auditorat du
 * travail BE — orientation et checklist de saisine du parquet
 * spécialisé en droit social pénal</i> (art. 138bis C. jud. + art. 24
 * CIC + Loi 03/08/1992 + Loi 06/06/2010).
 *
 * <p>Couvre : POST BE 200 (verdicts SAISINE_AUDITORAT_RECOMMANDEE /
 * DENONCIATION_INSPECTION_PREALABLE / SAISINE_NON_PERTINENTE /
 * A_QUALIFIER), POST FR 404 (gate BE strict), POST caseFile autre
 * workspace 404, POST dossier non DROIT_DU_TRAVAIL 400, GET 404 sans
 * POST, GET retour persisté, validations Bean Validation 400,
 * basculement inspection préalable travail non déclaré, prescription
 * bloquante, partie civile recommandée si recours pénal, urgence
 * signalée.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AuditoratTravailBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/auditorat-travail-be";

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
        User uBe = save(new User(), u -> { u.setEmail("audit-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-auditbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE AUDIT " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE AUDIT " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-auditbe-" + ts, "audit-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("audit-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-auditfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR AUDIT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR AUDIT " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-auditfr-" + ts, "audit-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("audit-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-auditbe2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 AUDIT " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 AUDIT " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — infraction pénale sociale, dénonciation simple, sans drapeau. */
    private Map<String, Object> bodyInfractionPenaleSociale() {
        Map<String, Object> m = new HashMap<>();
        m.put("natureFait", "INFRACTION_PENALE_SOCIALE");
        m.put("modeSaisineEnvisage", "DENONCIATION_SIMPLE");
        m.put("dateFaits", "2024-06-01");
        m.put("faitsPrescrits", false);
        m.put("inspectionDejaSaisie", false);
        m.put("plainteCivileDeposee", false);
        m.put("urgenceSecuritePersonnes", false);
        m.put("recoursPenalEnvisage", false);
        m.put("employeurPersonneMorale", true);
        return m;
    }

    @Test
    void POST_workspaceBe_infractionPenaleSociale_returnsSaisineRecommandee() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInfractionPenaleSociale())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_AUDITORAT_RECOMMANDEE"))
                .andExpect(jsonPath("$.modeSaisineRecommande").value("DENONCIATION_SIMPLE"))
                .andExpect(jsonPath("$.unaViaApplicable").value(true))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("art. 138bis")));
    }

    @Test
    void POST_workspaceBe_accidentGrave_returnsSaisineRecommandee() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("natureFait", "ACCIDENT_TRAVAIL_GRAVE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_AUDITORAT_RECOMMANDEE"))
                .andExpect(jsonPath("$.raison").value("SAISINE_ACCIDENT_GRAVE_AT"));
    }

    @Test
    void POST_workspaceBe_travailNonDeclareSansInspection_returnsInspectionPrealable() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("natureFait", "TRAVAIL_NON_DECLARE_SUSPECTE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DENONCIATION_INSPECTION_PREALABLE"))
                .andExpect(jsonPath("$.modeSaisineRecommande").value("SIGNALEMENT_INSPECTION_SOCIALE"))
                .andExpect(jsonPath("$.inspectionPrealableRequise").value(true));
    }

    @Test
    void POST_workspaceBe_travailNonDeclareAvecInspection_returnsSaisineRecommandee() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("natureFait", "TRAVAIL_NON_DECLARE_SUSPECTE");
        body.put("inspectionDejaSaisie", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_AUDITORAT_RECOMMANDEE"))
                .andExpect(jsonPath("$.inspectionPrealableRequise").value(false));
    }

    @Test
    void POST_workspaceBe_litigeCivilPur_returnsSaisineNonPertinente() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("natureFait", "LITIGE_CIVIL_PUR");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_NON_PERTINENTE"))
                .andExpect(jsonPath("$.modeSaisineRecommande").value("VOIE_CIVILE_PARALLELE"))
                .andExpect(jsonPath("$.voieCivileConcurrente").value(true))
                .andExpect(jsonPath("$.raison").value("COMPETENCE_TRIBUNAL_TRAVAIL_ART_578"));
    }

    @Test
    void POST_workspaceBe_faitsPrescrits_returnsSaisineNonPertinente() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("faitsPrescrits", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_NON_PERTINENTE"))
                .andExpect(jsonPath("$.prescriptionBloquante").value(true))
                .andExpect(jsonPath("$.raison").value("PRESCRIPTION_PENALE_ACQUISE"));
    }

    @Test
    void POST_workspaceBe_autre_returnsAQualifier() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("natureFait", "AUTRE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_QUALIFIER"))
                .andExpect(jsonPath("$.modeSaisineRecommande").value("INDETERMINE"))
                .andExpect(jsonPath("$.raison").value("QUALIFICATION_OUVERTE"));
    }

    @Test
    void POST_workspaceBe_recoursPenalEnvisage_recommandePlainteDirecte() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("natureFait", "HARCELEMENT_PENAL");
        body.put("recoursPenalEnvisage", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_AUDITORAT_RECOMMANDEE"))
                .andExpect(jsonPath("$.modeSaisineRecommande").value("PLAINTE_DIRECTE"))
                .andExpect(jsonPath("$.constitutionPartieCivileRecommandee").value(true));
    }

    @Test
    void POST_workspaceBe_avecPlainteCivileEtInfractionSociale_avisObligatoire() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("plainteCivileDeposee", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisAuditeurObligatoireCivil").value(true))
                .andExpect(jsonPath("$.voieCivileConcurrente").value(true));
    }

    @Test
    void POST_workspaceBe_urgenceSecurite_drapeauActif() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.put("natureFait", "ACCIDENT_TRAVAIL_GRAVE");
        body.put("urgenceSecuritePersonnes", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgenceSignalee").value(true));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInfractionPenaleSociale())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInfractionPenaleSociale())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInfractionPenaleSociale())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInfractionPenaleSociale())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_AUDITORAT_RECOMMANDEE"))
                .andExpect(jsonPath("$.natureFait").value("INFRACTION_PENALE_SOCIALE"));
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
    void POST_natureFaitManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.remove("natureFait");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_modeSaisineEnvisageManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.remove("modeSaisineEnvisage");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_faitsPrescritsManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.remove("faitsPrescrits");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_employeurPersonneMoraleManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyInfractionPenaleSociale();
        body.remove("employeurPersonneMorale");
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
