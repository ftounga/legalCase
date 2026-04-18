package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
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

    // --- SF-DT-04-04 : enrichissement pré-remplissage ---

    // U-DT04-07 : pré-remplissage complet — identité salarié + employeur + demandes additionnelles
    @Test
    void prefill_fullTravailData_populatesDemandeurDefendeurAndDemandes() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "travail_extracted_data": {
                    "nom_salarie": "Dupont",
                    "prenom_salarie": "Jean",
                    "adresse_salarie": "12 rue de la Paix, 75002 Paris",
                    "poste": "Comptable",
                    "nom_employeur": "Acme SAS",
                    "adresse_employeur": "5 avenue des Champs, 75008 Paris",
                    "siret_employeur": "12345678901234",
                    "representant_employeur": "Martin Dupond"
                  },
                  "compensation_data": {
                    "type_rupture": "LICENCIEMENT",
                    "anciennete_annees": 5,
                    "anciennete_mois": 0,
                    "salaire_reference_mensuel": 3000
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        // Demandeur : 4 champs pré-remplis (nom, prénom, adresse, profession)
        assertThat(response.demandeur().nom()).isEqualTo("Dupont");
        assertThat(response.demandeur().prenom()).isEqualTo("Jean");
        assertThat(response.demandeur().adresse()).isEqualTo("12 rue de la Paix, 75002 Paris");
        assertThat(response.demandeur().profession()).isEqualTo("Comptable");
        // Défendeur : 4 champs pré-remplis
        assertThat(response.defendeur().nom()).isEqualTo("Acme SAS");
        assertThat(response.defendeur().adresse()).isEqualTo("5 avenue des Champs, 75008 Paris");
        assertThat(response.defendeur().siret()).isEqualTo("12345678901234");
        assertThat(response.defendeur().representant()).isEqualTo("Martin Dupond");
        // Demandes : indemnité de licenciement + préavis (2 mois ≥ 2 ans) + DI sans cause
        assertThat(response.demandes()).hasSize(3);
        assertThat(response.demandes().get(0).label()).contains("Indemnité de licenciement");
        assertThat(response.demandes().get(1).label()).contains("Indemnité compensatoire de préavis (2 mois)");
        assertThat(response.demandes().get(1).montant()).isEqualTo(6000.0);
        assertThat(response.demandes().get(2).label()).contains("Dommages et intérêts");
        assertThat(response.demandes().get(2).montant()).isEqualTo(3000.0);
    }

    // U-DT04-08 : identité partielle — seuls les champs extraits sont pré-remplis
    @Test
    void prefill_partialIdentity_populatesOnlyAvailableFields() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "travail_extracted_data": {
                    "nom_salarie": "Dupont"
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.demandeur().nom()).isEqualTo("Dupont");
        assertThat(response.demandeur().prenom()).isNull();
        assertThat(response.demandeur().adresse()).isNull();
        assertThat(response.defendeur().nom()).isNull();
        assertThat(response.demandes()).isEmpty();
    }

    // U-DT04-09 : licenciement sans salaire → pas de demandes additionnelles
    @Test
    void prefill_licenciementWithoutSalary_noAdditionalDemandes() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "compensation_data": {
                    "type_rupture": "LICENCIEMENT",
                    "anciennete_annees": 5,
                    "anciennete_mois": 0,
                    "salaire_reference_mensuel": null
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        // salaire null → compensationEstimate construit avec salaire 0 → pas de prefill préavis / DI
        assertThat(response.demandes()).allMatch(d -> !d.label().contains("préavis"));
        assertThat(response.demandes()).allMatch(d -> !d.label().contains("Dommages"));
    }

    // U-DT04-10 : démission → pas d'indemnité de licenciement ni demandes additionnelles
    @Test
    void prefill_demission_noIndemnityDemandes() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "compensation_data": {
                    "type_rupture": "DEMISSION",
                    "anciennete_annees": 5,
                    "anciennete_mois": 0,
                    "salaire_reference_mensuel": 3000
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        // Démission : compensationEstimate est null ou indemnité 0 → pas de demandes licenciement/préavis/DI
        assertThat(response.demandes()).allMatch(d -> !d.label().contains("préavis"));
        assertThat(response.demandes()).allMatch(d -> !d.label().contains("Dommages"));
    }

    // U-DT04-11 : licenciement < 2 ans → préavis 1 mois (vs 2 pour ≥ 2 ans)
    @Test
    void prefill_licenciementLessThan2Years_preavis1Mois() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "compensation_data": {
                    "type_rupture": "LICENCIEMENT",
                    "anciennete_annees": 1,
                    "anciennete_mois": 6,
                    "salaire_reference_mensuel": 2500
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        var preavis = response.demandes().stream().filter(d -> d.label().contains("préavis")).findFirst();
        assertThat(preavis).isPresent();
        assertThat(preavis.get().label()).contains("(1 mois)");
        assertThat(preavis.get().montant()).isEqualTo(2500.0);
    }

    // U-DT04-12 : fiche déjà persistée → pas de re-prefill, renvoie l'existante
    @Test
    void prefill_existingFiche_keepsUserEdits() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);

        PrudhomeFiche existing = fiche(caseFileId, caseFile);
        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(existing));
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        // La fiche "Dupont" de l'helper est retournée — pas de pré-remplissage écrasé
        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.demandeur().nom()).isEqualTo("Dupont");
        // caseAnalysisRepository n'est PAS interrogé
        verify(caseAnalysisRepository, never())
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any());
    }

    // U-DT04-13 : rétrocompat — analyse sans travail_extracted_data (ancienne) → fallback correct
    @Test
    void prefill_legacyAnalysisWithoutTravailData_backwardCompatible() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "compensation_data": {
                    "type_rupture": "LICENCIEMENT",
                    "anciennete_annees": 5,
                    "anciennete_mois": 0,
                    "salaire_reference_mensuel": 3000
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        PrudhomeFicheResponse response = service.get(caseFileId, oidcUser, null);

        // Demandeur vide (champ IA non présent), mais demandes licenciement existent
        assertThat(response.demandeur().nom()).isEmpty();
        assertThat(response.defendeur().nom()).isNull();
        assertThat(response.demandes()).hasSize(3); // licenciement + préavis + DI
    }

    // --- helpers SF-DT-04-04 ---

    /** Setup classique pour un dossier accessible + pieces list vide. Retourne le caseFileId. */
    private UUID setupPrefillContext() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);
        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile)).thenReturn(List.of());
        return caseFileId;
    }

    private CaseAnalysis analysisWith(String resultJson) {
        CaseAnalysis a = new CaseAnalysis();
        a.setId(UUID.randomUUID());
        a.setVersion(1);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult(resultJson);
        a.setUpdatedAt(Instant.now());
        return a;
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
