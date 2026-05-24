package fr.ailegalcase.casefile.jurisprudence;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.jurisprudencemapping.ConclusionsJurisprudenceContext;
import fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool;
import fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationResponse;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * F-JU-02 / SF-JU-02-02 — tests unitaires Mockito de
 * {@link CaseFileJurisprudenceApplicableService}.
 *
 * <p>Couverture : 200 nominal avec entries, 200 liste vide, 404 dossier
 * inexistant, 404 dossier appartenant à un autre workspace (pas de fuite
 * d'existence — pattern miroir {@code JurisprudenceCitationService} F-242).</p>
 */
@ExtendWith(MockitoExtension.class)
class CaseFileJurisprudenceApplicableServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private ConclusionsJurisprudenceContext jurisprudenceContext;
    @Mock private OidcUser oidcUser;
    @Mock private Principal principal;

    private CaseFileJurisprudenceApplicableService service;

    private User user;
    private Workspace workspace;
    private UUID caseFileId;

    @BeforeEach
    void setUp() {
        service = new CaseFileJurisprudenceApplicableService(
                caseFileRepository, currentUserResolver, workspaceMemberRepository, jurisprudenceContext);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("avocat@example.com");

        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setName("Cabinet");

        caseFileId = UUID.randomUUID();
    }

    @Test
    void getForCaseFile_nominal_returnsMappedEntries() {
        // Given
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);

        when(currentUserResolver.resolve(any(), anyString(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));

        ToolJurisprudenceCitationResponse citation = new ToolJurisprudenceCitationResponse(
                UUID.randomUUID(),
                "Cass. soc. 8 janv. 2025, n° 23-12.345",
                "Cour de cassation, chambre sociale",
                LocalDate.of(2025, 1, 8),
                "23-12.345",
                "https://legifrance.gouv.fr/some/url",
                "Le barème Macron s'applique sans exception.",
                Instant.now(),
                new BigDecimal("0.95"));

        when(jurisprudenceContext.collectForCaseFile(caseFileId)).thenReturn(List.of(
                new ToolJurisprudenceCitationByTool(
                        "f-dt-30-indemnite-licenciement-macron",
                        "default",
                        List.of(citation))));

        // When
        JurisprudenceApplicableResponse response = service.getForCaseFile(
                caseFileId, oidcUser, "google", principal);

        // Then
        assertThat(response.entries()).hasSize(1);
        JurisprudenceApplicableEntry entry = response.entries().get(0);
        assertThat(entry.toolId()).isEqualTo("f-dt-30-indemnite-licenciement-macron");
        assertThat(entry.brancheCalculId()).isEqualTo("default");
        assertThat(entry.citations()).hasSize(1);
        JurisprudenceCitationDto dto = entry.citations().get(0);
        assertThat(dto.arretRef()).isEqualTo("Cass. soc. 8 janv. 2025, n° 23-12.345");
        assertThat(dto.juridiction()).isEqualTo("Cour de cassation, chambre sociale");
        assertThat(dto.numeroPourvoi()).isEqualTo("23-12.345");
        assertThat(dto.chapeauOfficiel()).contains("barème Macron");
        assertThat(dto.confidenceScore()).isEqualByComparingTo("0.95");
    }

    @Test
    void getForCaseFile_emptyContext_returns200EmptyList() {
        // Given : aucun ToolUsageContributor V1 → contexte retourne liste vide
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);

        when(currentUserResolver.resolve(any(), anyString(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(jurisprudenceContext.collectForCaseFile(caseFileId)).thenReturn(List.of());

        // When
        JurisprudenceApplicableResponse response = service.getForCaseFile(
                caseFileId, oidcUser, "google", principal);

        // Then
        assertThat(response.entries()).isEmpty();
    }

    @Test
    void getForCaseFile_unknownCaseFile_throws404() {
        // Given
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);

        when(currentUserResolver.resolve(any(), anyString(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getForCaseFile(caseFileId, oidcUser, "google", principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void getForCaseFile_otherWorkspace_throws404NotLeakingExistence() {
        // Given : dossier existe MAIS appartient à un autre workspace
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(UUID.randomUUID());

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setWorkspace(otherWorkspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);

        when(currentUserResolver.resolve(any(), anyString(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));

        // When / Then : 404 — pas 403 (pas de fuite d'existence — pattern miroir
        // SF-242-01 / SF-98-52 du même codebase).
        assertThatThrownBy(() -> service.getForCaseFile(caseFileId, oidcUser, "google", principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }
}
