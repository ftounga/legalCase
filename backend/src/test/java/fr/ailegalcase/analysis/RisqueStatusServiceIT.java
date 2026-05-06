package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * F-195 SF-195-01 — IT régression cohérence F-IA-02 STRICTE.
 *
 * <p>Vérifie que le PUT statut F-195 ne mute PAS le JSON
 * {@code analysis_result.risques}, ne touche PAS au {@code score_risque} IA
 * brut (colonnes risk_score / risk_level inchangées) et ne crée pas de
 * {@code score_risque_avocat_json}. Tous les effets matérialisés se
 * déclenchent uniquement au prochain run de Synthèse enrichie.</p>
 *
 * <p>Pattern miroir {@link PieceManquanteStatusServiceIT} (F-194).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test",
        "anthropic.api-key=test"})
@AutoConfigureMockMvc
class RisqueStatusServiceIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired CaseAnalysisRepository caseAnalysisRepository;
    @Autowired RisqueStatusRepository risqueStatusRepository;
    @Autowired ObjectMapper objectMapper;

    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authUser;
    private CaseFile caseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u = new User(); u.setEmail("rsi-" + ts + "@ex.com"); u.setStatus("ACTIVE");
        u = userRepository.save(u);
        AuthAccount a = new AuthAccount(); a.setUser(u); a.setProvider("GOOGLE");
        a.setProviderUserId("g-rsi-" + ts);
        authAccountRepository.save(a);
        Workspace ws = new Workspace();
        ws.setName("W-" + ts); ws.setSlug("ws-rsi-" + ts); ws.setOwner(u);
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
        authUser = buildAuth("g-rsi-" + ts, "rsi-" + ts + "@ex.com");
    }

    @Test
    void PUT_status_doesNotMutateAnalysisResultRisques_FIA02Strict() throws Exception {
        // Crée une analyse DONE avec risques JSON et score_risque IA
        String initialJson = "{\"risques\":[{\"texte\":\"Harcèlement moral subi\"}],"
                + "\"score_risque\":{\"niveau\":\"ELEVE\",\"valeur\":80}}";
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(caseFile);
        a.setVersion(1);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult(initialJson);
        a.setRiskScore(80);
        a.setRiskLevel("ELEVE");
        a = caseAnalysisRepository.save(a);

        // PUT statut VALIDE sur le risque
        Map<String, String> body = Map.of(
                "risqueLibelleOriginal", "Harcèlement moral subi",
                "statut", "VALIDE");
        mockMvc.perform(put("/api/v1/case-files/" + caseFile.getId() + "/risques/status")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Recharger l'analyse depuis la DB et vérifier que risques JSON et score_risque IA brut sont INTACTS
        CaseAnalysis reloaded = caseAnalysisRepository.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getAnalysisResult()).isEqualTo(initialJson);
        // risk_score IA brut inchangé (F-IA-02 strict)
        assertThat(reloaded.getRiskScore()).isEqualTo(80);
        assertThat(reloaded.getRiskLevel()).isEqualTo("ELEVE");
        // risques_alignment_json reste null (pas matérialisé hors run enrichi)
        assertThat(reloaded.getRisquesAlignmentJson()).isNull();
        // score_risque_avocat_json reste null (pas recomputé hors run enrichi)
        assertThat(reloaded.getScoreRisqueAvocatJson()).isNull();
    }

    @Test
    void PUT_status_2puts_doesNotDuplicateAndDoesNotMutateJson() throws Exception {
        String initialJson = "{\"risques\":[{\"texte\":\"Discrimination\"}]}";
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(caseFile);
        a.setVersion(1);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult(initialJson);
        a = caseAnalysisRepository.save(a);

        Map<String, String> body1 = Map.of(
                "risqueLibelleOriginal", "Discrimination",
                "statut", "A_CREUSER");
        mockMvc.perform(put("/api/v1/case-files/" + caseFile.getId() + "/risques/status")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk());

        Map<String, String> body2 = Map.of(
                "risqueLibelleOriginal", "Discrimination",
                "statut", "VALIDE");
        mockMvc.perform(put("/api/v1/case-files/" + caseFile.getId() + "/risques/status")
                        .with(authentication(authUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk());

        // analysis_result intact
        CaseAnalysis reloaded = caseAnalysisRepository.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getAnalysisResult()).isEqualTo(initialJson);
        // 1 seule entrée en DB
        List<RisqueStatus> all = risqueStatusRepository.findByCaseFileId(caseFile.getId());
        long count = all.stream().filter(s ->
                "discrimination".equals(s.getRisqueLibelleNormalise())).count();
        assertThat(count).isEqualTo(1L);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> c = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken t = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), c);
        return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), t), List.of(), "google");
    }
}
