package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class PrudhomeFicheServiceTest {

    private final PrudhomeFicheRepository ficheRepository = mock(PrudhomeFicheRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OidcUser oidcUser = mock(OidcUser.class);

    private final PrudhomeFicheService service = new PrudhomeFicheService(
            ficheRepository, caseFileRepository, workspaceMemberRepository,
            currentUserResolver, caseAnalysisRepository, documentRepository, objectMapper);

    // --- GET ---

    // U-DT04-01 : fiche existante → retournée avec pièces
    @Test
    void get_existingFiche_returnsMappedResponse() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);

        PrudhomeFiche fiche = fiche(caseFileId, caseFile);
        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(fiche));
        Document doc = document(caseFile, "contrat.pdf");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of(doc));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.id()).isEqualTo(fiche.getId());
        assertThat(response.demandeur().nom()).isEqualTo("Dupont");
        assertThat(response.piecesList()).hasSize(1);
        assertThat(response.piecesList().get(0).numero()).isEqualTo(1);
        assertThat(response.piecesList().get(0).nom()).isEqualTo("contrat.pdf");
    }

    // U-DT04-02 : aucune fiche → réponse vide pré-remplie avec pièces
    @Test
    void get_noFiche_returnsPrefilledWithPieces() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);

        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        Document doc = document(caseFile, "bulletin.pdf");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of(doc));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.id()).isNull();
        assertThat(response.demandes()).isEmpty();
        assertThat(response.piecesList()).hasSize(1);
    }

    // U-DT04-03 : dossier inexistant → 404
    @Test
    void get_unknownCaseFile_throws404() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        setupWorkspaceOnly(workspace);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(caseFileId, oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // U-DT04-04 : dossier appartenant à un autre workspace → 404 opaque
    @Test
    void get_differentWorkspace_throws404() {
        UUID caseFileId = UUID.randomUUID();
        Workspace userWorkspace = workspace();
        Workspace otherWorkspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, otherWorkspace);
        setupWorkspaceOnly(userWorkspace);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));

        assertThatThrownBy(() -> service.get(caseFileId, oidcUser, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // --- UPSERT ---

    // U-DT04-05 : upsert création — fiche inexistante → sauvegardée et retournée
    @Test
    void upsert_noExistingFiche_createsAndReturns() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);

        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());

        PrudhomeFiche saved = fiche(caseFileId, caseFile);
        when(ficheRepository.save(any(PrudhomeFiche.class))).thenReturn(saved);

        PrudhomeFicheRequest request = request("Martin", "Renault SAS");
        PrudhomeFicheResponse response = service.upsert(caseFileId, request, oidcUser, null);

        assertThat(response.id()).isEqualTo(saved.getId());
        verify(ficheRepository).save(any(PrudhomeFiche.class));
    }

    // U-DT04-06 : upsert mise à jour — fiche existante → modifiée et retournée
    @Test
    void upsert_existingFiche_updatesAndReturns() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);

        PrudhomeFiche existing = fiche(caseFileId, caseFile);
        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(existing));
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());
        when(ficheRepository.save(any(PrudhomeFiche.class))).thenReturn(existing);

        PrudhomeFicheRequest request = request("Lemaire", "SNCF");
        service.upsert(caseFileId, request, oidcUser, null);

        verify(ficheRepository).save(existing);
    }

    // --- helpers ---

    private Workspace workspace() {
        Workspace w = new Workspace();
        w.setId(UUID.randomUUID());
        return w;
    }

    private CaseFile caseFile(UUID id, Workspace workspace) {
        CaseFile cf = new CaseFile();
        cf.setId(id);
        cf.setWorkspace(workspace);
        return cf;
    }

    private void setupWorkspaceOnly(Workspace workspace) {
        User user = new User();
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
    }

    private void setupAccess(Workspace workspace, CaseFile caseFile, UUID caseFileId) {
        setupWorkspaceOnly(workspace);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
    }

    private PrudhomeFiche fiche(UUID caseFileId, CaseFile caseFile) {
        PrudhomeFiche f = new PrudhomeFiche();
        f.setId(UUID.randomUUID());
        f.setCaseFile(caseFile);
        f.setDemandeur("{\"nom\":\"Dupont\",\"prenom\":null,\"adresse\":null,\"telephone\":null,\"email\":null,\"profession\":null}");
        f.setDefendeur("{\"nom\":null,\"adresse\":null,\"siret\":null,\"representant\":null}");
        f.setDemandes("[]");
        f.setUpdatedAt(Instant.now());
        return f;
    }

    private Document document(CaseFile caseFile, String filename) {
        Document doc = new Document();
        doc.setCaseFile(caseFile);
        doc.setOriginalFilename(filename);
        return doc;
    }

    private PrudhomeFicheRequest request(String nomDemandeur, String nomDefendeur) {
        return new PrudhomeFicheRequest(
                new PrudhomeFicheRequest.Demandeur(nomDemandeur, null, null, null, null, null),
                new PrudhomeFicheRequest.Defendeur(nomDefendeur, null, null, null),
                List.of(),
                null,
                null
        );
    }
}
