package fr.ailegalcase.search;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class SynthesisSearchControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        caseAnalysisRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("search-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-search-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("search-test@example.com");
        workspace.setSlug("search-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setTitle("Dossier Dupont");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-search-sub", "search-test@example.com");
    }

    private CaseAnalysis buildDoneAnalysis(CaseFile cf, String resultJson, int version) {
        CaseAnalysis ca = new CaseAnalysis();
        ca.setCaseFile(cf);
        ca.setAnalysisType(AnalysisType.STANDARD);
        ca.setAnalysisStatus(AnalysisStatus.DONE);
        ca.setVersion(version);
        ca.setAnalysisResult(resultJson);
        return ca;
    }

    // I-01
    @Test
    void search_matchingTerm_returns200WithExcerpts() throws Exception {
        caseAnalysisRepository.save(buildDoneAnalysis(caseFile,
                "{\"faits\":[\"Le salarié a été licencié le 15 janvier.\"],\"points_juridiques\":[],\"risques\":[],\"questions_ouvertes\":[]}",
                1));

        mockMvc.perform(get("/api/v1/search").param("q", "licencié")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("licencié"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.results[0].caseFileTitle").value("Dossier Dupont"))
                .andExpect(jsonPath("$.results[0].excerpts").isArray())
                .andExpect(jsonPath("$.results[0].matchCount").value(1));
    }

    // I-02
    @Test
    void search_noMatch_returnsEmptyResults() throws Exception {
        caseAnalysisRepository.save(buildDoneAnalysis(caseFile,
                "{\"faits\":[\"Aucun rapport.\"],\"points_juridiques\":[],\"risques\":[],\"questions_ouvertes\":[]}",
                1));

        mockMvc.perform(get("/api/v1/search").param("q", "licenciement")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0))
                .andExpect(jsonPath("$.results").isEmpty());
    }

    // I-03
    @Test
    void search_qTooShort_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "a")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04
    @Test
    void search_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "licenciement"))
                .andExpect(status().isUnauthorized());
    }

    // I-05 — isolation workspace : only returns results from the authenticated user's workspace
    @Test
    void search_otherWorkspace_notReturned() throws Exception {
        // Second user with a different workspace + case file + analysis
        User other = new User();
        other.setEmail("other-search@example.com");
        other.setStatus("ACTIVE");
        userRepository.save(other);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(other);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-search-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-search@example.com");
        otherWorkspace.setSlug("other-search-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(other);
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(other);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        CaseFile otherCf = new CaseFile();
        otherCf.setTitle("Dossier Autre");
        otherCf.setLegalDomain("DROIT_DU_TRAVAIL");
        otherCf.setStatus("OPEN");
        otherCf.setWorkspace(otherWorkspace);
        otherCf.setCreatedBy(other);
        caseFileRepository.save(otherCf);

        caseAnalysisRepository.save(buildDoneAnalysis(otherCf,
                "{\"faits\":[\"licenciement dans autre workspace.\"],\"points_juridiques\":[],\"risques\":[],\"questions_ouvertes\":[]}",
                1));

        // auth user's workspace has no matching analysis
        mockMvc.perform(get("/api/v1/search").param("q", "licenciement")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0));
    }

    // I-06 — only latest DONE version is returned
    @Test
    void search_returnsOnlyLatestVersion() throws Exception {
        // version 1: matches, version 2 (latest): no match
        caseAnalysisRepository.save(buildDoneAnalysis(caseFile,
                "{\"faits\":[\"licenciement v1.\"],\"points_juridiques\":[],\"risques\":[],\"questions_ouvertes\":[]}",
                1));
        caseAnalysisRepository.save(buildDoneAnalysis(caseFile,
                "{\"faits\":[\"Aucun rapport v2.\"],\"points_juridiques\":[],\"risques\":[],\"questions_ouvertes\":[]}",
                2));

        mockMvc.perform(get("/api/v1/search").param("q", "licenciement")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResults").value(0));
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
