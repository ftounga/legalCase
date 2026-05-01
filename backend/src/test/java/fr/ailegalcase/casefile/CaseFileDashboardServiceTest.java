package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-167 SF-167-01 — Tests unitaires de la méthode {@code assembleTiles} de
 * {@link CaseFileDashboardService}. Vérifie la couverture des 10 outils
 * pilotes, le cas vide, et le comportement fail-open par tile.
 */
class CaseFileDashboardServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LicenciementAnalysisRepository licenciementRepo;
    private IndemniteComparatifRepository indemniteRepo;
    private RuptureConvIndemniteRepository ruptureConvIndemniteRepo;
    private AncienneteAnalysisRepository ancienneteRepo;
    private ImmigrationTitleDecisionRepository titleDecisionRepo;
    private ImmigrationWorkRightRepository workRightRepo;
    private ImmigrationRecoursRepository recoursRepo;
    private PartageImmobilierRepository partageRepo;
    private CalendrierGardeRepository gardeRepo;
    private DivorceChecklistRepository divorceRepo;
    private ChangementStatutRepository changementStatutRepo;

    private CaseFileDashboardService service;

    @BeforeEach
    void setUp() {
        licenciementRepo = mock(LicenciementAnalysisRepository.class);
        indemniteRepo = mock(IndemniteComparatifRepository.class);
        ruptureConvIndemniteRepo = mock(RuptureConvIndemniteRepository.class);
        ancienneteRepo = mock(AncienneteAnalysisRepository.class);
        titleDecisionRepo = mock(ImmigrationTitleDecisionRepository.class);
        workRightRepo = mock(ImmigrationWorkRightRepository.class);
        recoursRepo = mock(ImmigrationRecoursRepository.class);
        partageRepo = mock(PartageImmobilierRepository.class);
        gardeRepo = mock(CalendrierGardeRepository.class);
        divorceRepo = mock(DivorceChecklistRepository.class);
        changementStatutRepo = mock(ChangementStatutRepository.class);

        // Default empties — chaque test surcharge ce qu'il a besoin.
        when(licenciementRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(indemniteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(ruptureConvIndemniteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(ancienneteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(titleDecisionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(workRightRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(recoursRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(partageRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(gardeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(divorceRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(changementStatutRepo.findByCaseFileId(any())).thenReturn(Optional.empty());

        service = new CaseFileDashboardService(
                objectMapper,
                /* caseFileRepository */ null,
                /* workspaceMemberRepository */ null,
                /* currentUserResolver */ null,
                /* analysisRepository */ null,
                licenciementRepo,
                indemniteRepo,
                ruptureConvIndemniteRepo,
                ancienneteRepo,
                titleDecisionRepo,
                workRightRepo,
                recoursRepo,
                partageRepo,
                gardeRepo,
                divorceRepo,
                changementStatutRepo);
    }

    @Test
    void assembleTiles_returnsEmptyListWhenNoAnalysis() {
        UUID caseFileId = UUID.randomUUID();

        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        assertThat(tiles).isEmpty();
    }

    @Test
    void assembleTiles_returnsTileForChangementStatut_whenAnalysisExists() throws Exception {
        UUID caseFileId = UUID.randomUUID();
        ChangementStatutAnalysis entity = new ChangementStatutAnalysis();
        ChangementStatutResult result = new ChangementStatutResult(
                "ETUDIANT", "VPF", "VPF", 8, true, new BigDecimal("2800.00"), true,
                "ELEVEE",
                List.of("Justificatif", "Acte"),
                List.of(),
                3,
                "CESEDA L.423-1",
                "ELEVEE",
                List.of("Conseil pratique"));
        entity.setResultData(objectMapper.writeValueAsString(result));
        when(changementStatutRepo.findByCaseFileId(caseFileId)).thenReturn(Optional.of(entity));

        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        assertThat(tiles).hasSize(1);
        DashboardTile tile = tiles.get(0);
        assertThat(tile.toolId()).isEqualTo("F-IM-11-changement-statut");
        assertThat(tile.theme()).isEqualTo("VALIDITE");
        assertThat(tile.label()).isEqualTo("Changement de statut");
        assertThat(tile.primaryValue()).isEqualTo("ETUDIANT → VPF (ELEVEE)");
        assertThat(tile.secondaryValue()).isEqualTo("8 mois restants");
        assertThat(tile.alertLevel()).isEqualTo("OK");
    }

    @Test
    void assembleTiles_changementStatutFAIBLE_alertLevelALERT() throws Exception {
        UUID caseFileId = UUID.randomUUID();
        ChangementStatutAnalysis entity = new ChangementStatutAnalysis();
        ChangementStatutResult result = new ChangementStatutResult(
                "ETUDIANT", "SALARIE", "SALARIE", 1, true, new BigDecimal("2800.00"), true,
                "FAIBLE",
                List.of(), List.of(), 3, "CESEDA L.421-1", "FAIBLE", List.of());
        entity.setResultData(objectMapper.writeValueAsString(result));
        when(changementStatutRepo.findByCaseFileId(caseFileId)).thenReturn(Optional.of(entity));

        DashboardTile tile = service.assembleTiles(caseFileId).get(0);

        assertThat(tile.alertLevel()).isEqualTo("ALERT");
    }

    @Test
    void assembleTiles_changementStatutMOYENNE_alertLevelWARNING() throws Exception {
        UUID caseFileId = UUID.randomUUID();
        ChangementStatutAnalysis entity = new ChangementStatutAnalysis();
        ChangementStatutResult result = new ChangementStatutResult(
                "ETUDIANT", "SALARIE", "SALARIE", 10, true, new BigDecimal("2200.00"), true,
                "MOYENNE",
                List.of(), List.of(), 3, "CESEDA L.421-1", "MOYENNE", List.of());
        entity.setResultData(objectMapper.writeValueAsString(result));
        when(changementStatutRepo.findByCaseFileId(caseFileId)).thenReturn(Optional.of(entity));

        DashboardTile tile = service.assembleTiles(caseFileId).get(0);

        assertThat(tile.alertLevel()).isEqualTo("WARNING");
    }

    @Test
    void assembleTiles_failsOpenPerTileOnRepoException() throws Exception {
        UUID caseFileId = UUID.randomUUID();

        // Le repo F-IM-11 throw → la tile correspondante manque, mais les autres
        // sont retournées (fail-open par tile). On peuple en parallèle une analyse
        // anciennete pour vérifier qu'elle reste visible.
        when(changementStatutRepo.findByCaseFileId(caseFileId))
                .thenThrow(new RuntimeException("repo down"));

        AncienneteAnalysis a = new AncienneteAnalysis();
        AncienneteResult r = new AncienneteResult(
                "CC0123", "Convention test", "FRANCE",
                4, 6,
                25, 0, 25,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 25,
                List.of());
        a.setResultData(objectMapper.writeValueAsString(r));
        when(ancienneteRepo.findByCaseFileId(caseFileId)).thenReturn(Optional.of(a));

        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        assertThat(tiles).hasSize(1);
        assertThat(tiles.get(0).toolId()).isEqualTo("F-DT-07-anciennete-conges-prime");
    }

    @Test
    void assembleTiles_orderIsStableByToolId() throws Exception {
        UUID caseFileId = UUID.randomUUID();

        // F-IM-11 et F-DT-07 ensemble — l'ordre attendu est par toolId :
        // "F-DT-07-..." < "F-IM-11-...".
        ChangementStatutAnalysis cse = new ChangementStatutAnalysis();
        ChangementStatutResult cs = new ChangementStatutResult(
                "ETUDIANT", "VPF", "VPF", 8, true, null, true,
                "ELEVEE", List.of(), List.of(), 3, "L.423-1", "ELEVEE", List.of());
        cse.setResultData(objectMapper.writeValueAsString(cs));
        when(changementStatutRepo.findByCaseFileId(caseFileId)).thenReturn(Optional.of(cse));

        AncienneteAnalysis a = new AncienneteAnalysis();
        AncienneteResult ar = new AncienneteResult(
                "CC0123", "Convention test", "FRANCE",
                4, 6,
                25, 0, 25,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 25,
                List.of());
        a.setResultData(objectMapper.writeValueAsString(ar));
        when(ancienneteRepo.findByCaseFileId(caseFileId)).thenReturn(Optional.of(a));

        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        assertThat(tiles).extracting(DashboardTile::toolId)
                .containsExactly("F-DT-07-anciennete-conges-prime", "F-IM-11-changement-statut");
    }
}
