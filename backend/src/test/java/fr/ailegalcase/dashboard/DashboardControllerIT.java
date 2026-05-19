package fr.ailegalcase.dashboard;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DashboardControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired CaseAnalysisRepository caseAnalysisRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken userAAuth;
    private OAuth2AuthenticationToken userBAuth;
    private Workspace wsA;
    private Workspace wsB;
    private User userA;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        userA = new User();
        userA.setEmail("dash-a-" + ts + "@example.com");
        userA.setFirstName("Marie");
        userA.setStatus("ACTIVE");
        userA = userRepository.save(userA);

        AuthAccount accA = new AuthAccount();
        accA.setUser(userA);
        accA.setProvider("GOOGLE");
        accA.setProviderUserId("google-dash-a-" + ts);
        authAccountRepository.save(accA);

        wsA = new Workspace();
        wsA.setName("Workspace A " + ts);
        wsA.setSlug("ws-a-" + ts);
        wsA.setOwner(userA);
        wsA.setLegalDomain("DROIT_DU_TRAVAIL");
        wsA.setPlanCode("STARTER");
        wsA.setStatus("ACTIVE");
        wsA = workspaceRepository.save(wsA);

        WorkspaceMember memberA = new WorkspaceMember();
        memberA.setWorkspace(wsA);
        memberA.setUser(userA);
        memberA.setMemberRole("OWNER");
        memberA.setPrimary(true);
        workspaceMemberRepository.save(memberA);

        userAAuth = buildGoogleAuth("google-dash-a-" + ts, "dash-a-" + ts + "@example.com");

        // Workspace B — isolation test
        User userB = new User();
        userB.setEmail("dash-b-" + ts + "@example.com");
        userB.setStatus("ACTIVE");
        userB = userRepository.save(userB);

        AuthAccount accB = new AuthAccount();
        accB.setUser(userB);
        accB.setProvider("GOOGLE");
        accB.setProviderUserId("google-dash-b-" + ts);
        authAccountRepository.save(accB);

        wsB = new Workspace();
        wsB.setName("Workspace B " + ts);
        wsB.setSlug("ws-b-" + ts);
        wsB.setOwner(userB);
        wsB.setLegalDomain("DROIT_DU_TRAVAIL");
        wsB.setPlanCode("STARTER");
        wsB.setStatus("ACTIVE");
        wsB = workspaceRepository.save(wsB);

        WorkspaceMember memberB = new WorkspaceMember();
        memberB.setWorkspace(wsB);
        memberB.setUser(userB);
        memberB.setMemberRole("OWNER");
        memberB.setPrimary(true);
        workspaceMemberRepository.save(memberB);

        userBAuth = buildGoogleAuth("google-dash-b-" + ts, "dash-b-" + ts + "@example.com");
    }

    // IT-DASH-01 : GET 200 avec structure complète (champs existants + 3 nouveaux)
    @Test
    void GET_dashboard_retourne_200_avec_structure() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCases").isArray())
                .andExpect(jsonPath("$.openCasesCount").isNumber())
                .andExpect(jsonPath("$.urgentDeadlines").isArray())
                .andExpect(jsonPath("$.staleChecks").isArray())
                .andExpect(jsonPath("$.recentAnalyses").isArray())
                .andExpect(jsonPath("$.casesOpenedThisWeek").isNumber())
                .andExpect(jsonPath("$.weeklyActivity").isArray())
                .andExpect(jsonPath("$.weeklyActivity.length()").value(7));
    }

    // IT-DASH-02 : isolation workspace — workspace A ne voit pas les données de workspace B
    @Test
    void GET_dashboard_isolation_workspace() throws Exception {
        // Les deux workspaces sont vides — les deux GET doivent retourner 200 sans données croisées
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCasesCount").value(0));

        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userBAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCasesCount").value(0));
    }

    // IT-DASH-03 : userFirstName présent dans la réponse
    @Test
    void GET_dashboard_retourne_userFirstName() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userFirstName").value("Marie"));
    }

    // IT-DASH-04 : weeklyActivity contient exactement 7 entrées
    @Test
    void GET_dashboard_weeklyActivity_has_seven_entries() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyActivity").isArray())
                .andExpect(jsonPath("$.weeklyActivity.length()").value(7));
    }

    // IT-DASH-05 : isolation workspace casesOpenedThisWeek — un dossier de wsB n'est pas compté pour wsA
    @Test
    void GET_dashboard_casesOpenedThisWeek_isolation_workspace() throws Exception {
        // Créer un dossier dans wsB
        CaseFile cfB = new CaseFile();
        cfB.setTitle("Dossier WS-B");
        cfB.setLegalDomain("DROIT_DU_TRAVAIL");
        cfB.setStatus("ACTIVE");
        cfB.setWorkspace(wsB);
        cfB.setCreatedBy(wsB.getOwner());
        caseFileRepository.save(cfB);

        // wsA ne doit pas voir ce dossier
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casesOpenedThisWeek").value(0));

        // wsB doit voir son propre dossier
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userBAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casesOpenedThisWeek").value(1));
    }

    // IT-DASH-06 : isolation workspace weeklyActivity — une analyse de wsB n'est pas comptée pour wsA
    @Test
    void GET_dashboard_weeklyActivity_isolation_workspace() throws Exception {
        // Créer un dossier et une analyse dans wsB
        CaseFile cfB = new CaseFile();
        cfB.setTitle("Dossier WS-B analyse");
        cfB.setLegalDomain("DROIT_DU_TRAVAIL");
        cfB.setStatus("ACTIVE");
        cfB.setWorkspace(wsB);
        cfB.setCreatedBy(wsB.getOwner());
        caseFileRepository.save(cfB);

        CaseAnalysis caB = new CaseAnalysis();
        caB.setCaseFile(cfB);
        caB.setAnalysisType(AnalysisType.STANDARD);
        caB.setAnalysisStatus(AnalysisStatus.DONE);
        caB.setVersion(1);
        caseAnalysisRepository.save(caB);

        // wsA doit avoir tous ses jours à 0
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyActivity[0].analysesCount").value(0))
                .andExpect(jsonPath("$.weeklyActivity[6].analysesCount").value(0));
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
