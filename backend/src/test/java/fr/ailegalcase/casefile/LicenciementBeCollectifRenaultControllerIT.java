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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-219-07 : tests d'intégration end-to-end de l'outil
 * licenciement-be-collectif-renault — gate BE-only strict, isolation
 * workspace, gate DROIT_DU_TRAVAIL, persistance et upsert, validation
 * Bean, hiérarchie verdicts (seuil &gt; information &gt; consultation &gt;
 * notification &gt; délai 30 j).
 *
 * <p>Pattern miroir de {@link CumulRccAllocationsControllerIT}
 * (SF-219-04).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class LicenciementBeCollectifRenaultControllerIT {

    private static final String PATH_SUFFIX =
            "/decision-tools/licenciement-be-collectif-renault";

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
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailBeCf;
    private CaseFile travailFrCf;
    private CaseFile immBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // BE workspace DROIT_DU_TRAVAIL
        User uBe = save(new User(),
                u -> { u.setEmail("lcrbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-lcrbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSLCRBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CLCRBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-lcrbe-" + ts, "lcrbe-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject 404)
        User uFr = save(new User(),
                u -> { u.setEmail("lcrfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-lcrfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSLCRFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CLCRFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-lcrfr-" + ts, "lcrfr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject 400)
        User uOt = save(new User(),
                u -> { u.setEmail("lcrot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-lcrot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSLCROT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "CLCROT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-lcrot-" + ts, "lcrot-" + ts + "@ex.com");
    }

    // ── POST nominal : conforme ────────────────────────────────────────────

    @Test
    void POST_be_conforme_returns200_verdictConforme() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME"))
                .andExpect(jsonPath("$.conforme").value(true))
                .andExpect(jsonPath("$.seuilDeclenchementAtteint").value(true))
                .andExpect(jsonPath("$.seuilLegalApplicable").value(10))
                .andExpect(jsonPath("$.delaiAttenteRespecte").value(true))
                .andExpect(jsonPath("$.dateFinDelaiAttente").value("2026-03-03"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("13/02/1998")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("CCT n° 24")))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("checklist")));
    }

    // ── Seuil non atteint ──────────────────────────────────────────────────

    @Test
    void POST_be_sousLeSeuil_returns200_verdictNonApplicableSeuil() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("nombreLicenciementsEnvisages", 5);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_APPLICABLE_SEUIL"))
                .andExpect(jsonPath("$.seuilDeclenchementAtteint").value(false));
    }

    // ── Phase 1 manquante ──────────────────────────────────────────────────

    @Test
    void POST_be_informationManquante_returns200_phase1NonConforme() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("informationEcriteCePrealable", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("NON_CONFORME_PHASE_INFORMATION_INCOMPLETE"))
                .andExpect(jsonPath("$.conforme").value(false));
    }

    // ── Phase 2 manquante ──────────────────────────────────────────────────

    @Test
    void POST_be_consultationInsuffisante_returns200_phase2NonConforme() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("consultationEffectiveTenue", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("NON_CONFORME_CONSULTATION_INSUFFISANTE"));
    }

    // ── Phase 3 manquante (notification autorité) ──────────────────────────

    @Test
    void POST_be_notificationAutoriteManquante_returns200_phase3NonConforme()
            throws Exception {
        Map<String, Object> body = baseBody();
        body.put("notificationAutoriteRegionaleFaite", false);
        body.remove("dateNotificationAutorite");
        body.remove("datePremierPreavisNotifie");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE"))
                .andExpect(jsonPath("$.dateFinDelaiAttente").doesNotExist());
    }

    // ── Délai 30 jours non respecté ────────────────────────────────────────

    @Test
    void POST_be_preavisAvantFinDelai_returns200_delaiNonRespecte() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("datePremierPreavisNotifie", "2026-02-10"); // < 03/03

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE"))
                .andExpect(jsonPath("$.delaiAttenteRespecte").value(false));
    }

    // ── Gate BE-only ────────────────────────────────────────────────────────

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = baseBody();

        // user BE essaie d'accéder à un case file FR auquel il n'appartient pas
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ── Validation 400 ──────────────────────────────────────────────────────

    @Test
    void POST_dateProjetManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateProjet");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_tailleEntrepriseManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("tailleEntreprise");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifNegatif_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("effectifMoyen", -3);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nombreLicenciementsNegatif_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("nombreLicenciementsEnvisages", -1);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_phaseAtteinteManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("phaseAtteinte");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_notificationFaiteSansDate_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateNotificationAutorite");
        body.remove("datePremierPreavisNotifie");
        // notificationAutoriteRegionaleFaite reste true → validation throw

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── Upsert + GET ────────────────────────────────────────────────────────

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = baseBody();
        first.put("nombreLicenciementsEnvisages", 12);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreLicenciementsEnvisages").value(12));

        Map<String, Object> second = baseBody();
        second.put("nombreLicenciementsEnvisages", 25);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreLicenciementsEnvisages").value(25));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME"))
                .andExpect(jsonPath("$.seuilLegalApplicable").value(10));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404_isolationBE() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Map<String, Object> baseBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateProjet", "2026-01-10");
        body.put("tailleEntreprise", "PME_20_99");
        body.put("effectifMoyen", 50);
        body.put("nombreLicenciementsEnvisages", 15);
        body.put("phaseAtteinte", "DECISION_ET_NOTIFICATION");
        body.put("informationEcriteCePrealable", true);
        body.put("documentsLegauxCommuniques", true);
        body.put("consultationEffectiveTenue", true);
        body.put("reponsesMotiveesEmployeur", true);
        body.put("notificationAutoriteRegionaleFaite", true);
        body.put("dateNotificationAutorite", "2026-02-01");
        body.put("datePremierPreavisNotifie", "2026-03-05");
        return body;
    }

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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
