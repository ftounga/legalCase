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
 * SF-219-29 : tests d'integration de l'endpoint <i>Rente AT/MP vs
 * capitalisation BE</i> (Loi du 10/04/1971 art. 24 + Lois coordonnees
 * du 03/06/1970 art. 35 + AR du 21/12/1971 + AR du 24/02/2005 -
 * bareme).
 *
 * <p>Couvre : POST BE 200 (CAPITAL_FORFAITAIRE_LT_19 /
 * RENTE_ANNUELLE_GE_19 / INELIGIBLE_NON_RECONNU / IPP_NON_DETERMINE),
 * seuil 19% strict, conversion partielle apres delai 3 ans, POST FR
 * 404 (gate BE strict), POST caseFile autre workspace 404, POST
 * dossier non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour
 * persiste, validations Bean Validation 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AtMpRenteCapitalBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/at-mp-rente-capital-be";

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
        User uBe = save(new User(), u -> { u.setEmail("rente-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rente-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE RENTE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE RENTE " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-rente-" + ts, "rente-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("rentefr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rentefr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR RENTE " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR RENTE " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rentefr-" + ts, "rentefr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("rentebe2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-rentebe2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 RENTE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 RENTE " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — IPP 10% → capital forfaitaire. */
    private Map<String, Object> bodyCapitalForfaitaire() {
        Map<String, Object> m = new HashMap<>();
        m.put("origine", "ACCIDENT_TRAVAIL");
        m.put("statutReconnaissance", "RECONNU");
        m.put("dateConsolidation", "2024-06-01");
        m.put("tauxIpp", 10);
        m.put("remunerationBaseAnnuelle", 45000);
        m.put("dateNaissance", "1979-03-15");
        m.put("demandeConversionPartielle", false);
        return m;
    }

    /** Body — IPP 35% → rente annuelle. */
    private Map<String, Object> bodyRenteAnnuelle() {
        Map<String, Object> m = new HashMap<>();
        m.put("origine", "MALADIE_PROFESSIONNELLE");
        m.put("statutReconnaissance", "RECONNU");
        m.put("dateConsolidation", "2024-06-01");
        m.put("tauxIpp", 35);
        m.put("remunerationBaseAnnuelle", 45000);
        m.put("dateNaissance", "1979-03-15");
        m.put("demandeConversionPartielle", false);
        return m;
    }

    @Test
    void POST_workspaceBe_ippFaible_returnsCapitalForfaitaire() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCapitalForfaitaire())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CAPITAL_FORFAITAIRE_LT_19"))
                .andExpect(jsonPath("$.capitalisationDoffice").value(true))
                .andExpect(jsonPath("$.conversionPartiellePossible").value(false))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 10/04/1971")));
    }

    @Test
    void POST_workspaceBe_ippEleve_returnsRenteAnnuelle() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRenteAnnuelle())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RENTE_ANNUELLE_GE_19"))
                .andExpect(jsonPath("$.capitalisationDoffice").value(false))
                .andExpect(jsonPath("$.renteAnnuelle").value(15750.0));
    }

    @Test
    void POST_workspaceBe_ipp19Exact_returnsRenteAnnuelle_seuilEgal() throws Exception {
        Map<String, Object> body = bodyRenteAnnuelle();
        body.put("tauxIpp", 19);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RENTE_ANNUELLE_GE_19"));
    }

    @Test
    void POST_workspaceBe_conversionDemandeeDelaiEcoule_returnsConversionPossible() throws Exception {
        Map<String, Object> body = bodyRenteAnnuelle();
        body.put("demandeConversionPartielle", true);
        body.put("dateDemandeConversion", "2027-07-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RENTE_ANNUELLE_GE_19"))
                .andExpect(jsonPath("$.conversionPartiellePossible").value(true));
    }

    @Test
    void POST_workspaceBe_statutConteste_returnsIneligible() throws Exception {
        Map<String, Object> body = bodyRenteAnnuelle();
        body.put("statutReconnaissance", "CONTESTE");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_NON_RECONNU"));
    }

    @Test
    void POST_workspaceBe_statutEnCours_returnsIppNonDetermine() throws Exception {
        Map<String, Object> body = bodyRenteAnnuelle();
        body.put("statutReconnaissance", "EN_COURS");
        body.remove("tauxIpp");
        body.remove("dateConsolidation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("IPP_NON_DETERMINE"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCapitalForfaitaire())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCapitalForfaitaire())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCapitalForfaitaire())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyRenteAnnuelle())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RENTE_ANNUELLE_GE_19"))
                .andExpect(jsonPath("$.origine").value("MALADIE_PROFESSIONNELLE"));
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
    void POST_origineManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyCapitalForfaitaire();
        body.remove("origine");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_statutManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyCapitalForfaitaire();
        body.remove("statutReconnaissance");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyCapitalForfaitaire();
        body.remove("remunerationBaseAnnuelle");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyCapitalForfaitaire();
        body.put("remunerationBaseAnnuelle", -1000);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_tauxIppHors0_100_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyCapitalForfaitaire();
        body.put("tauxIpp", 150);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNaissanceManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyCapitalForfaitaire();
        body.remove("dateNaissance");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNaissanceApresConsolidation_returns400_serviceValidation() throws Exception {
        Map<String, Object> body = bodyCapitalForfaitaire();
        body.put("dateNaissance", "2030-01-01");
        body.put("dateConsolidation", "2024-06-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur MpFedrisReconnaissanceControllerIT) ----

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
