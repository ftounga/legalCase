package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CaseAnalysisControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;
    @Autowired private AnalysisJobRepository analysisJobRepository;
    @Autowired private DocumentAnalysisRepository documentAnalysisRepository;
    @Autowired private ChunkAnalysisRepository chunkAnalysisRepository;
    @Autowired private DocumentChunkRepository documentChunkRepository;
    @Autowired private DocumentExtractionRepository documentExtractionRepository;
    @Autowired private DocumentRepository documentRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        chunkAnalysisRepository.deleteAll();
        documentChunkRepository.deleteAll();
        documentAnalysisRepository.deleteAll();
        documentExtractionRepository.deleteAll();
        analysisJobRepository.deleteAll();
        caseAnalysisRepository.deleteAll();
        documentRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("analysis-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-analysis-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("analysis-test@example.com");
        workspace.setSlug("analysis-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
       workspace.setLegalDomain("DROIT_DU_TRAVAIL");

        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier Test Analyse");
        caseFile.setStatus("OPEN");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-analysis-sub", "analysis-test@example.com");
    }

    // I-01 : GET analyse DONE → 200 avec synthèse parsée
    @Test
    void get_doneAnalysis_returns200WithParsedContent() throws Exception {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setVersion(1);
        analysis.setModelUsed("claude-sonnet-4-6");
        analysis.setAnalysisResult("""
                {
                  "timeline": [{"date": "2024-01-15", "evenement": "Embauche"}],
                  "faits": ["Le salarié a été embauché."],
                  "points_juridiques": ["Article L1232-1"],
                  "risques": ["Risque de requalification"],
                  "questions_ouvertes": ["Délais respectés ?"]
                }
                """);
        caseAnalysisRepository.save(analysis);

        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.timeline[0].date").value("2024-01-15"))
                .andExpect(jsonPath("$.timeline[0].evenement").value("Embauche"))
                .andExpect(jsonPath("$.faits[0]").value("Le salarié a été embauché."))
                .andExpect(jsonPath("$.pointsJuridiques[0]").value("Article L1232-1"))
                .andExpect(jsonPath("$.risques[0]").value("Risque de requalification"))
                .andExpect(jsonPath("$.questionsOuvertes[0]").value("Délais respectés ?"))
                .andExpect(jsonPath("$.modelUsed").value("claude-sonnet-4-6"));
    }

    // I-02 : GET sans analyse DONE → 404
    @Test
    void get_noAnalysis_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-03 : GET dossier inconnu → 404
    @Test
    void get_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-04 : GET dossier d'un autre workspace → 404 (isolation workspace)
    @Test
    void get_otherWorkspaceCaseFile_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-analysis@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-analysis-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-analysis@example.com");
        otherWorkspace.setSlug("other-analysis-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherUser);
        otherCaseFile.setTitle("Dossier Autre Workspace");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(otherCaseFile);

        CaseAnalysis otherAnalysis = new CaseAnalysis();
        otherAnalysis.setCaseFile(otherCaseFile);
        otherAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
        otherAnalysis.setAnalysisType(AnalysisType.STANDARD);
        otherAnalysis.setVersion(1);
        otherAnalysis.setAnalysisResult("{\"faits\":[]}");
        caseAnalysisRepository.save(otherAnalysis);

        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis", otherCaseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-05 : GET sans auth → 401
    @Test
    void get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ─── Diff tests (SF-54-01) ────────────────────────────────────────────────

    // I-01 diff : GET /diff → 200 avec diff correct entre v1 et v2
    @Test
    void diff_nominal_returns200WithCorrectDiff() throws Exception {
        CaseAnalysis v1 = savedDoneAnalysis(caseFile,
                """
                {"faits":["Fait A","Fait B"],"points_juridiques":["Art. L1234"],"risques":[],"questions_ouvertes":[],
                 "timeline":[{"date":"2024-01-01","evenement":"Embauche"}]}
                """);
        CaseAnalysis v2 = savedDoneAnalysis(caseFile,
                """
                {"faits":["Fait B","Fait C"],"points_juridiques":["Art. L1234"],"risques":[],"questions_ouvertes":[],
                 "timeline":[{"date":"2024-01-01","evenement":"Embauche"},{"date":"2024-06-01","evenement":"Licenciement"}]}
                """);

        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/diff", caseFile.getId())
                        .param("fromId", v1.getId().toString())
                        .param("toId", v2.getId().toString())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faits.removed[0]").value("Fait A"))
                .andExpect(jsonPath("$.faits.added[0]").value("Fait C"))
                .andExpect(jsonPath("$.faits.unchanged[0]").value("Fait B"))
                .andExpect(jsonPath("$.pointsJuridiques.unchanged[0]").value("Art. L1234"))
                .andExpect(jsonPath("$.timeline.unchanged[0].evenement").value("Embauche"))
                .andExpect(jsonPath("$.timeline.added[0].evenement").value("Licenciement"))
                .andExpect(jsonPath("$.from.id").value(v1.getId().toString()))
                .andExpect(jsonPath("$.to.id").value(v2.getId().toString()));
    }

    // I-02 diff : GET /diff → 400 si fromId == toId
    @Test
    void diff_sameId_returns400() throws Exception {
        CaseAnalysis v1 = savedDoneAnalysis(caseFile, emptyAnalysisJson());
        String id = v1.getId().toString();

        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/diff", caseFile.getId())
                        .param("fromId", id)
                        .param("toId", id)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-03 diff : GET /diff → 409 si analyse non DONE
    @Test
    void diff_analysisNotDone_returns409() throws Exception {
        CaseAnalysis done = savedDoneAnalysis(caseFile, emptyAnalysisJson());

        CaseAnalysis processing = new CaseAnalysis();
        processing.setCaseFile(caseFile);
        processing.setAnalysisStatus(AnalysisStatus.PROCESSING);
        processing.setAnalysisType(AnalysisType.STANDARD);
        processing.setVersion(2);
        processing.setModelUsed("test");
        caseAnalysisRepository.save(processing);

        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/diff", caseFile.getId())
                        .param("fromId", done.getId().toString())
                        .param("toId", processing.getId().toString())
                        .with(authentication(auth)))
                .andExpect(status().isConflict());
    }

    // I-04 diff : GET /diff → 404 si analyse d'un autre dossier
    @Test
    void diff_analysisFromAnotherCaseFile_returns404() throws Exception {
        CaseAnalysis v1 = savedDoneAnalysis(caseFile, emptyAnalysisJson());

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(caseFile.getWorkspace());
        otherCaseFile.setCreatedBy(caseFile.getCreatedBy());
        otherCaseFile.setTitle("Autre dossier");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(otherCaseFile);

        CaseAnalysis otherAnalysis = savedDoneAnalysis(otherCaseFile, emptyAnalysisJson());

        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/diff", caseFile.getId())
                        .param("fromId", v1.getId().toString())
                        .param("toId", otherAnalysis.getId().toString())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-05 diff : GET /diff → 404 si dossier d'un autre workspace
    @Test
    void diff_caseFileFromAnotherWorkspace_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-diff@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-diff@example.com");
        otherWorkspace.setSlug("other-diff-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(otherWorkspace);

        CaseFile otherCaseFile = new CaseFile();
        otherCaseFile.setWorkspace(otherWorkspace);
        otherCaseFile.setCreatedBy(otherUser);
        otherCaseFile.setTitle("Dossier autre workspace");
        otherCaseFile.setStatus("OPEN");
        otherCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(otherCaseFile);

        CaseAnalysis v1 = savedDoneAnalysis(caseFile, emptyAnalysisJson());
        CaseAnalysis v2 = savedDoneAnalysis(caseFile, emptyAnalysisJson());

        mockMvc.perform(get("/api/v1/case-files/{id}/case-analysis/diff", otherCaseFile.getId())
                        .param("fromId", v1.getId().toString())
                        .param("toId", v2.getId().toString())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    private CaseAnalysis savedDoneAnalysis(CaseFile cf, String json) {
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(cf);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setVersion(caseAnalysisRepository.findMaxVersionByCaseFileId(cf.getId()) + 1);
        a.setModelUsed("test");
        a.setAnalysisResult(json);
        return caseAnalysisRepository.save(a);
    }

    private String emptyAnalysisJson() {
        return """
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """;
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
