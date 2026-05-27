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
 * SF-213-05 : tests d'intégration end-to-end de l'outil
 * licenciement-be-protection-grossesse — gate BE-only strict, isolation
 * workspace, persistance et upsert, validation Bean, matrice de verdict
 * (4 états), calcul indemnité 6 mois (art. 40 al. 3 Loi 16/03/1971).
 *
 * <p>Pattern mirroré de {@link LicenciementBeFormuleClaeysControllerIT}
 * (SF-213-04).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class LicenciementBeProtectionGrossesseControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/licenciement-be-protection-grossesse";

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
        User uBe = save(new User(), u -> { u.setEmail("grossesse-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-grossesse-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-grossesse-be-" + ts, "grossesse-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(), u -> { u.setEmail("grossesse-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-grossesse-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-grossesse-fr-" + ts, "grossesse-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(), u -> { u.setEmail("grossesse-o-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-grossesse-o-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-grossesse-o-" + ts, "grossesse-o-" + ts + "@ex.com");
    }

    // ── POST nominal / matrice de verdict ──────────────────────────────────

    @Test
    void POST_be_protection_applicable_non_notifiee_returns200() throws Exception {
        // Début grossesse 2026-01-15, accouchement 2026-10-22, licenciement J+2 sans notification
        // → PROTECTION_APPLICABLE_NON_NOTIFIEE + indemnité 3000 × 6 = 18 000 €
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateAccouchement", "2026-10-22");
        body.put("dateLicenciement", "2026-01-17");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PROTECTION_APPLICABLE_NON_NOTIFIEE"))
                .andExpect(jsonPath("$.licenciementDansLaPeriodeProtegee").value(true))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(18000.00))
                .andExpect(jsonPath("$.chargePreuveEmployeur").value(true))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Loi 16/03/1971")))
                .andExpect(jsonPath("$.formuleCalcul").value(
                        org.hamcrest.Matchers.containsString("18000")));
    }

    @Test
    void POST_be_protection_presumee_charge_inversee_returns200() throws Exception {
        // Notification écrite + licenciement J+9 sem → PROTECTION_PRESUMEE
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateAccouchement", "2026-10-22");
        body.put("dateLicenciement", "2026-03-19"); // J + 9 sem
        body.put("grossesseNotifieeParEcrit", true);
        body.put("remunerationMensuelleBrute", 3000.00);
        body.put("motifInvoqueParEmployeur", "Restructuration");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PROTECTION_PRESUMEE"))
                .andExpect(jsonPath("$.chargePreuveEmployeur").value(true))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(18000.00))
                .andExpect(jsonPath("$.motifInvoqueParEmployeur").value("Restructuration"));
    }

    @Test
    void POST_be_protection_applicable_hors_fenetre_10sem_returns200() throws Exception {
        // Notification écrite + licenciement J+11 sem → PROTECTION_APPLICABLE
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateAccouchement", "2026-10-22");
        body.put("dateLicenciement", "2026-04-02"); // J + 11 sem
        body.put("grossesseNotifieeParEcrit", true);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PROTECTION_APPLICABLE"))
                .andExpect(jsonPath("$.chargePreuveEmployeur").value(true))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(18000.00));
    }

    @Test
    void POST_be_hors_periode_protection_returns200_sans_indemnite() throws Exception {
        // Fin congé maternité = 2026-02-04 → fin protection = 2026-03-04.
        // Licenciement = 2026-03-05 → HORS.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2025-04-22");
        body.put("dateCongeMaterniteFinale", "2026-02-04");
        body.put("dateLicenciement", "2026-03-05");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HORS_PERIODE_PROTECTION"))
                .andExpect(jsonPath("$.licenciementDansLaPeriodeProtegee").value(false))
                .andExpect(jsonPath("$.indemniteForfaitaire").doesNotExist())
                .andExpect(jsonPath("$.chargePreuveEmployeur").value(false));
    }

    @Test
    void POST_be_avertissement_si_fin_protection_indeterminee() throws Exception {
        // Ni accouchement ni finConge → avertissement
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateLicenciement", "2026-03-01");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateFinProtection").doesNotExist())
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("Date fin protection indéterminée")));
    }

    // ── Gate BE-only ────────────────────────────────────────────────────────

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateLicenciement", "2026-03-01");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateLicenciement", "2026-03-01");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateLicenciement", "2026-03-01");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        // authBe sur le case file FR → pas membre du workspace FR → 404
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ── Validation 400 ──────────────────────────────────────────────────────

    @Test
    void POST_dateLicenciement_avant_dateDebutGrossesse_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateLicenciement", "2026-01-14"); // 1 jour avant
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationZero_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateLicenciement", "2026-03-01");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 0);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDebutGrossesse_manquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateLicenciement", "2026-03-01");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateLicenciement_manquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remuneration_manquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateLicenciement", "2026-03-01");
        body.put("grossesseNotifieeParEcrit", false);
        // remunerationMensuelleBrute omise → @NotNull

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── Upsert + GET ────────────────────────────────────────────────────────

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("dateDebutGrossesse", "2026-01-15");
        first.put("dateAccouchement", "2026-10-22");
        first.put("dateLicenciement", "2026-01-17");
        first.put("grossesseNotifieeParEcrit", false);
        first.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remunerationMensuelleBrute").value(3000.00))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(18000.00));

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("dateDebutGrossesse", "2026-01-15");
        second.put("dateAccouchement", "2026-10-22");
        second.put("dateLicenciement", "2026-01-17");
        second.put("grossesseNotifieeParEcrit", true);
        second.put("remunerationMensuelleBrute", 5000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remunerationMensuelleBrute").value(5000.00))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(30000.00))
                .andExpect(jsonPath("$.verdict").value("PROTECTION_PRESUMEE"));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateDebutGrossesse", "2026-01-15");
        body.put("dateAccouchement", "2026-10-22");
        body.put("dateLicenciement", "2026-01-17");
        body.put("grossesseNotifieeParEcrit", false);
        body.put("remunerationMensuelleBrute", 3000.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PROTECTION_APPLICABLE_NON_NOTIFIEE"))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(18000.00))
                .andExpect(jsonPath("$.dateDebutGrossesse").value("2026-01-15"))
                .andExpect(jsonPath("$.dateAccouchement").value("2026-10-22"))
                .andExpect(jsonPath("$.dateLicenciement").value("2026-01-17"));
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
