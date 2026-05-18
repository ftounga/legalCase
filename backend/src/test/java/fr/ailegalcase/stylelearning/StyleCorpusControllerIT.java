package fr.ailegalcase.stylelearning;

import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-98 / SF-98-46 — tests d'intégration de l'API du corpus de style.
 *
 * <p>Couvre : POST multipart 202, GET liste, PATCH active, DELETE 204, validation
 * 400 (type / taille), isolation workspace 404, rejet 401.</p>
 *
 * <p>Le worker {@code StyleCorpusExtractionService} est {@code @Profile({"local","prod"})} :
 * en profil de test il n'est pas chargé, donc après un POST le document reste
 * {@code PENDING}. {@code StorageService} et {@code RabbitTemplate} sont mockés.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class StyleCorpusControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired StyleCorpusRepository styleCorpusRepository;
    @MockBean StorageService storageService;
    @MockBean AnthropicService anthropicService;
    @MockBean RabbitTemplate rabbitTemplate;

    private OAuth2AuthenticationToken authA;
    private Workspace wsA;
    private User userA;
    private Workspace wsB;
    private User userB;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        userA = save(new User(), u -> { u.setEmail("scp-a-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(userA, "g-scp-a-" + ts);
        wsA = saveWs(userA, "WSA " + ts);
        saveMember(userA, wsA);
        authA = buildAuth("g-scp-a-" + ts, "scp-a-" + ts + "@ex.com");

        userB = save(new User(), u -> { u.setEmail("scp-b-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(userB, "g-scp-b-" + ts);
        wsB = saveWs(userB, "WSB " + ts);
        saveMember(userB, wsB);
    }

    private String url(UUID workspaceId) {
        return "/api/v1/workspaces/" + workspaceId + "/style-corpus/documents";
    }

    // ── POST multipart 202 (CA1) ─────────────────────────────────────────────

    @Test
    void POST_upload_nominal_returns202PendingAndPersistsRow() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "conclusion.pdf",
                "application/pdf", "%PDF-1.4 content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(url(wsA.getId())).file(file).with(authentication(authA)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));

        List<StyleCorpusDocument> docs = styleCorpusRepository
                .findByWorkspaceIdOrderByCreatedAtDesc(wsA.getId());
        org.assertj.core.api.Assertions.assertThat(docs).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(docs.get(0).getStatus())
                .isEqualTo(StyleCorpusDocumentStatus.PENDING);
        org.assertj.core.api.Assertions.assertThat(docs.get(0).isActive()).isTrue();
    }

    @Test
    void POST_upload_unsupportedType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "archive.zip",
                "application/zip", "zip".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(url(wsA.getId())).file(file).with(authentication(authA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upload_otherWorkspace_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "conclusion.pdf",
                "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        // L'avocat A tente de téléverser dans le workspace de B → 404.
        mockMvc.perform(multipart(url(wsB.getId())).file(file).with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upload_withoutAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "conclusion.pdf",
                "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(url(wsA.getId())).file(file))
                .andExpect(status().isUnauthorized());
    }

    // ── GET liste (CA4) ──────────────────────────────────────────────────────

    @Test
    void GET_list_returnsWorkspaceDocuments() throws Exception {
        persistDoc(wsA, userA, "conclusion-1.pdf", StyleCorpusDocumentStatus.DONE, true);
        persistDoc(wsA, userA, "conclusion-2.docx", StyleCorpusDocumentStatus.FAILED, false);

        mockMvc.perform(get(url(wsA.getId())).with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // La signature de style n'est jamais exposée.
                .andExpect(jsonPath("$[0].styleSignature").doesNotExist())
                .andExpect(jsonPath("$[1].styleSignature").doesNotExist());
    }

    @Test
    void GET_list_emptyWorkspace_returnsEmptyList() throws Exception {
        mockMvc.perform(get(url(wsA.getId())).with(authentication(authA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void GET_list_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(get(url(wsB.getId())).with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(url(wsA.getId())))
                .andExpect(status().isUnauthorized());
    }

    // ── PATCH active (CA5) ───────────────────────────────────────────────────

    @Test
    void PATCH_active_disablesDocument_returns200() throws Exception {
        StyleCorpusDocument doc = persistDoc(wsA, userA, "conclusion.pdf",
                StyleCorpusDocumentStatus.DONE, true);

        mockMvc.perform(patch(url(wsA.getId()) + "/" + doc.getId())
                        .with(authentication(authA))
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        org.assertj.core.api.Assertions.assertThat(
                        styleCorpusRepository.findById(doc.getId()).orElseThrow().isActive())
                .isFalse();
    }

    @Test
    void PATCH_active_unknownDocument_returns404() throws Exception {
        mockMvc.perform(patch(url(wsA.getId()) + "/" + UUID.randomUUID())
                        .with(authentication(authA))
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_active_otherWorkspaceDocument_returns404() throws Exception {
        StyleCorpusDocument otherDoc = persistDoc(wsB, userB, "conclusion.pdf",
                StyleCorpusDocumentStatus.DONE, true);

        mockMvc.perform(patch(url(wsB.getId()) + "/" + otherDoc.getId())
                        .with(authentication(authA))
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_active_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch(url(wsA.getId()) + "/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE 204 (CA6) ─────────────────────────────────────────────────────

    @Test
    void DELETE_document_returns204AndRemovesRow() throws Exception {
        StyleCorpusDocument doc = persistDoc(wsA, userA, "conclusion.pdf",
                StyleCorpusDocumentStatus.DONE, true);

        mockMvc.perform(delete(url(wsA.getId()) + "/" + doc.getId())
                        .with(authentication(authA)))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(styleCorpusRepository.findById(doc.getId()))
                .isEmpty();
    }

    @Test
    void DELETE_unknownDocument_returns404() throws Exception {
        mockMvc.perform(delete(url(wsA.getId()) + "/" + UUID.randomUUID())
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void DELETE_otherWorkspaceDocument_returns404() throws Exception {
        StyleCorpusDocument otherDoc = persistDoc(wsB, userB, "conclusion.pdf",
                StyleCorpusDocumentStatus.DONE, true);

        mockMvc.perform(delete(url(wsB.getId()) + "/" + otherDoc.getId())
                        .with(authentication(authA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void DELETE_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete(url(wsA.getId()) + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user);
        a.setProvider("GOOGLE");
        a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name) {
        Workspace ws = new Workspace();
        ws.setName(name);
        ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner);
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        ws.setCountry("FRANCE");
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws);
        m.setUser(user);
        m.setMemberRole("OWNER");
        m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private StyleCorpusDocument persistDoc(Workspace ws, User uploader, String filename,
                                           StyleCorpusDocumentStatus status, boolean active) {
        StyleCorpusDocument doc = new StyleCorpusDocument();
        doc.setWorkspace(ws);
        doc.setUploadedBy(uploader);
        doc.setOriginalFilename(filename);
        doc.setContentType("application/pdf");
        doc.setFileSize(1024L);
        doc.setStatus(status);
        doc.setActive(active);
        if (status == StyleCorpusDocumentStatus.DONE) {
            doc.setStyleSignature("Description de style — usage interne SF-98-47");
        }
        return styleCorpusRepository.save(doc);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
