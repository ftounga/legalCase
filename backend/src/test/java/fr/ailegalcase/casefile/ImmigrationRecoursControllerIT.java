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
class ImmigrationRecoursControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authImmigration;
    private OAuth2AuthenticationToken authOther;
    private CaseFile immigrationCaseFile;
    private CaseFile travailCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Immigration workspace + case file
        User userImm = new User();
        userImm.setEmail("rc-imm-" + ts + "@example.com");
        userImm.setStatus("ACTIVE");
        userImm = userRepository.save(userImm);

        AuthAccount accImm = new AuthAccount();
        accImm.setUser(userImm);
        accImm.setProvider("GOOGLE");
        accImm.setProviderUserId("google-rc-imm-" + ts);
        authAccountRepository.save(accImm);

        Workspace wsImm = new Workspace();
        wsImm.setName("Cabinet Immigration " + ts);
        wsImm.setSlug("ws-rc-imm-" + ts);
        wsImm.setOwner(userImm);
        wsImm.setLegalDomain("DROIT_IMMIGRATION");
        wsImm.setCountry("FRANCE");
        wsImm.setPlanCode("STARTER");
        wsImm.setStatus("ACTIVE");
        wsImm = workspaceRepository.save(wsImm);

        WorkspaceMember memberImm = new WorkspaceMember();
        memberImm.setWorkspace(wsImm);
        memberImm.setUser(userImm);
        memberImm.setMemberRole("OWNER");
        memberImm.setPrimary(true);
        workspaceMemberRepository.save(memberImm);

        immigrationCaseFile = new CaseFile();
        immigrationCaseFile.setTitle("Dossier recours " + ts);
        immigrationCaseFile.setWorkspace(wsImm);
        immigrationCaseFile.setCreatedBy(userImm);
        immigrationCaseFile.setLegalDomain("DROIT_IMMIGRATION");
        immigrationCaseFile.setStatus("OPEN");
        immigrationCaseFile = caseFileRepository.save(immigrationCaseFile);

        authImmigration = buildGoogleAuth("google-rc-imm-" + ts, "rc-imm-" + ts + "@example.com");

        // Other workspace (droit du travail)
        User userOther = new User();
        userOther.setEmail("rc-other-" + ts + "@example.com");
        userOther.setStatus("ACTIVE");
        userOther = userRepository.save(userOther);

        AuthAccount accOther = new AuthAccount();
        accOther.setUser(userOther);
        accOther.setProvider("GOOGLE");
        accOther.setProviderUserId("google-rc-other-" + ts);
        authAccountRepository.save(accOther);

        Workspace wsOther = new Workspace();
        wsOther.setName("Cabinet Travail " + ts);
        wsOther.setSlug("ws-rc-other-" + ts);
        wsOther.setOwner(userOther);
        wsOther.setLegalDomain("DROIT_DU_TRAVAIL");
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

        travailCaseFile = new CaseFile();
        travailCaseFile.setTitle("Dossier travail " + ts);
        travailCaseFile.setWorkspace(wsOther);
        travailCaseFile.setCreatedBy(userOther);
        travailCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        travailCaseFile.setStatus("OPEN");
        travailCaseFile = caseFileRepository.save(travailCaseFile);

        authOther = buildGoogleAuth("google-rc-other-" + ts, "rc-other-" + ts + "@example.com");
    }

    private Map<String, Object> validBody(String recoursType) {
        return Map.of(
                "recoursType", recoursType,
                "dateNotification", LocalDate.now().minusDays(5).toString(),
                "requerant", Map.of("nom", "Dupont", "prenom", "Jean", "nationalite", "Marocaine", "adresse", "12 rue Test, Paris"),
                "decisionContestee", Map.of("autorite", "Préfet de Paris", "date", LocalDate.now().minusDays(10).toString(), "reference", "PREF-2026-001"),
                "exposeFaits", "Le requérant réside en France depuis 5 ans."
        );
    }

    @Test
    void POST_france_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("RECOURS_GRACIEUX_PREFET"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoursType").value("RECOURS_GRACIEUX_PREFET"))
                .andExpect(jsonPath("$.document.enTete").isNotEmpty())
                .andExpect(jsonPath("$.document.visaTextes").isNotEmpty())
                .andExpect(jsonPath("$.document.moyensDroit").isNotEmpty())
                .andExpect(jsonPath("$.dateLimite").isNotEmpty());
    }

    @Test
    void POST_belgique_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("RECOURS_CCE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoursType").value("RECOURS_CCE"))
                .andExpect(jsonPath("$.recoursLabel").isNotEmpty());
    }

    @Test
    void POST_unknownType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("INCONNU"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_wrongDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("RECOURS_GRACIEUX_PREFET"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("RECOURS_GRACIEUX_PREFET"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("RECOURS_CNDA"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoursType").value("RECOURS_CNDA"))
                .andExpect(jsonPath("$.requerant.nom").value("Dupont"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesExisting() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("RECOURS_GRACIEUX_PREFET"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody("RECOURS_CGRA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoursType").value("RECOURS_CGRA"));

        mockMvc.perform(get("/api/v1/case-files/" + immigrationCaseFile.getId() + "/immigration/recours")
                        .with(authentication(authImmigration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoursType").value("RECOURS_CGRA"));
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
