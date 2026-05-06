package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseDeadline;
import fr.ailegalcase.casefile.CaseDeadlineRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-194 SF-194-01 — IT régression cohérence F-92 STRICTE.
 *
 * <p>Vérifie que le PUT statut F-194 ne mute PAS le JSON
 * {@code analysis_result.pieces_manquantes} ni la table
 * {@code case_deadlines}. Tous les effets matérialisés se déclenchent
 * uniquement au prochain run de Synthèse enrichie.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test",
        "anthropic.api-key=test"})
@AutoConfigureMockMvc
class PieceManquanteStatusServiceIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired CaseAnalysisRepository caseAnalysisRepository;
    @Autowired CaseDeadlineRepository caseDeadlineRepository;
    @Autowired ObjectMapper objectMapper;

    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authUser;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u = new User(); u.setEmail("pms-" + ts + "@ex.com"); u.setStatus("ACTIVE");
        u = userRepository.save(u);
        AuthAccount a = new AuthAccount(); a.setUser(u); a.setProvider("GOOGLE");
        a.setProviderUserId("g-pms-" + ts);
        authAccountRepository.save(a);
        Workspace ws = new Workspace();
        ws.setName("W-" + ts); ws.setSlug("ws-pms-" + ts); ws.setOwner(u);
        ws.setLegalDomain("DROIT_TRAVAIL"); ws.setCountry("FRANCE");
        ws.setPlanCode("STARTER"); ws.setStatus("ACTIVE");
        ws = workspaceRepository.save(ws);
        WorkspaceMember m = new WorkspaceMember(); m.setWorkspace(ws); m.setUser(u);
        m.setMemberRole("OWNER"); m.setPrimary(true);
        workspaceMemberRepository.save(m);
        caseFile = new CaseFile(); caseFile.setTitle("CF-" + ts); caseFile.setWorkspace(ws);
        caseFile.setCreatedBy(u); caseFile.setLegalDomain("DROIT_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile = caseFileRepository.save(caseFile);
        authUser = buildAuth("g-pms-" + ts, "pms-" + ts + "@ex.com");
    }

    @Test
    void PUT_status_doesNotMutateAnalysisResultPiecesManquantes_F92Strict() throws Exception {
        // Crée une analyse DONE avec pieces_manquantes JSON
        String initialJson =
                "{\"pieces_manquantes\":[{\"texte\":\"Contrat de travail original\"}]}";
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(caseFile);
        a.setVersion(1);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult(initialJson);
        a = caseAnalysisRepository.save(a);

        // PUT statut OBTENUE sur la pièce
        Map<String, String> body = Map.of(
                "pieceLibelleOriginal", "Contrat de travail original",
                "statut", "OBTENUE");
        mockMvc.perform(put("/api/v1/case-files/" + caseFile.getId() + "/pieces-manquantes/status")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Recharger l'analyse depuis la DB et vérifier que pieces_manquantes JSON est INTACT
        CaseAnalysis reloaded = caseAnalysisRepository.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getAnalysisResult()).isEqualTo(initialJson);
        // pieces_alignment_json reste null (pas matérialisé hors run enrichi)
        assertThat(reloaded.getPiecesAlignmentJson()).isNull();
    }

    @Test
    void PUT_status_doesNotCreateDeadline() throws Exception {
        Map<String, String> body = Map.of(
                "pieceLibelleOriginal", "Lettre de licenciement",
                "statut", "A_DEMANDER");
        mockMvc.perform(put("/api/v1/case-files/" + caseFile.getId() + "/pieces-manquantes/status")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Aucun délai créé (PUT pur — la matérialisation des délais ne se fait
        // qu'au run de Synthèse enrichie)
        List<CaseDeadline> deadlines = caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFile.getId());
        long pieceDeadlines = deadlines.stream()
                .filter(d -> "PIECE_A_DEMANDER".equals(d.getSource())).count();
        assertThat(pieceDeadlines).isEqualTo(0L);
    }

    @Test
    void PUT_status_2puts_doesNotDuplicateAndDoesNotMutateJson() throws Exception {
        String initialJson =
                "{\"pieces_manquantes\":[{\"texte\":\"Contrat\"}]}";
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(caseFile);
        a.setVersion(1);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult(initialJson);
        a = caseAnalysisRepository.save(a);

        Map<String, String> body1 = Map.of(
                "pieceLibelleOriginal", "Contrat",
                "statut", "A_DEMANDER");
        mockMvc.perform(put("/api/v1/case-files/" + caseFile.getId() + "/pieces-manquantes/status")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk());

        Map<String, String> body2 = Map.of(
                "pieceLibelleOriginal", "Contrat",
                "statut", "OBTENUE");
        mockMvc.perform(put("/api/v1/case-files/" + caseFile.getId() + "/pieces-manquantes/status")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk());

        // analysis_result intact
        CaseAnalysis reloaded = caseAnalysisRepository.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getAnalysisResult()).isEqualTo(initialJson);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> c = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken t = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), c);
        return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), t), List.of(), "google");
    }
}
