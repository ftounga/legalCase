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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AesHumanitaireControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authDt;
    private CaseFile immFrCf;
    private CaseFile immBeCf;
    private CaseFile dtFrCf;

    private final LocalDate entreeIlYa4Ans = LocalDate.now().minusYears(4);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR immigration
        User uFr = save(new User(), u -> { u.setEmail("aesh-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-aesh-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRIh " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRIh " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-aesh-fr-" + ts, "aesh-fr-" + ts + "@ex.com");

        // BE immigration — gate country FR → 400
        User uBe = save(new User(), u -> { u.setEmail("aesh-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-aesh-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEIh " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEIh " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-aesh-be-" + ts, "aesh-be-" + ts + "@ex.com");

        // FR droit du travail — gate legal_domain → 400
        User uDt = save(new User(), u -> { u.setEmail("aesh-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-aesh-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRTh " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRTh " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-aesh-dt-" + ts, "aesh-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate entree,
                                     MotifHumanitaire motif,
                                     Boolean preuvesMed,
                                     Boolean preuvesViolences,
                                     Boolean asileRejete,
                                     Boolean commissionSaisie,
                                     Boolean menace,
                                     LocalDate depot) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateEntreeFrance", entree != null ? entree.toString() : null);
        m.put("motifHumanitaireDominant", motif != null ? motif.name() : null);
        m.put("preuvesMedicales", preuvesMed);
        m.put("preuvesViolencesOuTraite", preuvesViolences);
        m.put("demandeAsileDeposeeEtRejetee", asileRejete);
        m.put("commissionTitreSejourSaisie", commissionSaisie);
        m.put("menaceOrdrePublic", menace);
        m.put("dateDepotDemande", depot != null ? depot.toString() : null);
        return m;
    }

    private Map<String, Object> bodyNominalElevee() {
        return body(entreeIlYa4Ans, MotifHumanitaire.VICTIME_VIOLENCES,
                false, true, false, true, false, null);
    }

    @Test
    void POST_fr_victimeViolences_avecPreuves_verdictEleve() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.motifHumanitaireDominant").value("VICTIME_VIOLENCES"))
                .andExpect(jsonPath("$.motifEligible").value(true))
                .andExpect(jsonPath("$.preuvesAdaptees").value(true))
                .andExpect(jsonPath("$.commissionRequise").value(true))
                .andExpect(jsonPath("$.pasMenace").value(true))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.435-2")));
    }

    @Test
    void POST_fr_medicalSansPreuves_verdictFaible() throws Exception {
        Map<String, Object> body = body(entreeIlYa4Ans,
                MotifHumanitaire.SITUATION_MEDICALE_PRECAIRE_HORS_L425_9,
                false, false, false, false, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preuvesAdaptees").value(false))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void POST_fr_menaceOrdrePublic_scoreZero() throws Exception {
        Map<String, Object> body = body(entreeIlYa4Ans,
                MotifHumanitaire.VICTIME_VIOLENCES,
                false, true, false, true, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasMenace").value(false))
                .andExpect(jsonPath("$.scoreGlobal").value(0))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aes-humanitaire")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr → dossier BE → isolation 404
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_nullMotif_returns400() throws Exception {
        Map<String, Object> body = body(entreeIlYa4Ans, null,
                false, true, false, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_futureDateEntree_returns400() throws Exception {
        Map<String, Object> body = body(LocalDate.now().plusDays(1),
                MotifHumanitaire.VICTIME_VIOLENCES,
                false, true, false, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));

        Map<String, Object> next = body(entreeIlYa4Ans,
                MotifHumanitaire.AUTRE_HUMANITAIRE,
                false, false, false, false, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifEligible").value(false))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.motifHumanitaireDominant").value("VICTIME_VIOLENCES"))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/aes-humanitaire")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

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
