package fr.ailegalcase.document;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class DocumentControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentExtractionRepository documentExtractionRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    @MockBean private StorageService storageService;
    @MockBean private ExtractionService extractionService;

    private OAuth2AuthenticationToken auth;
    private UUID caseFileId;
    private UUID otherCaseFileId;

    @BeforeEach
    void setUp() {
        documentExtractionRepository.deleteAll();
        documentRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        subscriptionRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        // User + workspace
        User user = new User();
        user.setEmail("doc-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-doc-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("doc-test@example.com");
        workspace.setSlug("doc-slug-" + System.currentTimeMillis());
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

        Subscription subscription = new Subscription();
        subscription.setWorkspaceId(workspace.getId());
        subscription.setPlanCode("STARTER");
        subscription.setStatus("ACTIVE");
        subscription.setStartedAt(Instant.now());
        subscriptionRepository.save(subscription);

        // Case file belonging to this workspace
        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier test upload");
        caseFile.setStatus("OPEN");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFileRepository.save(caseFile);
        caseFileId = caseFile.getId();

        // Case file in another workspace
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other@example.com");
        otherWorkspace.setSlug("other-slug-" + System.currentTimeMillis());
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
        otherCaseFileId = otherCaseFile.getId();

        auth = buildGoogleAuth("google-doc-sub", "doc-test@example.com");

        when(storageService.upload(anyString(), any(), anyString(), anyLong()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // I-01 : upload PDF valide → 201
    @Test
    void upload_validPdf_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("contrat.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.caseFileId").value(caseFileId.toString()));
    }

    // I-02 : type non supporté → 400 (SF-230-01 : image/png est maintenant accepté
    // pour upload natif, donc on teste un type vraiment hors-scope : image/svg+xml)
    @Test
    void upload_unsupportedType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.svg", "image/svg+xml", "<svg/>".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-03 : fichier vide → 400
    @Test
    void upload_emptyFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04 : sans auth → 401
    @Test
    void upload_withoutAuth_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    // I-05 : case file inconnu → 404
    @Test
    void upload_unknownCaseFile_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + UUID.randomUUID() + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-06 : case file d'un autre workspace → 404 (isolation)
    @Test
    void upload_caseFileBelongsToOtherWorkspace_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + otherCaseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-07 : upload DOCX valide → 201
    @Test
    void upload_validDocx_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "courrier.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("courrier.docx"));
    }

    // I-08 : GET /{docId}/download → 302 avec Location header
    @Test
    void download_existingDoc_returns302() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file).with(authentication(auth)))
                .andReturn().getResponse().getContentAsString();

        String docId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(uploadResponse).get("id").asText();

        when(storageService.presignedDownloadUrl(anyString(), any(Integer.class)))
                .thenReturn("http://minio:9000/legalcase-dev/some-key?X-Amz-Signature=abc");

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents/" + docId + "/download")
                        .with(authentication(auth)))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"));
    }

    // I-09 : download sans auth → 401
    @Test
    void download_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID() + "/download"))
                .andExpect(status().isUnauthorized());
    }

    // I-10 : download doc inconnu → 404
    @Test
    void download_unknownDoc_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID() + "/download")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-11 : GET liste vide → []
    @Test
    void list_noDocs_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // I-09 : GET liste avec docs → retourne les documents
    @Test
    void list_withDocs_returnsItems() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file).with(authentication(auth)));

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].originalFilename").value("contrat.pdf"))
                .andExpect(jsonPath("$[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$[0].caseFileId").value(caseFileId.toString()));
    }

    // I-10 : GET liste sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents"))
                .andExpect(status().isUnauthorized());
    }

    // I-11 : GET liste — dossier d'un autre workspace → 404
    @Test
    void list_otherWorkspaceCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + otherCaseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-12 : quota documents atteint (5/5 Starter) → 402
    @Test
    void upload_documentQuotaReached_returns402() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc" + i + ".pdf", "application/pdf", ("PDF " + i).getBytes());
            mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                            .file(file)
                            .with(authentication(auth)))
                    .andExpect(status().isCreated());
        }

        MockMultipartFile sixthFile = new MockMultipartFile(
                "file", "doc6.pdf", "application/pdf", "PDF 6".getBytes());
        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(sixthFile)
                        .with(authentication(auth)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("DOCUMENT_LIMIT_EXCEEDED"));
    }

    // I-13 : DELETE document valide → 204 et document supprimé
    @Test
    void delete_existingDoc_returns204() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contrat.pdf", "application/pdf", "PDF content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file).with(authentication(auth)))
                .andReturn().getResponse().getContentAsString();

        String docId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(uploadResponse).get("id").asText();

        mockMvc.perform(delete("/api/v1/case-files/" + caseFileId + "/documents/" + docId)
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // I-14 : DELETE sans auth → 401
    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // I-15 : DELETE document inconnu → 404
    @Test
    void delete_unknownDoc_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-16 : DELETE document d'un autre workspace → 404 (isolation)
    @Test
    void delete_docInOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/case-files/" + otherCaseFileId + "/documents/" + UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // SF-230-01 — upload natif des images JPG/PNG/HEIC/WebP
    // ========================================================================

    // I-IMG-01 : upload image JPG valide → 201 + contentType image/jpeg persisté
    @Test
    void upload_validJpeg_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "preuve.jpg", "image/jpeg", "JPEG content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.originalFilename").value("preuve.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.caseFileId").value(caseFileId.toString()));
    }

    // I-IMG-02 : upload image PNG valide → 201
    @Test
    void upload_validPng_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", "PNG content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    // I-IMG-03 : upload image HEIC valide → 201 (format Apple courant)
    @Test
    void upload_validHeic_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.heic", "image/heic", "HEIC content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/heic"));
    }

    // I-IMG-04 : upload image WebP valide → 201
    @Test
    void upload_validWebp_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.webp", "image/webp", "WebP content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/webp"));
    }

    // I-IMG-05 : upload image SVG → 400 (toujours hors scope, garde anti-régression)
    @Test
    void upload_imageSvg_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vector.svg", "image/svg+xml", "<svg/>".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-IMG-06 : upload image GIF → 400 (non listée dans SF-230-01 — animations
    // non supportées, format hors-scope)
    @Test
    void upload_imageGif_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "anim.gif", "image/gif", "GIF89a content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-IMG-07 : isolation workspace — upload image dans un dossier d'un autre
    // workspace → 404 (respect strict de l'isolation, identique au cas PDF)
    @Test
    void upload_jpegInOtherWorkspaceCaseFile_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "preuve.jpg", "image/jpeg", "JPEG content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + otherCaseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // ── SF-231-01 : ingestion vidéo MP4 / MOV ────────────────────────────

    // I-VIDEO-01 : upload MP4 valide avec header durée 12s → 201
    @Test
    void upload_videoMp4_withValidDurationHeader_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "fake mp4 content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "12")
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.originalFilename").value("video.mp4"));
    }

    // I-VIDEO-02 : upload MOV valide → 201
    @Test
    void upload_videoQuicktime_withValidDurationHeader_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mov", "video/quicktime", "fake mov content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "30")
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("video/quicktime"));
    }

    // I-VIDEO-03 : upload MP4 sans header durée → 400
    @Test
    void upload_videoMp4_withoutDurationHeader_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "fake mp4 content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-VIDEO-04 : upload MP4 avec durée > 60s → 400 VIDEO_TOO_LONG
    @Test
    void upload_videoMp4_durationExceedsLimit_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "fake mp4 content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "90")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-VIDEO-05 : upload MP4 > 100 Mo → 413 PAYLOAD_TOO_LARGE
    @Test
    void upload_videoMp4_oversize_returns413() throws Exception {
        // Génère 101 Mo (~1 octet de plus que la limite)
        byte[] hugePayload = new byte[101 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge.mp4", "video/mp4", hugePayload);

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "30")
                        .with(authentication(auth)))
                .andExpect(status().isPayloadTooLarge());
    }

    // I-VIDEO-06 : isolation workspace — vidéo postée sur un dossier d'un autre WS → 404
    @Test
    void upload_videoMp4_otherWorkspace_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "fake mp4 content".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + otherCaseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "12")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-VIDEO-07 : durée invalide (zéro) → 400
    @Test
    void upload_videoMp4_zeroDuration_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "video.mp4", "video/mp4", "fake".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "0")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // ── SF-231-03 : quotas mensuels minutes vidéo ──────────────────────────
    //
    // Note plans : la fixture de base utilise planCode "STARTER" qui n'est pas
    // reconnu par PlanLimitService → fallback FREE → 1 min/mois.
    // On bascule la subscription au plan voulu directement depuis chaque test.

    // I-VIDEO-Q-01 : SOLO quota 5 min — 5 uploads de 60 s consomment 5 min (au quota),
    // un 6e upload de 1 s ⇒ 5+1=6 > 5 → 402 VIDEO_QUOTA_EXCEEDED
    @Test
    void upload_videoMp4_soloQuotaExceeded_returns402_videoCode() throws Exception {
        switchSubscriptionTo("SOLO");
        // Pré-charge 5 min consommées (5 uploads × 60 s = 5 min)
        for (int i = 0; i < 5; i++) {
            uploadVideo("v-" + i + ".mp4", 60L).andExpect(status().isCreated());
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "ko.mp4", "video/mp4", "ko".getBytes());

        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "1")
                        .with(authentication(auth)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("VIDEO_QUOTA_EXCEEDED"));
    }

    // I-VIDEO-Q-02 : SOLO quota 5 min — 4 min déjà + 30 s ⇒ 4+1=5 ≤ 5 → OK (à la limite)
    @Test
    void upload_videoMp4_soloAtExactLimit_returns201() throws Exception {
        switchSubscriptionTo("SOLO");
        for (int i = 0; i < 4; i++) {
            uploadVideo("v-" + i + ".mp4", 60L).andExpect(status().isCreated());
        }

        // 30 s ⇒ 1 min ; total 4+1=5 = quota, accepté
        uploadVideo("ok.mp4", 30L).andExpect(status().isCreated());
    }

    // I-VIDEO-Q-03 : ENTERPRISE quota illimité — 60 min en un seul upload (60 s)
    // suffit : on vérifie qu'un upload nominal sur ENTERPRISE passe sans gate.
    // (Tester réellement 1000 min = 100 uploads à 60s chacun, lourd ; on valide le contrat.)
    @Test
    void upload_videoMp4_enterprise_unlimitedQuota_returns201() throws Exception {
        switchSubscriptionTo("ENTERPRISE");

        // Plusieurs uploads consécutifs sans plafond
        for (int i = 0; i < 3; i++) {
            uploadVideo("entr-" + i + ".mp4", 60L).andExpect(status().isCreated());
        }
    }

    // I-VIDEO-Q-04 : isolation workspace — la consommation d'un autre workspace
    // n'impacte pas le quota du nôtre. Cas vérifié indirectement par le filtre
    // sumMinutesByWorkspaceAndMonth (couvert par WorkspaceVideoUsageRepositoryIT) ;
    // ici on vérifie seulement qu'un upload OK se fait quand on est sous quota.
    @Test
    void upload_videoMp4_proPlan_within120minQuota_returns201() throws Exception {
        switchSubscriptionTo("PRO");
        // 3 uploads de 60 s = 3 min ≤ 120 → OK
        for (int i = 0; i < 3; i++) {
            uploadVideo("pro-" + i + ".mp4", 60L).andExpect(status().isCreated());
        }
    }

    // I-VIDEO-Q-05 : FREE / plan inconnu (STARTER) — 1 min de quota — 60 s OK puis 1 s → 402
    @Test
    void upload_videoMp4_starterPlan_secondUploadHits1MinFreeFallback() throws Exception {
        // Le setUp a déjà inséré une subscription "STARTER" → fallback FREE = 1 min
        // Premier upload de 60 s ⇒ 1 min ≤ 1 → OK
        uploadVideo("first.mp4", 60L).andExpect(status().isCreated());

        // Second upload de 1 s ⇒ 1+1=2 > 1 → 402
        MockMultipartFile file = new MockMultipartFile(
                "file", "second.mp4", "video/mp4", "x".getBytes());
        mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", "1")
                        .with(authentication(auth)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("VIDEO_QUOTA_EXCEEDED"));
    }

    // ── SF-261-01 : marquage « écritures adverses » ──────────────────────────

    // I-AP-01 : PATCH adverse-pleadings true → 200 + flag exposé, persisté (visible dans GET)
    @Test
    void markAdversePleadings_true_returns200AndPersists() throws Exception {
        String docId = uploadPdf("conclusions-adverses.pdf");

        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/documents/" + docId + "/adverse-pleadings")
                        .contentType("application/json")
                        .content("{\"adversePleadings\":true}")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.adversePleadings").value(true));

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adversePleadings").value(true));
    }

    // I-AP-02 : re-cliquer repasse à false
    @Test
    void markAdversePleadings_false_togglesBack() throws Exception {
        String docId = uploadPdf("doc.pdf");

        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/documents/" + docId + "/adverse-pleadings")
                        .contentType("application/json").content("{\"adversePleadings\":true}")
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/documents/" + docId + "/adverse-pleadings")
                        .contentType("application/json").content("{\"adversePleadings\":false}")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adversePleadings").value(false));
    }

    // I-AP-03 : body sans le champ adversePleadings → 400
    @Test
    void markAdversePleadings_missingField_returns400() throws Exception {
        String docId = uploadPdf("doc.pdf");

        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/documents/" + docId + "/adverse-pleadings")
                        .contentType("application/json").content("{}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-AP-04 : sans auth → 401
    @Test
    void markAdversePleadings_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID() + "/adverse-pleadings")
                        .contentType("application/json").content("{\"adversePleadings\":true}"))
                .andExpect(status().isUnauthorized());
    }

    // I-AP-05 : document inconnu → 404
    @Test
    void markAdversePleadings_unknownDocument_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/documents/" + UUID.randomUUID() + "/adverse-pleadings")
                        .contentType("application/json").content("{\"adversePleadings\":true}")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-AP-06 : dossier d'un autre workspace → 404 (isolation)
    @Test
    void markAdversePleadings_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + otherCaseFileId + "/documents/" + UUID.randomUUID() + "/adverse-pleadings")
                        .contentType("application/json").content("{\"adversePleadings\":true}")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-AP-07 : GET documents expose adversePleadings = false par défaut
    @Test
    void list_exposesAdversePleadingsDefaultFalse() throws Exception {
        uploadPdf("doc.pdf");

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/documents")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adversePleadings").value(false));
    }

    /** Utilitaire — upload un PDF et retourne l'id du document créé. */
    private String uploadPdf(String name) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", name, "application/pdf", ("PDF " + name).getBytes());
        String uploadResponse = mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file).with(authentication(auth)))
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(uploadResponse).get("id").asText();
    }

    /** Utilitaire — upload une vidéo MP4 avec la durée demandée en secondes. */
    private org.springframework.test.web.servlet.ResultActions uploadVideo(String name, long durationSeconds) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", name, "video/mp4", ("data-" + name).getBytes());
        return mockMvc.perform(multipart("/api/v1/case-files/" + caseFileId + "/documents")
                        .file(file)
                        .header("X-Video-Duration-Seconds", String.valueOf(durationSeconds))
                        .with(authentication(auth)));
    }

    /** Utilitaire — bascule la subscription du workspace courant vers le planCode demandé. */
    private void switchSubscriptionTo(String planCode) {
        Workspace ws = workspaceRepository.findAll().stream()
                .filter(w -> w.getName().equals("doc-test@example.com"))
                .findFirst()
                .orElseThrow();
        Subscription sub = subscriptionRepository.findByWorkspaceId(ws.getId()).orElseThrow();
        sub.setPlanCode(planCode);
        subscriptionRepository.save(sub);
        ws.setPlanCode(planCode);
        workspaceRepository.save(ws);
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
