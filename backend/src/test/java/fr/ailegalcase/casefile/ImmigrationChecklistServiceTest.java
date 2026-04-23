package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class ImmigrationChecklistServiceTest {

    private final ImmigrationPieceCheckRepository pieceCheckRepository = mock(ImmigrationPieceCheckRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final ImmigrationPieceAutoFillService autoFillService = mock(ImmigrationPieceAutoFillService.class);
    private final OidcUser oidcUser = mock(OidcUser.class);

    private final ImmigrationChecklistService service = new ImmigrationChecklistService(
            pieceCheckRepository, caseFileRepository, workspaceMemberRepository, currentUserResolver,
            autoFillService);

    // U-IM01-01 : GET dossier sans historique → toutes pièces INCONNU
    @Test
    void get_noPriorHistory_returnsAllPiecesInconnu() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = immigrationCaseFile(caseFileId, workspace());
        setupAccess(caseFile, caseFileId);
        when(pieceCheckRepository.findByCaseFileIdAndTitreTypeAndCountry(caseFileId, "VISA_ETUDIANT", "FRANCE"))
                .thenReturn(List.of());

        ImmigrationChecklistResponse response = service.get(caseFileId, "VISA_ETUDIANT", "FRANCE", oidcUser, null);

        assertThat(response.titreType()).isEqualTo("VISA_ETUDIANT");
        assertThat(response.country()).isEqualTo("FRANCE");
        assertThat(response.pieces()).isNotEmpty();
        assertThat(response.pieces()).allMatch(p -> "INCONNU".equals(p.statut()));
    }

    // U-IM01-02 : GET avec historique partiel → statuts fusionnés
    @Test
    void get_withPartialHistory_mergesStatuts() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = immigrationCaseFile(caseFileId, workspace());
        setupAccess(caseFile, caseFileId);

        ImmigrationPieceCheck existing = new ImmigrationPieceCheck();
        existing.setLabel("Passeport en cours de validité");
        existing.setStatut("PRESENT");

        when(pieceCheckRepository.findByCaseFileIdAndTitreTypeAndCountry(caseFileId, "VISA_ETUDIANT", "FRANCE"))
                .thenReturn(List.of(existing));

        ImmigrationChecklistResponse response = service.get(caseFileId, "VISA_ETUDIANT", "FRANCE", oidcUser, null);

        assertThat(response.pieces())
                .filteredOn(p -> "Passeport en cours de validité".equals(p.label()))
                .singleElement()
                .extracting(ImmigrationChecklistResponse.PieceItem::statut)
                .isEqualTo("PRESENT");
        assertThat(response.pieces())
                .filteredOn(p -> !"Passeport en cours de validité".equals(p.label()))
                .allMatch(p -> "INCONNU".equals(p.statut()));
    }

    // U-IM01-03 : PUT → statuts persistés, retour correct
    @Test
    void upsert_persistsStatutsAndReturns() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = immigrationCaseFile(caseFileId, workspace());
        setupAccess(caseFile, caseFileId);
        when(pieceCheckRepository.findByCaseFileIdAndTitreTypeAndCountry(any(), any(), any()))
                .thenReturn(List.of());
        when(pieceCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImmigrationChecklistRequest request = new ImmigrationChecklistRequest(
                "VISA_ETUDIANT", "FRANCE",
                List.of(new ImmigrationChecklistRequest.PieceItemRequest("Passeport en cours de validité", "PRESENT"))
        );

        ImmigrationChecklistResponse response = service.upsert(caseFileId, request, oidcUser, null);

        verify(pieceCheckRepository, atLeastOnce()).save(any(ImmigrationPieceCheck.class));
        assertThat(response.titreType()).isEqualTo("VISA_ETUDIANT");
    }

    // U-IM01-04 : 404 si dossier inexistant
    @Test
    void get_caseFileNotFound_throws404() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());
        User user = new User();
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        WorkspaceMember member = new WorkspaceMember();
        Workspace w = new Workspace(); w.setId(UUID.randomUUID());
        member.setWorkspace(w);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.get(UUID.randomUUID(), "VISA_ETUDIANT", "FRANCE", oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    // U-IM01-05 : 404 si workspace différent
    @Test
    void get_differentWorkspace_throws404() {
        UUID caseFileId = UUID.randomUUID();
        Workspace userWorkspace = workspace();
        Workspace otherWorkspace = workspace();
        CaseFile caseFile = immigrationCaseFile(caseFileId, otherWorkspace);

        User user = new User();
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(userWorkspace);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));

        assertThatThrownBy(() -> service.get(caseFileId, "VISA_ETUDIANT", "FRANCE", oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    // U-IM01-06 : 400 si legalDomain ≠ DROIT_IMMIGRATION
    @Test
    void get_notImmigrationDossier_throws400() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = caseFile(caseFileId, workspace(), "DROIT_DU_TRAVAIL");
        setupAccess(caseFile, caseFileId);

        assertThatThrownBy(() -> service.get(caseFileId, "VISA_ETUDIANT", "FRANCE", oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // U-IM01-07 : 400 si titreType inconnu
    @Test
    void get_unknownTitreType_throws400() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID(), "TYPE_INCONNU", "FRANCE", oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // U-IM01-08 : 400 si country non supportée
    @Test
    void get_unsupportedCountry_throws400() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID(), "VISA_ETUDIANT", "ALLEMAGNE", oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // U-IM01-09 : statut invalide dans PUT → 400
    @Test
    void upsert_invalidStatut_throws400() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = immigrationCaseFile(caseFileId, workspace());
        setupAccess(caseFile, caseFileId);
        when(pieceCheckRepository.findByCaseFileIdAndTitreTypeAndCountry(any(), any(), any()))
                .thenReturn(List.of());

        ImmigrationChecklistRequest request = new ImmigrationChecklistRequest(
                "VISA_ETUDIANT", "FRANCE",
                List.of(new ImmigrationChecklistRequest.PieceItemRequest("Passeport en cours de validité", "INVALIDE"))
        );

        assertThatThrownBy(() -> service.upsert(caseFileId, request, oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // --- helpers ---

    private Workspace workspace() {
        Workspace w = new Workspace();
        w.setId(UUID.randomUUID());
        return w;
    }

    private CaseFile immigrationCaseFile(UUID id, Workspace workspace) {
        return caseFile(id, workspace, "DROIT_IMMIGRATION");
    }

    private CaseFile caseFile(UUID id, Workspace workspace, String legalDomain) {
        CaseFile cf = new CaseFile();
        cf.setId(id);
        cf.setWorkspace(workspace);
        cf.setLegalDomain(legalDomain);
        return cf;
    }

    private void setupAccess(CaseFile caseFile, UUID caseFileId) {
        User user = new User();
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(caseFile.getWorkspace());
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
    }
}
