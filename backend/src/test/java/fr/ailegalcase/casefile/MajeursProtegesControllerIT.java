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
class MajeursProtegesControllerIT {

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
    private CaseFile famFrCf;
    private CaseFile famBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_FAMILLE
        User uFr = save(new User(), u -> { u.setEmail("mp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-mp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSMPFR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-mp-fr-" + ts, "mp-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE
        User uBe = save(new User(), u -> { u.setEmail("mp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-mp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSMPBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-mp-be-" + ts, "mp-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL
        User uDt = save(new User(), u -> { u.setEmail("mp-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-mp-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSMPDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-mp-dt-" + ts, "mp-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String regime,
                                     Boolean alterationMentale,
                                     Boolean alterationPhysique,
                                     Boolean certificat,
                                     LocalDate dateCert,
                                     Boolean consentement,
                                     String demandeur,
                                     List<String> actes,
                                     Boolean urgence,
                                     Boolean patrimoine,
                                     Boolean isolement) {
        Map<String, Object> m = new HashMap<>();
        m.put("regimeProtectionDemande", regime);
        m.put("altertationFacultesMentales", alterationMentale);
        m.put("altertationFacultesPhysiques", alterationPhysique);
        m.put("certificatMedicalCirconstancie", certificat);
        m.put("dateCertificatMedical", dateCert != null ? dateCert.toString() : null);
        m.put("consentementPersonneAProteger", consentement);
        m.put("demandeurFamilial", demandeur);
        m.put("actesEnvisages", actes);
        m.put("urgencePatrimoniale", urgence);
        m.put("patrimoineSignificatif", patrimoine);
        m.put("isolementSocial", isolement);
        return m;
    }

    private Map<String, Object> bodyHabilitationElevee() {
        return body("HABILITATION_FAMILIALE",
                true, false,
                true, LocalDate.of(2026, 4, 15),
                true,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false);
    }

    @Test
    void POST_fr_nominal_habilitation_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHabilitationElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreEligibilite").value(100))
                .andExpect(jsonPath("$.verdictAcceptabiliteJaf").value("ELEVEE"))
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("HABILITATION_FAMILIALE"))
                .andExpect(jsonPath("$.delaiProcedureMoisPrevisionnel").value(4))
                .andExpect(jsonPath("$.auditionPersonneObligatoire").value(true))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("433-441")));
    }

    @Test
    void POST_fr_sauvegarde_urgence() throws Exception {
        Map<String, Object> b = body("SAUVEGARDE_JUSTICE",
                true, false,
                true, LocalDate.of(2026, 4, 15),
                false,
                "CONJOINT",
                List.of("GESTION_PATRIMOINE"),
                true, true, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("SAUVEGARDE_JUSTICE"))
                .andExpect(jsonPath("$.delaiProcedureMoisPrevisionnel").value(4));
    }

    @Test
    void POST_fr_regimeInvalide_returns400() throws Exception {
        Map<String, Object> b = body("INVALIDE",
                true, false,
                true, LocalDate.of(2026, 4, 15),
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_demandeurInvalide_returns400() throws Exception {
        Map<String, Object> b = body("HABILITATION_FAMILIALE",
                true, false,
                true, LocalDate.of(2026, 4, 15),
                true,
                "AMI",
                List.of(),
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_regimeManquant_returns400() throws Exception {
        Map<String, Object> b = body(null,
                true, false,
                true, LocalDate.of(2026, 4, 15),
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/majeurs-proteges")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHabilitationElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHabilitationElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr essaie d'accéder à dossier BE → 404 isolation
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHabilitationElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHabilitationElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("HABILITATION_FAMILIALE"));

        // 2e appel : tutelle isolement social → différent
        Map<String, Object> next = body("TUTELLE",
                true, false,
                true, LocalDate.of(2026, 5, 1),
                false,
                "FRERE_SOEUR",
                List.of(),
                false, false, true);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("TUTELLE"))
                .andExpect(jsonPath("$.delaiProcedureMoisPrevisionnel").value(8));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyHabilitationElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreEligibilite").value(100))
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("HABILITATION_FAMILIALE"))
                .andExpect(jsonPath("$.actesEnvisages.length()").value(1));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
