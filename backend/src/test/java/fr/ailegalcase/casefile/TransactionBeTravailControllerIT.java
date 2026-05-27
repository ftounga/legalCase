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
 * SF-213-06 : tests d'intégration end-to-end de l'outil
 * transaction-be-travail — gate BE-only strict, isolation workspace,
 * persistance et upsert, validation Bean, matrice de verdict (4 états),
 * ratio + avertissement de lésion, sérialisation List&lt;Item&gt; checklist.
 *
 * <p>Pattern mirroré de {@link LicenciementBeProtectionGrossesseControllerIT}
 * (SF-213-05).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class TransactionBeTravailControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/transaction-be-travail";

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
        User uBe = save(new User(), u -> { u.setEmail("txn-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-txn-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-txn-be-" + ts, "txn-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(), u -> { u.setEmail("txn-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-txn-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-txn-fr-" + ts, "txn-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(), u -> { u.setEmail("txn-o-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-txn-o-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-txn-o-" + ts, "txn-o-" + ts + "@ex.com");
    }

    // ── POST nominal / matrice de verdict ──────────────────────────────────

    @Test
    void POST_be_toutesConditionsOK_returns200_VALIDE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("indemniteLegaleEtimee", 12000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of(
                "Renonciation à l'action en indemnité de rupture",
                "Renonciation à l'action CCT 109"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.raisonInvalidite").doesNotExist())
                .andExpect(jsonPath("$.ratioPourcentage").value(83.3))
                .andExpect(jsonPath("$.avertissement").doesNotExist())
                .andExpect(jsonPath("$.checklistRenonciations.length()").value(2))
                .andExpect(jsonPath("$.checklistRenonciations[0].valide").value(true))
                .andExpect(jsonPath("$.checklistRenonciations[1].valide").value(true))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Art. 2044")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("03/07/1978")));
    }

    @Test
    void POST_be_absenceConcessions_returns200_INVALIDE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("indemniteLegaleEtimee", 12000.00);
        body.put("concessionsEmployeurDescrites", false);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INVALIDE"))
                .andExpect(jsonPath("$.raisonInvalidite").value("ABSENCE_CONCESSIONS"));
    }

    @Test
    void POST_be_renonciationOrdrePublic_returns200_INVALIDE_PARTIELLE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("indemniteLegaleEtimee", 12000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R1", "R2"));
        body.put("renonciationOrdrePublicDetectee", true);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INVALIDE_PARTIELLE"))
                .andExpect(jsonPath("$.raisonInvalidite").value("RENONCIATION_ORDRE_PUBLIC"))
                .andExpect(jsonPath("$.checklistRenonciations[0].valide").value(false))
                .andExpect(jsonPath("$.checklistRenonciations[1].valide").value(false));
    }

    @Test
    void POST_be_litigeNonVise_returns200_A_COMPLETER() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("indemniteLegaleEtimee", 12000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_COMPLETER"))
                .andExpect(jsonPath("$.raisonInvalidite").value("LITIGE_NON_VISE"));
    }

    @Test
    void POST_be_checklistVide_returns200_A_COMPLETER() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("indemniteLegaleEtimee", 12000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of());
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_COMPLETER"))
                .andExpect(jsonPath("$.raisonInvalidite").value("LITIGE_NON_VISE"))
                .andExpect(jsonPath("$.checklistRenonciations.length()").value(0));
    }

    @Test
    void POST_be_ratio45percent_avertissementPresent() throws Exception {
        // 4500 / 10000 × 100 = 45.0 % → avertissement.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 4500.00);
        body.put("indemniteLegaleEtimee", 10000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.ratioPourcentage").value(45.0))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("lésion")));
    }

    @Test
    void POST_be_sansIndemnite_ratioNull() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        // indemniteLegaleEtimee omise
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.ratioPourcentage").doesNotExist())
                .andExpect(jsonPath("$.avertissement").doesNotExist());
    }

    // ── Gate BE-only ────────────────────────────────────────────────────────

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        // authBe sur le case file FR → pas membre du workspace FR → 404
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ── Validation 400 ──────────────────────────────────────────────────────

    @Test
    void POST_montantZero_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 0);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_montantNegatif_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", -100);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_montantManquant_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // montantTransactionBrut omis → @NotNull
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of("R"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_renonciationsListeesManquantes_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("concessionsEmployeurDescrites", true);
        // renonciationsListees omise → @NotNull
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── Upsert + GET + round-trip JSON checklist ───────────────────────────

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("montantTransactionBrut", 5000.00);
        first.put("indemniteLegaleEtimee", 10000.00);
        first.put("concessionsEmployeurDescrites", true);
        first.put("renonciationsListees", List.of("R1"));
        first.put("renonciationOrdrePublicDetectee", false);
        first.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTransactionBrut").value(5000.00))
                .andExpect(jsonPath("$.ratioPourcentage").value(50.0));

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("montantTransactionBrut", 15000.00);
        second.put("indemniteLegaleEtimee", 10000.00);
        second.put("concessionsEmployeurDescrites", true);
        second.put("renonciationsListees", List.of("R1", "R2", "R3"));
        second.put("renonciationOrdrePublicDetectee", false);
        second.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTransactionBrut").value(15000.00))
                .andExpect(jsonPath("$.ratioPourcentage").value(150.0))
                .andExpect(jsonPath("$.checklistRenonciations.length()").value(3));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot_roundTripChecklist() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantTransactionBrut", 10000.00);
        body.put("indemniteLegaleEtimee", 12000.00);
        body.put("concessionsEmployeurDescrites", true);
        body.put("renonciationsListees", List.of(
                "Renonciation à l'action en indemnité de rupture",
                "Renonciation à l'action CCT 109"));
        body.put("renonciationOrdrePublicDetectee", false);
        body.put("mentionContestation", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Round-trip JSON List<Item> : la sérialisation/désérialisation doit
        // restituer une liste d'objets {item, valide}, pas une chaîne.
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("VALIDE"))
                .andExpect(jsonPath("$.montantTransactionBrut").value(10000.00))
                .andExpect(jsonPath("$.indemniteLegaleEtimee").value(12000.00))
                .andExpect(jsonPath("$.renonciationsListees.length()").value(2))
                .andExpect(jsonPath("$.checklistRenonciations.length()").value(2))
                .andExpect(jsonPath("$.checklistRenonciations[0].item").value(
                        "Renonciation à l'action en indemnité de rupture"))
                .andExpect(jsonPath("$.checklistRenonciations[0].valide").value(true))
                .andExpect(jsonPath("$.checklistRenonciations[1].item").value(
                        "Renonciation à l'action CCT 109"))
                .andExpect(jsonPath("$.checklistRenonciations[1].valide").value(true));
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
