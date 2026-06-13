package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.workspace.*;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-288 / SF-288-01 — IT des endpoints de composition des conclusions.
 * {@link CaseFileDashboardService} est mocké pour piloter la liste des outils calculés
 * sans données *Analysis en base.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ConclusionCompositionControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private ConclusionCompositionExclusionRepository exclusionRepository;

    @MockBean private CaseFileDashboardService dashboardService;
    @MockBean private AdverseMoyenPersistenceService adverseMoyenPersistenceService;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private CaseFile foreignCaseFile;

    @BeforeEach
    void setUp() {
        exclusionRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("composition-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-composition-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("COMPOSITION-TEST");
        workspace.setSlug("composition-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier composition");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        // Dossier d'un AUTRE workspace (owner différent) pour le test 403.
        User otherUser = new User();
        otherUser.setEmail("other-composition@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("OTHER-WS");
        otherWorkspace.setSlug("other-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        foreignCaseFile = new CaseFile();
        foreignCaseFile.setTitle("Dossier étranger");
        foreignCaseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        foreignCaseFile.setStatus("OPEN");
        foreignCaseFile.setWorkspace(otherWorkspace);
        foreignCaseFile.setCreatedBy(otherUser);
        caseFileRepository.save(foreignCaseFile);

        auth = buildGoogleAuth("google-composition-sub", "composition-test@example.com");

        when(dashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of(
                new DashboardTile("F-DT-08", "VALIDITE", "Validité du licenciement",
                        "Sans cause", "2/7", "ALERT"),
                new DashboardTile("F-DT-09", "INDEMNITES", "Comparateur d'indemnités",
                        "18 000 €", null, "OK")));
        // Par défaut aucun moyen adverse → dimension ADVERSE_MOYEN omise.
        when(adverseMoyenPersistenceService.findPersisted(any())).thenReturn(List.of());
    }

    @Test
    void get_returnsCalculatedToolsAllIncluded() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions[0].key").value("DECISION_TOOL"))
                .andExpect(jsonPath("$.dimensions[0].items.length()").value(2))
                .andExpect(jsonPath("$.dimensions[0].items[0].included").value(true))
                .andExpect(jsonPath("$.dimensions[0].items[1].included").value(true));
    }

    @Test
    void put_persistsExclusion_andReturnsRecomputedComposition() throws Exception {
        String body = objectMapper.writeValueAsString(new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-08"))));

        mockMvc.perform(put("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions[0].items[?(@.key=='F-DT-08')].included").value(false))
                .andExpect(jsonPath("$.dimensions[0].items[?(@.key=='F-DT-09')].included").value(true));

        // persistance vérifiée via GET.
        mockMvc.perform(get("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions[0].items[?(@.key=='F-DT-08')].included").value(false));
    }

    @Test
    void get_withMoyens_returnsAdverseMoyenDimension() throws Exception {
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenPersistenceService.findPersisted(any()))
                .thenReturn(List.of(new AdverseMoyenWithId(moyenId,
                        new AdverseMoyen("Le licenciement repose sur une faute grave.",
                                List.of(), List.of()))));

        mockMvc.perform(get("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions[0].key").value("DECISION_TOOL"))
                .andExpect(jsonPath("$.dimensions[1].key").value("ADVERSE_MOYEN"))
                .andExpect(jsonPath("$.dimensions[1].label").value("Moyens adverses"))
                .andExpect(jsonPath("$.dimensions[1].items[0].key").value(moyenId.toString()))
                .andExpect(jsonPath("$.dimensions[1].items[0].included").value(true));
    }

    @Test
    void put_adverseMoyenExclusion_persistsAndReturns200() throws Exception {
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenPersistenceService.findPersisted(any()))
                .thenReturn(List.of(new AdverseMoyenWithId(moyenId,
                        new AdverseMoyen("Faute grave.", List.of(), List.of()))));

        String body = objectMapper.writeValueAsString(new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("ADVERSE_MOYEN", moyenId.toString()))));

        mockMvc.perform(put("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions[1].key").value("ADVERSE_MOYEN"))
                .andExpect(jsonPath("$.dimensions[1].items[0].included").value(false));

        // persistance vérifiée via GET.
        mockMvc.perform(get("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions[1].items[0].included").value(false));
    }

    @Test
    void put_unknownDimension_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("UNKNOWN_DIM", "x"))));

        mockMvc.perform(put("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_foreignWorkspace_returns404() throws Exception {
        // 404 non-leak (cohérent avec la famille /conclusions), pas 403.
        mockMvc.perform(get("/api/v1/case-files/{id}/conclusions/composition", foreignCaseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_foreignWorkspace_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-08"))));

        // 404 non-leak (cohérent avec la famille /conclusions), pas 403.
        mockMvc.perform(put("/api/v1/case-files/{id}/conclusions/composition", foreignCaseFile.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_caseFileNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/conclusions/composition", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/conclusions/composition", caseFile.getId()))
                .andExpect(status().isUnauthorized());
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
