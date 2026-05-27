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
 * SF-213-08 : tests d'intégration end-to-end de l'outil
 * licenciement-be-protection-deleguee — gate BE-only strict, isolation
 * workspace, persistance et upsert, validation Bean, calcul indemnité
 * forfaitaire 2 ans / 4 ans (aggravantes), délai 30 j de réintégration
 * (Loi 19/03/1991 art. 14), incohérence refus sans demande.
 *
 * <p>Pattern miroir de {@link HarcelementBeProcedureFormelleControllerIT}
 * (SF-213-07).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class LicenciementBeProtectionDelegueeControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/licenciement-be-protection-deleguee";
    private static final String DATE_ELECTION = "2024-05-15";
    private static final String DATE_LICENCIEMENT_DANS_MANDAT = "2026-04-10";

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
                u -> { u.setEmail("lbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-lbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSLBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CLBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-lbe-" + ts, "lbe-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(),
                u -> { u.setEmail("lfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-lfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSLFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CLFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-lfr-" + ts, "lfr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(),
                u -> { u.setEmail("lot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-lot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSLOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "CLOT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-lot-" + ts, "lot-" + ts + "@ex.com");
    }

    // ── POST nominal : licenciement dans mandat → INTERDIT + indemnité 2 ans ──

    @Test
    void POST_be_delegueEluDansMandat_returns200_verdictInterdit_indemnite2ans() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("LICENCIEMENT_INTERDIT_SANS_PROCEDURE"))
                .andExpect(jsonPath("$.licenciementDansProtection").value(true))
                .andExpect(jsonPath("$.dateFinProtection").value("2028-05-15"))
                .andExpect(jsonPath("$.anneesForfait").value(2))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(80000.00))
                .andExpect(jsonPath("$.dateLimiteDemandeReintegration").value("2026-05-10"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("19/03/1991")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("CCT n° 5")));
    }

    // ── Circonstances aggravantes → 4 ans ──────────────────────────────────

    @Test
    void POST_be_circonstancesAggravantes_indemnite4ans() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("circonstancesAggravantes", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anneesForfait").value(4))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(160000.00));
    }

    // ── Candidat non élu hors fenêtre 2 ans → HORS_PERIODE_PROTECTION ──────

    @Test
    void POST_be_candidatNonEluHorsFenetre_verdictHorsProtection() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("statutProtege", "CANDIDAT_NON_ELU");
        body.put("dateLicenciement", "2026-08-01"); // > 2 ans après 2024-05-15

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HORS_PERIODE_PROTECTION"))
                .andExpect(jsonPath("$.licenciementDansProtection").value(false))
                .andExpect(jsonPath("$.dateFinProtection").value("2026-05-15"))
                .andExpect(jsonPath("$.indemniteForfaitaire").doesNotExist())
                .andExpect(jsonPath("$.anneesForfait").doesNotExist());
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
    void POST_statutProtegeManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("statutProtege");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateLicenciementManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateLicenciement");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateElectionManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateElectionOuMandat");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationZero_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationAnnuelleBrute", 0);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_refusEmployeurSansDemande_returns400_incoherence() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("demandeReintegrationDansTrente", false);
        body.put("employeurRefuseReintegration", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_refusEmployeurAvecDemande_returns200() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("demandeReintegrationDansTrente", true);
        body.put("employeurRefuseReintegration", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeurRefuseReintegration").value(true))
                .andExpect(jsonPath("$.demandeReintegrationDansTrente").value(true));
    }

    // ── Upsert + GET ────────────────────────────────────────────────────────

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = baseBody();
        first.put("statutProtege", "DELEGUE_CCT5");
        first.put("dateLicenciement", "2026-04-10");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutProtege").value("DELEGUE_CCT5"))
                .andExpect(jsonPath("$.dateFinProtection").value("2026-05-15"));

        Map<String, Object> second = baseBody();
        second.put("statutProtege", "CONSEILLER_CPPT");
        second.put("circonstancesAggravantes", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutProtege").value("CONSEILLER_CPPT"))
                .andExpect(jsonPath("$.anneesForfait").value(4));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("statutProtege", "DELEGUE_SYNDICAL_ELU");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutProtege").value("DELEGUE_SYNDICAL_ELU"))
                .andExpect(jsonPath("$.dateLicenciement").value(DATE_LICENCIEMENT_DANS_MANDAT))
                .andExpect(jsonPath("$.dateElectionOuMandat").value(DATE_ELECTION))
                .andExpect(jsonPath("$.verdict")
                        .value("LICENCIEMENT_INTERDIT_SANS_PROCEDURE"))
                .andExpect(jsonPath("$.indemniteForfaitaire").value(80000.00))
                .andExpect(jsonPath("$.anneesForfait").value(2));
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
        body.put("statutProtege", "DELEGUE_SYNDICAL_ELU");
        body.put("ancienneteAnnees", 5);
        body.put("remunerationAnnuelleBrute", 40000.00);
        body.put("dateLicenciement", DATE_LICENCIEMENT_DANS_MANDAT);
        body.put("dateElectionOuMandat", DATE_ELECTION);
        body.put("demandeReintegrationDansTrente", false);
        body.put("employeurRefuseReintegration", false);
        body.put("circonstancesAggravantes", false);
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
