package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.RetainedPisteAlignment;
import fr.ailegalcase.analysis.RetainedPisteAlignmentService;
import fr.ailegalcase.analysis.RetainedPisteToolMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-192 SF-192-01 — Tests dédiés à la tile {@code F-192-retained-pistes-summary}
 * de {@link CaseFileDashboardService}.
 *
 * <p>Test isolé du big {@link CaseFileDashboardServiceTest} parce que la nouvelle
 * tile dépend du {@code CaseAnalysisRepository} qui était {@code null} dans le
 * setUp historique.</p>
 */
class CaseFileDashboardServiceRetainedPistesTest {

    private CaseAnalysisRepository analysisRepository;
    private RetainedPisteAlignmentService retainedPisteAlignmentService;
    private CaseFileDashboardService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        analysisRepository = mock(CaseAnalysisRepository.class);
        retainedPisteAlignmentService = mock(RetainedPisteAlignmentService.class);

        // Tous les autres repos = mocks vides (pas de tile autre)
        service = new CaseFileDashboardService(
                objectMapper, null, null, null, analysisRepository,
                mock(LicenciementAnalysisRepository.class),
                mock(IndemniteComparatifRepository.class),
                mock(RuptureConvIndemniteRepository.class),
                mock(AncienneteAnalysisRepository.class),
                mock(ImmigrationTitleDecisionRepository.class),
                mock(ImmigrationWorkRightRepository.class),
                mock(ImmigrationRecoursRepository.class),
                mock(PartageImmobilierRepository.class),
                mock(CalendrierGardeRepository.class),
                mock(DivorceChecklistRepository.class),
                mock(ChangementStatutRepository.class),
                mock(RuptureConvAnalysisRepository.class),
                mock(HarcelementNulliteRepository.class),
                mock(DiscriminationRepository.class),
                mock(LicenciementEconomiqueRepository.class),
                mock(PseRepository.class),
                mock(InaptitudeRepository.class),
                mock(LicenciementNulDetectionRepository.class),
                mock(IndemnitePrecariteCddRepository.class),
                mock(IndemniteFinMissionInterimRepository.class),
                mock(HeuresSupRepository.class),
                mock(RappelSalaireRepository.class),
                mock(TravailDissimuleRepository.class),
                mock(RequalificationCddCdiRepository.class),
                mock(RequalificationInterimCdiRepository.class),
                mock(NonConcurrenceRepository.class),
                mock(IndemnitePreavisRepository.class),
                mock(IndemniteCongesPayesRepository.class),
                mock(ProtectionRpRepository.class),
                mock(TransactionRepository.class),
                mock(DocumentsFinContratRepository.class),
                mock(AtMpRepository.class),
                mock(ReferePrudhomalRepository.class),
                mock(ContestationAreRepository.class),
                mock(MotifGraveBeRepository.class),
                mock(AvantagesConventionnelsBeRepository.class),
                mock(CreditTempsBeRepository.class),
                mock(ProcedureNulliteLicenciementRepository.class),
                mock(RupturePeriodeEssaiRepository.class),
                mock(AbandonPostePresomptionDemissionRepository.class),
                mock(CongesPayesArretMaladieRepository.class),
                mock(PriseActeRuptureRepository.class),
                mock(ResiliationJudiciaireCphRepository.class),
                mock(EtrangerMaladeRepository.class),
                mock(RegroupementFamilialRepository.class),
                mock(VpfLiensPersonnelsRepository.class),
                mock(VlsTsValidationRepository.class),
                mock(OqtfCategoriesRepository.class),
                mock(AesPresenceProuveeRepository.class),
                mock(RenouvellementDelaiRepository.class),
                mock(RecepisseAttestationRepository.class),
                mock(OfpraIntroductionRepository.class),
                mock(AjCndaRepository.class),
                mock(VictimeTraiteRepository.class),
                mock(LicenciementFauteGraveLourdRepository.class),
                mock(JldRetentionRepository.class),
                mock(DublinRecoursRepository.class),
                mock(CrrvRefusVisaRepository.class),
                mock(VictimeViolencesL4256Repository.class),
                mock(AcceptationRenonciationSuccessionRepository.class),
                mock(AutoriteParentaleBeRepository.class),
                mock(ContributionAlimentaireEnfantsBeRepository.class),
                mock(ContributionConjointBeRepository.class),
                mock(DivorceDcBeRepository.class),
                mock(DivorceDdiBeRepository.class),
                mock(LiquidationPartageBeRepository.class),
                mock(MediationFamilialePreSaisineRepository.class),
                mock(PacteSuccessoralBe2018Repository.class),
                mock(RegimeCommunauteLegaleBeRepository.class),
                mock(TribunalFamilleBeMesuresProvisoiresRepository.class),
                mock(DivorceAlterationRepository.class),
                mock(DivorceFauteRepository.class),
                mock(DivorceAccepteRepository.class),
                mock(DivorceDesunionIrremediableBeRepository.class),
                mock(MesuresProvisoiresRepository.class),
                mock(RevisionsPostDivorceRepository.class),
                mock(OrdonnanceProtectionRepository.class),
                mock(RecompensesRepository.class),
                mock(CommunauteUniverselleRepository.class),
                mock(PartageJudiciaireRepository.class),
                mock(AdoptionRepository.class),
                mock(ContestationPaterniteRepository.class),
                mock(RecherchePaterniteRepository.class),
                mock(ReconnaissancePaterneleRepository.class),
                mock(PossessionEtatRepository.class),
                mock(AutoriteParentaleRepository.class),
                mock(ChangementResidenceRepository.class),
                mock(DesaccordsParentauxRepository.class),
                mock(PacsDissolutionRepository.class),
                mock(SeparationCorpsRepository.class),
                mock(IndivisionRepository.class),
                mock(OrdonnanceRequeteRepository.class),
                mock(DevolutionLegaleRepository.class),
                mock(DonationRepository.class),
                mock(IndivisionSuccessoraleRepository.class),
                mock(PartageSuccessoralRepository.class),
                mock(RapportSuccessionRepository.class),
                mock(ReserveHereditaireRepository.class),
                mock(TestamentValiditeRepository.class),
                mock(MajeursProtegesRepository.class),
                mock(ChangementEtatCivilRepository.class),
                mock(PmaGpaBioethiqueRepository.class),
                mock(OqtfAvecDelaiRepository.class),
                mock(OqtfSansDelaiRepository.class),
                mock(ReferesAdminRepository.class),
                mock(AesEtudiantRepository.class),
                mock(AesFamilleRepository.class),
                mock(AesHumanitaireRepository.class),
                mock(AesMetiersTensionRepository.class),
                mock(AsileAvanceRepository.class),
                mock(NaturalisationRepository.class),
                mock(RegimeAlgerienRepository.class),
                mock(MineursImmigrationRepository.class),
                mock(MesuresEloignementRepository.class),
                mock(Annexe13BeRepository.class),
                mock(Belgian9bisRepository.class),
                mock(Belgian9terRepository.class),
                mock(Belgian40bisRepository.class),
                mock(Belgian40terRepository.class),
                mock(PrescriptionBeLitigeTravailRepository.class),
                mock(C4OnemChecklistRepository.class),
                retainedPisteAlignmentService,
                mock(fr.ailegalcase.analysis.ProcedureCheckAlignmentService.class),
                mock(fr.ailegalcase.analysis.PieceManquanteAlignmentService.class),
                mock(fr.ailegalcase.analysis.RisqueAlignmentService.class),
                mock(fr.ailegalcase.analysis.AiQuestionAlignmentService.class),
                mock(DashboardTileCrashRecorder.class));
    }

    @Test
    void noAnalysis_returnsEmptyTiles() {
        UUID caseFileId = UUID.randomUUID();
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.empty());

        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        assertThat(tiles).isEmpty();
    }

    @Test
    void analysisWithoutAlignmentJson_returnsNoTile() {
        UUID caseFileId = UUID.randomUUID();
        CaseAnalysis a = new CaseAnalysis();
        a.setRetainedPistesAlignmentJson(null);
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(retainedPisteAlignmentService.deserializeAlignment(any())).thenReturn(List.of());

        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        assertThat(tiles).isEmpty();
    }

    @Test
    void alignmentWith3RetainedAnd1Divergent_tileAlertLevelALERT() {
        UUID caseFileId = UUID.randomUUID();
        CaseAnalysis a = new CaseAnalysis();
        a.setRetainedPistesAlignmentJson("[{}]"); // contenu peu importe (mock)
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));

        List<RetainedPisteAlignment> alignments = List.of(
                new RetainedPisteAlignment(UUID.randomUUID(), "P1", "L.421-14", null,
                        List.of(), RetainedPisteToolMatcher.TOOL_TITRE,
                        RetainedPisteAlignment.STATUS_ALIGNED),
                new RetainedPisteAlignment(UUID.randomUUID(), "P2", "L.421-14", null,
                        List.of(), RetainedPisteToolMatcher.TOOL_TITRE,
                        RetainedPisteAlignment.STATUS_DIVERGENT),
                new RetainedPisteAlignment(UUID.randomUUID(), "P3", "L.421-14", null,
                        List.of(), RetainedPisteToolMatcher.TOOL_TITRE,
                        RetainedPisteAlignment.STATUS_ALIGNED));
        when(retainedPisteAlignmentService.deserializeAlignment(any())).thenReturn(alignments);

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-192-retained-pistes-summary".equals(t.toolId()))
                .findFirst()
                .orElseThrow();

        assertThat(tile.theme()).isEqualTo("DIAGNOSTIC");
        assertThat(tile.label()).isEqualTo("Pistes stratégiques retenues");
        assertThat(tile.primaryValue()).isEqualTo("3 retenues");
        assertThat(tile.secondaryValue()).isEqualTo("1 en divergence");
        assertThat(tile.alertLevel()).isEqualTo("ALERT");
    }

    @Test
    void alignmentWithNotAnalyzedOnly_tileAlertLevelWARNING() {
        UUID caseFileId = UUID.randomUUID();
        CaseAnalysis a = new CaseAnalysis();
        a.setRetainedPistesAlignmentJson("[{}]");
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));

        List<RetainedPisteAlignment> alignments = List.of(
                new RetainedPisteAlignment(UUID.randomUUID(), "P1", "L.421-14", null,
                        List.of(), RetainedPisteToolMatcher.TOOL_TITRE,
                        RetainedPisteAlignment.STATUS_NOT_ANALYZED));
        when(retainedPisteAlignmentService.deserializeAlignment(any())).thenReturn(alignments);

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-192-retained-pistes-summary".equals(t.toolId()))
                .findFirst().orElseThrow();

        assertThat(tile.alertLevel()).isEqualTo("WARNING");
        assertThat(tile.secondaryValue()).isEqualTo("0 en divergence");
    }

    @Test
    void alignmentAllAligned_tileAlertLevelOK() {
        UUID caseFileId = UUID.randomUUID();
        CaseAnalysis a = new CaseAnalysis();
        a.setRetainedPistesAlignmentJson("[{}]");
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));

        List<RetainedPisteAlignment> alignments = List.of(
                new RetainedPisteAlignment(UUID.randomUUID(), "P1", "L.421-14", null,
                        List.of(), RetainedPisteToolMatcher.TOOL_TITRE,
                        RetainedPisteAlignment.STATUS_ALIGNED));
        when(retainedPisteAlignmentService.deserializeAlignment(any())).thenReturn(alignments);

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-192-retained-pistes-summary".equals(t.toolId()))
                .findFirst().orElseThrow();

        assertThat(tile.alertLevel()).isEqualTo("OK");
        assertThat(tile.primaryValue()).isEqualTo("1 retenue");
    }
}
