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
class AncienneteControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authTravail;
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailCaseFile;
    private CaseFile immigrationCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User userTravail = new User();
        userTravail.setEmail("anc-trav-" + ts + "@example.com");
        userTravail.setStatus("ACTIVE");
        userTravail = userRepository.save(userTravail);

        AuthAccount accTravail = new AuthAccount();
        accTravail.setUser(userTravail);
        accTravail.setProvider("GOOGLE");
        accTravail.setProviderUserId("google-anc-trav-" + ts);
        authAccountRepository.save(accTravail);

        Workspace wsTravail = new Workspace();
        wsTravail.setName("Cabinet Travail " + ts);
        wsTravail.setSlug("ws-anc-trav-" + ts);
        wsTravail.setOwner(userTravail);
        wsTravail.setLegalDomain("DROIT_DU_TRAVAIL");
        wsTravail.setCountry("FRANCE");
        wsTravail.setPlanCode("STARTER");
        wsTravail.setStatus("ACTIVE");
        wsTravail = workspaceRepository.save(wsTravail);

        WorkspaceMember memberTravail = new WorkspaceMember();
        memberTravail.setWorkspace(wsTravail);
        memberTravail.setUser(userTravail);
        memberTravail.setMemberRole("OWNER");
        memberTravail.setPrimary(true);
        workspaceMemberRepository.save(memberTravail);

        travailCaseFile = new CaseFile();
        travailCaseFile.setTitle("Dossier ancienneté " + ts);
        travailCaseFile.setWorkspace(wsTravail);
        travailCaseFile.setCreatedBy(userTravail);
        travailCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        travailCaseFile.setStatus("OPEN");
        travailCaseFile = caseFileRepository.save(travailCaseFile);

        authTravail = buildGoogleAuth("google-anc-trav-" + ts, "anc-trav-" + ts + "@example.com");

        User userOther = new User();
        userOther.setEmail("anc-other-" + ts + "@example.com");
        userOther.setStatus("ACTIVE");
        userOther = userRepository.save(userOther);

        AuthAccount accOther = new AuthAccount();
        accOther.setUser(userOther);
        accOther.setProvider("GOOGLE");
        accOther.setProviderUserId("google-anc-other-" + ts);
        authAccountRepository.save(accOther);

        Workspace wsOther = new Workspace();
        wsOther.setName("Cabinet Imm " + ts);
        wsOther.setSlug("ws-anc-other-" + ts);
        wsOther.setOwner(userOther);
        wsOther.setLegalDomain("DROIT_IMMIGRATION");
        wsOther.setCountry("FRANCE");
        wsOther.setPlanCode("STARTER");
        wsOther.setStatus("ACTIVE");
        wsOther = workspaceRepository.save(wsOther);

        WorkspaceMember memberOther = new WorkspaceMember();
        memberOther.setWorkspace(wsOther);
        memberOther.setUser(userOther);
        memberOther.setMemberRole("OWNER");
        memberOther.setPrimary(true);
        workspaceMemberRepository.save(memberOther);

        immigrationCaseFile = new CaseFile();
        immigrationCaseFile.setTitle("Dossier imm " + ts);
        immigrationCaseFile.setWorkspace(wsOther);
        immigrationCaseFile.setCreatedBy(userOther);
        immigrationCaseFile.setLegalDomain("DROIT_IMMIGRATION");
        immigrationCaseFile.setStatus("OPEN");
        immigrationCaseFile = caseFileRepository.save(immigrationCaseFile);

        authOther = buildGoogleAuth("google-anc-other-" + ts, "anc-other-" + ts + "@example.com");
    }

    @Test
    void POST_france_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "METALLURGIE",
                                "dateEntree", LocalDate.now().minusYears(10).toString(),
                                "salaireBase", 3000,
                                "congesContrat", 25,
                                "primeContrat", 5
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conventionCode").value("METALLURGIE"))
                .andExpect(jsonPath("$.congesTotalJours").isNumber())
                .andExpect(jsonPath("$.ecarts").isArray());
    }

    @Test
    void POST_belgique_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "CP200",
                                "dateEntree", LocalDate.now().minusYears(5).toString(),
                                "salaireBase", 2500,
                                "congesContrat", 20,
                                "primeContrat", 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
    }

    @Test
    void POST_unknownConvention_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "INCONNU",
                                "dateEntree", "2020-01-01",
                                "salaireBase", 3000,
                                "congesContrat", 25,
                                "primeContrat", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/anciennete")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "METALLURGIE",
                                "dateEntree", "2020-01-01",
                                "salaireBase", 3000,
                                "congesContrat", 25,
                                "primeContrat", 0
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "METALLURGIE",
                                "dateEntree", "2020-01-01",
                                "salaireBase", 3000,
                                "congesContrat", 25,
                                "primeContrat", 0
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "SYNTEC",
                                "dateEntree", LocalDate.now().minusYears(7).toString(),
                                "salaireBase", 4000,
                                "congesContrat", 25,
                                "primeContrat", 3
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conventionCode").value("SYNTEC"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replaces() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "METALLURGIE",
                                "dateEntree", "2020-01-01",
                                "salaireBase", 3000,
                                "congesContrat", 25,
                                "primeContrat", 0
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/anciennete")
                        .with(authentication(authTravail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "conventionCode", "CP124",
                                "dateEntree", "2018-06-01",
                                "salaireBase", 2800,
                                "congesContrat", 20,
                                "primeContrat", 2
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conventionCode").value("CP124"));
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
