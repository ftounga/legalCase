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

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CaseNoteControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseNoteRepository noteRepository;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private User user;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("note-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-note-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("NOTE-TEST");
        workspace.setSlug("note-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier Notes");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFileRepository.save(caseFile);

        auth = buildGoogleAuth("google-note-sub", "note-test@example.com");
    }

    // I-01 : GET liste → 200 avec notes en ordre décroissant
    @Test
    void list_returnsNotesOrderedByCreatedAtDesc() throws Exception {
        CaseNote n1 = saveNote("Note ancienne");
        CaseNote n2 = saveNote("Note récente");

        mockMvc.perform(get("/api/v1/case-files/{id}/notes", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Note récente"))
                .andExpect(jsonPath("$[1].content").value("Note ancienne"));
    }

    // I-02 : POST → 201 + note retournée
    @Test
    void create_validPayload_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/notes", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Ma première note."}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Ma première note."))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.authorEmail").value("note-test@example.com"));
    }

    // I-03 : content vide → 400
    @Test
    void create_emptyContent_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/{id}/notes", caseFile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":""}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04 : PUT auteur → 200
    @Test
    void update_byAuthor_returns200() throws Exception {
        CaseNote note = saveNote("Contenu original");

        mockMvc.perform(put("/api/v1/case-files/{id}/notes/{noteId}", caseFile.getId(), note.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Contenu modifié."}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Contenu modifié."));
    }

    // I-05 : DELETE auteur → 204
    @Test
    void delete_byAuthor_returns204() throws Exception {
        CaseNote note = saveNote("À supprimer");

        mockMvc.perform(delete("/api/v1/case-files/{id}/notes/{noteId}", caseFile.getId(), note.getId())
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());
    }

    // I-06 : PUT autre utilisateur → 403
    @Test
    void update_byOtherUser_returns403() throws Exception {
        // Créer un second user dans le même workspace
        User other = createOtherUserInWorkspace("other-note@example.com", "google-other-note-sub",
                caseFile.getWorkspace());
        OAuth2AuthenticationToken otherAuth = buildGoogleAuth("google-other-note-sub", "other-note@example.com");

        CaseNote note = saveNote("Note de l'auteur");

        mockMvc.perform(put("/api/v1/case-files/{id}/notes/{noteId}", caseFile.getId(), note.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Tentative de modification."}
                                """)
                        .with(authentication(otherAuth)))
                .andExpect(status().isForbidden());
    }

    // I-07 : DELETE autre utilisateur → 403
    @Test
    void delete_byOtherUser_returns403() throws Exception {
        User other = createOtherUserInWorkspace("other-note2@example.com", "google-other-note2-sub",
                caseFile.getWorkspace());
        OAuth2AuthenticationToken otherAuth = buildGoogleAuth("google-other-note2-sub", "other-note2@example.com");

        CaseNote note = saveNote("Note de l'auteur");

        mockMvc.perform(delete("/api/v1/case-files/{id}/notes/{noteId}", caseFile.getId(), note.getId())
                        .with(authentication(otherAuth)))
                .andExpect(status().isForbidden());
    }

    // I-08 : dossier hors workspace → 404
    @Test
    void list_caseFileFromOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/notes", UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-09 : sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{id}/notes", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    private CaseNote saveNote(String content) {
        CaseNote note = new CaseNote();
        note.setCaseFile(caseFile);
        note.setCreatedBy(user);
        note.setContent(content);
        return noteRepository.save(note);
    }

    private User createOtherUserInWorkspace(String email, String sub, Workspace workspace) {
        User other = new User();
        other.setEmail(email);
        other.setStatus("ACTIVE");
        userRepository.save(other);

        AuthAccount account = new AuthAccount();
        account.setUser(other);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(other);
        member.setMemberRole("MEMBER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        return other;
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
