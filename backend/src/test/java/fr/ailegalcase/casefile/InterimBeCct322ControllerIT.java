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
 * SF-219-14 : tests d'intégration de l'endpoint <i>statut intérim BE
 * CCT n° 322</i> (Loi du 24/07/1987 + CCT n° 322 du 14/06/2010 du CNT).
 *
 * <p>Couvre : POST BE 200 (verdicts ELIGIBLE_MISSION_REGULIERE /
 * INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT / INELIGIBLE_MOTIF_NON_AUTORISE
 * / INELIGIBLE_DUREE_MAX_DEPASSEE / INELIGIBLE_PARITE_SALARIALE_VIOLEE
 * / FRAGILE_CONTRAT_OU_DIMONA_MANQUANT / A_ANALYSER), POST FR 404 (gate
 * BE strict), POST caseFile autre workspace 404, POST dossier non
 * DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400 (champs requis et bornes).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class InterimBeCct322ControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/interim-be-cct-322";

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
        User uBe = save(new User(), u -> { u.setEmail("itm-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-itm-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE ITM " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE ITM " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-itm-be-" + ts, "itm-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("itm-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-itm-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR ITM " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR ITM " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-itm-fr-" + ts, "itm-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("itm-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-itm-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 ITM " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 ITM " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — mission intérim éligible (surcroît temporaire, parité OK). */
    private Map<String, Object> bodyEligible() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutMission", "2024-06-01");
        m.put("motifMission", "SURCROIT_TEMPORAIRE_DE_TRAVAIL");
        m.put("remplacementGreveOuLockout", false);
        m.put("dureeTotaleMissionJours", 90);
        m.put("dureeMaxLegaleJours", 180);
        m.put("salaireHoraireIntimaireBrut", 16.00);
        m.put("salaireHoraireReferenceBrut", 16.00);
        m.put("contratEcritSigne", true);
        m.put("dimonaDeclareeParEti", true);
        return m;
    }

    @Test
    void POST_workspaceBe_eligible_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_MISSION_REGULIERE"))
                .andExpect(jsonPath("$.motifAutorise").value(true))
                .andExpect(jsonPath("$.dureeRespectee").value(true))
                .andExpect(jsonPath("$.pariteRespectee").value(true))
                .andExpect(jsonPath("$.formalismeRespecte").value(true))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("24/07/1987")));
    }

    @Test
    void POST_workspaceBe_greveLockout_returnsInterditGrevePrime() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("remplacementGreveOuLockout", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT"))
                .andExpect(jsonPath("$.raison").value("MOTIF_INTERDIT_GREVE_LOCKOUT"));
    }

    @Test
    void POST_workspaceBe_motifNonAutorise_returnsMotifNonAutorise() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("motifMission", "AUTRE_NON_AUTORISE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_MOTIF_NON_AUTORISE"))
                .andExpect(jsonPath("$.motifAutorise").value(false));
    }

    @Test
    void POST_workspaceBe_motifArtistique_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("motifMission", "MOTIF_ARTISTIQUE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("MOTIF_ZONE_GRISE_ANALYSE_CAS_PAR_CAS"));
    }

    @Test
    void POST_workspaceBe_dureeDepassee_returnsDureeMax() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dureeTotaleMissionJours", 400);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_DUREE_MAX_DEPASSEE"))
                .andExpect(jsonPath("$.joursExcedentaires").value(220));
    }

    @Test
    void POST_workspaceBe_pariteViolee_returnsPariteVioleeWithEcart() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("salaireHoraireIntimaireBrut", 12.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_PARITE_SALARIALE_VIOLEE"))
                .andExpect(jsonPath("$.pariteRespectee").value(false))
                .andExpect(jsonPath("$.ecartPariteSalariale").value(4.00));
    }

    @Test
    void POST_workspaceBe_dimonaManquante_returnsFragileFormalisme() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dimonaDeclareeParEti", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FRAGILE_CONTRAT_OU_DIMONA_MANQUANT"))
                .andExpect(jsonPath("$.formalismeRespecte").value(false));
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
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_MISSION_REGULIERE"))
                .andExpect(jsonPath("$.motifMission").value("SURCROIT_TEMPORAIRE_DE_TRAVAIL"));
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
        body.remove("dateDebutMission");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_motifManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.remove("motifMission");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeTotaleNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dureeTotaleMissionJours", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeMaxLegaleZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dureeMaxLegaleJours", 0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireReferenceZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("salaireHoraireReferenceBrut", 0.0);
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
