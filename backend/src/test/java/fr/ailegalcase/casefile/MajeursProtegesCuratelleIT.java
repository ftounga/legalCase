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

/**
 * SF-FA-25-03 : tests d'intégration pour les régimes CURATELLE_SIMPLE
 * (art. 440 al. 1 Cciv) et CURATELLE_RENFORCEE (art. 472 Cciv).
 *
 * <p>Couvre l'extension du contrat API : nouveaux champs request
 * {@code incapaciteGestionQuotidienne} et response {@code eligible} +
 * {@code criteresNonRemplis}, et la persistance via la colonne ajoutée
 * en migration 159.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class MajeursProtegesCuratelleIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private CaseFile famFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User uFr = save(new User(), u -> { u.setEmail("mp-cur-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-mp-cur-" + ts);
        Workspace wsFr = saveWs(uFr, "WSCUR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFCUR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-mp-cur-" + ts, "mp-cur-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String regime,
                                     Boolean alterationMentale,
                                     Boolean alterationPhysique,
                                     Boolean certificat,
                                     Boolean consentement,
                                     String demandeur,
                                     List<String> actes,
                                     Boolean urgence,
                                     Boolean patrimoine,
                                     Boolean isolement,
                                     Boolean incapaciteQuotidienne) {
        Map<String, Object> m = new HashMap<>();
        m.put("regimeProtectionDemande", regime);
        m.put("altertationFacultesMentales", alterationMentale);
        m.put("altertationFacultesPhysiques", alterationPhysique);
        m.put("certificatMedicalCirconstancie", certificat);
        m.put("dateCertificatMedical", LocalDate.of(2026, 4, 15).toString());
        m.put("consentementPersonneAProteger", consentement);
        m.put("demandeurFamilial", demandeur);
        m.put("actesEnvisages", actes);
        m.put("urgencePatrimoniale", urgence);
        m.put("patrimoineSignificatif", patrimoine);
        m.put("isolementSocial", isolement);
        if (incapaciteQuotidienne != null) {
            m.put("incapaciteGestionQuotidienne", incapaciteQuotidienne);
        }
        return m;
    }

    @Test
    void POST_curatelleSimple_eligibleTrue() throws Exception {
        Map<String, Object> b = body("CURATELLE_SIMPLE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("CURATELLE_SIMPLE"))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.criteresNonRemplis").isEmpty())
                .andExpect(jsonPath("$.incapaciteGestionQuotidienne").value(false))
                .andExpect(jsonPath("$.delaiProcedureMoisPrevisionnel").value(8));
    }

    @Test
    void POST_curatelleRenforcee_incapaciteGestionQuotidienne_eligibleTrue() throws Exception {
        Map<String, Object> b = body("CURATELLE_RENFORCEE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true);

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("CURATELLE_RENFORCEE"))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.incapaciteGestionQuotidienne").value(true));
    }

    @Test
    void POST_curatelleRenforcee_sansIncapacite_eligibleFalse_critereExplicite() throws Exception {
        Map<String, Object> b = body("CURATELLE_RENFORCEE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.criteresNonRemplis[0]")
                        .value(org.hamcrest.Matchers.containsString("Incapacité")));
    }

    @Test
    void POST_curatelleRenforcee_remplaceSauvegardePrecedente() throws Exception {
        // 1er POST : sauvegarde
        Map<String, Object> b1 = body("SAUVEGARDE_JUSTICE",
                true, false, true, false,
                "CONJOINT",
                List.of("GESTION_PATRIMOINE"),
                true, true, false,
                null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("SAUVEGARDE_JUSTICE"));

        // 2e POST : curatelle renforcée → upsert
        Map<String, Object> b2 = body("CURATELLE_RENFORCEE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("CURATELLE_RENFORCEE"))
                .andExpect(jsonPath("$.incapaciteGestionQuotidienne").value(true));
    }

    @Test
    void GET_apresPostCuratelle_persistanceIncapacite() throws Exception {
        Map<String, Object> b = body("CURATELLE_RENFORCEE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incapaciteGestionQuotidienne").value(true))
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("CURATELLE_RENFORCEE"))
                .andExpect(jsonPath("$.eligible").value(true));
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
