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
 * SF-219-17 : tests d'intégration de l'endpoint <i>clause d'écolage
 * BE</i> (art. 22bis Loi 03/07/1978).
 *
 * <p>Couvre : POST BE 200 (verdicts VALIDE_REMBOURSEMENT_DEGRESSIF /
 * VALIDE_DUREE_EXPIREE / INOPPOSABLE_MOTIF_DEPART /
 * NULLE_FORME_ECRITE_MANQUANTE / NULLE_FORMATION_OBLIGATOIRE /
 * NULLE_COUT_INSUFFISANT / NULLE_DUREE_EXCESSIVE / A_ANALYSER), POST FR
 * 404 (gate BE strict), POST caseFile autre workspace 404, POST dossier
 * non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ClauseEcolageBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/clause-ecolage-be";

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
        User uBe = save(new User(), u -> { u.setEmail("eco-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-eco-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE ECO " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE ECO " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-eco-be-" + ts, "eco-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("eco-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-eco-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR ECO " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR ECO " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-eco-fr-" + ts, "eco-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("eco-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-eco-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 ECO " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 ECO " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — clause valide tier 1 (départ démission 6 mois après fin formation). */
    private Map<String, Object> bodyNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("typeFormation", "SPECIFIQUE");
        m.put("clauseEcriteAvantEntreeFormation", true);
        m.put("coutReelFormationEuros", 6000.00);
        m.put("rmmmgMensuelEuros", 2000.00);
        m.put("dureeEfficaciteMois", 36);
        m.put("dateFinFormation", "2023-01-01");
        m.put("dateDepartTravailleur", "2023-07-01");
        m.put("motifDepart", "DEMISSION_TRAVAILLEUR");
        return m;
    }

    @Test
    void POST_workspaceBe_nominalDemission_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE_REMBOURSEMENT_DEGRESSIF"))
                .andExpect(jsonPath("$.tierDureeAuDepart").value(1))
                // 100 % × 6000 = 6000 ; plafond 80 % = 4800 → min = 4800
                .andExpect(jsonPath("$.montantBrutDueEuros").value(6000.00))
                .andExpect(jsonPath("$.plafond80Euros").value(4800.00))
                .andExpect(jsonPath("$.montantDuFinalEuros").value(4800.00))
                .andExpect(jsonPath("$.coutMinLegalEuros").value(1000.00))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("22bis")));
    }

    @Test
    void POST_workspaceBe_motifInopposable_returnsInopposable() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("motifDepart", "LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INOPPOSABLE_MOTIF_DEPART"))
                .andExpect(jsonPath("$.raison").value("MOTIF_DEPART_EXCLU"))
                .andExpect(jsonPath("$.montantDuFinalEuros").value(0.00));
    }

    @Test
    void POST_workspaceBe_formationObligatoire_returnsNulle() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("typeFormation", "OBLIGATOIRE_LEGALE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NULLE_FORMATION_OBLIGATOIRE"))
                .andExpect(jsonPath("$.raison").value("FORMATION_OBLIGATOIRE_LEGALE"));
    }

    @Test
    void POST_workspaceBe_typeIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("typeFormation", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("TYPE_FORMATION_INDETERMINE"));
    }

    @Test
    void POST_workspaceBe_formeEcriteManquante_returnsNulleForme() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("clauseEcriteAvantEntreeFormation", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NULLE_FORME_ECRITE_MANQUANTE"))
                .andExpect(jsonPath("$.raison").value("FORME_ECRITE_MANQUANTE"));
    }

    @Test
    void POST_workspaceBe_coutInsuffisant_returnsNulleCout() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("coutReelFormationEuros", 800.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NULLE_COUT_INSUFFISANT"))
                .andExpect(jsonPath("$.raison").value("COUT_INFERIEUR_MOITIE_RMMMG"))
                .andExpect(jsonPath("$.coutMinLegalEuros").value(1000.00));
    }

    @Test
    void POST_workspaceBe_dureeExcessive_returnsNulleDuree() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dureeEfficaciteMois", 48);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NULLE_DUREE_EXCESSIVE"))
                .andExpect(jsonPath("$.raison").value("DUREE_EFFICACITE_EXCESSIVE"));
    }

    @Test
    void POST_workspaceBe_dureeExpiree_returnsValideDureeExpiree() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dateDepartTravailleur", "2026-06-01"); // 41 mois après fin formation → > 36

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE_DUREE_EXPIREE"))
                .andExpect(jsonPath("$.tierDureeAuDepart").value(4))
                .andExpect(jsonPath("$.montantDuFinalEuros").value(0.00));
    }

    @Test
    void POST_workspaceBe_tier3_returns33Pct() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dateDepartTravailleur", "2025-07-01"); // 30 mois → tier 3

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE_REMBOURSEMENT_DEGRESSIF"))
                .andExpect(jsonPath("$.tierDureeAuDepart").value(3))
                // 0.3333 × 6000 = 1999.80
                .andExpect(jsonPath("$.montantDuFinalEuros").value(1999.80));
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
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
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
                .andExpect(jsonPath("$.verdict").value("VALIDE_REMBOURSEMENT_DEGRESSIF"))
                .andExpect(jsonPath("$.typeFormation").value("SPECIFIQUE"))
                .andExpect(jsonPath("$.dureeEfficaciteMois").value(36));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeEfficaciteMois").value(36));

        Map<String, Object> second = bodyNominal();
        second.put("dureeEfficaciteMois", 24);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeEfficaciteMois").value(24));
    }

    @Test
    void POST_typeFormationManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.remove("typeFormation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_coutNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("coutReelFormationEuros", -1.00);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_rmmmgZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("rmmmgMensuelEuros", 0.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dureeEfficaciteMois", 0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_motifManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.remove("motifDepart");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur ClauseNonConcurrenceBeControllerIT) ----

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
