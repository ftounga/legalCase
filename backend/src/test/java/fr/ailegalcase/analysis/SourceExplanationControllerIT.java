package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.*;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test",
        "anthropic.api-key=test"})
@AutoConfigureMockMvc
class SourceExplanationControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired CaseAnalysisRepository caseAnalysisRepository;
    @Autowired SourceExplanationRepository sourceExplanationRepository;

    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authUser1, authUser2;
    private CaseFile caseFileWs1;
    private CaseFile caseFileWs2;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User u1 = new User(); u1.setEmail("sea-"+ts+"@ex.com"); u1.setStatus("ACTIVE"); u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE"); a1.setProviderUserId("g-sea-"+ts); authAccountRepository.save(a1);
        Workspace ws1 = new Workspace(); ws1.setName("W1"+ts); ws1.setSlug("ws-sea-1-"+ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL"); ws1.setCountry("FRANCE"); ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE");
        ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1);
        m1.setMemberRole("OWNER"); m1.setPrimary(true); workspaceMemberRepository.save(m1);
        caseFileWs1 = new CaseFile(); caseFileWs1.setTitle("CF1"+ts); caseFileWs1.setWorkspace(ws1);
        caseFileWs1.setCreatedBy(u1); caseFileWs1.setLegalDomain("DROIT_DU_TRAVAIL"); caseFileWs1.setStatus("OPEN");
        caseFileWs1 = caseFileRepository.save(caseFileWs1);
        authUser1 = bAuth("g-sea-"+ts, "sea-"+ts+"@ex.com");

        User u2 = new User(); u2.setEmail("sea2-"+ts+"@ex.com"); u2.setStatus("ACTIVE"); u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE"); a2.setProviderUserId("g-sea2-"+ts); authAccountRepository.save(a2);
        Workspace ws2 = new Workspace(); ws2.setName("W2"+ts); ws2.setSlug("ws-sea-2-"+ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_DU_TRAVAIL"); ws2.setCountry("FRANCE"); ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE");
        ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2);
        m2.setMemberRole("OWNER"); m2.setPrimary(true); workspaceMemberRepository.save(m2);
        caseFileWs2 = new CaseFile(); caseFileWs2.setTitle("CF2"+ts); caseFileWs2.setWorkspace(ws2);
        caseFileWs2.setCreatedBy(u2); caseFileWs2.setLegalDomain("DROIT_DU_TRAVAIL"); caseFileWs2.setStatus("OPEN");
        caseFileWs2 = caseFileRepository.save(caseFileWs2);
        authUser2 = bAuth("g-sea2-"+ts, "sea2-"+ts+"@ex.com");
    }

    @Test
    void GET_noAnalysis_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileWs1.getId() + "/source-explanations")
                        .with(authentication(authUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void GET_withExplanations_returnsEnrichedList() throws Exception {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFileWs1);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisResult("{}");
        analysis.setVersion(1);
        analysis = caseAnalysisRepository.save(analysis);

        SourceExplanation e1 = new SourceExplanation();
        e1.setCaseAnalysis(analysis);
        e1.setSourceKey("convention_collective");
        e1.setSourceType(SourceExplanation.SourceType.ANALYSIS_DETECTION);
        e1.setLabel("Analyse du dossier");
        e1.setSentence("La convention BTP s'applique.");
        sourceExplanationRepository.save(e1);

        mockMvc.perform(get("/api/v1/case-files/" + caseFileWs1.getId() + "/source-explanations")
                        .with(authentication(authUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sourceKey").value("convention_collective"))
                .andExpect(jsonPath("$[0].sentence").value("La convention BTP s'applique."))
                .andExpect(jsonPath("$[0].actionType").value("NONE"));
    }

    @Test
    void GET_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileWs2.getId() + "/source-explanations")
                        .with(authentication(authUser1)))
                .andExpect(status().isNotFound());
    }

    private OAuth2AuthenticationToken bAuth(String sub, String email) {
        Map<String, Object> c = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken t = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), c);
        return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), t), List.of(), "google");
    }
}
