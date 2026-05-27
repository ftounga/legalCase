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
 * SF-213-07 : tests d'intégration end-to-end de l'outil
 * harcelement-be-procedure-formelle — gate BE-only strict, isolation
 * workspace, persistance et upsert, validation Bean, checklists par étape,
 * délai 90 j enquête CPAP (AR 10/04/2014), fenêtre protection 12 mois
 * (art. 32sexies), avertissements taille entreprise / CPAP, round-trip
 * JSON List&lt;Item&gt;.
 *
 * <p>Pattern miroir de {@link TransactionBeTravailControllerIT} (SF-213-06).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class HarcelementBeProcedureFormelleControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/harcelement-be-procedure-formelle";
    private static final String DATE_DEPOT = "2026-05-20";

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
                u -> { u.setEmail("hbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-hbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-hbe-" + ts, "hbe-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(),
                u -> { u.setEmail("hfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-hfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-hfr-" + ts, "hfr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(),
                u -> { u.setEmail("hot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-hot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-hot-" + ts, "hot-" + ts + "@ex.com");
    }

    // ── POST nominal / checklists par étape ────────────────────────────────

    @Test
    void POST_be_avantDepot_returns200_3items() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapeProcedure").value("AVANT_DEPOT"))
                .andExpect(jsonPath("$.checklistItems.length()").value(3))
                .andExpect(jsonPath("$.prochainDelaiFatal").doesNotExist())
                .andExpect(jsonPath("$.representaillesPossibles").value(false))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("32bis-32sexies")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("10/04/2014")));
    }

    @Test
    void POST_be_demandeFormelle_delai90j_etProtection12mois() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "SEXUEL");
        body.put("etapeProcedure", "DEMANDE_FORMELLE_EN_COURS");
        body.put("dateDepotPlainte", DATE_DEPOT);
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklistItems.length()").value(3))
                .andExpect(jsonPath("$.checklistItems[0].statut").value("EN_COURS"))
                .andExpect(jsonPath("$.checklistItems[0].dateEcheance").value("2026-08-18"))
                .andExpect(jsonPath("$.checklistItems[2].statut").value("ACTIF"))
                .andExpect(jsonPath("$.checklistItems[2].dateEcheance").value("2027-05-20"))
                .andExpect(jsonPath("$.prochainDelaiFatal").value("2026-08-18"))
                .andExpect(jsonPath("$.dateDebutProtectionRepresailles").value(DATE_DEPOT))
                .andExpect(jsonPath("$.dateFinProtectionRepresailles").value("2027-05-20"));
    }

    @Test
    void POST_be_mesureDefavorable_avecDate_representaillesPossiblesTrue() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "MESURE_DEFAVORABLE_APRES_PLAINTE");
        body.put("dateDepotPlainte", DATE_DEPOT);
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", true);
        body.put("delaiDepuisDepotJours", 30);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.representaillesPossibles").value(true))
                .andExpect(jsonPath("$.checklistItems.length()").value(3))
                .andExpect(jsonPath("$.checklistItems[0].statut").value("ACTIF"));
    }

    // ── Avertissements ─────────────────────────────────────────────────────

    @Test
    void POST_be_grandeEntrepriseSansCPAP_avertissementManquement() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", false);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("manquement")));
    }

    @Test
    void POST_be_petiteEntrepriseSansCPAP_avertissementSPF() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", false);
        body.put("entrepriseTaille", "MOINS_DE_50");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("SPF Emploi")));
    }

    @Test
    void POST_be_mesureAuDela365j_avertissementPresomptionAffaiblie() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "MESURE_DEFAVORABLE_APRES_PLAINTE");
        body.put("dateDepotPlainte", DATE_DEPOT);
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", true);
        body.put("delaiDepuisDepotJours", 400);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("présomption")));
    }

    // ── Gate BE-only ────────────────────────────────────────────────────────

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ── Validation 400 ──────────────────────────────────────────────────────

    @Test
    void POST_typeHarcelementManquant_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // typeHarcelement omis
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_etapeProcedureManquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        // etapeProcedure omise
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDepotManquanteHorsAvantDepot_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "DEMANDE_FORMELLE_EN_COURS");
        // dateDepotPlainte omise alors qu'on n'est pas en AVANT_DEPOT
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_entrepriseTailleManquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "MORAL");
        body.put("etapeProcedure", "AVANT_DEPOT");
        body.put("entreprisePossedeCPAP", true);
        // entrepriseTaille omise
        body.put("mesureDefavorableApres", false);

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
        first.put("typeHarcelement", "MORAL");
        first.put("etapeProcedure", "AVANT_DEPOT");
        first.put("entreprisePossedeCPAP", true);
        first.put("entrepriseTaille", "MOINS_DE_50");
        first.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapeProcedure").value("AVANT_DEPOT"))
                .andExpect(jsonPath("$.checklistItems.length()").value(3));

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("typeHarcelement", "SEXUEL");
        second.put("etapeProcedure", "DEMANDE_FORMELLE_EN_COURS");
        second.put("dateDepotPlainte", DATE_DEPOT);
        second.put("entreprisePossedeCPAP", true);
        second.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        second.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeHarcelement").value("SEXUEL"))
                .andExpect(jsonPath("$.etapeProcedure").value("DEMANDE_FORMELLE_EN_COURS"))
                .andExpect(jsonPath("$.checklistItems.length()").value(3))
                .andExpect(jsonPath("$.prochainDelaiFatal").value("2026-08-18"));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot_roundTripChecklist() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeHarcelement", "LES_DEUX");
        body.put("etapeProcedure", "DEMANDE_FORMELLE_EN_COURS");
        body.put("dateDepotPlainte", DATE_DEPOT);
        body.put("entreprisePossedeCPAP", true);
        body.put("entrepriseTaille", "CINQUANTE_ET_PLUS");
        body.put("mesureDefavorableApres", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Round-trip JSON List<Item> : la sérialisation/désérialisation doit
        // restituer une liste d'objets {item, statut, dateEcheance}.
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeHarcelement").value("LES_DEUX"))
                .andExpect(jsonPath("$.etapeProcedure").value("DEMANDE_FORMELLE_EN_COURS"))
                .andExpect(jsonPath("$.dateDepotPlainte").value(DATE_DEPOT))
                .andExpect(jsonPath("$.checklistItems.length()").value(3))
                .andExpect(jsonPath("$.checklistItems[0].item").value(
                        org.hamcrest.Matchers.containsString("Enquête CPAP")))
                .andExpect(jsonPath("$.checklistItems[0].statut").value("EN_COURS"))
                .andExpect(jsonPath("$.checklistItems[0].dateEcheance").value("2026-08-18"))
                .andExpect(jsonPath("$.checklistItems[2].statut").value("ACTIF"))
                .andExpect(jsonPath("$.prochainDelaiFatal").value("2026-08-18"));
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
