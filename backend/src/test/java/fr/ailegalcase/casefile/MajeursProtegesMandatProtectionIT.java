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
 * SF-FA-25-05 : tests d'intégration pour le régime
 * MANDAT_PROTECTION_FUTURE (art. 477-494 Cciv) — clôture F-FA-25 6/6
 * régimes.
 *
 * <p>Couvre l'extension du contrat API : 2 nouveaux champs request
 * {@code mandatPrealableSigne} + {@code formeMandatProtection} et response
 * symétrique ; persistance via les colonnes ajoutées en migration 161.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class MajeursProtegesMandatProtectionIT {

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
        User uFr = save(new User(), u -> { u.setEmail("mp-mpf-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-mp-mpf-" + ts);
        Workspace wsFr = saveWs(uFr, "WSMPF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFMPF " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-mp-mpf-" + ts, "mp-mpf-" + ts + "@ex.com");
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
                                     Boolean incapaciteQuotidienne,
                                     Boolean altertationGrave,
                                     Boolean mandatPrealableSigne,
                                     String formeMandatProtection) {
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
        if (altertationGrave != null) {
            m.put("altertationGrave", altertationGrave);
        }
        if (mandatPrealableSigne != null) {
            m.put("mandatPrealableSigne", mandatPrealableSigne);
        }
        if (formeMandatProtection != null) {
            m.put("formeMandatProtection", formeMandatProtection);
        }
        return m;
    }

    @Test
    void POST_mandatNotarie_eligibleTrue_verdictELEVEE() throws Exception {
        // tous critères mandat + NOTARIE → eligible=true verdict ELEVEE
        Map<String, Object> b = body("MANDAT_PROTECTION_FUTURE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false, false,
                true, "NOTARIE");

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("MANDAT_PROTECTION_FUTURE"))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.criteresNonRemplis").isEmpty())
                .andExpect(jsonPath("$.mandatPrealableSigne").value(true))
                .andExpect(jsonPath("$.formeMandatProtection").value("NOTARIE"))
                .andExpect(jsonPath("$.verdictAcceptabiliteJaf").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiProcedureMoisPrevisionnel").value(4));
    }

    @Test
    void POST_mandatSansPrealable_critereExplicite() throws Exception {
        // sans mandat préalable → critère explicite + autre régime recommandé
        Map<String, Object> b = body("MANDAT_PROTECTION_FUTURE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false,
                false, false,
                false /* sans mandat */, "NOTARIE");

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.mandatPrealableSigne").value(false))
                .andExpect(jsonPath("$.criteresNonRemplis[0]")
                        .value(org.hamcrest.Matchers.containsString("Mandat préalable")));
    }

    @Test
    void POST_mandatSousSeingPrive_acteGrave_verdictMOYENNE() throws Exception {
        // sous seing privé pour GESTION_PATRIMOINE → MOYENNE
        Map<String, Object> b = body("MANDAT_PROTECTION_FUTURE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false,
                false, false,
                true, "SOUS_SEING_PRIVE");

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.formeMandatProtection").value("SOUS_SEING_PRIVE"))
                .andExpect(jsonPath("$.verdictAcceptabiliteJaf").value("MOYENNE"));
    }

    @Test
    void POST_upsert_tutellePuisMandat() throws Exception {
        // 1er POST : tutelle
        Map<String, Object> b1 = body("TUTELLE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true, true,
                null, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("TUTELLE"));

        // 2e POST : mandat → upsert remplace
        Map<String, Object> b2 = body("MANDAT_PROTECTION_FUTURE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false, false,
                true, "NOTARIE");
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("MANDAT_PROTECTION_FUTURE"))
                .andExpect(jsonPath("$.mandatPrealableSigne").value(true))
                .andExpect(jsonPath("$.formeMandatProtection").value("NOTARIE"));
    }

    @Test
    void GET_apresPostMandat_persistanceChamps() throws Exception {
        Map<String, Object> b = body("MANDAT_PROTECTION_FUTURE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false, false,
                true, "NOTARIE");
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mandatPrealableSigne").value(true))
                .andExpect(jsonPath("$.formeMandatProtection").value("NOTARIE"))
                .andExpect(jsonPath("$.regimeOptimalRecommande").value("MANDAT_PROTECTION_FUTURE"))
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    void POST_mandat_formeInvalide_400() throws Exception {
        Map<String, Object> b = body("MANDAT_PROTECTION_FUTURE",
                true, false, true, false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false, false,
                true, "INVALIDE");

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/majeurs-proteges")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
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
