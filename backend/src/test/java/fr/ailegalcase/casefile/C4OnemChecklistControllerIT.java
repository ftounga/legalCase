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
 * SF-207-02 : tests d'intégration de l'endpoint C4 ONEM checklist.
 *
 * <p>Couvre les cas mini-spec : POST BE 200 (nominal CONFORME), POST FR 404
 * (isolation pays — gate BE strict), POST caseFile autre workspace 404
 * (isolation workspace), GET après POST, GET sans POST 404, validation 400
 * (Bean Validation sur nomSalarie / dateEntreeService), BCE format invalide
 * 400, et faute grave (verdict RISQUE_EXCLUSION_FAUTE_GRAVE).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class C4OnemChecklistControllerIT {

    private static final String URL = "/api/v1/case-files/%s/decision-tools/c4-onem-checklist";

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
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("c4-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-c4-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE C4 " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE C4 " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-c4-be-" + ts, "c4-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("c4-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-c4-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR C4 " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR C4 " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-c4-fr-" + ts, "c4-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("c4-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-c4-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 C4 " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 C4 " + ts, "DROIT_DU_TRAVAIL");
    }

    /**
     * Body complet et conforme (10 mentions présentes, fauteGrave=false).
     */
    private Map<String, Object> bodyConforme() {
        Map<String, Object> m = new HashMap<>();
        m.put("raisonSocialeEmployeur", "ACME SA");
        m.put("numeroBce", "0123456789");
        m.put("nomSalarie", "Dupont Jean");
        m.put("numeroNationalRegistre", "85010112345");
        m.put("dateEntreeService", "2020-01-15");
        m.put("dateSortieService", "2026-04-30");
        m.put("categorieOnem", "1");
        m.put("motifExplicite", "Licenciement pour réorganisation économique");
        m.put("fauteGraveMentionnee", false);
        m.put("preavisPresteJours", 30);
        m.put("dernierSalaireMensuelBrut", 3450.75);
        return m;
    }

    @Test
    void POST_workspaceBe_conforme_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("CONFORME"))
                .andExpect(jsonPath("$.mentionsManquantes").isArray())
                .andExpect(jsonPath("$.mentionsManquantes.length()").value(0))
                .andExpect(jsonPath("$.fauteGraveDetectee").value(false))
                .andExpect(jsonPath("$.exclusionOnemRange").doesNotExist())
                .andExpect(jsonPath("$.lettreRectificativeProposee").doesNotExist())
                .andExpect(jsonPath("$.etapeSuivante").value("AUCUNE"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("25 novembre 1991")));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        // Avocat FR essaie l'endpoint sur son propre dossier FR → 404
        // (gate BE strict, l'outil n'existe pas côté FR).
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        // Avocat BE essaie d'accéder à un caseFile d'un autre workspace BE → 404
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isNotFound());
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
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("CONFORME"))
                .andExpect(jsonPath("$.raisonSocialeEmployeur").value("ACME SA"))
                .andExpect(jsonPath("$.numeroBce").value("0123456789"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_nomSalarieManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("nomSalarie");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateEntreeManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("dateEntreeService");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_bceFormatInvalide_returns400() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("numeroBce", "BE12345"); // mal formaté
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fauteGraveTrue_verdictRISQUE_EXCLUSION() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("fauteGraveMentionnee", true);
        body.put("categorieOnem", "9");
        body.put("motifExplicite", "Insubordination répétée le 12/04");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RISQUE_EXCLUSION_FAUTE_GRAVE"))
                .andExpect(jsonPath("$.fauteGraveDetectee").value(true))
                .andExpect(jsonPath("$.exclusionOnemRange.minSemaines").value(4))
                .andExpect(jsonPath("$.exclusionOnemRange.maxSemaines").value(52))
                .andExpect(jsonPath("$.etapeSuivante").value("CONTESTATION_C4"))
                .andExpect(jsonPath("$.lettreRectificativeProposee").doesNotExist());
    }

    @Test
    void POST_mentionManquante_verdictNON_CONFORME_avecLettre() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("numeroBce"); // BCE absent → mention manquante
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME"))
                .andExpect(jsonPath("$.mentionsManquantes",
                        org.hamcrest.Matchers.hasItem("NUMERO_BCE")))
                .andExpect(jsonPath("$.lettreRectificativeProposee").exists())
                .andExpect(jsonPath("$.etapeSuivante").value("RECTIFICATION_AUPRES_EMPLOYEUR"));
    }

    // ---- helpers (alignés sur PrescriptionBeLitigeTravailControllerIT) ----

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
