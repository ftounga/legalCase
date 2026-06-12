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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F-285 / SF-285-01 — IT de la qualification d'entrée du dossier (intake).
 * Mirror {@link ContradictoireControllerIT} (isolation workspace via le dossier).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CaseIntakeControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private Workspace workspace;
    private User user;

    @BeforeEach
    void setUp() {
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("intake-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-intake-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("INTAKE-TEST");
        workspace.setSlug("intake-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier intake");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-intake-sub", "intake-test@example.com");
    }

    // I-01 : dossier jamais qualifié → tout-null + qualified=false
    @Test
    void get_neverQualified_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/intake", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualified").value(false))
                .andExpect(jsonPath("$.qualifiedAt").doesNotExist())
                .andExpect(jsonPath("$.disputeType").doesNotExist());
    }

    // I-02 : PUT qualifie → 200, qualified=true, qualifiedAt posé
    @Test
    void update_qualifies_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/case-files/{id}/intake", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"disputeType":"Contestation de licenciement","admissibility":"RECEVABLE",
                                 "prescriptionStatus":"DANS_LES_DELAIS","prescriptionDeadline":"2026-12-31",
                                 "jurisdiction":"Conseil de prud'hommes","estimatedValueCents":2500000,
                                 "notes":"Dossier solide"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualified").value(true))
                .andExpect(jsonPath("$.qualifiedAt").exists())
                .andExpect(jsonPath("$.disputeType").value("Contestation de licenciement"))
                .andExpect(jsonPath("$.admissibility").value("RECEVABLE"))
                .andExpect(jsonPath("$.prescriptionStatus").value("DANS_LES_DELAIS"))
                .andExpect(jsonPath("$.prescriptionDeadline").value("2026-12-31"))
                .andExpect(jsonPath("$.estimatedValueCents").value(2500000));
    }

    // I-03 : valeur négative → 400
    @Test
    void update_negativeValue_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/case-files/{id}/intake", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estimatedValueCents":-1}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04 : enum invalide → 400
    @Test
    void update_invalidEnum_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/case-files/{id}/intake", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"admissibility":"PEUT_ETRE"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-05 : qualifiedAt préservé entre deux PUT
    @Test
    void update_preservesQualifiedAt() throws Exception {
        mockMvc.perform(put("/api/v1/case-files/{id}/intake", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"disputeType":"Litige initial"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        Instant firstQualifiedAt = caseFileRepository.findById(caseFile.getId())
                .orElseThrow().getIntakeQualifiedAt();

        mockMvc.perform(put("/api/v1/case-files/{id}/intake", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"disputeType":"Litige modifié"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disputeType").value("Litige modifié"));

        Instant secondQualifiedAt = caseFileRepository.findById(caseFile.getId())
                .orElseThrow().getIntakeQualifiedAt();
        org.junit.jupiter.api.Assertions.assertEquals(firstQualifiedAt, secondQualifiedAt,
                "qualifiedAt must be preserved across updates");
    }

    // I-06 : dossier d'un autre workspace → 404 (isolation)
    @Test
    void get_caseFileFromOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/intake", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-07 : sans auth → 401
    @Test
    void get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/intake", caseFile.getId()))
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
