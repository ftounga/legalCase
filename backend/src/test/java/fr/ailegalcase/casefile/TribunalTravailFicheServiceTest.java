package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TribunalTravailFicheServiceTest {

    private final TribunalTravailFicheRepository ficheRepository = mock(TribunalTravailFicheRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final DocumentPieceRepository documentPieceRepository = mock(DocumentPieceRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OidcUser oidcUser = mock(OidcUser.class);

    private final TribunalTravailFicheService service = new TribunalTravailFicheService(
            ficheRepository, caseFileRepository, workspaceMemberRepository,
            currentUserResolver, caseAnalysisRepository, documentPieceRepository, objectMapper);

    // U-DT06-05-01 : identité complète (salarié + employeur) → requerant/defendeur pré-remplis
    @Test
    void prefill_fullIdentity_populatesRequerantAndDefendeur() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "travail_extracted_data": {
                    "nom_salarie": "Janssens",
                    "prenom_salarie": "Pieter",
                    "adresse_salarie": "Rue de la Loi 16, 1000 Bruxelles",
                    "nom_employeur": "ACME SA",
                    "adresse_employeur": "Avenue Louise 500, 1050 Bruxelles",
                    "bce_employeur": "0123456789",
                    "representant_employeur": "Sophie Dubois"
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.requerant().nom()).isEqualTo("Janssens");
        assertThat(response.requerant().prenom()).isEqualTo("Pieter");
        assertThat(response.requerant().domicile()).isEqualTo("Rue de la Loi 16, 1000 Bruxelles");
        assertThat(response.requerant().registreNational()).isNull();
        assertThat(response.defendeur().nom()).isEqualTo("ACME SA");
        assertThat(response.defendeur().siegeSocial()).isEqualTo("Avenue Louise 500, 1050 Bruxelles");
        assertThat(response.defendeur().numeroBce()).isEqualTo("0123456789");
        assertThat(response.defendeur().representant()).isEqualTo("Sophie Dubois");
    }

    // U-DT06-05-02 : identité partielle → seuls les champs extraits sont pré-remplis
    @Test
    void prefill_partialIdentity_populatesOnlyAvailableFields() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "travail_extracted_data": {
                    "nom_salarie": "Janssens"
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.requerant().nom()).isEqualTo("Janssens");
        assertThat(response.requerant().prenom()).isNull();
        assertThat(response.defendeur().nom()).isNull();
        assertThat(response.defendeur().numeroBce()).isNull();
        assertThat(response.demandes()).isEmpty();
    }

    // U-DT06-05-03 : dates → contratInfo.dateDebut/dateFin pré-remplis
    @Test
    void prefill_dates_populatesContratInfo() {
        UUID caseFileId = setupPrefillContext();
        CaseAnalysis analysis = analysisWith("""
                {
                  "travail_extracted_data": {
                    "date_entree": "2020-01-15",
                    "date_licenciement": "2026-03-30"
                  }
                }
                """);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.of(analysis));

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.contratInfo().dateDebut()).isEqualTo("2020-01-15");
        assertThat(response.contratInfo().dateFin()).isEqualTo("2026-03-30");
        assertThat(response.contratInfo().typeContrat()).isNull();
        assertThat(response.contratInfo().motifRupture()).isNull();
    }

    // U-DT06-05-04 : LICENCIEMENT + salaire connu → préavis + CCT 109 ajoutés
    @Test
    void prefill_licenciementWithSalary_addsPreavisAndCct109() {
        UUID caseFileId = setupPrefillContext();
        // 5 ans d'ancienneté → préavis = 15 semaines (table Loi 26/12/2013)
        // Salaire hebdo = 3000 × 12/52 ≈ 692.31 €
        // Préavis = 692.31 × 15 = 10384.62 €
        // CCT 109 indicatif = 692.31 × 10 = 6923.08 €
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

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.demandes()).hasSize(2);
        var preavis = response.demandes().get(0);
        assertThat(preavis.label()).contains("Indemnité compensatoire de préavis").contains("15 semaines");
        assertThat(preavis.montant()).isEqualTo(10384.62);

        var cct109 = response.demandes().get(1);
        assertThat(cct109.label()).contains("manifestement déraisonnable").contains("CCT 109").contains("10 semaines");
        assertThat(cct109.montant()).isEqualTo(6923.08);
    }

    // U-DT06-05-05 : DEMISSION → pas de CCT 109 ni préavis
    @Test
    void prefill_demission_noCct109NoPreavis() {
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

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        // DEMISSION : compensationEstimate indemnité = 0, pas de filtre ici, mais CCT 109 filtré par type_rupture
        assertThat(response.demandes()).allMatch(d -> !d.label().contains("manifestement déraisonnable"));
    }

    // U-DT06-05-06 : salaire = null / donneesPartielles → pas de CCT 109
    @Test
    void prefill_licenciementWithoutSalary_noCct109() {
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

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.demandes()).allMatch(d -> !d.label().contains("manifestement déraisonnable"));
    }

    // U-DT06-05-07 : fiche déjà persistée → pas de re-prefill
    @Test
    void prefill_existingFiche_keepsUserEditsNoReprefill() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);

        TribunalTravailFiche existing = fiche(caseFileId, caseFile);
        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(existing));
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any())).thenReturn(List.of());

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.requerant().nom()).isEqualTo("Janssens");
        verify(caseAnalysisRepository, never())
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any());
    }

    // U-DT06-05-08 : rétrocompat — analyse sans travail_extracted_data → préavis seulement (pas d'identité)
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

        TribunalTravailFicheResponse response = service.get(caseFileId, oidcUser, null);

        // Requérant vide (nom défaut ""), défendeur null, mais demandes préavis + CCT 109 présentes
        assertThat(response.requerant().nom()).isEmpty();
        assertThat(response.defendeur().nom()).isNull();
        assertThat(response.demandes()).hasSize(2); // préavis + CCT 109
    }

    // --- helpers ---

    private UUID setupPrefillContext() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);
        setupAccess(workspace, caseFile, caseFileId);
        when(ficheRepository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any())).thenReturn(List.of());
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

    private Workspace workspace() {
        Workspace w = new Workspace();
        w.setId(UUID.randomUUID());
        w.setCountry("BELGIQUE");
        return w;
    }

    private CaseFile caseFile(UUID id, Workspace workspace) {
        CaseFile cf = new CaseFile();
        cf.setId(id);
        cf.setWorkspace(workspace);
        cf.setLegalDomain("DROIT_DU_TRAVAIL");
        return cf;
    }

    private void setupAccess(Workspace workspace, CaseFile caseFile, UUID caseFileId) {
        User user = new User();
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
    }

    private TribunalTravailFiche fiche(UUID caseFileId, CaseFile caseFile) {
        TribunalTravailFiche f = new TribunalTravailFiche();
        f.setId(UUID.randomUUID());
        f.setCaseFile(caseFile);
        f.setRequerant("{\"nom\":\"Janssens\",\"prenom\":null,\"domicile\":null,\"registreNational\":null}");
        f.setDefendeur("{\"nom\":null,\"siegeSocial\":null,\"numeroBce\":null,\"representant\":null}");
        f.setProcedureInfo("{\"tribunal\":null,\"division\":null,\"langue\":\"FR\",\"commissionParitaire\":null}");
        f.setContratInfo("{\"typeContrat\":null,\"dateDebut\":null,\"dateFin\":null,\"motifRupture\":null}");
        f.setDemandes("[]");
        f.setUpdatedAt(Instant.now());
        return f;
    }
}
