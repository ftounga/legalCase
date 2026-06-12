package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-283 / SF-283-01 — tests d'intégration des phases procédurales (CRUD +
 * isolation workspace via le dossier).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CasePhaseControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CasePhaseRepository phaseRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        phaseRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("phase-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-phase-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("PHASE-TEST");
        workspace.setSlug("phase-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier phases");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-phase-sub", "phase-test@example.com");
    }

    // I-01 : timeline vide → currentPhase null
    @Test
    void timeline_empty_currentPhaseNull() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/phases", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phases.length()").value(0))
                .andExpect(jsonPath("$.currentPhase").doesNotExist());
    }

    // I-02 : POST phase → 201
    @Test
    void create_phase_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/phases", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phase":"SAISINE","label":"Requête déposée","enteredAt":"2026-01-10"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phase").value("SAISINE"))
                .andExpect(jsonPath("$.enteredAt").value("2026-01-10"));
    }

    // I-03 : phase manquante → 400
    @Test
    void create_missingPhase_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/phases", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enteredAt":"2026-01-10"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04 : phase hors référentiel → 400
    @Test
    void create_unknownPhase_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/phases", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phase":"INEXISTANTE","enteredAt":"2026-01-10"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-05 : timeline ordonnée + phase courante = la plus récente
    @Test
    void timeline_ordered_currentPhaseIsMostRecent() throws Exception {
        savePhase(CasePhaseType.SAISINE, LocalDate.of(2026, 1, 10));
        savePhase(CasePhaseType.FOND, LocalDate.of(2026, 4, 1));

        mockMvc.perform(get("/api/v1/case-files/{id}/phases", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phases.length()").value(2))
                .andExpect(jsonPath("$.phases[0].phase").value("SAISINE"))
                .andExpect(jsonPath("$.phases[1].phase").value("FOND"))
                .andExpect(jsonPath("$.currentPhase").value("FOND"));
    }

    // I-06 : PUT → 200
    @Test
    void update_returns200() throws Exception {
        CasePhase p = savePhase(CasePhaseType.SAISINE, LocalDate.of(2026, 1, 10));
        mockMvc.perform(put("/api/v1/case-files/{id}/phases/{pid}", caseFile.getId(), p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phase":"CONCILIATION","label":"Audience BCO","enteredAt":"2026-02-15"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("CONCILIATION"))
                .andExpect(jsonPath("$.label").value("Audience BCO"));
    }

    // I-07 : DELETE → 204
    @Test
    void delete_returns204() throws Exception {
        CasePhase p = savePhase(CasePhaseType.SAISINE, LocalDate.of(2026, 1, 10));
        mockMvc.perform(delete("/api/v1/case-files/{id}/phases/{pid}", caseFile.getId(), p.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());
    }

    // I-08 : dossier d'un autre workspace → 404
    @Test
    void timeline_caseFileFromOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/phases", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-09 : phase d'un AUTRE dossier (même URL) → 404 (isolation par dossier)
    @Test
    void update_phaseFromAnotherCaseFile_returns404() throws Exception {
        CaseFile other = new CaseFile();
        other.setTitle("Autre dossier");
        other.setLegalDomain("DROIT_DU_TRAVAIL");
        other.setStatus("OPEN");
        other.setWorkspace(workspace);
        other.setCreatedBy(user);
        caseFileRepository.save(other);

        CasePhase p = new CasePhase();
        p.setCaseFile(other);
        p.setPhase(CasePhaseType.SAISINE);
        p.setEnteredAt(LocalDate.of(2026, 1, 10));
        phaseRepository.save(p);

        mockMvc.perform(put("/api/v1/case-files/{id}/phases/{pid}", caseFile.getId(), p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phase":"FOND","enteredAt":"2026-04-01"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-10 : sans auth → 401
    @Test
    void timeline_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/phases", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    private CasePhase savePhase(CasePhaseType type, LocalDate enteredAt) {
        CasePhase p = new CasePhase();
        p.setCaseFile(caseFile);
        p.setPhase(type);
        p.setEnteredAt(enteredAt);
        return phaseRepository.save(p);
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
