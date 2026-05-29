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
    // SF-DT-36-03 — 16 outils orphelins du dashboard (correctif câblage)
    private ProcedureNulliteLicenciementRepository procedureNulliteLicenciementRepo;
    // SF-DT-38-02 — qualification rupture période d'essai (FR, F-DT-38).
    private RupturePeriodeEssaiRepository rupturePeriodeEssaiRepo;
    // SF-206-01 : F-DT-42 abandon de poste / présomption de démission (FR)
    private AbandonPostePresomptionDemissionRepository abandonPostePresomptionDemissionRepo;
    // SF-206-03 : F-DT-75 congés payés acquis pendant arrêt maladie (FR)
    private CongesPayesArretMaladieRepository congesPayesArretMaladieRepo;
    // SF-206-05 : F-DT-39 prise d'acte de la rupture (FR)
    private PriseActeRuptureRepository priseActeRuptureRepo;
    // SF-206-07 : F-DT-40 résiliation judiciaire CPH (FR)
    private ResiliationJudiciaireCphRepository resiliationJudiciaireCphRepo;
    // SF-214-01 : F-IM-25 étranger malade L.425-9 CESEDA (FR)
    private EtrangerMaladeRepository etrangerMaladeRepo;
    // SF-214-03 : F-IM-26 regroupement familial L.434-1+ CESEDA (FR)
    private RegroupementFamilialRepository regroupementFamilialRepo;
    // SF-214-05 : F-IM-27 VPF liens personnels L.423-23 CESEDA (FR)
    private VpfLiensPersonnelsRepository vpfLiensPersonnelsRepo;
    // SF-214-07 : F-IM-28 validation VLS-TS OFII 3 mois R. 311-3 CESEDA (FR)
    private VlsTsValidationRepository vlsTsValidationRepo;
    // SF-212-01 : F-DT-36 licenciement faute grave/lourde (FR)
    private LicenciementFauteGraveLourdRepository licenciementFauteGraveLourdRepo;
    private JldRetentionRepository jldRetentionRepo;
    private DublinRecoursRepository dublinRecoursRepo;
    private CrrvRefusVisaRepository crrvRefusVisaRepo;
    private VictimeViolencesL4256Repository victimeViolencesL4256Repo;
    private AcceptationRenonciationSuccessionRepository acceptationRenonciationSuccessionRepo;
    private AutoriteParentaleBeRepository autoriteParentaleBeRepo;
    private ContributionAlimentaireEnfantsBeRepository contributionAlimentaireEnfantsBeRepo;
    private ContributionConjointBeRepository contributionConjointBeRepo;
    private DivorceDcBeRepository divorceDcBeRepo;
    private DivorceDdiBeRepository divorceDdiBeRepo;
    private LiquidationPartageBeRepository liquidationPartageBeRepo;
    private MediationFamilialePreSaisineRepository mediationFamilialePreSaisineRepo;
    private PacteSuccessoralBe2018Repository pacteSuccessoralBe2018Repo;
    private RegimeCommunauteLegaleBeRepository regimeCommunauteLegaleBeRepo;
    private TribunalFamilleBeMesuresProvisoiresRepository tribunalFamilleBeMesuresProvisoiresRepo;
    // SF-167-03 — Famille FR + BE
    private DivorceAlterationRepository divorceAlterationRepo;
    private DivorceFauteRepository divorceFauteRepo;
    private DivorceAccepteRepository divorceAccepteRepo;
    private DivorceDesunionIrremediableBeRepository divorceDesunionIrremediableBeRepo;
    private MesuresProvisoiresRepository mesuresProvisoiresRepo;
    private RevisionsPostDivorceRepository revisionsPostDivorceRepo;
    private OrdonnanceProtectionRepository ordonnanceProtectionRepo;
    private RecompensesRepository recompensesRepo;
    private CommunauteUniverselleRepository communauteUniverselleRepo;
    private PartageJudiciaireRepository partageJudiciaireRepo;
    private AdoptionRepository adoptionRepo;
    private ContestationPaterniteRepository contestationPaterniteRepo;
    private RecherchePaterniteRepository recherchePaterniteRepo;
    private ReconnaissancePaterneleRepository reconnaissancePaterneleRepo;
    private PossessionEtatRepository possessionEtatRepo;
    private AutoriteParentaleRepository autoriteParentaleRepo;
    private ChangementResidenceRepository changementResidenceRepo;
    private DesaccordsParentauxRepository desaccordsParentauxRepo;
    private PacsDissolutionRepository pacsDissolutionRepo;
    private SeparationCorpsRepository separationCorpsRepo;
    private IndivisionRepository indivisionRepo;
    private OrdonnanceRequeteRepository ordonnanceRequeteRepo;
    private DevolutionLegaleRepository devolutionLegaleRepo;
    private DonationRepository donationRepo;
    private IndivisionSuccessoraleRepository indivisionSuccessoraleRepo;
    private PartageSuccessoralRepository partageSuccessoralRepo;
    private RapportSuccessionRepository rapportSuccessionRepo;
    private ReserveHereditaireRepository reserveHereditaireRepo;
    private TestamentValiditeRepository testamentValiditeRepo;
    private MajeursProtegesRepository majeursProtegesRepo;
    private ChangementEtatCivilRepository changementEtatCivilRepo;
    private PmaGpaBioethiqueRepository pmaGpaBioethiqueRepo;
    // SF-167-04 — Immigration FR + BE
    private OqtfAvecDelaiRepository oqtfAvecDelaiRepo;
    private OqtfSansDelaiRepository oqtfSansDelaiRepo;
    private ReferesAdminRepository referesAdminRepo;
    private AesEtudiantRepository aesEtudiantRepo;
    private AesFamilleRepository aesFamilleRepo;
    private AesHumanitaireRepository aesHumanitaireRepo;
    private AesMetiersTensionRepository aesMetiersTensionRepo;
    private AsileAvanceRepository asileAvanceRepo;
    private NaturalisationRepository naturalisationRepo;
    private RegimeAlgerienRepository regimeAlgerienRepo;
    private MineursImmigrationRepository mineursImmigrationRepo;
    private MesuresEloignementRepository mesuresEloignementRepo;
    private Annexe13BeRepository annexe13BeRepo;
    private Belgian9bisRepository belgian9bisRepo;
    private Belgian9terRepository belgian9terRepo;
    private Belgian40bisRepository belgian40bisRepo;
    private Belgian40terRepository belgian40terRepo;
    private PrescriptionBeLitigeTravailRepository prescriptionBeLitigeTravailRepo;
    private C4OnemChecklistRepository c4OnemChecklistRepo;
    private fr.ailegalcase.analysis.RetainedPisteAlignmentService retainedPisteAlignmentService;
    private fr.ailegalcase.analysis.ProcedureCheckAlignmentService procedureCheckAlignmentService;
    private fr.ailegalcase.analysis.PieceManquanteAlignmentService pieceManquanteAlignmentService;
    private fr.ailegalcase.analysis.RisqueAlignmentService risqueAlignmentService;
    private fr.ailegalcase.analysis.AiQuestionAlignmentService aiQuestionAlignmentService;
    private fr.ailegalcase.analysis.CaseAnalysisRepository analysisRepositoryMock;

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
        procedureNulliteLicenciementRepo = mock(ProcedureNulliteLicenciementRepository.class);
        rupturePeriodeEssaiRepo = mock(RupturePeriodeEssaiRepository.class);
        abandonPostePresomptionDemissionRepo = mock(AbandonPostePresomptionDemissionRepository.class);
        congesPayesArretMaladieRepo = mock(CongesPayesArretMaladieRepository.class);
        priseActeRuptureRepo = mock(PriseActeRuptureRepository.class);
        resiliationJudiciaireCphRepo = mock(ResiliationJudiciaireCphRepository.class);
        etrangerMaladeRepo = mock(EtrangerMaladeRepository.class);
        regroupementFamilialRepo = mock(RegroupementFamilialRepository.class);
        vpfLiensPersonnelsRepo = mock(VpfLiensPersonnelsRepository.class);
        vlsTsValidationRepo = mock(VlsTsValidationRepository.class);
        licenciementFauteGraveLourdRepo = mock(LicenciementFauteGraveLourdRepository.class);
        jldRetentionRepo = mock(JldRetentionRepository.class);
        dublinRecoursRepo = mock(DublinRecoursRepository.class);
        crrvRefusVisaRepo = mock(CrrvRefusVisaRepository.class);
        victimeViolencesL4256Repo = mock(VictimeViolencesL4256Repository.class);
        acceptationRenonciationSuccessionRepo = mock(AcceptationRenonciationSuccessionRepository.class);
        autoriteParentaleBeRepo = mock(AutoriteParentaleBeRepository.class);
        contributionAlimentaireEnfantsBeRepo = mock(ContributionAlimentaireEnfantsBeRepository.class);
        contributionConjointBeRepo = mock(ContributionConjointBeRepository.class);
        divorceDcBeRepo = mock(DivorceDcBeRepository.class);
        divorceDdiBeRepo = mock(DivorceDdiBeRepository.class);
        liquidationPartageBeRepo = mock(LiquidationPartageBeRepository.class);
        mediationFamilialePreSaisineRepo = mock(MediationFamilialePreSaisineRepository.class);
        pacteSuccessoralBe2018Repo = mock(PacteSuccessoralBe2018Repository.class);
        regimeCommunauteLegaleBeRepo = mock(RegimeCommunauteLegaleBeRepository.class);
        tribunalFamilleBeMesuresProvisoiresRepo = mock(TribunalFamilleBeMesuresProvisoiresRepository.class);
        divorceAlterationRepo = mock(DivorceAlterationRepository.class);
        divorceFauteRepo = mock(DivorceFauteRepository.class);
        divorceAccepteRepo = mock(DivorceAccepteRepository.class);
        divorceDesunionIrremediableBeRepo = mock(DivorceDesunionIrremediableBeRepository.class);
        mesuresProvisoiresRepo = mock(MesuresProvisoiresRepository.class);
        revisionsPostDivorceRepo = mock(RevisionsPostDivorceRepository.class);
        ordonnanceProtectionRepo = mock(OrdonnanceProtectionRepository.class);
        recompensesRepo = mock(RecompensesRepository.class);
        communauteUniverselleRepo = mock(CommunauteUniverselleRepository.class);
        partageJudiciaireRepo = mock(PartageJudiciaireRepository.class);
        adoptionRepo = mock(AdoptionRepository.class);
        contestationPaterniteRepo = mock(ContestationPaterniteRepository.class);
        recherchePaterniteRepo = mock(RecherchePaterniteRepository.class);
        reconnaissancePaterneleRepo = mock(ReconnaissancePaterneleRepository.class);
        possessionEtatRepo = mock(PossessionEtatRepository.class);
        autoriteParentaleRepo = mock(AutoriteParentaleRepository.class);
        changementResidenceRepo = mock(ChangementResidenceRepository.class);
        desaccordsParentauxRepo = mock(DesaccordsParentauxRepository.class);
        pacsDissolutionRepo = mock(PacsDissolutionRepository.class);
        separationCorpsRepo = mock(SeparationCorpsRepository.class);
        indivisionRepo = mock(IndivisionRepository.class);
        ordonnanceRequeteRepo = mock(OrdonnanceRequeteRepository.class);
        devolutionLegaleRepo = mock(DevolutionLegaleRepository.class);
        donationRepo = mock(DonationRepository.class);
        indivisionSuccessoraleRepo = mock(IndivisionSuccessoraleRepository.class);
        partageSuccessoralRepo = mock(PartageSuccessoralRepository.class);
        rapportSuccessionRepo = mock(RapportSuccessionRepository.class);
        reserveHereditaireRepo = mock(ReserveHereditaireRepository.class);
        testamentValiditeRepo = mock(TestamentValiditeRepository.class);
        majeursProtegesRepo = mock(MajeursProtegesRepository.class);
        changementEtatCivilRepo = mock(ChangementEtatCivilRepository.class);
        pmaGpaBioethiqueRepo = mock(PmaGpaBioethiqueRepository.class);
        oqtfAvecDelaiRepo = mock(OqtfAvecDelaiRepository.class);
        oqtfSansDelaiRepo = mock(OqtfSansDelaiRepository.class);
        referesAdminRepo = mock(ReferesAdminRepository.class);
        aesEtudiantRepo = mock(AesEtudiantRepository.class);
        aesFamilleRepo = mock(AesFamilleRepository.class);
        aesHumanitaireRepo = mock(AesHumanitaireRepository.class);
        aesMetiersTensionRepo = mock(AesMetiersTensionRepository.class);
        asileAvanceRepo = mock(AsileAvanceRepository.class);
        naturalisationRepo = mock(NaturalisationRepository.class);
        regimeAlgerienRepo = mock(RegimeAlgerienRepository.class);
        mineursImmigrationRepo = mock(MineursImmigrationRepository.class);
        mesuresEloignementRepo = mock(MesuresEloignementRepository.class);
        annexe13BeRepo = mock(Annexe13BeRepository.class);
        belgian9bisRepo = mock(Belgian9bisRepository.class);
        belgian9terRepo = mock(Belgian9terRepository.class);
        belgian40bisRepo = mock(Belgian40bisRepository.class);
        belgian40terRepo = mock(Belgian40terRepository.class);
        prescriptionBeLitigeTravailRepo = mock(PrescriptionBeLitigeTravailRepository.class);
        c4OnemChecklistRepo = mock(C4OnemChecklistRepository.class);
        retainedPisteAlignmentService = mock(fr.ailegalcase.analysis.RetainedPisteAlignmentService.class);
        when(retainedPisteAlignmentService.deserializeAlignment(any())).thenReturn(java.util.List.of());
        procedureCheckAlignmentService = mock(fr.ailegalcase.analysis.ProcedureCheckAlignmentService.class);
        when(procedureCheckAlignmentService.deserializeAlignment(any())).thenReturn(java.util.List.of());
        pieceManquanteAlignmentService = mock(fr.ailegalcase.analysis.PieceManquanteAlignmentService.class);
        when(pieceManquanteAlignmentService.deserializeAlignment(any())).thenReturn(java.util.List.of());
        risqueAlignmentService = mock(fr.ailegalcase.analysis.RisqueAlignmentService.class);
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(java.util.List.of());
        aiQuestionAlignmentService = mock(fr.ailegalcase.analysis.AiQuestionAlignmentService.class);
        when(aiQuestionAlignmentService.deserializeAlignment(any())).thenReturn(java.util.List.of());
        // F-194 SF-194-01 — mock CaseAnalysisRepository pour tests tile alignment
        analysisRepositoryMock = mock(fr.ailegalcase.analysis.CaseAnalysisRepository.class);
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                any(), any())).thenReturn(java.util.Optional.empty());

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
        when(procedureNulliteLicenciementRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(rupturePeriodeEssaiRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(abandonPostePresomptionDemissionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(jldRetentionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(dublinRecoursRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(crrvRefusVisaRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(victimeViolencesL4256Repo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(acceptationRenonciationSuccessionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(autoriteParentaleBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(contributionAlimentaireEnfantsBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(contributionConjointBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(divorceDcBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(divorceDdiBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(liquidationPartageBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(mediationFamilialePreSaisineRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(pacteSuccessoralBe2018Repo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(regimeCommunauteLegaleBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(tribunalFamilleBeMesuresProvisoiresRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(divorceAlterationRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(divorceFauteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(divorceAccepteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(divorceDesunionIrremediableBeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(mesuresProvisoiresRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(revisionsPostDivorceRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(ordonnanceProtectionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(recompensesRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(communauteUniverselleRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(partageJudiciaireRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(adoptionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(contestationPaterniteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(recherchePaterniteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(reconnaissancePaterneleRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(possessionEtatRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(autoriteParentaleRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(changementResidenceRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(desaccordsParentauxRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(pacsDissolutionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(separationCorpsRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(indivisionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(ordonnanceRequeteRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(devolutionLegaleRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(donationRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(indivisionSuccessoraleRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(partageSuccessoralRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(rapportSuccessionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(reserveHereditaireRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(testamentValiditeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(majeursProtegesRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(changementEtatCivilRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(pmaGpaBioethiqueRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(oqtfAvecDelaiRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(oqtfSansDelaiRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(referesAdminRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(aesEtudiantRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(aesFamilleRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(aesHumanitaireRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(aesMetiersTensionRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(asileAvanceRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(naturalisationRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(regimeAlgerienRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(mineursImmigrationRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(mesuresEloignementRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(annexe13BeRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(belgian9bisRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(belgian9terRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(belgian40bisRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(belgian40terRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(prescriptionBeLitigeTravailRepo.findByCaseFileId(any())).thenReturn(Optional.empty());
        when(c4OnemChecklistRepo.findByCaseFileId(any())).thenReturn(Optional.empty());

        service = new CaseFileDashboardService(
                objectMapper,
                /* caseFileRepository */ null,
                /* workspaceMemberRepository */ null,
                /* currentUserResolver */ null,
                analysisRepositoryMock,
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
                avantagesConventionnelsBeRepo, creditTempsBeRepo,
                procedureNulliteLicenciementRepo,
                rupturePeriodeEssaiRepo,
                abandonPostePresomptionDemissionRepo,
                congesPayesArretMaladieRepo,
                priseActeRuptureRepo,
                resiliationJudiciaireCphRepo,
                etrangerMaladeRepo,
                regroupementFamilialRepo,
                vpfLiensPersonnelsRepo,
                vlsTsValidationRepo,
                licenciementFauteGraveLourdRepo,
                jldRetentionRepo, dublinRecoursRepo, crrvRefusVisaRepo,
                victimeViolencesL4256Repo,
                acceptationRenonciationSuccessionRepo, autoriteParentaleBeRepo,
                contributionAlimentaireEnfantsBeRepo, contributionConjointBeRepo,
                divorceDcBeRepo, divorceDdiBeRepo, liquidationPartageBeRepo,
                mediationFamilialePreSaisineRepo, pacteSuccessoralBe2018Repo,
                regimeCommunauteLegaleBeRepo, tribunalFamilleBeMesuresProvisoiresRepo,
                divorceAlterationRepo, divorceFauteRepo, divorceAccepteRepo,
                divorceDesunionIrremediableBeRepo, mesuresProvisoiresRepo,
                revisionsPostDivorceRepo, ordonnanceProtectionRepo,
                recompensesRepo, communauteUniverselleRepo, partageJudiciaireRepo,
                adoptionRepo, contestationPaterniteRepo, recherchePaterniteRepo,
                reconnaissancePaterneleRepo, possessionEtatRepo,
                autoriteParentaleRepo, changementResidenceRepo,
                desaccordsParentauxRepo, pacsDissolutionRepo, separationCorpsRepo,
                indivisionRepo, ordonnanceRequeteRepo, devolutionLegaleRepo,
                donationRepo, indivisionSuccessoraleRepo, partageSuccessoralRepo,
                rapportSuccessionRepo, reserveHereditaireRepo,
                testamentValiditeRepo, majeursProtegesRepo,
                changementEtatCivilRepo, pmaGpaBioethiqueRepo,
                oqtfAvecDelaiRepo, oqtfSansDelaiRepo, referesAdminRepo,
                aesEtudiantRepo, aesFamilleRepo, aesHumanitaireRepo,
                aesMetiersTensionRepo, asileAvanceRepo, naturalisationRepo,
                regimeAlgerienRepo, mineursImmigrationRepo, mesuresEloignementRepo,
                annexe13BeRepo, belgian9bisRepo, belgian9terRepo,
                belgian40bisRepo, belgian40terRepo,
                prescriptionBeLitigeTravailRepo,
                c4OnemChecklistRepo,
                retainedPisteAlignmentService,
                procedureCheckAlignmentService,
                pieceManquanteAlignmentService,
                risqueAlignmentService,
                aiQuestionAlignmentService,
                mock(DashboardTileCrashRecorder.class));
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
                        (Runnable) this::seedCreditTempsBe),
                Arguments.of("F-DT-36-procedure-nullite-licenciement", "VALIDITE",
                        (Runnable) this::seedProcedureNulliteLicenciement)
        );
    }

    /**
     * SF-DT-36-03 — correctif câblage dashboard F-DT-36 : le mapping verdict →
     * alertLevel respecte la convention couleur de SF-DT-36-02 (rouge réservé
     * au verdict NULLITE_AVEREE).
     */
    @ParameterizedTest(name = "verdict {0} -> alertLevel {1}")
    @org.junit.jupiter.params.provider.CsvSource({
            "NULLITE_AVEREE,ALERT",
            "NULLITE_PROBABLE,WARNING",
            "PROCEDURE_REGULIERE,OK"
    })
    void assembleTiles_procedureNullite_mappeVerdictVersAlertLevel(
            String verdictName, String expectedAlert) {
        seedProcedureNulliteLicenciement(
                ProcedureNulliteLicenciementCalculator.Verdict.valueOf(verdictName));
        DashboardTile tile = service.assembleTiles(UUID.randomUUID()).stream()
                .filter(t -> "F-DT-36-procedure-nullite-licenciement".equals(t.toolId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tile F-DT-36 absente"));
        assertThat(tile.alertLevel()).isEqualTo(expectedAlert);
        assertThat(tile.primaryValue()).isEqualTo(verdictName);
        assertThat(tile.theme()).isEqualTo("VALIDITE");
    }

    /**
     * SF-DT-36-03 — correctif câblage des 4 outils Immigration FR orphelins du
     * dashboard (F-IM-21/22/23/24) : chacun, une fois calculé/persisté, émet
     * désormais sa tuile avec le bon thème et le bon alertLevel.
     */
    @ParameterizedTest(name = "[{index}] {0} -> {2}")
    @MethodSource("immigrationOrphanTilesData")
    void assembleTiles_cableLesOutilsImmigrationOrphelins(
            String expectedToolId, String expectedTheme, String expectedAlert,
            Runnable seedRepo) {
        seedRepo.run();
        DashboardTile tile = service.assembleTiles(UUID.randomUUID()).stream()
                .filter(t -> expectedToolId.equals(t.toolId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tile " + expectedToolId + " absente"));
        assertThat(tile.theme()).isEqualTo(expectedTheme);
        assertThat(tile.alertLevel()).isEqualTo(expectedAlert);
        assertThat(tile.primaryValue()).isNotNull();
        assertThat(tile.secondaryValue()).isNotNull();
    }

    private Stream<Arguments> immigrationOrphanTilesData() {
        return Stream.of(
                Arguments.of("F-IM-21-jld-retention-fr", "DELAIS", "ALERT",
                        (Runnable) () -> seedJldRetention("EXPIRE")),
                Arguments.of("F-IM-22-dublin-recours-fr", "DELAIS", "WARNING",
                        (Runnable) () -> seedDublinRecours("URGENT")),
                Arguments.of("F-IM-23-crrv-refus-visa-fr", "DELAIS", "OK",
                        (Runnable) () -> seedCrrvRefusVisa("DISPONIBLE")),
                Arguments.of("F-IM-24-victime-violences-l4256-fr", "VALIDITE", "ALERT",
                        (Runnable) () -> seedVictimeViolences("NON_ELIGIBLE"))
        );
    }

    /**
     * SF-DT-36-03 — correctif câblage des 11 outils Famille BE orphelins du
     * dashboard : chacun, une fois calculé/persisté, émet désormais sa tuile
     * avec le bon toolId, le bon thème et l'alertLevel attendu.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("familleBeOrphanTilesData")
    void assembleTiles_cableLesOutilsFamilleBeOrphelins(
            String expectedToolId, String expectedTheme, String expectedAlert,
            Runnable seedRepo) {
        seedRepo.run();
        DashboardTile tile = service.assembleTiles(UUID.randomUUID()).stream()
                .filter(t -> expectedToolId.equals(t.toolId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tile " + expectedToolId + " absente"));
        assertThat(tile.theme()).isEqualTo(expectedTheme);
        assertThat(tile.primaryValue()).isNotNull();
        assertThat(tile.alertLevel()).isEqualTo(expectedAlert);
    }

    private Stream<Arguments> familleBeOrphanTilesData() {
        return Stream.of(
                Arguments.of("acceptation-renonciation-succession", "VALIDITE", null,
                        (Runnable) this::seedAcceptationRenonciation),
                Arguments.of("autorite-parentale-be", "VALIDITE", null,
                        (Runnable) this::seedAutoriteParentaleBe),
                Arguments.of("contribution-alimentaire-enfants-be", "INDEMNITES", null,
                        (Runnable) this::seedContributionAlimentaireEnfantsBe),
                Arguments.of("contribution-conjoint-be", "INDEMNITES", null,
                        (Runnable) this::seedContributionConjointBe),
                Arguments.of("divorce-dc-be", "VALIDITE", "OK",
                        (Runnable) this::seedDivorceDcBe),
                Arguments.of("divorce-ddi-3voies-be", "DELAIS", null,
                        (Runnable) this::seedDivorceDdiBe),
                Arguments.of("liquidation-partage-be", "DELAIS", null,
                        (Runnable) this::seedLiquidationPartageBe),
                Arguments.of("mediation-familiale-pre-saisine", "DOCUMENTS", "ALERT",
                        (Runnable) this::seedMediationFamilialePreSaisine),
                Arguments.of("pacte-successoral-be-2018", "VALIDITE", "ALERT",
                        (Runnable) this::seedPacteSuccessoralBe2018),
                Arguments.of("regime-mat-be-communaute-legale", "DIAGNOSTIC", null,
                        (Runnable) this::seedRegimeCommunauteLegaleBe),
                Arguments.of("tribunal-famille-be-mesures-prov", "DELAIS", null,
                        (Runnable) this::seedTribunalFamilleBeMesuresProvisoires)
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

    /** SF-DT-36-03 — seed F-DT-36 avec verdict NULLITE_PROBABLE par défaut. */
    private void seedProcedureNulliteLicenciement() {
        seedProcedureNulliteLicenciement(
                ProcedureNulliteLicenciementCalculator.Verdict.NULLITE_PROBABLE);
    }

    private void seedProcedureNulliteLicenciement(
            ProcedureNulliteLicenciementCalculator.Verdict verdict) {
        try {
            ProcedureNulliteLicenciementAnalysis e = new ProcedureNulliteLicenciementAnalysis();
            ProcedureNulliteLicenciementResponse r = new ProcedureNulliteLicenciementResponse(
                    UUID.randomUUID(),
                    true, null, null, true, null,
                    true, true, true, null,
                    false, false, null, false, false, null,
                    verdict, 20, List.of(), List.of(), List.of(),
                    "FRANCE", java.time.Instant.now());
            e.setSnapshotData(objectMapper.writeValueAsString(r));
            when(procedureNulliteLicenciementRepo.findByCaseFileId(any()))
                    .thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    /** SF-DT-36-03 — seed F-IM-21 JLD rétention avec le statut de délai donné. */
    private void seedJldRetention(String statut) {
        try {
            JldRetentionAnalysis e = new JldRetentionAnalysis();
            JldRetentionResult r = new JldRetentionResult(
                    null, "AUTRE", false, null, null, null, null,
                    5L, statut, "formule", "CESEDA L.741", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(jldRetentionRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    /** SF-DT-36-03 — seed F-IM-22 recours Dublin avec le statut de délai donné. */
    private void seedDublinRecours(String statut) {
        try {
            DublinRecoursAnalysis e = new DublinRecoursAnalysis();
            DublinRecoursResult r = new DublinRecoursResult(
                    null, "ALLEMAGNE", "AUTRE", false, null, null, null,
                    3L, statut, "SUSPENSIF", "formule", "Règlement UE 604/2013", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(dublinRecoursRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    /** SF-DT-36-03 — seed F-IM-23 recours CRRV avec le statut de délai donné. */
    private void seedCrrvRefusVisa(String statut) {
        try {
            CrrvRefusVisaAnalysis e = new CrrvRefusVisaAnalysis();
            CrrvRefusVisaResult r = new CrrvRefusVisaResult(
                    null, "COURT_SEJOUR", "AUTRE", false, null, null,
                    10L, statut, "formule", "CESEDA L.312-1", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(crrvRefusVisaRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    /** SF-DT-36-03 — seed F-IM-24 victime de violences L.425-6 avec le score donné. */
    private void seedVictimeViolences(String eligibiliteScore) {
        try {
            VictimeViolencesL4256Analysis e = new VictimeViolencesL4256Analysis();
            VictimeViolencesL4256Result r = new VictimeViolencesL4256Result(
                    null, "JAF", 6, null, 0, "ALGERIENNE", eligibiliteScore,
                    List.of("Ordonnance de protection en cours"), List.of(),
                    12, "formule", "CESEDA L.425-6", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(victimeViolencesL4256Repo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    // SF-DT-36-03 — seeds des 11 outils Famille BE orphelins. Snapshot JSON
    // minimal : seuls les champs lus par la tuile sont fournis (Jackson
    // complète les champs absents à null/défaut).
    private void seedAcceptationRenonciation() {
        AcceptationRenonciationSuccessionAnalysis e = new AcceptationRenonciationSuccessionAnalysis();
        e.setResultData("{\"optionRecommandee\":\"RENONCIATION\",\"delaiRestantJours\":90}");
        when(acceptationRenonciationSuccessionRepo.findByCaseFileId(any()))
                .thenReturn(Optional.of(e));
    }

    private void seedAutoriteParentaleBe() {
        AutoriteParentaleBeAnalysis e = new AutoriteParentaleBeAnalysis();
        e.setSnapshotData("{}");
        when(autoriteParentaleBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedContributionAlimentaireEnfantsBe() {
        ContributionAlimentaireEnfantsBeAnalysis e = new ContributionAlimentaireEnfantsBeAnalysis();
        e.setSnapshotData("{}");
        when(contributionAlimentaireEnfantsBeRepo.findByCaseFileId(any()))
                .thenReturn(Optional.of(e));
    }

    private void seedContributionConjointBe() {
        ContributionConjointBeAnalysis e = new ContributionConjointBeAnalysis();
        e.setSnapshotData("{}");
        when(contributionConjointBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedDivorceDcBe() {
        DivorceDcBeAnalysis e = new DivorceDcBeAnalysis();
        e.setResultData("{\"verdict\":\"RECEVABLE\"}");
        when(divorceDcBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedDivorceDdiBe() {
        DivorceDdiBeAnalysis e = new DivorceDdiBeAnalysis();
        e.setResultData("{\"voieRecommandee\":\"VOIE_2_COMMUNE_6_MOIS\",\"joursSeparation\":200}");
        when(divorceDdiBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedLiquidationPartageBe() {
        LiquidationPartageBeAnalysis e = new LiquidationPartageBeAnalysis();
        e.setSnapshotData("{\"prochaineEtape\":\"Établir l'inventaire\"}");
        when(liquidationPartageBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedMediationFamilialePreSaisine() {
        MediationFamilialePreSaisineAnalysis e = new MediationFamilialePreSaisineAnalysis();
        e.setResultData("{\"verdict\":\"IRRECEVABLE\"}");
        when(mediationFamilialePreSaisineRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedPacteSuccessoralBe2018() {
        PacteSuccessoralBe2018Analysis e = new PacteSuccessoralBe2018Analysis();
        e.setResultData("{\"verdict\":\"NUL\"}");
        when(pacteSuccessoralBe2018Repo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedRegimeCommunauteLegaleBe() {
        RegimeCommunauteLegaleBeAnalysis e = new RegimeCommunauteLegaleBeAnalysis();
        e.setSnapshotData("{}");
        when(regimeCommunauteLegaleBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
    }

    private void seedTribunalFamilleBeMesuresProvisoires() {
        TribunalFamilleBeMesuresProvisoiresAnalysis e = new TribunalFamilleBeMesuresProvisoiresAnalysis();
        e.setResultData("{\"verdict\":\"URGENT_REFERE\",\"scoreUrgence\":80}");
        when(tribunalFamilleBeMesuresProvisoiresRepo.findByCaseFileId(any()))
                .thenReturn(Optional.of(e));
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

    // ────────────────────────────────────────────────────────────────────
    // SF-167-03 — Tests représentatifs Famille FR + BE
    // (mini-spec : 2-3 outils — F-FA-09, F-FA-11, F-FA-24-rapport-succession)
    // ────────────────────────────────────────────────────────────────────

    @Test
    void assembleTiles_divorceFaute_returnsCompleteTile() throws Exception {
        seedDivorceFaute();
        UUID caseFileId = UUID.randomUUID();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-FA-09-divorce-faute".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("VALIDITE");
        assertThat(tile.label()).isEqualTo("Divorce pour faute");
        assertThat(tile.primaryValue()).isEqualTo("PROBABLE");
        assertThat(tile.secondaryValue()).contains("faute(s) invoquée(s)").contains("/100");
        assertThat(tile.alertLevel()).isIn("OK", "WARNING", "ALERT", null);
    }

    @Test
    void assembleTiles_desunionIrremediableBe_returnsCompleteTile() throws Exception {
        seedDesunionIrremediableBe();
        UUID caseFileId = UUID.randomUUID();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-FA-11-desunion-irremediable-be".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("VALIDITE");
        assertThat(tile.label()).isEqualTo("Désunion irrémédiable BE");
        assertThat(tile.primaryValue()).isEqualTo("ELEVEE");
        assertThat(tile.secondaryValue()).contains("/").contains("mois");
    }

    @Test
    void assembleTiles_rapportSuccession_returnsCompleteTile() throws Exception {
        seedRapportSuccession();
        UUID caseFileId = UUID.randomUUID();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-FA-24-rapport-succession".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("DIAGNOSTIC");
        assertThat(tile.label()).isEqualTo("Rapport à succession");
        assertThat(tile.primaryValue()).isEqualTo("RAPPORTABLE");
        assertThat(tile.secondaryValue()).contains("Rapportable").contains("€");
        assertThat(tile.alertLevel()).isEqualTo("WARNING");
    }

    // ---- Helpers de seed SF-167-03 ----------------------------------------

    private void seedDivorceFaute() {
        try {
            DivorceFauteAnalysis e = new DivorceFauteAnalysis();
            DivorceFauteResult r = new DivorceFauteResult(
                    List.of("VIOLENCE_PHYSIQUE"), true, false, 8,
                    new BigDecimal("45000"), new BigDecimal("60000"),
                    LocalDate.of(2026, 3, 1),
                    1, true, false,
                    65, "PROBABLE", "TORTS_EXCLUSIFS",
                    new BigDecimal("3000"), new BigDecimal("8000"),
                    new BigDecimal("12000"), new BigDecimal("36000"),
                    List.of(), "—",
                    "art. 242-246 + 266 + 270 Cciv", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(divorceFauteRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedDesunionIrremediableBe() {
        try {
            DivorceDesunionIrremediableBeAnalysis e = new DivorceDesunionIrremediableBeAnalysis();
            DivorceDesunionIrremediableBeResult r = new DivorceDesunionIrremediableBeResult(
                    LocalDate.of(2025, 1, 1), false, true, true, false,
                    LocalDate.of(2026, 4, 1),
                    15, 12, true, true,
                    80, "ELEVEE",
                    "art. 229 §3 Code civil belge",
                    "Délai 15 mois >= 12 mois (unilateral)",
                    List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(divorceDesunionIrremediableBeRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedRapportSuccession() {
        try {
            RapportSuccessionAnalysis e = new RapportSuccessionAnalysis();
            RapportSuccessionResult r = new RapportSuccessionResult(
                    new BigDecimal("50000"), LocalDate.of(2018, 6, 1),
                    new BigDecimal("75000"), false, false,
                    RapportSuccessionCalculator.QualiteHeritier.DESCENDANT,
                    "FRANCE",
                    RapportSuccessionCalculator.VerdictObligation.RAPPORTABLE,
                    RapportSuccessionCalculator.ModeRapport.RAPPORT_EN_VALEUR,
                    new BigDecimal("75000"),
                    5, 90, "art. 843 + 860 Cciv",
                    "Valeur au jour partage", List.of());
            e.setResultData(objectMapper.writeValueAsString(r));
            when(rapportSuccessionRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    // ────────────────────────────────────────────────────────────────────
    // SF-167-04 — Tests représentatifs Immigration FR + BE
    // (mini-spec : 3 outils — F-IM-08 OQTF avec délai, F-IM-13 Naturalisation,
    //  F-IM-14 9ter médical BE)
    // ────────────────────────────────────────────────────────────────────

    @Test
    void assembleTiles_oqtfAvecDelai_returnsCompleteTile() throws Exception {
        seedOqtfAvecDelai();
        UUID caseFileId = UUID.randomUUID();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-IM-08-oqtf-avec-delai-fr".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("DELAIS");
        assertThat(tile.label()).isEqualTo("OQTF avec délai");
        assertThat(tile.primaryValue()).isEqualTo("DANS_DELAI");
        assertThat(tile.secondaryValue()).contains("j restants");
        assertThat(tile.alertLevel()).isEqualTo("OK");
    }

    @Test
    void assembleTiles_naturalisation_returnsCompleteTile() throws Exception {
        seedNaturalisation();
        UUID caseFileId = UUID.randomUUID();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-IM-13-naturalisation".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("DIAGNOSTIC");
        assertThat(tile.label()).isEqualTo("Naturalisation");
        assertThat(tile.primaryValue()).isEqualTo("ELEVEE");
        assertThat(tile.secondaryValue()).contains("mois");
        assertThat(tile.alertLevel()).isEqualTo("OK");
    }

    @Test
    void assembleTiles_belgian9ter_returnsCompleteTile() throws Exception {
        seedBelgian9ter();
        UUID caseFileId = UUID.randomUUID();

        DashboardTile tile = service.assembleTiles(caseFileId).stream()
                .filter(t -> "F-IM-14-9ter-medical-be".equals(t.toolId()))
                .findFirst().orElseThrow();
        assertThat(tile.theme()).isEqualTo("DIAGNOSTIC");
        assertThat(tile.label()).isEqualTo("9ter médical BE");
        assertThat(tile.primaryValue()).isEqualTo("ELEVEE");
        assertThat(tile.secondaryValue()).contains("/100");
        assertThat(tile.alertLevel()).isEqualTo("OK");
    }

    // ---- Helpers de seed SF-167-04 ----------------------------------------

    private void seedOqtfAvecDelai() {
        try {
            OqtfAvecDelaiAnalysis e = new OqtfAvecDelaiAnalysis();
            OqtfAvecDelaiResult r = new OqtfAvecDelaiResult(
                    LocalDate.of(2026, 4, 1), "MENACE_OP", false, null,
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1),
                    20, "DANS_DELAI",
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 15),
                    List.of("L.521-1", "L.521-2"),
                    "Recours TA art. L.614-5 CESEDA",
                    "art. L.614-5 CESEDA", List.of("Conseil"));
            e.setResultData(objectMapper.writeValueAsString(r));
            when(oqtfAvecDelaiRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedNaturalisation() {
        try {
            NaturalisationAnalysis e = new NaturalisationAnalysis();
            NaturalisationResult r = new NaturalisationResult(
                    "DECRET_DROIT_COMMUN",
                    "Décret art. 21-15 Cciv",
                    "ELEVEE",
                    List.of(),
                    List.of("Casier judiciaire", "Justificatifs revenus"),
                    18,
                    "art. 21-15 Cciv",
                    "Naturalisation par décret",
                    List.of("Discrétion gouvernementale"));
            e.setResultData(objectMapper.writeValueAsString(r));
            when(naturalisationRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    private void seedBelgian9ter() {
        try {
            Belgian9terAnalysis e = new Belgian9terAnalysis();
            Belgian9terResult r = new Belgian9terResult(
                    LocalDate.of(2025, 1, 1), true, true, true, false,
                    LocalDate.of(2026, 4, 1),
                    true, true, true, true,
                    85, "ELEVEE",
                    List.of(),
                    LocalDate.of(2026, 10, 1),
                    "Régularisation 9ter — éligible",
                    "art. 9ter Loi 15/12/1980",
                    List.of("Certificat médical type"));
            e.setResultData(objectMapper.writeValueAsString(r));
            when(belgian9terRepo.findByCaseFileId(any())).thenReturn(Optional.of(e));
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }

    // ====================================================================
    //  F-194 SF-194-01 — tests tile pieces manquantes markables
    // ====================================================================

    @Test
    void f194Tile_aDemanderRecent_levelOk() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now()); // récent
        a.setPiecesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(pieceManquanteAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.PieceManquanteAlignment("Contrat",
                        fr.ailegalcase.analysis.PieceManquanteStatus.STATUT_A_DEMANDER, null, null)));

        var tiles = service.assembleTiles(caseFileId);
        var f194 = tiles.stream().filter(t -> "F-194-pieces-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f194).isNotNull();
        assertThat(f194.theme()).isEqualTo("DOCUMENTS");
        assertThat(f194.alertLevel()).isEqualTo("OK");
        assertThat(f194.primaryValue()).isEqualTo("1 pièce");
        assertThat(f194.secondaryValue()).contains("1 à demander");
    }

    @Test
    void f194Tile_aDemanderStale_levelWarning() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now().minus(java.time.Duration.ofDays(8)));
        a.setPiecesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(pieceManquanteAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.PieceManquanteAlignment("Contrat",
                        fr.ailegalcase.analysis.PieceManquanteStatus.STATUT_A_DEMANDER, null, null)));

        var tiles = service.assembleTiles(caseFileId);
        var f194 = tiles.stream().filter(t -> "F-194-pieces-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f194).isNotNull();
        assertThat(f194.alertLevel()).isEqualTo("WARNING");
    }

    @Test
    void f194Tile_mixStatuses_correctSecondary() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setPiecesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(pieceManquanteAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.PieceManquanteAlignment("A",
                        fr.ailegalcase.analysis.PieceManquanteStatus.STATUT_A_DEMANDER, null, null),
                new fr.ailegalcase.analysis.PieceManquanteAlignment("B",
                        fr.ailegalcase.analysis.PieceManquanteStatus.STATUT_A_DEMANDER, null, null),
                new fr.ailegalcase.analysis.PieceManquanteAlignment("C",
                        fr.ailegalcase.analysis.PieceManquanteStatus.STATUT_OBTENUE, null, null),
                new fr.ailegalcase.analysis.PieceManquanteAlignment("D",
                        fr.ailegalcase.analysis.PieceManquanteStatus.STATUT_NON_APPLICABLE, null, null)));

        var tiles = service.assembleTiles(caseFileId);
        var f194 = tiles.stream().filter(t -> "F-194-pieces-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f194).isNotNull();
        assertThat(f194.primaryValue()).isEqualTo("4 pièces");
        assertThat(f194.secondaryValue()).contains("2 à demander")
                .contains("1 obtenue")
                .contains("1 non applicable");
        assertThat(f194.alertLevel()).isEqualTo("OK"); // récent
    }

    @Test
    void f194Tile_emptyAlignment_returnsNoTile() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(pieceManquanteAlignmentService.deserializeAlignment(any())).thenReturn(List.of());

        var tiles = service.assembleTiles(caseFileId);
        assertThat(tiles.stream().anyMatch(t -> "F-194-pieces-summary".equals(t.toolId()))).isFalse();
    }

    // ====================================================================
    //  F-195 SF-195-01 — tests tile risques markables
    // ====================================================================

    @Test
    void f195Tile_validatedCriticalKeyword_levelAlert() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("Harcèlement moral subi",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_VALIDE, null, List.of())));

        var tiles = service.assembleTiles(caseFileId);
        var f195 = tiles.stream().filter(t -> "F-195-risques-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f195).isNotNull();
        assertThat(f195.theme()).isEqualTo("DIAGNOSTIC");
        assertThat(f195.alertLevel()).isEqualTo("ALERT");
        assertThat(f195.primaryValue()).isEqualTo("1 risque");
        assertThat(f195.secondaryValue()).contains("1 validé");
    }

    @Test
    void f195Tile_validatedNonCritical_levelWarning() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("Discrimination liée au sexe",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_VALIDE, null, List.of())));

        var tiles = service.assembleTiles(caseFileId);
        var f195 = tiles.stream().filter(t -> "F-195-risques-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f195).isNotNull();
        assertThat(f195.alertLevel()).isEqualTo("WARNING");
    }

    @Test
    void f195Tile_allEcartes_levelOk() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("R1",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_ECARTE, "raison", List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R2",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_ECARTE, "raison2", List.of())));

        var tiles = service.assembleTiles(caseFileId);
        var f195 = tiles.stream().filter(t -> "F-195-risques-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f195).isNotNull();
        assertThat(f195.alertLevel()).isEqualTo("OK");
        assertThat(f195.primaryValue()).isEqualTo("2 risques");
        assertThat(f195.secondaryValue()).contains("2 écartés");
    }

    @Test
    void f195Tile_mixStatuses_correctSecondary() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("Discrimination",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_VALIDE, null, List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R2",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_ECARTE, "raison", List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R3",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_A_CREUSER, null, List.of())));

        var tiles = service.assembleTiles(caseFileId);
        var f195 = tiles.stream().filter(t -> "F-195-risques-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f195).isNotNull();
        assertThat(f195.primaryValue()).isEqualTo("3 risques");
        assertThat(f195.secondaryValue()).contains("1 validé")
                .contains("1 écarté")
                .contains("1 à creuser");
        // Discrimination = pas critique → WARNING
        assertThat(f195.alertLevel()).isEqualTo("WARNING");
    }

    @Test
    void f195Tile_emptyAlignment_returnsNoTile() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of());

        var tiles = service.assembleTiles(caseFileId);
        assertThat(tiles.stream().anyMatch(t -> "F-195-risques-summary".equals(t.toolId()))).isFalse();
    }

    // ====================================================================
    //  F-196 SF-196-01 — tests tile questions complémentaires
    // ====================================================================

    @Test
    void f196Tile_allAnswered_levelOk() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setAiQuestionsAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(aiQuestionAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.AiQuestionAlignment(UUID.randomUUID(), "oui",
                        "Lettre de licenciement", "PIECE_OBTENUE"),
                new fr.ailegalcase.analysis.AiQuestionAlignment(UUID.randomUUID(), "non",
                        "Contrat de travail", "PIECE_MANQUANTE")));

        var tiles = service.assembleTiles(caseFileId);
        var f196 = tiles.stream().filter(t -> "F-196-questions-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f196).isNotNull();
        assertThat(f196.theme()).isEqualTo("DOCUMENTS");
        assertThat(f196.alertLevel()).isEqualTo("OK");
        assertThat(f196.primaryValue()).isEqualTo("2 questions");
        assertThat(f196.secondaryValue()).contains("2 répondues").contains("0 en attente");
    }

    @Test
    void f196Tile_someUnanswered_levelWarning() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setAiQuestionsAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(aiQuestionAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.AiQuestionAlignment(UUID.randomUUID(), "oui",
                        "Lettre de licenciement", "PIECE_OBTENUE"),
                new fr.ailegalcase.analysis.AiQuestionAlignment(UUID.randomUUID(), null,
                        null, "INFO_ONLY"),
                new fr.ailegalcase.analysis.AiQuestionAlignment(UUID.randomUUID(), "",
                        null, "INFO_ONLY")));

        var tiles = service.assembleTiles(caseFileId);
        var f196 = tiles.stream().filter(t -> "F-196-questions-summary".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f196).isNotNull();
        assertThat(f196.alertLevel()).isEqualTo("WARNING");
        assertThat(f196.primaryValue()).isEqualTo("3 questions");
        assertThat(f196.secondaryValue()).contains("1 répondue").contains("2 en attente");
    }

    @Test
    void f196Tile_emptyAlignment_returnsNoTile() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(aiQuestionAlignmentService.deserializeAlignment(any())).thenReturn(List.of());

        var tiles = service.assembleTiles(caseFileId);
        assertThat(tiles.stream().anyMatch(t -> "F-196-questions-summary".equals(t.toolId()))).isFalse();
    }

    // ====================================================================
    //  F-253 SF-253-01 — tile dédiée rappel des risques restant à arbitrer
    // ====================================================================

    @Test
    void f253Tile_aCreuserPresent_warning() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("R1",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_A_CREUSER, null, List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R2",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_A_CREUSER, null, List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R3",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_VALIDE, null, List.of())));

        var tiles = service.assembleTiles(caseFileId);
        var f253 = tiles.stream().filter(t -> "F-253-risques-a-creuser".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f253).isNotNull();
        assertThat(f253.theme()).isEqualTo("DIAGNOSTIC");
        assertThat(f253.label()).isEqualTo("Risques à arbitrer");
        assertThat(f253.primaryValue()).isEqualTo("2 à creuser");
        assertThat(f253.secondaryValue()).isEqualTo("Curation à compléter");
        assertThat(f253.alertLevel()).isEqualTo("WARNING");
    }

    @Test
    void f253Tile_singularPrimary() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("R1",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_A_CREUSER, null, List.of())));

        var tiles = service.assembleTiles(caseFileId);
        var f253 = tiles.stream().filter(t -> "F-253-risques-a-creuser".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f253).isNotNull();
        assertThat(f253.primaryValue()).isEqualTo("1 à creuser");
    }

    @Test
    void f253Tile_zeroACreuser_noTile() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("R1",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_VALIDE, null, List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R2",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_ECARTE, "raison", List.of())));

        var tiles = service.assembleTiles(caseFileId);
        assertThat(tiles.stream().anyMatch(t -> "F-253-risques-a-creuser".equals(t.toolId()))).isFalse();
    }

    @Test
    void f253Tile_emptyAlignment_noTile() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of());

        var tiles = service.assembleTiles(caseFileId);
        assertThat(tiles.stream().anyMatch(t -> "F-253-risques-a-creuser".equals(t.toolId()))).isFalse();
    }

    @Test
    void f253Tile_noAnalysis_noTile() {
        UUID caseFileId = UUID.randomUUID();
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.empty());

        var tiles = service.assembleTiles(caseFileId);
        assertThat(tiles.stream().anyMatch(t -> "F-253-risques-a-creuser".equals(t.toolId()))).isFalse();
    }

    @Test
    void f253Tile_cohabitsWithF195() {
        UUID caseFileId = UUID.randomUUID();
        fr.ailegalcase.analysis.CaseAnalysis a = new fr.ailegalcase.analysis.CaseAnalysis();
        a.setAnalysisStatus(fr.ailegalcase.analysis.AnalysisStatus.DONE);
        a.setUpdatedAt(java.time.Instant.now());
        a.setRisquesAlignmentJson("[]");
        when(analysisRepositoryMock.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE))
                .thenReturn(Optional.of(a));
        when(risqueAlignmentService.deserializeAlignment(any())).thenReturn(List.of(
                new fr.ailegalcase.analysis.RisqueAlignment("Discrimination",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_VALIDE, null, List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R2",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_ECARTE, "raison", List.of()),
                new fr.ailegalcase.analysis.RisqueAlignment("R3",
                        fr.ailegalcase.analysis.RisqueStatus.STATUT_A_CREUSER, null, List.of())));

        var tiles = service.assembleTiles(caseFileId);
        // Les 2 tiles cohabitent
        assertThat(tiles.stream().anyMatch(t -> "F-195-risques-summary".equals(t.toolId()))).isTrue();
        var f253 = tiles.stream().filter(t -> "F-253-risques-a-creuser".equals(t.toolId()))
                .findFirst().orElse(null);
        assertThat(f253).isNotNull();
        assertThat(f253.primaryValue()).isEqualTo("1 à creuser");
    }
}
