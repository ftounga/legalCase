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

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class OrdonnanceRequeteControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFamFr;
    private OAuth2AuthenticationToken authFamBe;
    private OAuth2AuthenticationToken authTravail;
    private CaseFile famFrCf;
    private CaseFile famBeCf;
    private CaseFile travailCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace FR — droit de la famille
        User u1 = new User(); u1.setEmail("ord-fam-fr-" + ts + "@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-ord-famfr-" + ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("ORD FAM FR " + ts); ws1.setSlug("ws-ord-famfr-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_FAMILLE"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE"); ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1); m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        famFrCf = new CaseFile(); famFrCf.setTitle("ORD FAM FR " + ts); famFrCf.setWorkspace(ws1); famFrCf.setCreatedBy(u1);
        famFrCf.setLegalDomain("DROIT_FAMILLE"); famFrCf.setStatus("OPEN"); famFrCf = caseFileRepository.save(famFrCf);
        authFamFr = buildAuth("g-ord-famfr-" + ts, "ord-fam-fr-" + ts + "@ex.com");

        // Workspace BE — droit de la famille
        User u2 = new User(); u2.setEmail("ord-fam-be-" + ts + "@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-ord-fambe-" + ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("ORD FAM BE " + ts); ws2.setSlug("ws-ord-fambe-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_FAMILLE"); ws2.setCountry("BELGIQUE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE"); ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2); m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        famBeCf = new CaseFile(); famBeCf.setTitle("ORD FAM BE " + ts); famBeCf.setWorkspace(ws2); famBeCf.setCreatedBy(u2);
        famBeCf.setLegalDomain("DROIT_FAMILLE"); famBeCf.setStatus("OPEN"); famBeCf = caseFileRepository.save(famBeCf);
        authFamBe = buildAuth("g-ord-fambe-" + ts, "ord-fam-be-" + ts + "@ex.com");

        // Workspace FR — droit du travail (gate domaine)
        User u3 = new User(); u3.setEmail("ord-trav-" + ts + "@ex.com"); u3.setStatus("ACTIVE"); u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount(); a3.setUser(u3); a3.setProvider("GOOGLE"); a3.setProviderUserId("g-ord-trav-" + ts); authAccountRepository.save(a3);
        Workspace ws3 = new Workspace(); ws3.setName("ORD TRAV " + ts); ws3.setSlug("ws-ord-trav-" + ts); ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_DU_TRAVAIL"); ws3.setCountry("FRANCE"); ws3.setPlanCode("STARTER"); ws3.setStatus("ACTIVE"); ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember(); m3.setWorkspace(ws3); m3.setUser(u3); m3.setMemberRole("OWNER"); m3.setPrimary(true); workspaceMemberRepository.save(m3);
        travailCf = new CaseFile(); travailCf.setTitle("ORD TRAV " + ts); travailCf.setWorkspace(ws3); travailCf.setCreatedBy(u3);
        travailCf.setLegalDomain("DROIT_DU_TRAVAIL"); travailCf.setStatus("OPEN"); travailCf = caseFileRepository.save(travailCf);
        authTravail = buildAuth("g-ord-trav-" + ts, "ord-trav-" + ts + "@ex.com");
    }

    @Test
    void POST_toutOK_returnsELEVEE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAccordeProbabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.scoreEligibilite").value(100))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.recoursAdverseDelaiJours").value(15))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("493")));
    }

    @Test
    void POST_motifIST_avec_enfants_returnsDelai_1_3() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("motifRequete", "ENLEVEMENT_INTERNATIONAL");
        body.put("presenceEnfants", true);

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delaiTypiqueJoursMin").value(1))
                .andExpect(jsonPath("$.delaiTypiqueJoursMax").value(3))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("373-2-6")));
    }

    @Test
    void POST_sansUrgence_returnsMOYENNE() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("urgenceJustifiee", false);

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAccordeProbabilite").value("MOYENNE"));
    }

    @Test
    void POST_sansDerogation_returnsFAIBLE() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("derogationContradictoireJustifiee", false);

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAccordeProbabilite").value("FAIBLE"));
    }

    @Test
    void POST_workspaceTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authTravail)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_motifAbsent_returns400() throws Exception {
        Map<String, Object> body = nominalBody();
        body.remove("motifRequete");
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAccordeProbabilite").value("ELEVEE"));

        Map<String, Object> updated = nominalBody();
        updated.put("derogationContradictoireJustifiee", false);

        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAccordeProbabilite").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAccordeProbabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_workspaceBELGIQUE_returnsOK_countryBELGIQUE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/ordonnance-requete-analysis")
                        .with(authentication(authFamBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("1025")));
    }

    private Map<String, Object> nominalBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("motifRequete", "DETOURNEMENT_PATRIMOINE");
        body.put("urgenceJustifiee", true);
        body.put("derogationContradictoireJustifiee", true);
        body.put("pieceJustificativeFournie", true);
        body.put("presenceEnfants", false);
        body.put("commentaireUrgence", "Suspicion de transferts massifs avant procédure");
        return body;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
