package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-167 SF-167-01 / SF-167-02 — Tests unitaires de la méthode
 * {@code assembleTiles} de {@link CaseFileDashboardService}.
 *
 * <ul>
 *   <li>SF-167-01 : 10 outils pilotes (changement de statut, ancienneté…).</li>
 *   <li>SF-167-02 : extension à ~25 outils Travail FR + BE — couvert par
 *       le test paramétré {@link #assembleTiles_extendsAllTravailMappers}.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CaseFileDashboardServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    // SF-167-02
    private RuptureConvAnalysisRepository ruptureConvAnalysisRepo;
    private HarcelementNulliteRepository harcelementNulliteRepo;
    private DiscriminationRepository discriminationRepo;
    private LicenciementEconomiqueRepository licenciementEconomiqueRepo;
    private PseRepository pseRepo;
    private InaptitudeRepository inaptitudeRepo;
    private LicenciementNulDetectionRepository licenciementNulDetectionRepo;
    private IndemnitePrecariteCddRepository indemnitePrecariteCddRepo;
    private IndemniteFinMissionInterimRepository indemniteFinMissionInterimRepo;
    private HeuresSupRepository heuresSupRepo;
    private RappelSalaireRepository rappelSalaireRepo;
    private TravailDissimuleRepository travailDissimuleRepo;
    private RequalificationCddCdiRepository requalificationCddCdiRepo;
    private RequalificationInterimCdiRepository requalificationInterimCdiRepo;
    private NonConcurrenceRepository nonConcurrenceRepo;
    private IndemnitePreavisRepository indemnitePreavisRepo;
    private IndemniteCongesPayesRepository indemniteCongesPayesRepo;
    private ProtectionRpRepository protectionRpRepo;
    private TransactionRepository transactionRepo;
    private DocumentsFinContratRepository documentsFinContratRepo;
    private AtMpRepository atMpRepo;
    private ReferePrudhomalRepository referePrudhomalRepo;
    private ContestationAreRepository contestationAreRepo;
    private MotifGraveBeRepository motifGraveBeRepo;
    private AvantagesConventionnelsBeRepository avantagesConventionnelsBeRepo;
    private CreditTempsBeRepository creditTempsBeRepo;

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
        ruptureConvAnalysisRepo = mock(RuptureConvAnalysisRepository.class);
        harcelementNulliteRepo = mock(HarcelementNulliteRepository.class);
        discriminationRepo = mock(DiscriminationRepository.class);
        licenciementEconomiqueRepo = mock(LicenciementEconomiqueRepository.class);
        pseRepo = mock(PseRepository.class);
        inaptitudeRepo = mock(InaptitudeRepository.class);
        licenciementNulDetectionRepo = mock(LicenciementNulDetectionRepository.class);
        indemnitePrecariteCddRepo = mock(IndemnitePrecariteCddRepository.class);
        indemniteFinMissionInterimRepo = mock(IndemniteFinMissionInterimRepository.class);
        heuresSupRepo = mock(HeuresSupRepository.class);
        rappelSalaireRepo = mock(RappelSalaireRepository.class);
        travailDissimuleRepo = mock(TravailDissimuleRepository.class);
        requalificationCddCdiRepo = mock(RequalificationCddCdiRepository.class);
        requalificationInterimCdiRepo = mock(RequalificationInterimCdiRepository.class);
        nonConcurrenceRepo = mock(NonConcurrenceRepository.class);
        indemnitePreavisRepo = mock(IndemnitePreavisRepository.class);
        indemniteCongesPayesRepo = mock(IndemniteCongesPayesRepository.class);
        protectionRpRepo = mock(ProtectionRpRepository.class);
        transactionRepo = mock(TransactionRepository.class);
        documentsFinContratRepo = mock(DocumentsFinContratRepository.class);
        atMpRepo = mock(AtMpRepository.class);
        referePrudhomalRepo = mock(ReferePrudhomalRepository.class);
        contestationAreRepo = mock(ContestationAreRepository.class);
        motifGraveBeRepo = mock(MotifGraveBeRepository.class);
        avantagesConventionnelsBeRepo = mock(AvantagesConventionnelsBeRepository.class);
        creditTempsBeRepo = mock(CreditTempsBeRepository.class);

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
        when(ruptureConvAnalysisRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(harcelementNulliteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(discriminationRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(licenciementEconomiqueRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(pseRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(inaptitudeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(licenciementNulDetectionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(indemnitePrecariteCddRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(indemniteFinMissionInterimRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(heuresSupRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(rappelSalaireRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(travailDissimuleRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(requalificationCddCdiRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(requalificationInterimCdiRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(nonConcurrenceRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(indemnitePreavisRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(indemniteCongesPayesRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(protectionRpRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(transactionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(documentsFinContratRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(atMpRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(referePrudhomalRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(contestationAreRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(motifGraveBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(avantagesConventionnelsBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(creditTempsBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());

        service = new CaseFileDashboardService(
                objectMapper,
                /* caseFileRepository */ null,
                /* workspaceMemberRepository */ null,
                /* currentUserResolver */ null,
                /* analysisRepository */ null,
                licenciementRepo, indemniteRepo, ruptureConvIndemniteRepo, ancienneteRepo,
                titleDecisionRepo, workRightRepo, recoursRepo,
                partageRepo, gardeRepo, divorceRepo, changementStatutRepo,
                ruptureConvAnalysisRepo, harcelementNulliteRepo, discriminationRepo,
                licenciementEconomiqueRepo, pseRepo, inaptitudeRepo,
                licenciementNulDetectionRepo, indemnitePrecariteCddRepo,
                indemniteFinMissionInterimRepo, heuresSupRepo, rappelSalaireRepo,
                travailDissimuleRepo, requalificationCddCdiRepo,
                requalificationInterimCdiRepo, nonConcurrenceRepo,
                indemnitePreavisRepo, indemniteCongesPayesRepo, protectionRpRepo,
                transactionRepo, documentsFinContratRepo, atMpRepo,
                referePrudhomalRepo, contestationAreRepo, motifGraveBeRepo,
                avantagesConventionnelsBeRepo, creditTempsBeRepo);
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

    // ────────────────────────────────────────────────────────────────────
    // SF-167-02 — Tests d'extension Travail FR + BE
    // ────────────────────────────────────────────────────────────────────

    /**
     * Test paramétré : pour chaque outil de SF-167-02, on configure son repo
     * pour retourner une analyse minimale et on vérifie que la tile produite
     * a bien {@code toolId}, {@code theme}, {@code label}, {@code primaryValue}
     * non-null.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("travailMappersData")
    void assembleTiles_extendsAllTravailMappers(String expectedToolId,
                                                String expectedTheme,
                                                Runnable seedRepo) throws Exception {
        seedRepo.run();
        UUID caseFileId = UUID.randomUUID(); // les seedRepo utilisent any() — non lié à cet UUID
        // pour s'assurer que le seedRepo s'applique à n'importe quel ID, on a stubbé sur any()
        // dans le helper. On le redéclare ici pour la clarté.
        List<DashboardTile> tiles = service.assembleTiles(caseFileId);

        DashboardTile tile = tiles.stream()
                .filter(t -> expectedToolId.equals(t.toolId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Tile " + expectedToolId + " absente parmi : "
                                + tiles.stream().map(DashboardTile::toolId).toList()));
        assertThat(tile.theme()).isEqualTo(expectedTheme);
        assertThat(tile.label()).isNotNull();
        assertThat(tile.primaryValue()).isNotNull();
    }

    /** Sources des 26 mappers SF-167-02 (Travail FR+BE + F-132). */
    private Stream<Arguments> travailMappersData() throws Exception {
        return Stream.of(
                Arguments.of("F-DT-10-rupture-conv-validity", "VALIDITE",
                        (Runnable) this::seedRuptureConv),
                Arguments.of("F-DT-11-harcelement-licenciement-nul", "VALIDITE",
                        (Runnable) this::seedHarcelement),
                Arguments.of("F-DT-12-discrimination-dommages-interets", "INDEMNITES",
                        (Runnable) this::seedDiscrimination),
                Arguments.of("F-DT-13-licenciement-economique", "VALIDITE",
                        (Runnable) this::seedLicenciementEco),
                Arguments.of("F-DT-14-pse-validite", "VALIDITE",
                        (Runnable) this::seedPse),
                Arguments.of("F-DT-15-inaptitude", "INDEMNITES",
                        (Runnable) this::seedInaptitude),
                Arguments.of("F-DT-16-licenciement-nul-detection", "VALIDITE",
                        (Runnable) this::seedLicenciementNulDetection),
                Arguments.of("F-DT-17-indemnite-precarite-cdd", "INDEMNITES",
                        (Runnable) this::seedPrecariteCdd),
                Arguments.of("F-DT-18-fin-mission-interim", "INDEMNITES",
                        (Runnable) this::seedFinMissionInterim),
                Arguments.of("F-DT-19-heures-sup", "INDEMNITES",
                        (Runnable) this::seedHeuresSup),
                Arguments.of("F-DT-20-rappel-salaire", "INDEMNITES",
                        (Runnable) this::seedRappelSalaire),
                Arguments.of("F-DT-21-travail-dissimule", "INDEMNITES",
                        (Runnable) this::seedTravailDissimule),
                Arguments.of("F-DT-22-requalification-cdd-cdi", "VALIDITE",
                        (Runnable) this::seedRequalifCdd),
                Arguments.of("F-DT-23-requalification-interim-cdi", "VALIDITE",
                        (Runnable) this::seedRequalifInterim),
                Arguments.of("F-DT-24-non-concurrence", "VALIDITE",
                        (Runnable) this::seedNonConcurrence),
                Arguments.of("F-DT-25-indemnite-preavis", "INDEMNITES",
                        (Runnable) this::seedPreavis),
                Arguments.of("F-DT-26-conges-payes-indemnite", "INDEMNITES",
                        (Runnable) this::seedCongesPayes),
                Arguments.of("F-DT-30-protection-rp", "VALIDITE",
                        (Runnable) this::seedProtectionRp),
                Arguments.of("F-DT-31-transaction", "INDEMNITES",
                        (Runnable) this::seedTransaction),
                Arguments.of("F-DT-32-documents-fin-contrat", "DOCUMENTS",
                        (Runnable) this::seedDocsFinContrat),
                Arguments.of("F-DT-33-at-mp", "DELAIS",
                        (Runnable) this::seedAtMp),
                Arguments.of("F-DT-34-refere-prudhomal", "DELAIS",
                        (Runnable) this::seedReferePrudhomal),
                Arguments.of("F-DT-35-contestation-are-fr", "INDEMNITES",
                        (Runnable) this::seedContestationAre),
                Arguments.of("F-132-rupture-conv-indemnite", "INDEMNITES",
                        (Runnable) this::seedRuptureConvIndemnite),
                Arguments.of("F-DT-27-motif-grave-be", "VALIDITE",
                        (Runnable) this::seedMotifGraveBe),
                Arguments.of("F-DT-28-avantages-conventionnels-be", "INDEMNITES",
                        (Runnable) this::seedAvantagesBe),
                Arguments.of("F-DT-29-credit-temps-be", "DELAIS",
                        (Runnable) this::seedCreditTempsBe)
        );
    }

    @Test
    void assembleTiles_inaptitude_returnsCompleteTile() throws Exception {
        UUID caseFileId = UUID.randomUUID();
        seedInaptitude();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-DT-15-inaptitude".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("INDEMNITES");
        assertThat(tile.label()).isEqualTo("Inaptitude");
        assertThat(tile.primaryValue()).startsWith("Total : ");
        assertThat(tile.alertLevel()).isIn("OK", "WARNING");
    }

    @Test
    void assembleTiles_heuresSup_returnsCompleteTile() throws Exception {
        UUID caseFileId = UUID.randomUUID();
        seedHeuresSup();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-DT-19-heures-sup".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("INDEMNITES");
        assertThat(tile.label()).isEqualTo("Heures sup.");
        assertThat(tile.primaryValue()).contains("€");
        assertThat(tile.secondaryValue()).contains("h déclarées");
    }

    // ---- Helpers de seed pour les mappers SF-167-02 ----------------------

    private void seedRuptureConv() {
        try {
            RuptureConvAnalysis e = new RuptureConvAnalysis();
            RuptureConvAnalysisResult r = new RuptureConvAnalysisResult(
                    "FRANCE", 0, "VALIDE", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(ruptureConvAnalysisRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedHarcelement() {
        try {
            HarcelementNulliteAnalysis e = new HarcelementNulliteAnalysis();
            HarcelementNulliteResult r = new HarcelementNulliteResult(
                    new BigDecimal("2500"),
                    HarcelementNulliteCalculator.MotifNullite.HARCELEMENT_MORAL,
                    "FRANCE", new BigDecimal("15000"),
                    "Salaire × 6 mois", "L.1152-1 CT", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(harcelementNulliteRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedDiscrimination() {
        try {
            DiscriminationAnalysis e = new DiscriminationAnalysis();
            DiscriminationResult r = new DiscriminationResult(
                    new BigDecimal("2500"), "Origine", "Refus de promotion", "FRANCE",
                    new BigDecimal("3000"), new BigDecimal("8000"), new BigDecimal("15000"),
                    "Fourchette indicative", "L.1134-5 CT", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(discriminationRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedLicenciementEco() {
        try {
            LicenciementEconomiqueAnalysis e = new LicenciementEconomiqueAnalysis();
            LicenciementEconomiqueResult r = new LicenciementEconomiqueResult(
                    LicenciementEconomiqueCalculator.MotifEconomique.DIFFICULTES_ECONOMIQUES,
                    List.of(), List.of(),
                    45, 60, 2,
                    LicenciementEconomiqueCalculator.QualitesProf.MOYEN,
                    List.of(), false, false,
                    LocalDate.of(2026, 1, 15), "FRANCE",
                    50, 30, 20, 50,
                    LicenciementEconomiqueCalculator.VerdictRisque.MOYENNE,
                    List.of(), true, true,
                    "L.1233-3 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(licenciementEconomiqueRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedPse() {
        try {
            PseAnalysis e = new PseAnalysis();
            PseResult r = new PseResult(
                    250, 30, 30,
                    PseCalculator.ModeAdoption.ACCORD_COLLECTIF_MAJORITAIRE,
                    PseCalculator.AvisCse.FAVORABLE,
                    PseCalculator.StatutDreets.HOMOLOGUE,
                    LocalDate.of(2026, 2, 1),
                    List.of(), LocalDate.of(2026, 1, 1), "FRANCE",
                    true, 80,
                    PseCalculator.VerdictValidite.VALIDE,
                    List.of(), List.of(),
                    60, "L.1233-24-1 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(pseRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedInaptitude() {
        try {
            InaptitudeAnalysis e = new InaptitudeAnalysis();
            InaptitudeResult r = new InaptitudeResult(
                    new BigDecimal("2500"), 5,
                    InaptitudeCalculator.OrigineInaptitude.NON_PROFESSIONNELLE,
                    true, LocalDate.of(2026, 1, 10), "FRANCE",
                    new BigDecimal("3125"), new BigDecimal("5000"),
                    BigDecimal.ZERO, new BigDecimal("8125"),
                    "Calcul indem. inaptitude", "L.1226-2 CT", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(inaptitudeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedLicenciementNulDetection() {
        try {
            LicenciementNulDetectionAnalysis e = new LicenciementNulDetectionAnalysis();
            LicenciementNulDetectionResult r = new LicenciementNulDetectionResult(
                    List.of(), 0, false, 30,
                    LicenciementNulDetectionCalculator.VerdictProbabilite.FAIBLE,
                    new BigDecimal("15000"), 6, true,
                    "L.1132-4 CT", "—", List.of(), "FRANCE");
            e.setSnapshotData(objectMapper.writeValueAsString(r));
            when(licenciementNulDetectionRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedPrecariteCdd() {
        try {
            IndemnitePrecariteCddAnalysis e = new IndemnitePrecariteCddAnalysis();
            IndemnitePrecariteCddResult r = new IndemnitePrecariteCddResult(
                    new BigDecimal("18000"), 10, "",
                    new BigDecimal("1800"), "10% × 18000", "L.1243-8 CT", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(indemnitePrecariteCddRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedFinMissionInterim() {
        try {
            IndemniteFinMissionInterimAnalysis e = new IndemniteFinMissionInterimAnalysis();
            IndemniteFinMissionInterimResult r = new IndemniteFinMissionInterimResult(
                    new BigDecimal("15000"), 90, "",
                    LocalDate.of(2026, 2, 15),
                    new BigDecimal("10"), new BigDecimal("1500"),
                    false, "L.1251-32 CT", "10% × 15000", List.of(), "FRANCE");
            e.setResultData(objectMapper.writeValueAsString(r));
            when(indemniteFinMissionInterimRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedHeuresSup() {
        try {
            HeuresSupAnalysis e = new HeuresSupAnalysis();
            HeuresSupResult r = new HeuresSupResult(
                    new BigDecimal("12.50"),
                    20, 10, 5,
                    new BigDecimal("25"), new BigDecimal("50"),
                    0, 0, "FRANCE",
                    new BigDecimal("62.50"), new BigDecimal("62.50"), BigDecimal.ZERO,
                    new BigDecimal("125.00"),
                    BigDecimal.ZERO,
                    "—", "L.3121-28 CT", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(heuresSupRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedRappelSalaire() {
        try {
            RappelSalaireAnalysis e = new RappelSalaireAnalysis();
            RappelSalaireResult r = new RappelSalaireResult(
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                    new BigDecimal("2500"), new BigDecimal("2300"),
                    "CC0123", 5, false, BigDecimal.ZERO,
                    RappelSalaireMethodeCpSurRappel.DIX_POURCENT,
                    12, new BigDecimal("200"),
                    new BigDecimal("2400"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("2400"), new BigDecimal("240"),
                    new BigDecimal("2640"), "L.3221-1 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(rappelSalaireRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedTravailDissimule() {
        try {
            TravailDissimuleAnalysis e = new TravailDissimuleAnalysis();
            TravailDissimuleResult r = new TravailDissimuleResult(
                    new BigDecimal("2500"),
                    new BigDecimal("15000"),
                    "Salaire × 6 mois", "L.8223-1 CT", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(travailDissimuleRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedRequalifCdd() {
        try {
            RequalificationCddCdiAnalysis e = new RequalificationCddCdiAnalysis();
            RequalificationCddCdiResult r = new RequalificationCddCdiResult(
                    "Surcroît", false, null, List.of(), true,
                    18, new BigDecimal("2500"),
                    LocalDate.of(2026, 2, 1),
                    50, "MOYENNE",
                    new BigDecimal("2500"), new BigDecimal("4500"),
                    new BigDecimal("7000"), "L.1245-2 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(requalificationCddCdiRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedRequalifInterim() {
        try {
            RequalificationInterimCdiAnalysis e = new RequalificationInterimCdiAnalysis();
            RequalificationInterimCdiResult r = new RequalificationInterimCdiResult(
                    "Remplacement", false, null, List.of(), true,
                    20, new BigDecimal("2400"),
                    LocalDate.of(2026, 2, 1), true,
                    55, "MOYENNE",
                    new BigDecimal("2400"), new BigDecimal("4800"),
                    new BigDecimal("7200"), "L.1251-41 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(requalificationInterimCdiRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedNonConcurrence() {
        try {
            NonConcurrenceAnalysis e = new NonConcurrenceAnalysis();
            NonConcurrenceResult r = new NonConcurrenceResult(
                    true, true, true, true,
                    new BigDecimal("33"),
                    100,
                    NonConcurrenceCalculator.VerdictValidite.VALIDE,
                    new BigDecimal("9000"), BigDecimal.ZERO,
                    "Cass. soc. 10/07/2002", "—", List.of(), "FRANCE");
            e.setSnapshotData(objectMapper.writeValueAsString(r));
            when(nonConcurrenceRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedPreavis() {
        try {
            IndemnitePreavisAnalysis e = new IndemnitePreavisAnalysis();
            IndemnitePreavisResult r = new IndemnitePreavisResult(
                    36, IndemnitePreavisFonction.EMPLOYE,
                    "CC0123", new BigDecimal("2500"),
                    false, false,
                    LocalDate.of(2026, 2, 1), 2,
                    IndemnitePreavisSourceDuree.LEGALE,
                    new BigDecimal("5000"),
                    "L.1234-1 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(indemnitePreavisRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedCongesPayes() {
        try {
            IndemniteCongesPayesAnalysis e = new IndemniteCongesPayesAnalysis();
            IndemniteCongesPayesResult r = new IndemniteCongesPayesResult(
                    new BigDecimal("30000"), 25, 10,
                    new BigDecimal("2500"),
                    LocalDate.of(2026, 2, 1),
                    null, 15,
                    new BigDecimal("3000"), new BigDecimal("3125"),
                    IndemniteCongesPayesMethode.MAINTIEN,
                    new BigDecimal("3125"),
                    "L.3141-26 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(indemniteCongesPayesRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedProtectionRp() {
        try {
            ProtectionRpAnalysis e = new ProtectionRpAnalysis();
            ProtectionRpResult r = new ProtectionRpResult(
                    ProtectionRpCalculator.StatutProtege.MEMBRE_CSE_TITULAIRE,
                    LocalDate.of(2027, 1, 1),
                    LocalDate.of(2026, 2, 1),
                    ProtectionRpCalculator.ProcedureSuivie.AUTORISATION_OBTENUE,
                    ProtectionRpCalculator.MotifLicenciement.FAUTE_GRAVE,
                    2500.0, "FRANCE",
                    true, 90,
                    ProtectionRpCalculator.VerdictLegalite.VALIDE,
                    List.of(), List.of(),
                    50000.0, 30000.0,
                    60, "L.2411-1 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(protectionRpRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedTransaction() {
        try {
            TransactionAnalysis e = new TransactionAnalysis();
            TransactionResult r = new TransactionResult(
                    true, new BigDecimal("60"), true,
                    85,
                    TransactionCalculator.VerdictValidite.VALIDE,
                    false,
                    "Art. 2044 Cciv", "—", List.of(), "FRANCE");
            e.setSnapshotData(objectMapper.writeValueAsString(r));
            when(transactionRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedDocsFinContrat() {
        try {
            DocumentsFinContratAnalysis e = new DocumentsFinContratAnalysis();
            DocumentsFinContratResult r = new DocumentsFinContratResult(
                    true, true, true, false,
                    0,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    100,
                    DocumentsFinContratCalculator.VerdictRisqueContentieux.FAIBLE,
                    "L.1234-19 CT", "—", List.of(), List.of(), "FRANCE");
            e.setSnapshotData(objectMapper.writeValueAsString(r));
            when(documentsFinContratRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedAtMp() {
        try {
            AtMpAnalysis e = new AtMpAnalysis();
            AtMpResult r = new AtMpResult(
                    "RECONNAISSANCE_AT", "Reconnaissance AT (CSS L.411-1)",
                    "ELEVEE", 90, "CPAM", false,
                    List.of("CMI"), List.of(),
                    "CSS L.411-1", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(atMpRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedReferePrudhomal() {
        try {
            ReferePrudhomalAnalysis e = new ReferePrudhomalAnalysis();
            ReferePrudhomalResult r = new ReferePrudhomalResult(
                    "PROVISION", "SALAIRE",
                    new BigDecimal("3000"), true, List.of(), true, false,
                    LocalDate.of(2026, 2, 1), 12,
                    75, "ELEVEE", 30, 7, new BigDecimal("3000"),
                    "R.1454-1 CT", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(referePrudhomalRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedContestationAre() {
        try {
            ContestationAreAnalysis e = new ContestationAreAnalysis();
            ContestationAreResult r = new ContestationAreResult(
                    "REJET_OUVERTURE_DROITS", "ARTICLE_2", LocalDate.of(2026, 1, 15),
                    LocalDate.of(2026, 2, 1), List.of(),
                    new BigDecimal("12000"), false,
                    true, true, true,
                    65, "ELEVEE", 6, false,
                    "Règlement 14/04/2017", "—", List.of());
            e.setSnapshotData(objectMapper.writeValueAsString(r));
            when(contestationAreRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedRuptureConvIndemnite() {
        try {
            RuptureConvIndemniteAnalysis e = new RuptureConvIndemniteAnalysis();
            RuptureConvIndemniteResult r = new RuptureConvIndemniteResult(
                    5, new BigDecimal("2500"),
                    new BigDecimal("3125"),
                    "1/4 × 5 × 2500", "R.1234-2 CT", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(ruptureConvIndemniteRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedMotifGraveBe() {
        try {
            MotifGraveBeAnalysis e = new MotifGraveBeAnalysis();
            MotifGraveBeResult r = new MotifGraveBeResult(
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 3),
                    LocalDate.of(2026, 2, 5),
                    8, new BigDecimal("3000"),
                    2, 2, true,
                    new BigDecimal("9000"), new BigDecimal("4500"), new BigDecimal("18000"),
                    "—", "art. 35 Loi 03/07/1978", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(motifGraveBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedAvantagesBe() {
        try {
            AvantagesConventionnelsBeAnalysis e = new AvantagesConventionnelsBeAnalysis();
            AvantagesConventionnelsBeResult r = new AvantagesConventionnelsBeResult(
                    new BigDecimal("3000"), 230, 24, "200", 2026,
                    false, true, false, false, false,
                    230,
                    new BigDecimal("250"), new BigDecimal("250"),
                    new BigDecimal("3000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("3500"),
                    "—", "Loi 28/06/1971", List.of(), "BELGIUM");
            e.setResultData(objectMapper.writeValueAsString(r));
            when(avantagesConventionnelsBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedCreditTempsBe() {
        try {
            CreditTempsBeAnalysis e = new CreditTempsBeAnalysis();
            CreditTempsBeResult r = new CreditTempsBeResult(
                    "CREDIT_TEMPS_FIN_CARRIERE", true, 90, "ELEVEE",
                    List.of(), new BigDecimal("550"), 60,
                    "CCT 103", "—", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(creditTempsBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
}
