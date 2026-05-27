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
class SinglePermitBeControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile immBeCf;
    private CaseFile immFrCf;
    private CaseFile dtBeCf;

    /** Permit valable ~6 mois encore — évite EXPIRE et URGENT pour les tests nominaux. */
    private final LocalDate debutPermit = LocalDate.now().minusMonths(6);
    private final LocalDate finPermit = LocalDate.now().plusMonths(6);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uBe = save(new User(), u -> { u.setEmail("singlepermit-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-singlepermit-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-singlepermit-be-" + ts, "singlepermit-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("singlepermit-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-singlepermit-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-singlepermit-fr-" + ts, "singlepermit-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("singlepermit-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-singlepermit-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBET " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBET " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-singlepermit-dt-" + ts, "singlepermit-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate debut, LocalDate fin, String region,
                                     String typeActivite, String motif) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutPermit", debut.toString());
        m.put("dateFinPermit", fin.toString());
        m.put("regionInstruction", region);
        m.put("typeActivite", typeActivite);
        m.put("motifDemande", motif);
        return m;
    }

    @Test
    void POST_be_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "WALLONIE", "SALARIE", "RENOUVELLEMENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionInstruction").value("WALLONIE"))
                .andExpect(jsonPath("$.typeActivite").value("SALARIE"))
                .andExpect(jsonPath("$.motifDemande").value("RENOUVELLEMENT"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.statutRenouvellement").exists())
                .andExpect(jsonPath("$.dateLimiteDemande").exists())
                .andExpect(jsonPath("$.joursAvantExpiration").exists())
                .andExpect(jsonPath("$.regionCompetente").value(org.hamcrest.Matchers.containsString("FOREM")))
                .andExpect(jsonPath("$.etapesProchaines").isArray())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("30/04/1999")));
    }

    @Test
    void POST_region_bruxelles_returns_ACTIRIS_in_regionCompetente() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "BRUXELLES", "SALARIE", "NOUVEAU"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionInstruction").value("BRUXELLES"))
                .andExpect(jsonPath("$.regionCompetente").value(org.hamcrest.Matchers.containsString("ACTIRIS")));
    }

    @Test
    void POST_motif_nouveau_returns_5_etapes_avec_visa_D() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "FLANDRE", "CHERCHEUR", "NOUVEAU"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motifDemande").value("NOUVEAU"))
                .andExpect(jsonPath("$.regionCompetente").value(org.hamcrest.Matchers.containsString("VDAB")))
                .andExpect(jsonPath("$.etapesProchaines.length()").value(5));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "WALLONIE", "SALARIE", "NOUVEAU"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "WALLONIE", "SALARIE", "NOUVEAU"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation)
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "WALLONIE", "SALARIE", "NOUVEAU"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_datesIncoherentes_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(finPermit, debutPermit, "WALLONIE", "SALARIE", "NOUVEAU"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_invalidRegion_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "ZONE_INCONNUE", "SALARIE", "NOUVEAU"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "WALLONIE", "SALARIE", "NOUVEAU"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionInstruction").value("WALLONIE"))
                .andExpect(jsonPath("$.motifDemande").value("NOUVEAU"));

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "BRUXELLES", "ETUDIANT", "RENOUVELLEMENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionInstruction").value("BRUXELLES"))
                .andExpect(jsonPath("$.typeActivite").value("ETUDIANT"))
                .andExpect(jsonPath("$.motifDemande").value("RENOUVELLEMENT"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(debutPermit, finPermit, "FLANDRE", "STAGIAIRE", "NOUVEAU"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionInstruction").value("FLANDRE"))
                .andExpect(jsonPath("$.typeActivite").value("STAGIAIRE"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/single-permit-be-analysis")
                        .with(authentication(authBe)))
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
