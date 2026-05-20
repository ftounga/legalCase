package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.ProcedureCheckAlignment;
import fr.ailegalcase.analysis.ProcedureCheckAlignmentService;
import fr.ailegalcase.analysis.ProcedureCheckToolMatcher;
import fr.ailegalcase.analysis.RetainedPisteAlignmentService;
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
 * F-193 SF-193-01 — Tests dédiés à la tile {@code F-193-procedure-checks-summary}
 * de {@link CaseFileDashboardService}.
 *
 * <p>Pattern miroir {@link CaseFileDashboardServiceRetainedPistesTest} (F-192).</p>
 */
class CaseFileDashboardServiceProcedureChecksTest {

    private CaseAnalysisRepository analysisRepository;
    private ProcedureCheckAlignmentService procedureCheckAlignmentService;
    private CaseFileDashboardService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        analysisRepository = mock(CaseAnalysisRepository.class);
        procedureCheckAlignmentService = mock(ProcedureCheckAlignmentService.class);

        // Mock F-192 service : aucun alignement (pas de tile concurrente)
        RetainedPisteAlignmentService retainedPisteAlignmentService =
                mock(RetainedPisteAlignmentService.class);
        when(retainedPisteAlignmentService.deserializeAlignment(any())).thenReturn(List.of());

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
                procedureCheckAlignmentService,
                pieceManquanteAlignmentServiceForTest(),
                risqueAlignmentServiceForTest(),
                aiQuestionAlignmentServiceForTest(),
                mock(DashboardTileCrashRecorder.class));
    }

    /** F-194 SF-194-01 — mock du service avec deserializeAlignment vide pour ne pas perturber les tests F-193. */
    private fr.ailegalcase.analysis.PieceManquanteAlignmentService pieceManquanteAlignmentServiceForTest() {
        fr.ailegalcase.analysis.PieceManquanteAlignmentService m =
                mock(fr.ailegalcase.analysis.PieceManquanteAlignmentService.class);
        when(m.deserializeAlignment(any())).thenReturn(List.of());
        return m;
    }

    /** F-195 SF-195-01 — mock du service avec deserializeAlignment vide pour ne pas perturber les tests F-193. */
    private fr.ailegalcase.analysis.RisqueAlignmentService risqueAlignmentServiceForTest() {
        fr.ailegalcase.analysis.RisqueAlignmentService m =
                mock(fr.ailegalcase.analysis.RisqueAlignmentService.class);
        when(m.deserializeAlignment(any())).thenReturn(List.of());
        return m;
    }

    /** F-196 SF-196-01 — mock du service avec deserializeAlignment vide. */
    private fr.ailegalcase.analysis.AiQuestionAlignmentService aiQuestionAlignmentServiceForTest() {
        fr.ailegalcase.analysis.AiQuestionAlignmentService m =
                mock(fr.ailegalcase.analysis.AiQuestionAlignmentService.class);
        when(m.deserializeAlignment(any())).thenReturn(List.of());
        return m;
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
        a.setProcedureChecksAlignmentJson(null);
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(procedureCheckAlignmentService.deserializeAlignment(any())).thenReturn(List.of());

        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        assertThat(tiles).isEmpty();
    }

    @Test
    void alignmentWith3ChecksAnd1NonCompliant_tileAlertLevelALERT() {
        UUID caseFileId = UUID.randomUUID();
        CaseAnalysis a = new CaseAnalysis();
        a.setProcedureChecksAlignmentJson("[{}]");
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));

        List<ProcedureCheckAlignment> alignments = List.of(
                new ProcedureCheckAlignment(UUID.randomUUID(), "C1", "FR_CONVOCATION", "VERIFIED",
                        null, null, ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT,
                        ProcedureCheckAlignment.STATUS_ALIGNED),
                new ProcedureCheckAlignment(UUID.randomUUID(), "C2", "FR_DELAI_NOTIFICATION", "NON_COMPLIANT",
                        null, "Notification absente", ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT,
                        ProcedureCheckAlignment.STATUS_NON_COMPLIANT_FLAG),
                new ProcedureCheckAlignment(UUID.randomUUID(), "C3", "IM05_MOTIF", "VERIFIED",
                        "TRAVAIL", null, ProcedureCheckToolMatcher.TOOL_IM_05_TITRE,
                        ProcedureCheckAlignment.STATUS_ALIGNED));
        when(procedureCheckAlignmentService.deserializeAlignment(any())).thenReturn(alignments);

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-193-procedure-checks-summary".equals(t.toolId()))
                .findFirst()
                .orElseThrow();

        assertThat(tile.theme()).isEqualTo("DELAIS");
        assertThat(tile.label()).isEqualTo("Conformité procédurale");
        assertThat(tile.primaryValue()).isEqualTo("3 points");
        assertThat(tile.secondaryValue()).isEqualTo("1 non conforme · 0 à vérifier");
        assertThat(tile.alertLevel()).isEqualTo("ALERT");
    }

    @Test
    void alignmentWithToCheckOnly_tileAlertLevelWARNING() {
        UUID caseFileId = UUID.randomUUID();
        CaseAnalysis a = new CaseAnalysis();
        a.setProcedureChecksAlignmentJson("[{}]");
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));

        List<ProcedureCheckAlignment> alignments = List.of(
                new ProcedureCheckAlignment(UUID.randomUUID(), "C1", "FR_CONVOCATION", "TO_CHECK",
                        null, null, ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT,
                        ProcedureCheckAlignment.STATUS_TO_VERIFY_FLAG));
        when(procedureCheckAlignmentService.deserializeAlignment(any())).thenReturn(alignments);

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-193-procedure-checks-summary".equals(t.toolId()))
                .findFirst().orElseThrow();

        assertThat(tile.alertLevel()).isEqualTo("WARNING");
        assertThat(tile.secondaryValue()).isEqualTo("0 non conforme · 1 à vérifier");
    }

    @Test
    void alignmentAllAligned_tileAlertLevelOK() {
        UUID caseFileId = UUID.randomUUID();
        CaseAnalysis a = new CaseAnalysis();
        a.setProcedureChecksAlignmentJson("[{}]");
        when(analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));

        List<ProcedureCheckAlignment> alignments = List.of(
                new ProcedureCheckAlignment(UUID.randomUUID(), "C1", "FR_CONVOCATION", "VERIFIED",
                        null, null, ProcedureCheckToolMatcher.TOOL_DT_08_LICENCIEMENT,
                        ProcedureCheckAlignment.STATUS_ALIGNED));
        when(procedureCheckAlignmentService.deserializeAlignment(any())).thenReturn(alignments);

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-193-procedure-checks-summary".equals(t.toolId()))
                .findFirst().orElseThrow();

        assertThat(tile.alertLevel()).isEqualTo("OK");
        assertThat(tile.primaryValue()).isEqualTo("1 point");
    }
}
