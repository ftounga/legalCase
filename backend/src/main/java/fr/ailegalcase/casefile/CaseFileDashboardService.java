package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
import fr.ailegalcase.analysis.ProcedureCheckAlignment;
import fr.ailegalcase.analysis.ProcedureCheckAlignmentService;
import fr.ailegalcase.analysis.RetainedPisteAlignment;
import fr.ailegalcase.analysis.RetainedPisteAlignmentService;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CaseFileDashboardService {

    private static final Logger log = LoggerFactory.getLogger(CaseFileDashboardService.class);
    private final ObjectMapper objectMapper;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final CaseAnalysisRepository analysisRepository;
    private final LicenciementAnalysisRepository licenciementRepo;
    private final IndemniteComparatifRepository indemniteRepo;
    private final RuptureConvIndemniteRepository ruptureConvIndemniteRepo;
    private final AncienneteAnalysisRepository ancienneteRepo;
    private final ImmigrationTitleDecisionRepository titleDecisionRepo;
    private final ImmigrationWorkRightRepository workRightRepo;
    private final ImmigrationRecoursRepository recoursRepo;
    private final PartageImmobilierRepository partageRepo;
    private final CalendrierGardeRepository gardeRepo;
    private final DivorceChecklistRepository divorceRepo;
    private final ChangementStatutRepository changementStatutRepo;
    // SF-167-02 : repos Travail FR + BE additionnels (extension à ~25 outils).
    private final RuptureConvAnalysisRepository ruptureConvAnalysisRepo;
    private final HarcelementNulliteRepository harcelementNulliteRepo;
    private final DiscriminationRepository discriminationRepo;
    private final LicenciementEconomiqueRepository licenciementEconomiqueRepo;
    private final PseRepository pseRepo;
    private final InaptitudeRepository inaptitudeRepo;
    private final LicenciementNulDetectionRepository licenciementNulDetectionRepo;
    private final IndemnitePrecariteCddRepository indemnitePrecariteCddRepo;
    private final IndemniteFinMissionInterimRepository indemniteFinMissionInterimRepo;
    private final HeuresSupRepository heuresSupRepo;
    private final RappelSalaireRepository rappelSalaireRepo;
    private final TravailDissimuleRepository travailDissimuleRepo;
    private final RequalificationCddCdiRepository requalificationCddCdiRepo;
    private final RequalificationInterimCdiRepository requalificationInterimCdiRepo;
    private final NonConcurrenceRepository nonConcurrenceRepo;
    private final IndemnitePreavisRepository indemnitePreavisRepo;
    private final IndemniteCongesPayesRepository indemniteCongesPayesRepo;
    private final ProtectionRpRepository protectionRpRepo;
    private final TransactionRepository transactionRepo;
    private final DocumentsFinContratRepository documentsFinContratRepo;
    private final AtMpRepository atMpRepo;
    private final ReferePrudhomalRepository referePrudhomalRepo;
    private final ContestationAreRepository contestationAreRepo;
    private final MotifGraveBeRepository motifGraveBeRepo;
    private final AvantagesConventionnelsBeRepository avantagesConventionnelsBeRepo;
    private final CreditTempsBeRepository creditTempsBeRepo;
    // SF-167-03 : repos Famille FR + BE additionnels (extension à ~31 outils Famille restants).
    private final DivorceAlterationRepository divorceAlterationRepo;
    private final DivorceFauteRepository divorceFauteRepo;
    private final DivorceAccepteRepository divorceAccepteRepo;
    private final DivorceDesunionIrremediableBeRepository divorceDesunionIrremediableBeRepo;
    private final MesuresProvisoiresRepository mesuresProvisoiresRepo;
    private final RevisionsPostDivorceRepository revisionsPostDivorceRepo;
    private final OrdonnanceProtectionRepository ordonnanceProtectionRepo;
    private final RecompensesRepository recompensesRepo;
    private final CommunauteUniverselleRepository communauteUniverselleRepo;
    private final PartageJudiciaireRepository partageJudiciaireRepo;
    private final AdoptionRepository adoptionRepo;
    private final ContestationPaterniteRepository contestationPaterniteRepo;
    private final RecherchePaterniteRepository recherchePaterniteRepo;
    private final ReconnaissancePaterneleRepository reconnaissancePaterneleRepo;
    private final PossessionEtatRepository possessionEtatRepo;
    private final AutoriteParentaleRepository autoriteParentaleRepo;
    private final ChangementResidenceRepository changementResidenceRepo;
    private final DesaccordsParentauxRepository desaccordsParentauxRepo;
    private final PacsDissolutionRepository pacsDissolutionRepo;
    private final SeparationCorpsRepository separationCorpsRepo;
    private final IndivisionRepository indivisionRepo;
    private final OrdonnanceRequeteRepository ordonnanceRequeteRepo;
    private final DevolutionLegaleRepository devolutionLegaleRepo;
    private final DonationRepository donationRepo;
    private final IndivisionSuccessoraleRepository indivisionSuccessoraleRepo;
    private final PartageSuccessoralRepository partageSuccessoralRepo;
    private final RapportSuccessionRepository rapportSuccessionRepo;
    private final ReserveHereditaireRepository reserveHereditaireRepo;
    private final TestamentValiditeRepository testamentValiditeRepo;
    private final MajeursProtegesRepository majeursProtegesRepo;
    private final ChangementEtatCivilRepository changementEtatCivilRepo;
    private final PmaGpaBioethiqueRepository pmaGpaBioethiqueRepo;
    // SF-167-04 : repos Immigration FR + BE additionnels (extension à 17 outils Immigration restants).
    private final OqtfAvecDelaiRepository oqtfAvecDelaiRepo;
    private final OqtfSansDelaiRepository oqtfSansDelaiRepo;
    private final ReferesAdminRepository referesAdminRepo;
    private final AesEtudiantRepository aesEtudiantRepo;
    private final AesFamilleRepository aesFamilleRepo;
    private final AesHumanitaireRepository aesHumanitaireRepo;
    private final AesMetiersTensionRepository aesMetiersTensionRepo;
    private final AsileAvanceRepository asileAvanceRepo;
    private final NaturalisationRepository naturalisationRepo;
    private final RegimeAlgerienRepository regimeAlgerienRepo;
    private final MineursImmigrationRepository mineursImmigrationRepo;
    private final MesuresEloignementRepository mesuresEloignementRepo;
    private final Annexe13BeRepository annexe13BeRepo;
    private final Belgian9bisRepository belgian9bisRepo;
    private final Belgian9terRepository belgian9terRepo;
    private final Belgian40bisRepository belgian40bisRepo;
    private final Belgian40terRepository belgian40terRepo;
    // F-192 SF-192-01 — pistes RETAINED matérialisées sur la dernière analyse DONE.
    private final RetainedPisteAlignmentService retainedPisteAlignmentService;
    // F-193 SF-193-01 — checks F-96 matérialisés sur la dernière analyse DONE.
    private final ProcedureCheckAlignmentService procedureCheckAlignmentService;

    public CaseFileDashboardService(ObjectMapper objectMapper, CaseFileRepository caseFileRepository,
                                     WorkspaceMemberRepository workspaceMemberRepository,
                                     CurrentUserResolver currentUserResolver,
                                     CaseAnalysisRepository analysisRepository,
                                     LicenciementAnalysisRepository licenciementRepo,
                                     IndemniteComparatifRepository indemniteRepo,
                                     RuptureConvIndemniteRepository ruptureConvIndemniteRepo,
                                     AncienneteAnalysisRepository ancienneteRepo,
                                     ImmigrationTitleDecisionRepository titleDecisionRepo,
                                     ImmigrationWorkRightRepository workRightRepo,
                                     ImmigrationRecoursRepository recoursRepo,
                                     PartageImmobilierRepository partageRepo,
                                     CalendrierGardeRepository gardeRepo,
                                     DivorceChecklistRepository divorceRepo,
                                     ChangementStatutRepository changementStatutRepo,
                                     RuptureConvAnalysisRepository ruptureConvAnalysisRepo,
                                     HarcelementNulliteRepository harcelementNulliteRepo,
                                     DiscriminationRepository discriminationRepo,
                                     LicenciementEconomiqueRepository licenciementEconomiqueRepo,
                                     PseRepository pseRepo,
                                     InaptitudeRepository inaptitudeRepo,
                                     LicenciementNulDetectionRepository licenciementNulDetectionRepo,
                                     IndemnitePrecariteCddRepository indemnitePrecariteCddRepo,
                                     IndemniteFinMissionInterimRepository indemniteFinMissionInterimRepo,
                                     HeuresSupRepository heuresSupRepo,
                                     RappelSalaireRepository rappelSalaireRepo,
                                     TravailDissimuleRepository travailDissimuleRepo,
                                     RequalificationCddCdiRepository requalificationCddCdiRepo,
                                     RequalificationInterimCdiRepository requalificationInterimCdiRepo,
                                     NonConcurrenceRepository nonConcurrenceRepo,
                                     IndemnitePreavisRepository indemnitePreavisRepo,
                                     IndemniteCongesPayesRepository indemniteCongesPayesRepo,
                                     ProtectionRpRepository protectionRpRepo,
                                     TransactionRepository transactionRepo,
                                     DocumentsFinContratRepository documentsFinContratRepo,
                                     AtMpRepository atMpRepo,
                                     ReferePrudhomalRepository referePrudhomalRepo,
                                     ContestationAreRepository contestationAreRepo,
                                     MotifGraveBeRepository motifGraveBeRepo,
                                     AvantagesConventionnelsBeRepository avantagesConventionnelsBeRepo,
                                     CreditTempsBeRepository creditTempsBeRepo,
                                     DivorceAlterationRepository divorceAlterationRepo,
                                     DivorceFauteRepository divorceFauteRepo,
                                     DivorceAccepteRepository divorceAccepteRepo,
                                     DivorceDesunionIrremediableBeRepository divorceDesunionIrremediableBeRepo,
                                     MesuresProvisoiresRepository mesuresProvisoiresRepo,
                                     RevisionsPostDivorceRepository revisionsPostDivorceRepo,
                                     OrdonnanceProtectionRepository ordonnanceProtectionRepo,
                                     RecompensesRepository recompensesRepo,
                                     CommunauteUniverselleRepository communauteUniverselleRepo,
                                     PartageJudiciaireRepository partageJudiciaireRepo,
                                     AdoptionRepository adoptionRepo,
                                     ContestationPaterniteRepository contestationPaterniteRepo,
                                     RecherchePaterniteRepository recherchePaterniteRepo,
                                     ReconnaissancePaterneleRepository reconnaissancePaterneleRepo,
                                     PossessionEtatRepository possessionEtatRepo,
                                     AutoriteParentaleRepository autoriteParentaleRepo,
                                     ChangementResidenceRepository changementResidenceRepo,
                                     DesaccordsParentauxRepository desaccordsParentauxRepo,
                                     PacsDissolutionRepository pacsDissolutionRepo,
                                     SeparationCorpsRepository separationCorpsRepo,
                                     IndivisionRepository indivisionRepo,
                                     OrdonnanceRequeteRepository ordonnanceRequeteRepo,
                                     DevolutionLegaleRepository devolutionLegaleRepo,
                                     DonationRepository donationRepo,
                                     IndivisionSuccessoraleRepository indivisionSuccessoraleRepo,
                                     PartageSuccessoralRepository partageSuccessoralRepo,
                                     RapportSuccessionRepository rapportSuccessionRepo,
                                     ReserveHereditaireRepository reserveHereditaireRepo,
                                     TestamentValiditeRepository testamentValiditeRepo,
                                     MajeursProtegesRepository majeursProtegesRepo,
                                     ChangementEtatCivilRepository changementEtatCivilRepo,
                                     PmaGpaBioethiqueRepository pmaGpaBioethiqueRepo,
                                     OqtfAvecDelaiRepository oqtfAvecDelaiRepo,
                                     OqtfSansDelaiRepository oqtfSansDelaiRepo,
                                     ReferesAdminRepository referesAdminRepo,
                                     AesEtudiantRepository aesEtudiantRepo,
                                     AesFamilleRepository aesFamilleRepo,
                                     AesHumanitaireRepository aesHumanitaireRepo,
                                     AesMetiersTensionRepository aesMetiersTensionRepo,
                                     AsileAvanceRepository asileAvanceRepo,
                                     NaturalisationRepository naturalisationRepo,
                                     RegimeAlgerienRepository regimeAlgerienRepo,
                                     MineursImmigrationRepository mineursImmigrationRepo,
                                     MesuresEloignementRepository mesuresEloignementRepo,
                                     Annexe13BeRepository annexe13BeRepo,
                                     Belgian9bisRepository belgian9bisRepo,
                                     Belgian9terRepository belgian9terRepo,
                                     Belgian40bisRepository belgian40bisRepo,
                                     Belgian40terRepository belgian40terRepo,
                                     RetainedPisteAlignmentService retainedPisteAlignmentService,
                                     ProcedureCheckAlignmentService procedureCheckAlignmentService) {
        this.objectMapper = objectMapper;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.analysisRepository = analysisRepository;
        this.licenciementRepo = licenciementRepo;
        this.indemniteRepo = indemniteRepo;
        this.ruptureConvIndemniteRepo = ruptureConvIndemniteRepo;
        this.ancienneteRepo = ancienneteRepo;
        this.titleDecisionRepo = titleDecisionRepo;
        this.workRightRepo = workRightRepo;
        this.recoursRepo = recoursRepo;
        this.partageRepo = partageRepo;
        this.gardeRepo = gardeRepo;
        this.divorceRepo = divorceRepo;
        this.changementStatutRepo = changementStatutRepo;
        this.ruptureConvAnalysisRepo = ruptureConvAnalysisRepo;
        this.harcelementNulliteRepo = harcelementNulliteRepo;
        this.discriminationRepo = discriminationRepo;
        this.licenciementEconomiqueRepo = licenciementEconomiqueRepo;
        this.pseRepo = pseRepo;
        this.inaptitudeRepo = inaptitudeRepo;
        this.licenciementNulDetectionRepo = licenciementNulDetectionRepo;
        this.indemnitePrecariteCddRepo = indemnitePrecariteCddRepo;
        this.indemniteFinMissionInterimRepo = indemniteFinMissionInterimRepo;
        this.heuresSupRepo = heuresSupRepo;
        this.rappelSalaireRepo = rappelSalaireRepo;
        this.travailDissimuleRepo = travailDissimuleRepo;
        this.requalificationCddCdiRepo = requalificationCddCdiRepo;
        this.requalificationInterimCdiRepo = requalificationInterimCdiRepo;
        this.nonConcurrenceRepo = nonConcurrenceRepo;
        this.indemnitePreavisRepo = indemnitePreavisRepo;
        this.indemniteCongesPayesRepo = indemniteCongesPayesRepo;
        this.protectionRpRepo = protectionRpRepo;
        this.transactionRepo = transactionRepo;
        this.documentsFinContratRepo = documentsFinContratRepo;
        this.atMpRepo = atMpRepo;
        this.referePrudhomalRepo = referePrudhomalRepo;
        this.contestationAreRepo = contestationAreRepo;
        this.motifGraveBeRepo = motifGraveBeRepo;
        this.avantagesConventionnelsBeRepo = avantagesConventionnelsBeRepo;
        this.creditTempsBeRepo = creditTempsBeRepo;
        this.divorceAlterationRepo = divorceAlterationRepo;
        this.divorceFauteRepo = divorceFauteRepo;
        this.divorceAccepteRepo = divorceAccepteRepo;
        this.divorceDesunionIrremediableBeRepo = divorceDesunionIrremediableBeRepo;
        this.mesuresProvisoiresRepo = mesuresProvisoiresRepo;
        this.revisionsPostDivorceRepo = revisionsPostDivorceRepo;
        this.ordonnanceProtectionRepo = ordonnanceProtectionRepo;
        this.recompensesRepo = recompensesRepo;
        this.communauteUniverselleRepo = communauteUniverselleRepo;
        this.partageJudiciaireRepo = partageJudiciaireRepo;
        this.adoptionRepo = adoptionRepo;
        this.contestationPaterniteRepo = contestationPaterniteRepo;
        this.recherchePaterniteRepo = recherchePaterniteRepo;
        this.reconnaissancePaterneleRepo = reconnaissancePaterneleRepo;
        this.possessionEtatRepo = possessionEtatRepo;
        this.autoriteParentaleRepo = autoriteParentaleRepo;
        this.changementResidenceRepo = changementResidenceRepo;
        this.desaccordsParentauxRepo = desaccordsParentauxRepo;
        this.pacsDissolutionRepo = pacsDissolutionRepo;
        this.separationCorpsRepo = separationCorpsRepo;
        this.indivisionRepo = indivisionRepo;
        this.ordonnanceRequeteRepo = ordonnanceRequeteRepo;
        this.devolutionLegaleRepo = devolutionLegaleRepo;
        this.donationRepo = donationRepo;
        this.indivisionSuccessoraleRepo = indivisionSuccessoraleRepo;
        this.partageSuccessoralRepo = partageSuccessoralRepo;
        this.rapportSuccessionRepo = rapportSuccessionRepo;
        this.reserveHereditaireRepo = reserveHereditaireRepo;
        this.testamentValiditeRepo = testamentValiditeRepo;
        this.majeursProtegesRepo = majeursProtegesRepo;
        this.changementEtatCivilRepo = changementEtatCivilRepo;
        this.pmaGpaBioethiqueRepo = pmaGpaBioethiqueRepo;
        this.oqtfAvecDelaiRepo = oqtfAvecDelaiRepo;
        this.oqtfSansDelaiRepo = oqtfSansDelaiRepo;
        this.referesAdminRepo = referesAdminRepo;
        this.aesEtudiantRepo = aesEtudiantRepo;
        this.aesFamilleRepo = aesFamilleRepo;
        this.aesHumanitaireRepo = aesHumanitaireRepo;
        this.aesMetiersTensionRepo = aesMetiersTensionRepo;
        this.asileAvanceRepo = asileAvanceRepo;
        this.naturalisationRepo = naturalisationRepo;
        this.regimeAlgerienRepo = regimeAlgerienRepo;
        this.mineursImmigrationRepo = mineursImmigrationRepo;
        this.mesuresEloignementRepo = mesuresEloignementRepo;
        this.annexe13BeRepo = annexe13BeRepo;
        this.belgian9bisRepo = belgian9bisRepo;
        this.belgian9terRepo = belgian9terRepo;
        this.belgian40bisRepo = belgian40bisRepo;
        this.belgian40terRepo = belgian40terRepo;
        this.retainedPisteAlignmentService = retainedPisteAlignmentService;
        this.procedureCheckAlignmentService = procedureCheckAlignmentService;
    }

    @Transactional(readOnly = true)
    public CaseFileDashboardResponse getDashboard(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");

        // Risk score from latest analysis
        Integer riskScore = null;
        String riskLevel = null;
        var latestAnalysis = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latestAnalysis.isPresent()) {
            riskScore = latestAnalysis.get().getRiskScore();
            riskLevel = latestAnalysis.get().getRiskLevel();
        }

        return new CaseFileDashboardResponse(
                caseFileId, cf.getLegalDomain(), riskScore, riskLevel,
                assembleTiles(caseFileId)
        );
    }

    /**
     * F-167 SF-167-01 — Assemble la liste générique de {@link DashboardTile} pour
     * les 10 outils pilotes. Chaque mapper est exécuté en isolation : si un
     * repository échoue (ou si la désérialisation crashe), seule la tile
     * concernée est absente — les autres restent visibles (fail-open par tile).
     *
     * <p>Ordre stable par {@code toolId} pour faciliter la lecture client.</p>
     */
    List<DashboardTile> assembleTiles(UUID caseFileId) {
        List<DashboardTile> tiles = new ArrayList<>();
        // ── SF-167-01 — pilotes ──────────────────────────────────────────────
        addSafely(tiles, () -> tileFromLicenciementAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromIndemniteComparatifAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAncienneteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromImmigrationTitleDecisionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromImmigrationWorkRightAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromImmigrationRecoursAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromChangementStatutAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromPartageImmobilierAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromCalendrierGardeAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromChecklistDivorceAnalysis(caseFileId));
        // ── SF-167-02 — extension Travail FR + BE ────────────────────────────
        addSafely(tiles, () -> tileFromRuptureConvAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromHarcelementNulliteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDiscriminationAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromLicenciementEconomiqueAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromPseAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromInaptitudeAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromLicenciementNulDetectionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromIndemnitePrecariteCddAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromIndemniteFinMissionInterimAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromHeuresSupAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRappelSalaireAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromTravailDissimuleAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRequalificationCddCdiAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRequalificationInterimCdiAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromNonConcurrenceAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromIndemnitePreavisAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromIndemniteCongesPayesAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromProtectionRpAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromTransactionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDocumentsFinContratAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAtMpAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromReferePrudhomalAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromContestationAreAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRuptureConvIndemniteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromMotifGraveBeAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAvantagesConventionnelsBeAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromCreditTempsBeAnalysis(caseFileId));
        // ── SF-167-03 — extension Famille FR + BE ────────────────────────────
        addSafely(tiles, () -> tileFromDivorceAlterationAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDivorceFauteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDivorceAccepteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDivorceDesunionIrremediableBeAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromMesuresProvisoiresAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRevisionsPostDivorceAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromOrdonnanceProtectionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRecompensesAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromCommunauteUniverselleAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromPartageJudiciaireAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAdoptionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromContestationPaterniteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRecherchePaterniteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromReconnaissancePaterneleAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromPossessionEtatAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAutoriteParentaleAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromChangementResidenceAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDesaccordsParentauxAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromPacsDissolutionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromSeparationCorpsAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromIndivisionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromOrdonnanceRequeteAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDevolutionLegaleAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromDonationAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromIndivisionSuccessoraleAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromPartageSuccessoralAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRapportSuccessionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromReserveHereditaireAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromTestamentValiditeAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromMajeursProtegesAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromChangementEtatCivilAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromPmaGpaBioethiqueAnalysis(caseFileId));
        // ── SF-167-04 — extension Immigration FR + BE ────────────────────────
        addSafely(tiles, () -> tileFromOqtfAvecDelaiAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromOqtfSansDelaiAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromReferesAdminAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAesEtudiantAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAesFamilleAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAesHumanitaireAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAesMetiersTensionAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAsileAvanceAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromNaturalisationAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromRegimeAlgerienAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromMineursImmigrationAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromMesuresEloignementAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromAnnexe13BeAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromBelgian9bisAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromBelgian9terAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromBelgian40bisAnalysis(caseFileId));
        addSafely(tiles, () -> tileFromBelgian40terAnalysis(caseFileId));
        // ── F-192 SF-192-01 — pistes RETAINED matérialisées ───────────────────
        addSafely(tiles, () -> tileFromRetainedPistesAlignment(caseFileId));
        // ── F-193 SF-193-01 — checks F-96 matérialisés ─────────────────────
        addSafely(tiles, () -> tileFromProcedureChecksAlignment(caseFileId));
        tiles.sort(Comparator.comparing(DashboardTile::toolId));
        return tiles;
    }

    /**
     * F-192 SF-192-01 — Tile dashboard agrégeant les pistes stratégiques
     * RETAINED matérialisées sur la dernière analyse {@code DONE} du dossier.
     *
     * <ul>
     *   <li>{@code alertLevel = ALERT} si ≥ 1 piste {@code DIVERGENT}</li>
     *   <li>{@code alertLevel = WARNING} si 0 {@code DIVERGENT} mais ≥ 1
     *       {@code NOT_ANALYZED}</li>
     *   <li>{@code alertLevel = OK} sinon</li>
     * </ul>
     *
     * <p>Renvoie {@code null} si aucune analyse {@code DONE} ou si l'alignement
     * matérialisé est vide (analyse legacy pré-F-192 ou run dans lequel la
     * matérialisation a échoué fail-open).</p>
     */
    private DashboardTile tileFromRetainedPistesAlignment(UUID caseFileId) {
        if (retainedPisteAlignmentService == null || analysisRepository == null) return null;
        var latest = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latest.isEmpty()) return null;
        List<RetainedPisteAlignment> alignments = retainedPisteAlignmentService
                .deserializeAlignment(latest.get().getRetainedPistesAlignmentJson());
        if (alignments == null || alignments.isEmpty()) return null;

        long divergent = alignments.stream()
                .filter(a -> RetainedPisteAlignment.STATUS_DIVERGENT.equals(a.matchStatus()))
                .count();
        long notAnalyzed = alignments.stream()
                .filter(a -> RetainedPisteAlignment.STATUS_NOT_ANALYZED.equals(a.matchStatus()))
                .count();

        String alertLevel;
        if (divergent > 0) alertLevel = "ALERT";
        else if (notAnalyzed > 0) alertLevel = "WARNING";
        else alertLevel = "OK";

        String primary = alignments.size() + " retenue" + (alignments.size() > 1 ? "s" : "");
        String secondary = divergent + " en divergence";

        return new DashboardTile(
                "F-192-retained-pistes-summary",
                "DIAGNOSTIC",
                "Pistes stratégiques retenues",
                primary,
                secondary,
                alertLevel);
    }

    /**
     * F-193 SF-193-01 — Tile dashboard agrégeant les points procéduraux F-96
     * matérialisés sur la dernière analyse {@code DONE} du dossier.
     *
     * <ul>
     *   <li>{@code alertLevel = ALERT} si ≥ 1 check {@code NON_COMPLIANT_FLAG}</li>
     *   <li>{@code alertLevel = WARNING} si 0 NON_COMPLIANT_FLAG mais ≥ 1
     *       {@code TO_VERIFY_FLAG}</li>
     *   <li>{@code alertLevel = OK} sinon</li>
     * </ul>
     *
     * <p>Thème {@code DELAIS} (vs {@code DIAGNOSTIC} pour F-192) — les
     * vérifications procédurales relèvent plus des délais que du diagnostic
     * (cf. mini-spec § Notes et décisions).</p>
     *
     * <p>Renvoie {@code null} si aucune analyse {@code DONE} ou si l'alignement
     * matérialisé est vide (analyse legacy pré-F-193 ou run dans lequel la
     * matérialisation a échoué fail-open).</p>
     */
    private DashboardTile tileFromProcedureChecksAlignment(UUID caseFileId) {
        if (procedureCheckAlignmentService == null || analysisRepository == null) return null;
        var latest = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latest.isEmpty()) return null;
        List<ProcedureCheckAlignment> alignments = procedureCheckAlignmentService
                .deserializeAlignment(latest.get().getProcedureChecksAlignmentJson());
        if (alignments == null || alignments.isEmpty()) return null;

        long nonCompliant = alignments.stream()
                .filter(a -> ProcedureCheckAlignment.STATUS_NON_COMPLIANT_FLAG.equals(a.matchStatus()))
                .count();
        long toVerify = alignments.stream()
                .filter(a -> ProcedureCheckAlignment.STATUS_TO_VERIFY_FLAG.equals(a.matchStatus()))
                .count();

        String alertLevel;
        if (nonCompliant > 0) alertLevel = "ALERT";
        else if (toVerify > 0) alertLevel = "WARNING";
        else alertLevel = "OK";

        int total = alignments.size();
        String primary = total + " point" + (total > 1 ? "s" : "");
        String secondary = nonCompliant + " non conforme" + (nonCompliant > 1 ? "s" : "")
                + " · " + toVerify + " à vérifier";

        return new DashboardTile(
                "F-193-procedure-checks-summary",
                "DELAIS",
                "Conformité procédurale",
                primary,
                secondary,
                alertLevel);
    }

    private void addSafely(List<DashboardTile> tiles, Supplier<DashboardTile> supplier) {
        try {
            DashboardTile t = supplier.get();
            if (t != null) {
                tiles.add(t);
            }
        } catch (Exception e) {
            log.warn("F-167 SF-167-01 — fail-open per tile: {}", e.toString());
        }
    }

    // ---- Mappers par outil pilote ------------------------------------------

    private DashboardTile tileFromLicenciementAnalysis(UUID caseFileId) {
        return licenciementRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), LicenciementAnalysisResult.class);
                int nonConformes = (int) r.criteres().stream().filter(c -> "NON".equals(c.reponse())).count();
                String alertLevel = "VALIDE".equals(r.verdict()) ? "OK" : "ALERT";
                return new DashboardTile(
                        "F-DT-08-licenciement-validity",
                        "VALIDITE",
                        "Validité licenciement",
                        r.verdict(),
                        nonConformes + "/" + r.criteres().size() + " critères non conformes",
                        alertLevel);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private DashboardTile tileFromIndemniteComparatifAnalysis(UUID caseFileId) {
        // F-167 SF-167-05 — lecture directe (logique précédemment hébergée dans
        // buildIndemnite() supprimé). Préserve la priorité RuptureConv > Macron
        // observée par SF-132-02 : si une analyse "Indemnité rupture
        // conventionnelle" existe, elle prime sur la fourchette Macron
        // (laquelle retournerait 0—0 € sur ce type de dossier).
        BigDecimal basse;
        BigDecimal haute;
        String baremeSource;

        var ruptureConvOpt = ruptureConvIndemniteRepo.findByCaseFileId(caseFileId);
        if (ruptureConvOpt.isPresent()) {
            try {
                var r = objectMapper.readValue(ruptureConvOpt.get().getResultData(), RuptureConvIndemniteResult.class);
                basse = r.indemniteLegaleMinimum();
                haute = r.indemniteLegaleMinimum();
                baremeSource = "Indemnité légale de licenciement (art. R1234-2)";
            } catch (Exception ex) {
                return null;
            }
        } else {
            var indemniteOpt = indemniteRepo.findByCaseFileId(caseFileId);
            if (indemniteOpt.isEmpty()) {
                return null;
            }
            try {
                var r = objectMapper.readValue(indemniteOpt.get().getResultData(), IndemniteComparatifResult.class);
                // SF-132-03 : legacy NEGOCIATION_LIBRE (rupture amiable BE) —
                // la card avait affiché "0 — 0 €" à tort avant la refonte.
                // L'outil dédié vit désormais côté frontend.
                if ("NEGOCIATION_LIBRE".equals(r.displayMode())) {
                    return null;
                }
                basse = r.fourchetteBasseMontant();
                haute = r.fourhetteHauteMontant();
                baremeSource = r.baremeSource();
            } catch (Exception ex) {
                return null;
            }
        }

        String primary = formatEuros(basse) + " – " + formatEuros(haute) + " €";
        return new DashboardTile(
                "F-DT-09-comparateur-indemnites",
                "INDEMNITES",
                "Indemnités",
                primary,
                baremeSource,
                null);
    }

    private DashboardTile tileFromAncienneteAnalysis(UUID caseFileId) {
        return ancienneteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AncienneteResult.class);
                int ecarts = (int) r.ecarts().stream().filter(ec -> "ECART".equals(ec.verdict())).count();
                String primary = r.ancienneteAnnees() + " an(s) " + r.ancienneteMois() + " mois";
                String secondary = r.congesTotalJours() + " jours congés";
                return new DashboardTile(
                        "F-DT-07-anciennete-conges-prime",
                        "INDEMNITES",
                        "Ancienneté & congés",
                        primary,
                        secondary,
                        ecarts > 0 ? "WARNING" : "OK");
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private DashboardTile tileFromImmigrationTitleDecisionAnalysis(UUID caseFileId) {
        return titleDecisionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var recs = objectMapper.readValue(e.getRecommendedTitles(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, TitleRecommendation.class));
                @SuppressWarnings("unchecked")
                List<TitleRecommendation> list = (List<TitleRecommendation>) recs;
                String primary = list.size() + " recommandation(s)";
                String secondary = list.isEmpty() ? null : list.get(0).label();
                return new DashboardTile(
                        "F-IM-05-arbre-decisionnel-titre",
                        "DIAGNOSTIC",
                        "Titre de séjour recommandé",
                        primary,
                        secondary,
                        "OK");
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private DashboardTile tileFromImmigrationWorkRightAnalysis(UUID caseFileId) {
        return workRightRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), WorkRightResult.class);
                String alert = "OUI".equals(r.droitTravail())
                        ? "OK"
                        : ("NON".equals(r.droitTravail()) ? "ALERT" : "WARNING");
                return new DashboardTile(
                        "F-IM-07-droit-au-travail",
                        "DIAGNOSTIC",
                        "Droit au travail",
                        r.droitTravail(),
                        r.titreLabel(),
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private DashboardTile tileFromImmigrationRecoursAnalysis(UUID caseFileId) {
        return recoursRepo.findByCaseFileId(caseFileId).map(e -> {
            var type = ImmigrationRecoursReferentiel.getByCode(e.getRecoursType());
            String label = type != null ? type.label() : e.getRecoursType();
            String dateLimite = e.getDateLimite() != null ? e.getDateLimite().toString() : null;
            boolean depasse = e.getDateLimite() != null && java.time.LocalDate.now().isAfter(e.getDateLimite());
            return new DashboardTile(
                    "F-IM-06-recours",
                    "DELAIS",
                    "Recours",
                    label,
                    dateLimite,
                    depasse ? "ALERT" : "OK");
        }).orElse(null);
    }

    /**
     * F-167 SF-167-01 — F-IM-11 Changement de statut.
     * <strong>Cas réel "Immigration Chen 5"</strong> : avant cette SF, l'analyse
     * persistait mais n'apparaissait pas dans le dashboard. Cette tile la rend
     * visible immédiatement.
     */
    private DashboardTile tileFromChangementStatutAnalysis(UUID caseFileId) {
        return changementStatutRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ChangementStatutResult.class);
                String primary = r.titreActuel() + " → " + r.titreEnvisage()
                        + " (" + r.verdictTransition() + ")";
                String secondary = r.dureeRestanteMois() + " mois restants";
                String alert;
                switch (r.verdictTransition() == null ? "" : r.verdictTransition()) {
                    case "ELEVEE" -> alert = "OK";
                    case "FAIBLE" -> alert = "ALERT";
                    default -> alert = "WARNING";
                }
                return new DashboardTile(
                        "F-IM-11-changement-statut",
                        "VALIDITE",
                        "Changement de statut",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private DashboardTile tileFromPartageImmobilierAnalysis(UUID caseFileId) {
        return partageRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PartageImmobilierResult.class);
                String primary = r.soulte() != null
                        ? "Soulte : " + formatEuros(r.soulte()) + " €"
                        : "—";
                String secondary = r.coutTotal() != null
                        ? "Coût total : " + formatEuros(r.coutTotal()) + " €"
                        : null;
                return new DashboardTile(
                        "F-FA-05-partage-immobilier",
                        "INDEMNITES",
                        "Partage immobilier",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private DashboardTile tileFromCalendrierGardeAnalysis(UUID caseFileId) {
        return gardeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), CalendrierGardeResult.class);
                String secondary = r.joursParAnParentA() + "j / " + r.joursParAnParentB() + "j";
                return new DashboardTile(
                        "F-FA-06-calendrier-garde",
                        "DOCUMENTS",
                        "Calendrier de garde",
                        r.gardeLabel(),
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private DashboardTile tileFromChecklistDivorceAnalysis(UUID caseFileId) {
        return divorceRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceChecklistResult.class);
                int total = r.etapesTotal() + r.piecesTotal();
                int done = r.etapesCompletees() + r.piecesPresentes();
                int pct = total > 0 ? (done * 100) / total : 0;
                String primary = r.etapesCompletees() + "/" + r.etapesTotal() + " étapes";
                String secondary = pct + "%";
                return new DashboardTile(
                        "F-FA-07-checklist-divorce",
                        "DIAGNOSTIC",
                        "Checklist divorce",
                        primary,
                        secondary,
                        pct < 50 ? "WARNING" : "OK");
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    // ---- SF-167-02 — Mappers Travail FR + BE -------------------------------

    /** F-DT-10 Rupture conventionnelle — validité (FR + BE). */
    private DashboardTile tileFromRuptureConvAnalysis(UUID caseFileId) {
        return ruptureConvAnalysisRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RuptureConvAnalysisResult.class);
                int nonConformes = (int) r.criteres().stream().filter(c -> "NON".equals(c.reponse())).count();
                String alert = switch (r.verdict() == null ? "" : r.verdict()) {
                    case "VALIDE" -> "OK";
                    case "INVALIDE", "RISQUE_ELEVE" -> "ALERT";
                    case "RISQUE_MODERE" -> "WARNING";
                    default -> null;
                };
                return new DashboardTile(
                        "F-DT-10-rupture-conv-validity",
                        "VALIDITE",
                        "Validité rupture conv.",
                        r.verdict(),
                        nonConformes + "/" + r.criteres().size() + " critères non conformes",
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-11 Harcèlement / licenciement nul (FR + BE). */
    private DashboardTile tileFromHarcelementNulliteAnalysis(UUID caseFileId) {
        return harcelementNulliteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), HarcelementNulliteResult.class);
                String primary = "Indemnité min. : " + formatEuros(r.indemniteMinimumNullite()) + " €";
                String motif = r.motifNullite() != null ? r.motifNullite().name() : null;
                return new DashboardTile(
                        "F-DT-11-harcelement-licenciement-nul",
                        "VALIDITE",
                        "Harcèlement / nullité",
                        primary,
                        motif,
                        "ALERT");
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-12 Discrimination — dommages-intérêts (FR + BE). */
    private DashboardTile tileFromDiscriminationAnalysis(UUID caseFileId) {
        return discriminationRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DiscriminationResult.class);
                String primary = formatEuros(r.fourchetteMin()) + " – " + formatEuros(r.fourchetteMax()) + " €";
                String secondary = r.motifDiscrimination();
                return new DashboardTile(
                        "F-DT-12-discrimination-dommages-interets",
                        "INDEMNITES",
                        "Discrimination",
                        primary,
                        secondary,
                        "WARNING");
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-13 Licenciement économique — risque de requalification (FR). */
    private DashboardTile tileFromLicenciementEconomiqueAnalysis(UUID caseFileId) {
        return licenciementEconomiqueRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), LicenciementEconomiqueResult.class);
                String verdict = r.verdictRisqueRequalification() != null
                        ? r.verdictRisqueRequalification().name()
                        : null;
                String alert = mapVerdictRisque(verdict);
                return new DashboardTile(
                        "F-DT-13-licenciement-economique",
                        "VALIDITE",
                        "Licenciement éco.",
                        verdict != null ? verdict : "—",
                        "Score : " + r.scoreGlobal() + "/100",
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-14 PSE — validité (FR). */
    private DashboardTile tileFromPseAnalysis(UUID caseFileId) {
        return pseRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PseResult.class);
                String verdict = r.verdictValidite() != null ? r.verdictValidite().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "INVALIDE" -> "ALERT";
                    default -> null;
                };
                return new DashboardTile(
                        "F-DT-14-pse-validite",
                        "VALIDITE",
                        "PSE — validité",
                        verdict != null ? verdict : "—",
                        "Score : " + r.scoreConformite() + "/100",
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-15 Inaptitude — indemnité de licenciement (FR + BE). */
    private DashboardTile tileFromInaptitudeAnalysis(UUID caseFileId) {
        return inaptitudeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), InaptitudeResult.class);
                String primary = "Total : " + formatEuros(r.total()) + " €";
                String origine = r.origineInaptitude() != null ? r.origineInaptitude().name() : null;
                String alert = r.reclassementRespecte() ? "OK" : "WARNING";
                return new DashboardTile(
                        "F-DT-15-inaptitude",
                        "INDEMNITES",
                        "Inaptitude",
                        primary,
                        origine,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-16 Détection licenciement nul (FR). */
    private DashboardTile tileFromLicenciementNulDetectionAnalysis(UUID caseFileId) {
        return licenciementNulDetectionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getSnapshotData(), LicenciementNulDetectionResult.class);
                String verdict = r.verdictProbabiliteNullite() != null
                        ? r.verdictProbabiliteNullite().name()
                        : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.nombreProtectionsActives() + " protection(s) active(s)";
                return new DashboardTile(
                        "F-DT-16-licenciement-nul-detection",
                        "VALIDITE",
                        "Détection nullité",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-17 Indemnité de précarité CDD (FR). */
    private DashboardTile tileFromIndemnitePrecariteCddAnalysis(UUID caseFileId) {
        return indemnitePrecariteCddRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), IndemnitePrecariteCddResult.class);
                String primary = formatEuros(r.indemnitePrecarite()) + " €";
                String secondary = r.casExclusion() != null && !r.casExclusion().isBlank()
                        ? "Exclusion : " + r.casExclusion()
                        : "Taux : " + r.tauxPrecarite() + " %";
                String alert = (r.casExclusion() != null && !r.casExclusion().isBlank()) ? "WARNING" : "OK";
                return new DashboardTile(
                        "F-DT-17-indemnite-precarite-cdd",
                        "INDEMNITES",
                        "Précarité CDD",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-18 Indemnité fin de mission intérim (FR). */
    private DashboardTile tileFromIndemniteFinMissionInterimAnalysis(UUID caseFileId) {
        return indemniteFinMissionInterimRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), IndemniteFinMissionInterimResult.class);
                String primary = formatEuros(r.montantIndemniteEur()) + " €";
                String secondary = r.exclusionRetenue() && r.motifExclusion() != null
                        ? "Exclusion : " + r.motifExclusion()
                        : "Taux : " + r.tauxApplique() + " %";
                String alert = r.exclusionRetenue() ? "WARNING" : "OK";
                return new DashboardTile(
                        "F-DT-18-fin-mission-interim",
                        "INDEMNITES",
                        "Fin mission intérim",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-19 Heures supplémentaires (FR + BE). */
    private DashboardTile tileFromHeuresSupAnalysis(UUID caseFileId) {
        return heuresSupRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), HeuresSupResult.class);
                String primary = formatEuros(r.rappelTotal()) + " €";
                int totalHeures = r.heuresSupDeclarees25pct() + r.heuresSupDeclarees50pct()
                        + r.heuresSupSemaine() + r.heuresDimancheJoursFeries();
                String secondary = totalHeures + " h déclarées";
                return new DashboardTile(
                        "F-DT-19-heures-sup",
                        "INDEMNITES",
                        "Heures sup.",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-20 Rappel de salaire (FR). */
    private DashboardTile tileFromRappelSalaireAnalysis(UUID caseFileId) {
        return rappelSalaireRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RappelSalaireResult.class);
                String primary = formatEuros(r.totalAvecCpEur()) + " €";
                String secondary = r.nbMoisPeriode() + " mois — " + formatEuros(r.differentielMensuelEur()) + " €/mois";
                return new DashboardTile(
                        "F-DT-20-rappel-salaire",
                        "INDEMNITES",
                        "Rappel de salaire",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-21 Travail dissimulé (FR). */
    private DashboardTile tileFromTravailDissimuleAnalysis(UUID caseFileId) {
        return travailDissimuleRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), TravailDissimuleResult.class);
                String primary = formatEuros(r.indemniteForfaitaire()) + " €";
                String secondary = "Salaire mensuel × 6 mois";
                return new DashboardTile(
                        "F-DT-21-travail-dissimule",
                        "INDEMNITES",
                        "Travail dissimulé",
                        primary,
                        secondary,
                        "WARNING");
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-22 Requalification CDD → CDI (FR). */
    private DashboardTile tileFromRequalificationCddCdiAnalysis(UUID caseFileId) {
        return requalificationCddCdiRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RequalificationCddCdiResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteRequalification());
                String secondary = formatEuros(r.totalDommagesIndemniteEur()) + " € indemnités";
                return new DashboardTile(
                        "F-DT-22-requalification-cdd-cdi",
                        "VALIDITE",
                        "Requalif. CDD → CDI",
                        r.verdictProbabiliteRequalification() != null ? r.verdictProbabiliteRequalification() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-23 Requalification intérim → CDI (FR). */
    private DashboardTile tileFromRequalificationInterimCdiAnalysis(UUID caseFileId) {
        return requalificationInterimCdiRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RequalificationInterimCdiResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteRequalification());
                String secondary = formatEuros(r.totalDommagesIndemniteEur()) + " € indemnités";
                return new DashboardTile(
                        "F-DT-23-requalification-interim-cdi",
                        "VALIDITE",
                        "Requalif. intérim → CDI",
                        r.verdictProbabiliteRequalification() != null ? r.verdictProbabiliteRequalification() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-24 Clause de non-concurrence (FR). */
    private DashboardTile tileFromNonConcurrenceAnalysis(UUID caseFileId) {
        return nonConcurrenceRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getSnapshotData(), NonConcurrenceResult.class);
                String verdict = r.verdictValidite() != null ? r.verdictValidite().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "INVALIDE", "NUL" -> "ALERT";
                    default -> null;
                };
                String secondary = "Score : " + r.scoreValidite() + "/100";
                return new DashboardTile(
                        "F-DT-24-non-concurrence",
                        "VALIDITE",
                        "Non-concurrence",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-25 Indemnité compensatrice de préavis (FR). */
    private DashboardTile tileFromIndemnitePreavisAnalysis(UUID caseFileId) {
        return indemnitePreavisRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), IndemnitePreavisResult.class);
                String primary = formatEuros(r.montantIndemniteEur()) + " €";
                String secondary = r.dureePreavisMois() + " mois de préavis"
                        + (r.exemptionRetenue() ? " (exemption retenue)" : "");
                return new DashboardTile(
                        "F-DT-25-indemnite-preavis",
                        "INDEMNITES",
                        "Indemnité préavis",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-26 Indemnité compensatrice de congés payés (FR). */
    private DashboardTile tileFromIndemniteCongesPayesAnalysis(UUID caseFileId) {
        return indemniteCongesPayesRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), IndemniteCongesPayesResult.class);
                String primary = formatEuros(r.montantIndemniteEur()) + " €";
                String secondary = r.joursDus() + " jours dus — méthode "
                        + (r.methodeRetenue() != null ? r.methodeRetenue().name() : "?");
                return new DashboardTile(
                        "F-DT-26-conges-payes-indemnite",
                        "INDEMNITES",
                        "Congés payés",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-30 Protection représentants du personnel (FR). */
    private DashboardTile tileFromProtectionRpAnalysis(UUID caseFileId) {
        return protectionRpRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ProtectionRpResult.class);
                String verdict = r.verdictLegalite() != null ? r.verdictLegalite().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "NUL" -> "ALERT";
                    default -> null;
                };
                String secondary = "Score : " + r.scoreConformite() + "/100";
                return new DashboardTile(
                        "F-DT-30-protection-rp",
                        "VALIDITE",
                        "Protection RP",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-31 Transaction — validité du protocole (FR + BE). */
    private DashboardTile tileFromTransactionAnalysis(UUID caseFileId) {
        return transactionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getSnapshotData(), TransactionResult.class);
                String verdict = r.verdictValiditeContrat() != null ? r.verdictValiditeContrat().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "INVALIDE", "NUL" -> "ALERT";
                    default -> null;
                };
                String secondary = "Score : " + r.scoreValidite() + "/100";
                return new DashboardTile(
                        "F-DT-31-transaction",
                        "INDEMNITES",
                        "Transaction",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-32 Documents de fin de contrat (FR). */
    private DashboardTile tileFromDocumentsFinContratAnalysis(UUID caseFileId) {
        return documentsFinContratRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getSnapshotData(), DocumentsFinContratResult.class);
                String verdict = r.verdictRisqueContentieux() != null
                        ? r.verdictRisqueContentieux().name()
                        : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.totalSanctionsCumulables() + " sanction(s) cumulable(s)";
                return new DashboardTile(
                        "F-DT-32-documents-fin-contrat",
                        "DOCUMENTS",
                        "Docs fin de contrat",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-33 Accident du travail / Maladie professionnelle (FR). */
    private DashboardTile tileFromAtMpAnalysis(UUID caseFileId) {
        return atMpRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AtMpResult.class);
                String alert = mapVerdictRisque(r.verdictRecevabilite());
                String secondary = r.delaiInstructionJours() + " j d'instruction (" + r.competence() + ")";
                return new DashboardTile(
                        "F-DT-33-at-mp",
                        "DELAIS",
                        "AT/MP",
                        r.dispositifLibelle() != null ? r.dispositifLibelle() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-34 Référé prud'homal (FR). */
    private DashboardTile tileFromReferePrudhomalAnalysis(UUID caseFileId) {
        return referePrudhomalRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ReferePrudhomalResult.class);
                String alert = mapVerdictRisque(r.verdictRecommandation());
                String secondary = "Audience : ~" + r.delaiAudienceJoursPrevisionnel() + " j";
                return new DashboardTile(
                        "F-DT-34-refere-prudhomal",
                        "DELAIS",
                        "Référé prud'homal",
                        r.verdictRecommandation() != null ? r.verdictRecommandation() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-35 Contestation décision France Travail / ARE (FR). */
    private DashboardTile tileFromContestationAreAnalysis(UUID caseFileId) {
        return contestationAreRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getSnapshotData(), ContestationAreResult.class);
                String alert = mapVerdictRisque(r.verdictRecommandation());
                String secondary = "Score réussite : " + r.scoreSuccessProbable() + "/100";
                return new DashboardTile(
                        "F-DT-35-contestation-are-fr",
                        "INDEMNITES",
                        "Contestation ARE",
                        r.verdictRecommandation() != null ? r.verdictRecommandation() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-132 Indemnité légale de rupture conventionnelle (FR).
     * Réutilise le repo {@code ruptureConvIndemniteRepo} déjà injecté pour la
     * priorité RuptureConv > Macron de
     * {@link #tileFromIndemniteComparatifAnalysis(UUID)}.
     */
    private DashboardTile tileFromRuptureConvIndemniteAnalysis(UUID caseFileId) {
        return ruptureConvIndemniteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RuptureConvIndemniteResult.class);
                String primary = formatEuros(r.indemniteLegaleMinimum()) + " €";
                String secondary = r.ancienneteAnnees() + " an(s) — " + formatEuros(r.salaireMensuel()) + " €/mois";
                return new DashboardTile(
                        "F-132-rupture-conv-indemnite",
                        "INDEMNITES",
                        "Indemnité rupture conv.",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-27 Motif grave BE — validité procédurale (BE). */
    private DashboardTile tileFromMotifGraveBeAnalysis(UUID caseFileId) {
        return motifGraveBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), MotifGraveBeResult.class);
                String primary = r.motifGraveProceduralementValide() ? "VALIDE" : "INVALIDE";
                String alert = r.motifGraveProceduralementValide() ? "OK" : "ALERT";
                String secondary = "Délai rupture : " + r.delaiRuptureJoursOuvrables() + " j ouvrables";
                return new DashboardTile(
                        "F-DT-27-motif-grave-be",
                        "VALIDITE",
                        "Motif grave BE",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-28 Avantages conventionnels BE (BE). */
    private DashboardTile tileFromAvantagesConventionnelsBeAnalysis(UUID caseFileId) {
        return avantagesConventionnelsBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AvantagesConventionnelsBeResult.class);
                String primary = formatEuros(r.totalAvantagesAnnuelsEur()) + " €";
                String secondary = "CP " + r.commissionParitaire() + " — " + r.annee();
                return new DashboardTile(
                        "F-DT-28-avantages-conventionnels-be",
                        "INDEMNITES",
                        "Avantages conv. BE",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-DT-29 Crédit-temps BE — éligibilité (BE). */
    private DashboardTile tileFromCreditTempsBeAnalysis(UUID caseFileId) {
        return creditTempsBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), CreditTempsBeResult.class);
                String alert = mapVerdictRisque(r.verdictEligibilite());
                if (alert == null) {
                    alert = r.eligible() ? "OK" : "ALERT";
                }
                String secondary = (r.indemniteOnemMensuelle() != null
                        ? formatEuros(r.indemniteOnemMensuelle()) + " €/mois ONEM — "
                        : "")
                        + r.dureeMaximaleMois() + " mois max.";
                return new DashboardTile(
                        "F-DT-29-credit-temps-be",
                        "DELAIS",
                        "Crédit-temps BE",
                        r.verdictEligibilite() != null ? r.verdictEligibilite() : (r.eligible() ? "ELIGIBLE" : "NON_ELIGIBLE"),
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    // ---- SF-167-03 — Mappers Famille FR + BE -------------------------------

    /** F-FA-08 Divorce pour altération définitive du lien conjugal (FR). */
    private DashboardTile tileFromDivorceAlterationAnalysis(UUID caseFileId) {
        return divorceAlterationRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceAlterationResult.class);
                String alert = mapVerdictRisque(r.verdictProbabilite());
                String secondary = "Score : " + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-FA-08-divorce-alteration",
                        "VALIDITE",
                        "Divorce pour altération",
                        r.verdictProbabilite() != null ? r.verdictProbabilite() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-09 Divorce pour faute (FR). */
    private DashboardTile tileFromDivorceFauteAnalysis(UUID caseFileId) {
        return divorceFauteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceFauteResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteDivorceFaute());
                String secondary = r.nombreFautesInvoquees() + " faute(s) invoquée(s) — score "
                        + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-FA-09-divorce-faute",
                        "VALIDITE",
                        "Divorce pour faute",
                        r.verdictProbabiliteDivorceFaute() != null ? r.verdictProbabiliteDivorceFaute() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-10 Divorce accepté (FR). */
    private DashboardTile tileFromDivorceAccepteAnalysis(UUID caseFileId) {
        return divorceAccepteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceAccepteResult.class);
                String alert = mapVerdictRisque(r.verdictEligibilite());
                String secondary = r.delaiProcedureMoisPrevisionnel() + " mois — score "
                        + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-FA-10-divorce-accepte",
                        "VALIDITE",
                        "Divorce accepté",
                        r.verdictEligibilite() != null ? r.verdictEligibilite() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-11 Divorce désunion irrémédiable (BE). */
    private DashboardTile tileFromDivorceDesunionIrremediableBeAnalysis(UUID caseFileId) {
        return divorceDesunionIrremediableBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceDesunionIrremediableBeResult.class);
                String alert = mapVerdictRisque(r.verdictProbabilite());
                String secondary = r.dureeSeparationMois() + "/" + r.seuilSeparationMois() + " mois";
                return new DashboardTile(
                        "F-FA-11-desunion-irremediable-be",
                        "VALIDITE",
                        "Désunion irrémédiable BE",
                        r.verdictProbabilite() != null ? r.verdictProbabilite() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-12 Mesures provisoires (FR). */
    private DashboardTile tileFromMesuresProvisoiresAnalysis(UUID caseFileId) {
        return mesuresProvisoiresRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), MesuresProvisoiresResult.class);
                String alert = mapVerdictRisque(r.verdictAcceptabilite());
                String primary = r.dateAudienceAOMP() != null
                        ? "Audience : " + r.dateAudienceAOMP().toString()
                        : "—";
                String secondary = r.pensionAlimentairePropose() != null
                        ? "Pension : " + formatEuros(r.pensionAlimentairePropose()) + " €/mois"
                        : "Score : " + r.scoreCohesionMesures() + "/100";
                return new DashboardTile(
                        "F-FA-12-mesures-provisoires",
                        "DELAIS",
                        "Mesures provisoires",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-13 Révisions post-divorce (FR). */
    private DashboardTile tileFromRevisionsPostDivorceAnalysis(UUID caseFileId) {
        return revisionsPostDivorceRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RevisionsPostDivorceResult.class);
                String alert = mapVerdictRisque(r.verdictRevisionPossible());
                String secondary = "Score : " + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-FA-13-revisions-post-divorce",
                        "DELAIS",
                        "Révisions post-divorce",
                        r.verdictRevisionPossible() != null ? r.verdictRevisionPossible() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-14 Ordonnance de protection (FR). */
    private DashboardTile tileFromOrdonnanceProtectionAnalysis(UUID caseFileId) {
        return ordonnanceProtectionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), OrdonnanceProtectionResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteOctroi());
                String secondary = "Délai : ~" + r.delaiTraitementJoursPrevisionnel() + " j — score "
                        + r.scoreVraisemblance() + "/100";
                return new DashboardTile(
                        "F-FA-14-ordonnance-protection",
                        "DELAIS",
                        "Ordonnance protection",
                        r.verdictProbabiliteOctroi() != null ? r.verdictProbabiliteOctroi() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-15 Récompenses entre époux (FR). */
    private DashboardTile tileFromRecompensesAnalysis(UUID caseFileId) {
        return recompensesRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RecompensesResult.class);
                String primary = "Solde net : " + formatEuros(r.soldeNetPourEpouxEur()) + " €";
                String secondary = r.recompenses() != null
                        ? r.recompenses().size() + " opération(s)"
                        : null;
                return new DashboardTile(
                        "F-FA-15-recompenses",
                        "INDEMNITES",
                        "Récompenses",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-16 Communauté universelle (FR). */
    private DashboardTile tileFromCommunauteUniverselleAnalysis(UUID caseFileId) {
        return communauteUniverselleRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), CommunauteUniverselleResult.class);
                String verdict = r.verdictValidite() != null ? r.verdictValidite().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "NUL" -> "ALERT";
                    default -> null;
                };
                String secondary = "Score : " + r.scoreValidite() + "/100";
                return new DashboardTile(
                        "F-FA-16-communaute-universelle",
                        "DIAGNOSTIC",
                        "Communauté universelle",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-17 Partage judiciaire (FR). */
    private DashboardTile tileFromPartageJudiciaireAnalysis(UUID caseFileId) {
        return partageJudiciaireRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PartageJudiciaireResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.dureeProcedureMois() + " mois — score "
                        + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-17-partage-judiciaire",
                        "DIAGNOSTIC",
                        "Partage judiciaire",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-18-adoption Recevabilité d'une adoption (FR). */
    private DashboardTile tileFromAdoptionAnalysis(UUID caseFileId) {
        return adoptionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AdoptionResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.formeRecommandee() != null
                        ? "Forme : " + r.formeRecommandee().name()
                        : null;
                return new DashboardTile(
                        "F-FA-18-adoption",
                        "DIAGNOSTIC",
                        "Adoption",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-18-contestation-paternite Contestation de paternité (FR). */
    private DashboardTile tileFromContestationPaterniteAnalysis(UUID caseFileId) {
        return contestationPaterniteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ContestationPaterniteResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.delaiPrescriptionRestantMois() + " mois restants";
                return new DashboardTile(
                        "F-FA-18-contestation-paternite",
                        "VALIDITE",
                        "Contestation paternité",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-18-recherche-paternite Recherche de paternité (FR). */
    private DashboardTile tileFromRecherchePaterniteAnalysis(UUID caseFileId) {
        return recherchePaterniteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RecherchePaterniteResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.delaiPrescriptionRestantMois() + " mois restants";
                return new DashboardTile(
                        "F-FA-18-recherche-paternite",
                        "VALIDITE",
                        "Recherche paternité",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-18-reconnaissance-paternelle Reconnaissance paternelle (FR). */
    private DashboardTile tileFromReconnaissancePaterneleAnalysis(UUID caseFileId) {
        return reconnaissancePaterneleRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ReconnaissancePaterneleResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = "Score : " + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-18-reconnaissance-paternelle",
                        "VALIDITE",
                        "Reconnaissance paternelle",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-18-possession-etat Possession d'état (FR). */
    private DashboardTile tileFromPossessionEtatAnalysis(UUID caseFileId) {
        return possessionEtatRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PossessionEtatResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.dureePossessionAnnees() + " an(s) de possession";
                return new DashboardTile(
                        "F-FA-18-possession-etat",
                        "VALIDITE",
                        "Possession d'état",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-19-autorite-parentale Autorité parentale (FR). */
    private DashboardTile tileFromAutoriteParentaleAnalysis(UUID caseFileId) {
        return autoriteParentaleRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AutoriteParentaleResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String secondary = "Score : " + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-19-autorite-parentale",
                        "DIAGNOSTIC",
                        "Autorité parentale",
                        r.verdictProbabiliteAcceptation() != null ? r.verdictProbabiliteAcceptation() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-19-changement-residence Changement de résidence (FR). */
    private DashboardTile tileFromChangementResidenceAnalysis(UUID caseFileId) {
        return changementResidenceRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ChangementResidenceResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String secondary = r.distanceKm() + " km — score " + r.scoreAcceptabilite() + "/100";
                return new DashboardTile(
                        "F-FA-19-changement-residence",
                        "DIAGNOSTIC",
                        "Changement résidence",
                        r.verdictProbabiliteAcceptation() != null ? r.verdictProbabiliteAcceptation() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-19-desaccords-parentaux Désaccords parentaux (FR). */
    private DashboardTile tileFromDesaccordsParentauxAnalysis(UUID caseFileId) {
        return desaccordsParentauxRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DesaccordsParentauxResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String secondary = "Score JAF : " + r.scoreEligibiliteJaf() + "/100";
                return new DashboardTile(
                        "F-FA-19-desaccords-parentaux",
                        "DIAGNOSTIC",
                        "Désaccords parentaux",
                        r.verdictProbabiliteAcceptation() != null ? r.verdictProbabiliteAcceptation() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-20 Dissolution PACS (FR). */
    private DashboardTile tileFromPacsDissolutionAnalysis(UUID caseFileId) {
        return pacsDissolutionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PacsDissolutionResult.class);
                String alert = mapVerdictRisque(r.verdictRecommandation());
                String secondary = r.dureeUnionAnnees() + " an(s) — score créances "
                        + r.scoreCreancesProbables() + "/100";
                return new DashboardTile(
                        "F-FA-20-pacs-dissolution",
                        "DIAGNOSTIC",
                        "Dissolution PACS",
                        r.verdictRecommandation() != null ? r.verdictRecommandation() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-21 Séparation de corps + conversion divorce (FR). */
    private DashboardTile tileFromSeparationCorpsAnalysis(UUID caseFileId) {
        return separationCorpsRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), SeparationCorpsResult.class);
                String alert = mapVerdictRisque(r.verdictConversion());
                String secondary = r.dureeSeparationAnnees() + " an(s) — score "
                        + r.scoreEligibiliteConversion() + "/100";
                return new DashboardTile(
                        "F-FA-21-separation-corps",
                        "DIAGNOSTIC",
                        "Séparation de corps",
                        r.verdictConversion() != null ? r.verdictConversion() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-22 Indivision (FR). */
    private DashboardTile tileFromIndivisionAnalysis(UUID caseFileId) {
        return indivisionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), IndivisionResult.class);
                String alert = mapVerdictRisque(r.verdictRecommandation());
                String secondary = r.nbIndivisaires() + " indivisaire(s) — score "
                        + r.scoreEligibilitePartageJudiciaire() + "/100";
                return new DashboardTile(
                        "F-FA-22-indivision",
                        "DIAGNOSTIC",
                        "Indivision",
                        r.verdictRecommandation() != null ? r.verdictRecommandation() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-23 Ordonnance sur requête (FR). */
    private DashboardTile tileFromOrdonnanceRequeteAnalysis(UUID caseFileId) {
        return ordonnanceRequeteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), OrdonnanceRequeteResult.class);
                String verdict = r.verdictAccordeProbabilite() != null
                        ? r.verdictAccordeProbabilite().name()
                        : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = "Délai : " + r.delaiTypiqueJoursMin() + "–"
                        + r.delaiTypiqueJoursMax() + " j";
                return new DashboardTile(
                        "F-FA-23-ordonnance-requete",
                        "DELAIS",
                        "Ordonnance sur requête",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-24-devolution-legale Dévolution légale successorale (FR). */
    private DashboardTile tileFromDevolutionLegaleAnalysis(UUID caseFileId) {
        return devolutionLegaleRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DevolutionLegaleResult.class);
                String primary = r.ordreActif() != null ? r.ordreActif().name() : "—";
                String secondary = (r.heritiersDesignes() != null
                        ? r.heritiersDesignes().size()
                        : 0) + " héritier(s) — score " + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-24-devolution-legale",
                        "DIAGNOSTIC",
                        "Dévolution légale",
                        primary,
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-24-donation Donation entre vifs (FR). */
    private DashboardTile tileFromDonationAnalysis(UUID caseFileId) {
        return donationRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DonationResult.class);
                String verdict = r.verdictValidite() != null ? r.verdictValidite().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "NUL" -> "ALERT";
                    default -> null;
                };
                String secondary = "Score : " + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-24-donation",
                        "DIAGNOSTIC",
                        "Donation",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-24-indivision-successorale Indivision successorale (FR). */
    private DashboardTile tileFromIndivisionSuccessoraleAnalysis(UUID caseFileId) {
        return indivisionSuccessoraleRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), IndivisionSuccessoraleResult.class);
                String secondary = r.nbHeritiers() + " héritier(s) — conflit "
                        + r.scoreConflictualite() + "/100";
                return new DashboardTile(
                        "F-FA-24-indivision-successorale",
                        "DIAGNOSTIC",
                        "Indivision successorale",
                        r.verdictGestion() != null ? r.verdictGestion() : "—",
                        secondary,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-24-partage-successoral Partage successoral (FR). */
    private DashboardTile tileFromPartageSuccessoralAnalysis(UUID caseFileId) {
        return partageSuccessoralRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PartageSuccessoralResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = mapVerdictRisque(verdict);
                String secondary = r.delaiInstructionMois() + " mois — score "
                        + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-24-partage-successoral",
                        "DIAGNOSTIC",
                        "Partage successoral",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-24-rapport-succession Rapport à succession (FR). */
    private DashboardTile tileFromRapportSuccessionAnalysis(UUID caseFileId) {
        return rapportSuccessionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RapportSuccessionResult.class);
                String verdict = r.verdictObligation() != null ? r.verdictObligation().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "RAPPORTABLE" -> "WARNING";
                    case "EXEMPT", "DISPENSÉ", "NON_OBLIGÉ" -> "OK";
                    default -> null;
                };
                String secondary = r.montantRapportable() != null
                        ? "Rapportable : " + formatEuros(r.montantRapportable()) + " €"
                        : "Score : " + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-24-rapport-succession",
                        "DIAGNOSTIC",
                        "Rapport à succession",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-24-reserve-heriditaire Réserve héréditaire (FR). */
    private DashboardTile tileFromReserveHereditaireAnalysis(UUID caseFileId) {
        return reserveHereditaireRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ReserveHereditaireResult.class);
                String verdict = r.verdictRecevabilite() != null ? r.verdictRecevabilite().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "RECEVABLE" -> "WARNING";
                    case "NON_RECEVABLE_PAS_EXCEDENT", "NON_RECEVABLE_PRESCRIPTION",
                         "NON_RECEVABLE_QUALITE", "NON_RECEVABLE" -> "OK";
                    default -> null;
                };
                String secondary = r.excedentReductibleEur() != null
                        ? "Excédent : " + formatEuros(r.excedentReductibleEur()) + " €"
                        : "Score : " + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-24-reserve-heriditaire",
                        "DIAGNOSTIC",
                        "Réserve héréditaire",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-24-testament-validite Validité testament (FR). */
    private DashboardTile tileFromTestamentValiditeAnalysis(UUID caseFileId) {
        return testamentValiditeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), TestamentValiditeResult.class);
                String verdict = r.verdictValidite() != null ? r.verdictValidite().name() : null;
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "NUL" -> "ALERT";
                    default -> null;
                };
                String secondary = "Score : " + r.scoreEligibilite() + "/100";
                return new DashboardTile(
                        "F-FA-24-testament-validite",
                        "VALIDITE",
                        "Validité testament",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-25 Majeurs protégés (FR). */
    private DashboardTile tileFromMajeursProtegesAnalysis(UUID caseFileId) {
        return majeursProtegesRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), MajeursProtegesResult.class);
                String alert = mapVerdictRisque(r.verdictAcceptabiliteJaf());
                String secondary = "Régime conseillé : "
                        + (r.regimeOptimalRecommande() != null ? r.regimeOptimalRecommande() : "—");
                return new DashboardTile(
                        "F-FA-25-majeurs-proteges",
                        "DIAGNOSTIC",
                        "Majeurs protégés",
                        r.verdictAcceptabiliteJaf() != null ? r.verdictAcceptabiliteJaf() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-26 Changement d'état civil (FR). */
    private DashboardTile tileFromChangementEtatCivilAnalysis(UUID caseFileId) {
        return changementEtatCivilRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ChangementEtatCivilResult.class);
                String alert = mapVerdictRisque(r.verdictAcceptabilite());
                String secondary = r.delaiInstructionMoisPrevisionnel() + " mois — score "
                        + r.scoreAcceptabilite() + "/100";
                return new DashboardTile(
                        "F-FA-26-changement-etat-civil",
                        "DIAGNOSTIC",
                        "Changement état civil",
                        r.verdictAcceptabilite() != null ? r.verdictAcceptabilite() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** F-FA-27 PMA / GPA / bioéthique (FR). */
    private DashboardTile tileFromPmaGpaBioethiqueAnalysis(UUID caseFileId) {
        return pmaGpaBioethiqueRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PmaGpaBioethiqueResult.class);
                String alert = mapVerdictRisque(r.verdictRecevabilite());
                String secondary = r.dispositif() + " — " + r.delaiInstructionMois() + " mois";
                return new DashboardTile(
                        "F-FA-27-pma-gpa",
                        "DIAGNOSTIC",
                        "PMA / GPA / bioéthique",
                        r.verdictRecevabilite() != null ? r.verdictRecevabilite() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    // ---- SF-167-04 — Mappers Immigration FR + BE ---------------------------

    /** F-IM-08 OQTF avec délai de départ volontaire (FR). */
    private DashboardTile tileFromOqtfAvecDelaiAnalysis(UUID caseFileId) {
        return oqtfAvecDelaiRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), OqtfAvecDelaiResult.class);
                String alert = switch (r.statutDelaiRecours() == null ? "" : r.statutDelaiRecours()) {
                    case "DANS_DELAI" -> "OK";
                    case "URGENT" -> "WARNING";
                    case "EXPIRE", "FORCLOS" -> "ALERT";
                    default -> null;
                };
                String primary = r.statutDelaiRecours() != null ? r.statutDelaiRecours() : "—";
                String secondary = r.joursRestantsAvantExpirationDelai() + " j restants";
                return new DashboardTile(
                        "F-IM-08-oqtf-avec-delai",
                        "DELAIS",
                        "OQTF avec délai",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-08 OQTF sans délai (FR — extrême urgence 48h). */
    private DashboardTile tileFromOqtfSansDelaiAnalysis(UUID caseFileId) {
        return oqtfSansDelaiRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), OqtfSansDelaiResult.class);
                String alert = switch (r.statutDelaiRecours() == null ? "" : r.statutDelaiRecours()) {
                    case "DANS_DELAI" -> "OK";
                    case "URGENT" -> "WARNING";
                    case "EXPIRE", "FORCLOS" -> "ALERT";
                    default -> "ALERT";
                };
                String primary = r.statutDelaiRecours() != null ? r.statutDelaiRecours() : "—";
                String secondary = r.heuresRestantes() + " h restantes (recours 48h)";
                return new DashboardTile(
                        "F-IM-08-oqtf-sans-delai",
                        "DELAIS",
                        "OQTF sans délai",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-08 Référés administratifs combinés (FR — L.521-1 + L.521-2 CJA). */
    private DashboardTile tileFromReferesAdminAnalysis(UUID caseFileId) {
        return referesAdminRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), ReferesAdminResult.class);
                String alert = mapVerdictRisque(r.verdictRecommandation());
                String primary = r.verdictRecommandation() != null ? r.verdictRecommandation() : "—";
                String secondary = "Suspension : " + r.scoreSuccessProbabiliteSuspension()
                        + "/100 — Liberté : " + r.scoreSuccessProbabiliteLiberte() + "/100";
                return new DashboardTile(
                        "F-IM-08-referes-admin",
                        "DELAIS",
                        "Référés admin",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-09 AES voie étudiante (FR — circulaire Valls/Darmanin). */
    private DashboardTile tileFromAesEtudiantAnalysis(UUID caseFileId) {
        return aesEtudiantRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AesEtudiantResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String primary = r.verdictProbabiliteAcceptation() != null
                        ? r.verdictProbabiliteAcceptation()
                        : "—";
                String secondary = "Score : " + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-IM-09-aes-etudiant",
                        "DIAGNOSTIC",
                        "AES — voie étudiante",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-09 AES voie familiale (FR — art. L.435-1 CESEDA). */
    private DashboardTile tileFromAesFamilleAnalysis(UUID caseFileId) {
        return aesFamilleRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AesFamilleResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String primary = r.verdictProbabiliteAcceptation() != null
                        ? r.verdictProbabiliteAcceptation()
                        : "—";
                String secondary = "Score : " + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-IM-09-aes-famille",
                        "DIAGNOSTIC",
                        "AES — voie familiale",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-09 AES voie humanitaire (FR — art. L.435-2 CESEDA). */
    private DashboardTile tileFromAesHumanitaireAnalysis(UUID caseFileId) {
        return aesHumanitaireRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AesHumanitaireResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String primary = r.verdictProbabiliteAcceptation() != null
                        ? r.verdictProbabiliteAcceptation()
                        : "—";
                String motif = r.motifHumanitaireDominant() != null
                        ? r.motifHumanitaireDominant().name()
                        : "Score : " + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-IM-09-aes-humanitaire",
                        "DIAGNOSTIC",
                        "AES — voie humanitaire",
                        primary,
                        motif,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-09 AES métier en tension (FR — art. L.435-4 CESEDA). */
    private DashboardTile tileFromAesMetiersTensionAnalysis(UUID caseFileId) {
        return aesMetiersTensionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AesMetiersTensionResult.class);
                String primary = r.conditionsReunies() ? "ELIGIBLE" : "NON_ELIGIBLE";
                String alert = r.conditionsReunies() ? "OK" : "ALERT";
                String secondary = r.metierEstEnTension()
                        ? "Métier en tension : " + (r.codeMetier() != null ? r.codeMetier() : "—")
                        : "Métier hors liste tension";
                return new DashboardTile(
                        "F-IM-09-aes-metiers-tension",
                        "DIAGNOSTIC",
                        "AES — métiers en tension",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-12 Asile avancé (FR — CESEDA Livre V). */
    private DashboardTile tileFromAsileAvanceAnalysis(UUID caseFileId) {
        return asileAvanceRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AsileAvanceResult.class);
                String verdict = r.verdictRecevabilite();
                String alert = switch (verdict == null ? "" : verdict) {
                    case "FRANCE_COMPETENTE", "RECEVABLE_REEXAMEN", "RECEVABLE_TRANSFERT",
                         "RECEVABLE_APATRIDIE", "RECEVABLE_PROTECTION_SUBSIDIAIRE" -> "OK";
                    case "ACCELEREE_APPLICABLE" -> "WARNING";
                    case "IRRECEVABLE", "ACCELEREE_NON_APPLICABLE" -> "ALERT";
                    default -> null;
                };
                String primary = verdict != null ? verdict : "—";
                String secondary = r.dispositifLibelle() != null
                        ? r.dispositifLibelle() + " — " + r.delaiInstructionMois() + " mois"
                        : r.delaiInstructionMois() + " mois";
                return new DashboardTile(
                        "F-IM-12-asile-avance",
                        "DIAGNOSTIC",
                        "Asile avancé",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-13 Naturalisation française (Code civil 21+). */
    private DashboardTile tileFromNaturalisationAnalysis(UUID caseFileId) {
        return naturalisationRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), NaturalisationResult.class);
                String alert = mapVerdictRisque(r.verdictRecevabilite());
                String primary = r.verdictRecevabilite() != null ? r.verdictRecevabilite() : "—";
                String secondary = (r.voieRecommandee() != null ? r.voieRecommandee() : r.voieNaturalisation())
                        + " — " + r.delaiInstructionMois() + " mois";
                return new DashboardTile(
                        "F-IM-13-naturalisation",
                        "DIAGNOSTIC",
                        "Naturalisation",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-17 Régime franco-algérien (accord 27/12/1968). */
    private DashboardTile tileFromRegimeAlgerienAnalysis(UUID caseFileId) {
        return regimeAlgerienRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RegimeAlgerienResult.class);
                String alert = mapVerdictRisque(r.verdictRecevabilite());
                String primary = r.verdictRecevabilite() != null ? r.verdictRecevabilite() : "—";
                String secondary = (r.voieRecommandee() != null ? r.voieRecommandee() : r.voieDemande())
                        + (r.dureeTitreAnnees() > 0 ? " — CRA " + r.dureeTitreAnnees() + " an(s)" : "");
                return new DashboardTile(
                        "F-IM-17-regime-algerien",
                        "DIAGNOSTIC",
                        "Régime algérien",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-19 Mineurs étrangers (FR). */
    private DashboardTile tileFromMineursImmigrationAnalysis(UUID caseFileId) {
        return mineursImmigrationRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), MineursImmigrationResult.class);
                String alert = mapVerdictRisque(r.verdictEligibilite());
                String primary = r.verdictEligibilite() != null ? r.verdictEligibilite() : "—";
                String dispositif = r.dispositifRecommande() != null
                        ? r.dispositifRecommande()
                        : r.dispositifVise();
                String secondary = (dispositif != null ? dispositif + " — " : "")
                        + r.ageAnnees() + " ans";
                return new DashboardTile(
                        "F-IM-19-mineurs-immigration",
                        "DIAGNOSTIC",
                        "Mineurs étrangers",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-20 Mesures d'éloignement (FR — Expulsion / IRTF / IAT). */
    private DashboardTile tileFromMesuresEloignementAnalysis(UUID caseFileId) {
        return mesuresEloignementRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), MesuresEloignementResult.class);
                String verdict = r.verdictLegalite();
                String alert = switch (verdict == null ? "" : verdict) {
                    case "VALIDE" -> "OK";
                    case "CONTESTABLE" -> "WARNING";
                    case "NUL", "INVALIDE" -> "ALERT";
                    default -> null;
                };
                String primary = verdict != null ? verdict : "—";
                String secondary = (r.dispositifRecommande() != null ? r.dispositifRecommande() + " — " : "")
                        + "Recours " + r.delaiRecoursJours() + " j (" + r.juridictionRecours() + ")";
                return new DashboardTile(
                        "F-IM-20-mesures-eloignement",
                        "DELAIS",
                        "Mesures d'éloignement",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-08 Annexe 13 — ordre de quitter le territoire belge (BE). */
    private DashboardTile tileFromAnnexe13BeAnalysis(UUID caseFileId) {
        return annexe13BeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), Annexe13BeResult.class);
                String alert = switch (r.statutRecoursAnnulation() == null ? "" : r.statutRecoursAnnulation()) {
                    case "DANS_DELAI" -> "OK";
                    case "URGENT" -> "WARNING";
                    case "EXPIRE", "FORCLOS" -> "ALERT";
                    default -> null;
                };
                String primary = r.statutRecoursAnnulation() != null ? r.statutRecoursAnnulation() : "—";
                String secondary = r.joursRestantsAvantExpirationAnnulation() + " j restants — délai départ "
                        + r.delaiDepartImposeJours() + " j";
                return new DashboardTile(
                        "F-IM-08-annexe13-be",
                        "DOCUMENTS",
                        "Annexe 13 BE",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-14 9bis humanitaire BE (art. 9bis Loi 15/12/1980). */
    private DashboardTile tileFromBelgian9bisAnalysis(UUID caseFileId) {
        return belgian9bisRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), Belgian9bisResult.class);
                String alert = mapVerdictRisque(r.verdictProbabilite());
                String primary = r.verdictProbabilite() != null ? r.verdictProbabilite() : "—";
                String secondary = "Score : " + r.scoreGlobal() + "/100 — présence "
                        + r.dureePresenceMois() + " mois";
                return new DashboardTile(
                        "F-IM-14-9bis-humanitaire-be",
                        "DIAGNOSTIC",
                        "9bis humanitaire BE",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-14 9ter médical BE (art. 9ter Loi 15/12/1980). */
    private DashboardTile tileFromBelgian9terAnalysis(UUID caseFileId) {
        return belgian9terRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), Belgian9terResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String primary = r.verdictProbabiliteAcceptation() != null
                        ? r.verdictProbabiliteAcceptation()
                        : "—";
                String secondary = "Score : " + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-IM-14-9ter-medical-be",
                        "DIAGNOSTIC",
                        "9ter médical BE",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-14 40bis cohabitant UE BE (art. 40bis Loi 15/12/1980). */
    private DashboardTile tileFromBelgian40bisAnalysis(UUID caseFileId) {
        return belgian40bisRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), Belgian40bisResult.class);
                String alert = mapVerdictRisque(r.verdictProbabilite());
                String primary = r.verdictProbabilite() != null ? r.verdictProbabilite() : "—";
                String secondary = (r.lienFamilial() != null ? r.lienFamilial() + " — " : "")
                        + "Score : " + r.scoreGlobal() + "/100";
                return new DashboardTile(
                        "F-IM-14-40bis-cohabitant-ue-be",
                        "DIAGNOSTIC",
                        "40bis cohabitant UE BE",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /** F-IM-14 40ter familial Belge BE (art. 40ter Loi 15/12/1980). */
    private DashboardTile tileFromBelgian40terAnalysis(UUID caseFileId) {
        return belgian40terRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), Belgian40terResult.class);
                String alert = mapVerdictRisque(r.verdictProbabiliteAcceptation());
                String primary = r.verdictProbabiliteAcceptation() != null
                        ? r.verdictProbabiliteAcceptation()
                        : "—";
                String revenus = r.revenusMensuelsNetsEur() != null
                        ? formatEuros(r.revenusMensuelsNetsEur()) + " €/mois"
                        : "—";
                String secondary = (r.lienFamilial() != null ? r.lienFamilial() + " — " : "")
                        + "Revenus : " + revenus;
                return new DashboardTile(
                        "F-IM-14-40ter-familial-belge-be",
                        "DIAGNOSTIC",
                        "40ter familial Belge BE",
                        primary,
                        secondary,
                        alert);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    /**
     * Convention d'alerte commune aux verdicts ELEVEE / MOYENNE / FAIBLE
     * (probabilité de succès ou de risque selon l'outil).
     */
    private static String mapVerdictRisque(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "ELEVEE", "ELEVE", "FAVORABLE", "VALIDE", "CONFORME" -> "OK";
            case "MOYENNE", "MOYEN", "MITIGE", "CONTESTABLE" -> "WARNING";
            case "FAIBLE", "INVALIDE", "NUL", "DEFAVORABLE", "NON_CONFORME" -> "ALERT";
            default -> null;
        };
    }

    private static String formatEuros(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE)
                .format(amount.setScale(0, java.math.RoundingMode.HALF_UP));
    }

}
