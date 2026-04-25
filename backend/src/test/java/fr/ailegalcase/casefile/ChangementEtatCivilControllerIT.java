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
class ChangementEtatCivilControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("cec-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cec-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSCECFR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-cec-fr-" + ts, "cec-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE
        User uBe = save(new User(), u -> { u.setEmail("cec-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cec-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSCECBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-cec-be-" + ts, "cec-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL
        User uDt = save(new User(), u -> { u.setEmail("cec-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-cec-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSCECDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-cec-dt-" + ts, "cec-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String type,
                                     String motif,
                                     List<String> preuves,
                                     Boolean majeur,
                                     Boolean consentement,
                                     Boolean datesConcordants,
                                     Boolean dejaChange,
                                     LocalDate dateNaissance,
                                     String departement) {
        Map<String, Object> m = new HashMap<>();
        m.put("typeChangement", type);
        m.put("motifInvoque", motif);
        m.put("preuvesProduites", preuves);
        m.put("majeurDemandeur", majeur);
        m.put("consentementParental", consentement);
        m.put("datesDocsConcordants", datesConcordants);
        m.put("dejaChangeAuparavant", dejaChange);
        m.put("dateNaissanceDemandeur", dateNaissance != null
                ? dateNaissance.toString() : null);
        m.put("departementDeclaration", departement);
        return m;
    }

    private Map<String, Object> bodyPrenomMairieElevee() {
        return body("PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, false,
                LocalDate.of(1985, 4, 15),
                "75");
    }

    @Test
    void POST_fr_prenom_nominal_mairie() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPrenomMairieElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.competenceProcedure").value("MAIRIE"))
                .andExpect(jsonPath("$.delaiInstructionMoisPrevisionnel").value(2))
                .andExpect(jsonPath("$.verdictAcceptabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("60")));
    }

    @Test
    void POST_fr_nom_interetLegitime30Ans_mairie() throws Exception {
        Map<String, Object> b = body("NOM",
                "INTERET_LEGITIME",
                List.of("JUSTIFICATIF_USAGE_30ANS", "ACTES_CIVILS",
                        "CERTIFICAT_NAISSANCE"),
                true, false, true, false,
                LocalDate.of(1985, 4, 15),
                "75");
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competenceProcedure").value("MAIRIE"))
                .andExpect(jsonPath("$.delaiInstructionMoisPrevisionnel").value(2));
    }

    @Test
    void POST_fr_sexe_juge_delai3mois() throws Exception {
        Map<String, Object> b = body("SEXE",
                "IDENTIFICATION_GENRE",
                List.of("TEMOIGNAGES", "CERTIFICAT_NAISSANCE"),
                true, false, true, false,
                LocalDate.of(1985, 4, 15),
                "75");
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competenceProcedure").value("JUGE"))
                .andExpect(jsonPath("$.delaiInstructionMoisPrevisionnel").value(3));
    }

    @Test
    void POST_fr_typeInvalide_returns400() throws Exception {
        Map<String, Object> b = body("INVALIDE",
                "MARIAGE",
                List.of(),
                true, false, true, false,
                null, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_motifInvalide_returns400() throws Exception {
        Map<String, Object> b = body("PRENOM",
                "INVALIDE",
                List.of(),
                true, false, true, false,
                null, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_typeManquant_returns400() throws Exception {
        Map<String, Object> b = body(null,
                "MARIAGE",
                List.of(),
                true, false, true, false,
                null, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/changement-etat-civil")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPrenomMairieElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPrenomMairieElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr essaie d'accéder à dossier BE → 404 isolation
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPrenomMairieElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPrenomMairieElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competenceProcedure").value("MAIRIE"));

        // 2e appel : sexe → JUGE
        Map<String, Object> next = body("SEXE",
                "IDENTIFICATION_GENRE",
                List.of("TEMOIGNAGES", "CERTIFICAT_NAISSANCE"),
                true, false, true, false,
                LocalDate.of(1985, 4, 15),
                "75");
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeChangement").value("SEXE"))
                .andExpect(jsonPath("$.competenceProcedure").value("JUGE"))
                .andExpect(jsonPath("$.delaiInstructionMoisPrevisionnel").value(3));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPrenomMairieElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.competenceProcedure").value("MAIRIE"))
                .andExpect(jsonPath("$.preuvesProduites.length()").value(2));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/changement-etat-civil")
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
