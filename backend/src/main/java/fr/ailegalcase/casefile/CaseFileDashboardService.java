package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiQuestionAlignment;
import fr.ailegalcase.analysis.AiQuestionAlignmentService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
import fr.ailegalcase.analysis.PieceManquanteAlignment;
import fr.ailegalcase.analysis.PieceManquanteAlignmentService;
import fr.ailegalcase.analysis.PieceManquanteStatus;
import fr.ailegalcase.analysis.ProcedureCheckAlignment;
import fr.ailegalcase.analysis.ProcedureCheckAlignmentService;
import fr.ailegalcase.analysis.RetainedPisteAlignment;
import fr.ailegalcase.analysis.RetainedPisteAlignmentService;
import fr.ailegalcase.analysis.RisqueAlignment;
import fr.ailegalcase.analysis.RisqueAlignmentService;
import fr.ailegalcase.analysis.RisqueStatus;
import fr.ailegalcase.analysis.RisqueToolMatcher;
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
    // SF-DT-36-03 : repos des 16 outils décisionnels orphelins du dashboard —
    // ils calculaient et persistaient leur résultat sans émettre de tuile
    // (audit transversal : F-DT-36 + F-IM-21/22/23/24 + 11 outils Famille BE).
    private final ProcedureNulliteLicenciementRepository procedureNulliteLicenciementRepo;
    // SF-DT-38-02 : qualification rupture période d'essai (FR, F-DT-38).
    private final RupturePeriodeEssaiRepository rupturePeriodeEssaiRepo;
    // SF-206-01 : F-DT-42 abandon de poste / présomption de démission (FR)
    private final AbandonPostePresomptionDemissionRepository abandonPostePresomptionDemissionRepo;
    // SF-206-03 : F-DT-75 congés payés acquis pendant arrêt maladie (FR)
    private final CongesPayesArretMaladieRepository congesPayesArretMaladieRepo;
    // SF-206-05 : F-DT-39 prise d'acte de la rupture aux torts de l'employeur (FR)
    private final PriseActeRuptureRepository priseActeRuptureRepo;
    // SF-206-07 : F-DT-40 résiliation judiciaire du contrat de travail aux torts de l'employeur (FR)
    private final ResiliationJudiciaireCphRepository resiliationJudiciaireCphRepo;
    // SF-214-01 : F-IM-25 étranger malade L.425-9 CESEDA (FR)
    private final EtrangerMaladeRepository etrangerMaladeRepo;
    // SF-214-03 : F-IM-26 regroupement familial L.434-1+ CESEDA (FR)
    private final RegroupementFamilialRepository regroupementFamilialRepo;
    // SF-214-05 : F-IM-27 VPF liens personnels L.423-23 CESEDA (FR)
    private final VpfLiensPersonnelsRepository vpfLiensPersonnelsRepo;
    // SF-214-07 : F-IM-28 validation VLS-TS OFII 3 mois R. 311-3 CESEDA (FR)
    private final VlsTsValidationRepository vlsTsValidationRepo;
    // SF-214-09 : F-IM-29 OQTF catégories L.611-1 CESEDA (FR)
    private final OqtfCategoriesRepository oqtfCategoriesRepo;
    // SF-214-11 : F-IM-30 AES calcul présence prouvée L.435-1/L.435-3 CESEDA (FR)
    private final AesPresenceProuveeRepository aesPresenceProuveeRepo;
    // SF-214-13 : F-IM-31 renouvellement délai de dépôt 2 mois avant R. 433-1 CESEDA (FR)
    private final RenouvellementDelaiRepository renouvellementDelaiRepo;
    // SF-212-01 : F-DT-36-licenciement-faute-grave-lourde qualification disciplinaire (FR)
    private final LicenciementFauteGraveLourdRepository licenciementFauteGraveLourdRepo;
    private final JldRetentionRepository jldRetentionRepo;
    private final DublinRecoursRepository dublinRecoursRepo;
    private final CrrvRefusVisaRepository crrvRefusVisaRepo;
    private final VictimeViolencesL4256Repository victimeViolencesL4256Repo;
    private final AcceptationRenonciationSuccessionRepository acceptationRenonciationSuccessionRepo;
    private final AutoriteParentaleBeRepository autoriteParentaleBeRepo;
    private final ContributionAlimentaireEnfantsBeRepository contributionAlimentaireEnfantsBeRepo;
    private final ContributionConjointBeRepository contributionConjointBeRepo;
    private final DivorceDcBeRepository divorceDcBeRepo;
    private final DivorceDdiBeRepository divorceDdiBeRepo;
    private final LiquidationPartageBeRepository liquidationPartageBeRepo;
    private final MediationFamilialePreSaisineRepository mediationFamilialePreSaisineRepo;
    private final PacteSuccessoralBe2018Repository pacteSuccessoralBe2018Repo;
    private final RegimeCommunauteLegaleBeRepository regimeCommunauteLegaleBeRepo;
    private final TribunalFamilleBeMesuresProvisoiresRepository tribunalFamilleBeMesuresProvisoiresRepo;
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
    // F-207 SF-207-01 — Prescription Travail BE (1 an post-rupture L.03/07/1978 + CCT 109 ;
    // 5 ans arriérés salaire pendant le contrat). Tuile dashboard couplée à
    // l'outil seedé `prescription-be-litige-travail` (migration 252/253). Orphan
    // résorbé par F-245 hotfix CI master (DashboardTileToolIdIntegrityIT).
    private final PrescriptionBeLitigeTravailRepository prescriptionBeLitigeTravailRepo;
    // F-207 SF-207-02 — Checklist C4 ONEM Travail BE (art. 144 AR 25/11/1991 ;
    // mentions obligatoires + risque exclusion faute grave 4-52 semaines).
    // Tuile dashboard couplée à l'outil seedé `c4-onem-checklist` (migration
    // 254/255). Orphan résorbé par F-245 hotfix CI master.
    private final C4OnemChecklistRepository c4OnemChecklistRepo;
    // F-192 SF-192-01 — pistes RETAINED matérialisées sur la dernière analyse DONE.
    private final RetainedPisteAlignmentService retainedPisteAlignmentService;
    // F-193 SF-193-01 — checks F-96 matérialisés sur la dernière analyse DONE.
    private final ProcedureCheckAlignmentService procedureCheckAlignmentService;
    // F-194 SF-194-01 — pièces manquantes markables matérialisées sur la dernière analyse DONE.
    private final PieceManquanteAlignmentService pieceManquanteAlignmentService;
    // F-195 SF-195-01 — risques markables matérialisés sur la dernière analyse DONE.
    private final RisqueAlignmentService risqueAlignmentService;
    // F-196 SF-196-01 — questions complémentaires F-94 matérialisées sur la dernière analyse DONE.
    private final AiQuestionAlignmentService aiQuestionAlignmentService;
    // F-180 SF-180-01 — persistance des crashes de mappers DashboardTile (audit super-admin).
    private final DashboardTileCrashRecorder crashRecorder;

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
                                     ProcedureNulliteLicenciementRepository procedureNulliteLicenciementRepo,
                                     RupturePeriodeEssaiRepository rupturePeriodeEssaiRepo,
                                     AbandonPostePresomptionDemissionRepository abandonPostePresomptionDemissionRepo,
                                     CongesPayesArretMaladieRepository congesPayesArretMaladieRepo,
                                     PriseActeRuptureRepository priseActeRuptureRepo,
                                     ResiliationJudiciaireCphRepository resiliationJudiciaireCphRepo,
                                     EtrangerMaladeRepository etrangerMaladeRepo,
                                     RegroupementFamilialRepository regroupementFamilialRepo,
                                     VpfLiensPersonnelsRepository vpfLiensPersonnelsRepo,
                                     VlsTsValidationRepository vlsTsValidationRepo,
                                     OqtfCategoriesRepository oqtfCategoriesRepo,
                                     AesPresenceProuveeRepository aesPresenceProuveeRepo,
                                     RenouvellementDelaiRepository renouvellementDelaiRepo,
                                     LicenciementFauteGraveLourdRepository licenciementFauteGraveLourdRepo,
                                     JldRetentionRepository jldRetentionRepo,
                                     DublinRecoursRepository dublinRecoursRepo,
                                     CrrvRefusVisaRepository crrvRefusVisaRepo,
                                     VictimeViolencesL4256Repository victimeViolencesL4256Repo,
                                     AcceptationRenonciationSuccessionRepository acceptationRenonciationSuccessionRepo,
                                     AutoriteParentaleBeRepository autoriteParentaleBeRepo,
                                     ContributionAlimentaireEnfantsBeRepository contributionAlimentaireEnfantsBeRepo,
                                     ContributionConjointBeRepository contributionConjointBeRepo,
                                     DivorceDcBeRepository divorceDcBeRepo,
                                     DivorceDdiBeRepository divorceDdiBeRepo,
                                     LiquidationPartageBeRepository liquidationPartageBeRepo,
                                     MediationFamilialePreSaisineRepository mediationFamilialePreSaisineRepo,
                                     PacteSuccessoralBe2018Repository pacteSuccessoralBe2018Repo,
                                     RegimeCommunauteLegaleBeRepository regimeCommunauteLegaleBeRepo,
                                     TribunalFamilleBeMesuresProvisoiresRepository tribunalFamilleBeMesuresProvisoiresRepo,
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
                                     PrescriptionBeLitigeTravailRepository prescriptionBeLitigeTravailRepo,
                                     C4OnemChecklistRepository c4OnemChecklistRepo,
                                     RetainedPisteAlignmentService retainedPisteAlignmentService,
                                     ProcedureCheckAlignmentService procedureCheckAlignmentService,
                                     PieceManquanteAlignmentService pieceManquanteAlignmentService,
                                     RisqueAlignmentService risqueAlignmentService,
                                     AiQuestionAlignmentService aiQuestionAlignmentService,
                                     DashboardTileCrashRecorder crashRecorder) {
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
        this.procedureNulliteLicenciementRepo = procedureNulliteLicenciementRepo;
        this.rupturePeriodeEssaiRepo = rupturePeriodeEssaiRepo;
        this.abandonPostePresomptionDemissionRepo = abandonPostePresomptionDemissionRepo;
        this.congesPayesArretMaladieRepo = congesPayesArretMaladieRepo;
        this.priseActeRuptureRepo = priseActeRuptureRepo;
        this.resiliationJudiciaireCphRepo = resiliationJudiciaireCphRepo;
        this.etrangerMaladeRepo = etrangerMaladeRepo;
        this.regroupementFamilialRepo = regroupementFamilialRepo;
        this.vpfLiensPersonnelsRepo = vpfLiensPersonnelsRepo;
        this.vlsTsValidationRepo = vlsTsValidationRepo;
        this.oqtfCategoriesRepo = oqtfCategoriesRepo;
        this.aesPresenceProuveeRepo = aesPresenceProuveeRepo;
        this.renouvellementDelaiRepo = renouvellementDelaiRepo;
        this.licenciementFauteGraveLourdRepo = licenciementFauteGraveLourdRepo;
        this.jldRetentionRepo = jldRetentionRepo;
        this.dublinRecoursRepo = dublinRecoursRepo;
        this.crrvRefusVisaRepo = crrvRefusVisaRepo;
        this.victimeViolencesL4256Repo = victimeViolencesL4256Repo;
        this.acceptationRenonciationSuccessionRepo = acceptationRenonciationSuccessionRepo;
        this.autoriteParentaleBeRepo = autoriteParentaleBeRepo;
        this.contributionAlimentaireEnfantsBeRepo = contributionAlimentaireEnfantsBeRepo;
        this.contributionConjointBeRepo = contributionConjointBeRepo;
        this.divorceDcBeRepo = divorceDcBeRepo;
        this.divorceDdiBeRepo = divorceDdiBeRepo;
        this.liquidationPartageBeRepo = liquidationPartageBeRepo;
        this.mediationFamilialePreSaisineRepo = mediationFamilialePreSaisineRepo;
        this.pacteSuccessoralBe2018Repo = pacteSuccessoralBe2018Repo;
        this.regimeCommunauteLegaleBeRepo = regimeCommunauteLegaleBeRepo;
        this.tribunalFamilleBeMesuresProvisoiresRepo = tribunalFamilleBeMesuresProvisoiresRepo;
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
        this.prescriptionBeLitigeTravailRepo = prescriptionBeLitigeTravailRepo;
        this.c4OnemChecklistRepo = c4OnemChecklistRepo;
        this.retainedPisteAlignmentService = retainedPisteAlignmentService;
        this.procedureCheckAlignmentService = procedureCheckAlignmentService;
        this.pieceManquanteAlignmentService = pieceManquanteAlignmentService;
        this.risqueAlignmentService = risqueAlignmentService;
        this.aiQuestionAlignmentService = aiQuestionAlignmentService;
        this.crashRecorder = crashRecorder;
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
        // F-195 SF-195-01 — score recomputé excluant ÉCARTÉ (parallèle, F-IA-02 préservé).
        String scoreRisqueAvocatJson = null;
        var latestAnalysis = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latestAnalysis.isPresent()) {
            riskScore = latestAnalysis.get().getRiskScore();
            riskLevel = latestAnalysis.get().getRiskLevel();
            scoreRisqueAvocatJson = latestAnalysis.get().getScoreRisqueAvocatJson();
        }

        return new CaseFileDashboardResponse(
                caseFileId, cf.getLegalDomain(), riskScore, riskLevel,
                scoreRisqueAvocatJson,
                assembleTiles(caseFileId)
        );
    }

    /**
     * F-98 SF-98-01 — Expose la liste agrégée des verdicts d'outils décisionnels
     * remplis d'un dossier, pour réutilisation hors dashboard (générateur de
     * conclusions). Lecture pure, sans contrôle d'accès : l'appelant
     * ({@code CaseConclusionService}) a déjà validé l'isolation workspace.
     *
     * @param caseFileId identifiant du dossier
     * @return les tiles décisionnelles du dossier (vide si aucun outil rempli)
     */
    public List<DashboardTile> assembleDecisionToolTiles(UUID caseFileId) {
        return assembleTiles(caseFileId);
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
        addSafely(tiles, "F-DT-08-licenciement-validity", caseFileId, () -> tileFromLicenciementAnalysis(caseFileId));
        addSafely(tiles, "F-DT-09-comparateur-indemnites", caseFileId, () -> tileFromIndemniteComparatifAnalysis(caseFileId));
        addSafely(tiles, "F-DT-07-anciennete-conges-prime", caseFileId, () -> tileFromAncienneteAnalysis(caseFileId));
        addSafely(tiles, "F-IM-05-arbre-decisionnel-titre", caseFileId, () -> tileFromImmigrationTitleDecisionAnalysis(caseFileId));
        addSafely(tiles, "F-IM-07-droit-au-travail", caseFileId, () -> tileFromImmigrationWorkRightAnalysis(caseFileId));
        addSafely(tiles, "F-IM-06-recours", caseFileId, () -> tileFromImmigrationRecoursAnalysis(caseFileId));
        addSafely(tiles, "F-IM-11-changement-statut", caseFileId, () -> tileFromChangementStatutAnalysis(caseFileId));
        addSafely(tiles, "F-FA-05-partage-immobilier", caseFileId, () -> tileFromPartageImmobilierAnalysis(caseFileId));
        addSafely(tiles, "F-FA-06-calendrier-garde", caseFileId, () -> tileFromCalendrierGardeAnalysis(caseFileId));
        addSafely(tiles, "F-FA-07-checklist-divorce", caseFileId, () -> tileFromChecklistDivorceAnalysis(caseFileId));
        // ── SF-167-02 — extension Travail FR + BE ────────────────────────────
        addSafely(tiles, "F-DT-10-rupture-conv-validity", caseFileId, () -> tileFromRuptureConvAnalysis(caseFileId));
        addSafely(tiles, "F-DT-11-harcelement-licenciement-nul", caseFileId, () -> tileFromHarcelementNulliteAnalysis(caseFileId));
        addSafely(tiles, "F-DT-12-discrimination-dommages-interets", caseFileId, () -> tileFromDiscriminationAnalysis(caseFileId));
        addSafely(tiles, "F-DT-13-licenciement-economique", caseFileId, () -> tileFromLicenciementEconomiqueAnalysis(caseFileId));
        addSafely(tiles, "F-DT-14-pse-validite", caseFileId, () -> tileFromPseAnalysis(caseFileId));
        addSafely(tiles, "F-DT-15-inaptitude", caseFileId, () -> tileFromInaptitudeAnalysis(caseFileId));
        addSafely(tiles, "F-DT-16-licenciement-nul-detection", caseFileId, () -> tileFromLicenciementNulDetectionAnalysis(caseFileId));
        addSafely(tiles, "F-DT-17-indemnite-precarite-cdd", caseFileId, () -> tileFromIndemnitePrecariteCddAnalysis(caseFileId));
        addSafely(tiles, "F-DT-18-fin-mission-interim", caseFileId, () -> tileFromIndemniteFinMissionInterimAnalysis(caseFileId));
        addSafely(tiles, "F-DT-19-heures-sup", caseFileId, () -> tileFromHeuresSupAnalysis(caseFileId));
        addSafely(tiles, "F-DT-20-rappel-salaire", caseFileId, () -> tileFromRappelSalaireAnalysis(caseFileId));
        addSafely(tiles, "F-DT-21-travail-dissimule", caseFileId, () -> tileFromTravailDissimuleAnalysis(caseFileId));
        addSafely(tiles, "F-DT-22-requalification-cdd-cdi", caseFileId, () -> tileFromRequalificationCddCdiAnalysis(caseFileId));
        addSafely(tiles, "F-DT-23-requalification-interim-cdi", caseFileId, () -> tileFromRequalificationInterimCdiAnalysis(caseFileId));
        addSafely(tiles, "F-DT-24-non-concurrence", caseFileId, () -> tileFromNonConcurrenceAnalysis(caseFileId));
        addSafely(tiles, "F-DT-25-indemnite-preavis", caseFileId, () -> tileFromIndemnitePreavisAnalysis(caseFileId));
        addSafely(tiles, "F-DT-26-conges-payes-indemnite", caseFileId, () -> tileFromIndemniteCongesPayesAnalysis(caseFileId));
        addSafely(tiles, "F-DT-30-protection-rp", caseFileId, () -> tileFromProtectionRpAnalysis(caseFileId));
        addSafely(tiles, "F-DT-31-transaction", caseFileId, () -> tileFromTransactionAnalysis(caseFileId));
        addSafely(tiles, "F-DT-32-documents-fin-contrat", caseFileId, () -> tileFromDocumentsFinContratAnalysis(caseFileId));
        addSafely(tiles, "F-DT-33-at-mp", caseFileId, () -> tileFromAtMpAnalysis(caseFileId));
        addSafely(tiles, "F-DT-34-refere-prudhomal", caseFileId, () -> tileFromReferePrudhomalAnalysis(caseFileId));
        addSafely(tiles, "F-DT-35-contestation-are-fr", caseFileId, () -> tileFromContestationAreAnalysis(caseFileId));
        addSafely(tiles, "F-132-rupture-conv-indemnite", caseFileId, () -> tileFromRuptureConvIndemniteAnalysis(caseFileId));
        addSafely(tiles, "F-DT-27-motif-grave-be", caseFileId, () -> tileFromMotifGraveBeAnalysis(caseFileId));
        addSafely(tiles, "F-DT-28-avantages-conventionnels-be", caseFileId, () -> tileFromAvantagesConventionnelsBeAnalysis(caseFileId));
        addSafely(tiles, "F-DT-29-credit-temps-be", caseFileId, () -> tileFromCreditTempsBeAnalysis(caseFileId));
        // ── SF-DT-36-03 — correctif câblage des 16 outils orphelins du dashboard
        addSafely(tiles, "F-DT-36-procedure-nullite-licenciement", caseFileId, () -> tileFromProcedureNulliteLicenciementAnalysis(caseFileId));
        // SF-DT-38-02 : qualification rupture période d'essai (FR, F-DT-38).
        addSafely(tiles, "F-DT-38-rupture-periode-essai", caseFileId, () -> tileFromRupturePeriodeEssaiAnalysis(caseFileId));
        // SF-206-01 : F-DT-42 abandon de poste / présomption de démission (FR)
        addSafely(tiles, "F-DT-42-abandon-poste-presomption-demission", caseFileId, () -> tileFromAbandonPostePresomptionDemissionAnalysis(caseFileId));
        // SF-206-03 : F-DT-75 congés payés acquis pendant arrêt maladie (FR)
        addSafely(tiles, "F-DT-75-conges-payes-arret-maladie", caseFileId, () -> tileFromCongesPayesArretMaladieAnalysis(caseFileId));
        // SF-206-05 : F-DT-39 prise d'acte de la rupture aux torts de l'employeur (FR)
        addSafely(tiles, "F-DT-39-prise-acte-rupture", caseFileId, () -> tileFromPriseActeRuptureAnalysis(caseFileId));
        // SF-206-07 : F-DT-40 résiliation judiciaire du contrat de travail aux torts de l'employeur (FR)
        addSafely(tiles, "F-DT-40-resiliation-judiciaire-cph", caseFileId, () -> tileFromResiliationJudiciaireCphAnalysis(caseFileId));
        // SF-212-01 : F-DT-36-licenciement-faute-grave-lourde qualification disciplinaire (FR)
        addSafely(tiles, "F-DT-36-licenciement-faute-grave-lourde", caseFileId, () -> tileFromLicenciementFauteGraveLourdAnalysis(caseFileId));
        addSafely(tiles, "F-IM-21-jld-retention-fr", caseFileId, () -> tileFromJldRetentionAnalysis(caseFileId));
        addSafely(tiles, "F-IM-22-dublin-recours-fr", caseFileId, () -> tileFromDublinRecoursAnalysis(caseFileId));
        addSafely(tiles, "F-IM-23-crrv-refus-visa-fr", caseFileId, () -> tileFromCrrvRefusVisaAnalysis(caseFileId));
        addSafely(tiles, "F-IM-24-victime-violences-l4256-fr", caseFileId, () -> tileFromVictimeViolencesL4256Analysis(caseFileId));
        // SF-214-01 : F-IM-25 étranger malade L.425-9 CESEDA (FR)
        addSafely(tiles, "F-IM-25-etranger-malade-l4259-fr", caseFileId, () -> tileFromEtrangerMaladeAnalysis(caseFileId));
        // SF-214-03 : F-IM-26 regroupement familial L.434-1+ CESEDA (FR)
        addSafely(tiles, "F-IM-26-regroupement-familial-fr", caseFileId, () -> tileFromRegroupementFamilialAnalysis(caseFileId));
        // SF-214-05 : F-IM-27 VPF liens personnels L.423-23 CESEDA (FR)
        addSafely(tiles, "F-IM-27-vpf-liens-personnels-l42323-fr", caseFileId, () -> tileFromVpfLiensPersonnelsAnalysis(caseFileId));
        // SF-214-07 : F-IM-28 validation VLS-TS OFII 3 mois R. 311-3 CESEDA (FR)
        addSafely(tiles, "F-IM-28-vls-ts-validation-ofii-fr", caseFileId, () -> tileFromVlsTsValidationAnalysis(caseFileId));
        // SF-214-09 : F-IM-29 OQTF catégories L.611-1 CESEDA (FR)
        addSafely(tiles, "F-IM-29-oqtf-categories-l6111-fr", caseFileId, () -> tileFromOqtfCategoriesAnalysis(caseFileId));
        addSafely(tiles, "F-IM-30-aes-presence-prouvee-fr", caseFileId, () -> tileFromAesPresenceProuveeAnalysis(caseFileId));
        // SF-214-13 : F-IM-31 renouvellement délai de dépôt 2 mois avant R. 433-1 CESEDA (FR)
        addSafely(tiles, "F-IM-31-renouvellement-delai-depot-fr", caseFileId, () -> tileFromRenouvellementDelaiAnalysis(caseFileId));
        addSafely(tiles, "acceptation-renonciation-succession", caseFileId, () -> tileFromAcceptationRenonciationSuccessionAnalysis(caseFileId));
        addSafely(tiles, "autorite-parentale-be", caseFileId, () -> tileFromAutoriteParentaleBeAnalysis(caseFileId));
        addSafely(tiles, "contribution-alimentaire-enfants-be", caseFileId, () -> tileFromContributionAlimentaireEnfantsBeAnalysis(caseFileId));
        addSafely(tiles, "contribution-conjoint-be", caseFileId, () -> tileFromContributionConjointBeAnalysis(caseFileId));
        addSafely(tiles, "divorce-dc-be", caseFileId, () -> tileFromDivorceDcBeAnalysis(caseFileId));
        addSafely(tiles, "divorce-ddi-3voies-be", caseFileId, () -> tileFromDivorceDdiBeAnalysis(caseFileId));
        addSafely(tiles, "liquidation-partage-be", caseFileId, () -> tileFromLiquidationPartageBeAnalysis(caseFileId));
        addSafely(tiles, "mediation-familiale-pre-saisine", caseFileId, () -> tileFromMediationFamilialePreSaisineAnalysis(caseFileId));
        addSafely(tiles, "pacte-successoral-be-2018", caseFileId, () -> tileFromPacteSuccessoralBe2018Analysis(caseFileId));
        addSafely(tiles, "regime-mat-be-communaute-legale", caseFileId, () -> tileFromRegimeCommunauteLegaleBeAnalysis(caseFileId));
        addSafely(tiles, "tribunal-famille-be-mesures-prov", caseFileId, () -> tileFromTribunalFamilleBeMesuresProvisoiresAnalysis(caseFileId));
        // ── SF-167-03 — extension Famille FR + BE ────────────────────────────
        addSafely(tiles, "F-FA-08-divorce-alteration", caseFileId, () -> tileFromDivorceAlterationAnalysis(caseFileId));
        addSafely(tiles, "F-FA-09-divorce-faute", caseFileId, () -> tileFromDivorceFauteAnalysis(caseFileId));
        addSafely(tiles, "F-FA-10-divorce-accepte", caseFileId, () -> tileFromDivorceAccepteAnalysis(caseFileId));
        addSafely(tiles, "F-FA-11-desunion-irremediable-be", caseFileId, () -> tileFromDivorceDesunionIrremediableBeAnalysis(caseFileId));
        addSafely(tiles, "F-FA-12-mesures-provisoires", caseFileId, () -> tileFromMesuresProvisoiresAnalysis(caseFileId));
        addSafely(tiles, "F-FA-13-revisions-post-divorce", caseFileId, () -> tileFromRevisionsPostDivorceAnalysis(caseFileId));
        addSafely(tiles, "F-FA-14-ordonnance-protection", caseFileId, () -> tileFromOrdonnanceProtectionAnalysis(caseFileId));
        addSafely(tiles, "F-FA-15-recompenses", caseFileId, () -> tileFromRecompensesAnalysis(caseFileId));
        addSafely(tiles, "F-FA-16-communaute-universelle", caseFileId, () -> tileFromCommunauteUniverselleAnalysis(caseFileId));
        addSafely(tiles, "F-FA-17-partage-judiciaire", caseFileId, () -> tileFromPartageJudiciaireAnalysis(caseFileId));
        addSafely(tiles, "F-FA-18-adoption", caseFileId, () -> tileFromAdoptionAnalysis(caseFileId));
        addSafely(tiles, "F-FA-18-contestation-paternite", caseFileId, () -> tileFromContestationPaterniteAnalysis(caseFileId));
        addSafely(tiles, "F-FA-18-recherche-paternite", caseFileId, () -> tileFromRecherchePaterniteAnalysis(caseFileId));
        addSafely(tiles, "F-FA-18-reconnaissance-paternelle", caseFileId, () -> tileFromReconnaissancePaterneleAnalysis(caseFileId));
        addSafely(tiles, "F-FA-18-possession-etat", caseFileId, () -> tileFromPossessionEtatAnalysis(caseFileId));
        addSafely(tiles, "F-FA-19-autorite-parentale", caseFileId, () -> tileFromAutoriteParentaleAnalysis(caseFileId));
        addSafely(tiles, "F-FA-19-changement-residence", caseFileId, () -> tileFromChangementResidenceAnalysis(caseFileId));
        addSafely(tiles, "F-FA-19-desaccords-parentaux", caseFileId, () -> tileFromDesaccordsParentauxAnalysis(caseFileId));
        addSafely(tiles, "F-FA-20-pacs-dissolution", caseFileId, () -> tileFromPacsDissolutionAnalysis(caseFileId));
        addSafely(tiles, "F-FA-21-separation-corps", caseFileId, () -> tileFromSeparationCorpsAnalysis(caseFileId));
        addSafely(tiles, "F-FA-22-indivision", caseFileId, () -> tileFromIndivisionAnalysis(caseFileId));
        addSafely(tiles, "F-FA-23-ordonnance-requete", caseFileId, () -> tileFromOrdonnanceRequeteAnalysis(caseFileId));
        addSafely(tiles, "F-FA-24-devolution-legale", caseFileId, () -> tileFromDevolutionLegaleAnalysis(caseFileId));
        addSafely(tiles, "F-FA-24-donation", caseFileId, () -> tileFromDonationAnalysis(caseFileId));
        addSafely(tiles, "F-FA-24-indivision-successorale", caseFileId, () -> tileFromIndivisionSuccessoraleAnalysis(caseFileId));
        addSafely(tiles, "F-FA-24-partage-successoral", caseFileId, () -> tileFromPartageSuccessoralAnalysis(caseFileId));
        addSafely(tiles, "F-FA-24-rapport-succession", caseFileId, () -> tileFromRapportSuccessionAnalysis(caseFileId));
        addSafely(tiles, "F-FA-24-reserve-heriditaire", caseFileId, () -> tileFromReserveHereditaireAnalysis(caseFileId));
        addSafely(tiles, "F-FA-24-testament-validite", caseFileId, () -> tileFromTestamentValiditeAnalysis(caseFileId));
        addSafely(tiles, "F-FA-25-majeurs-proteges", caseFileId, () -> tileFromMajeursProtegesAnalysis(caseFileId));
        addSafely(tiles, "F-FA-26-changement-etat-civil", caseFileId, () -> tileFromChangementEtatCivilAnalysis(caseFileId));
        addSafely(tiles, "F-FA-27-pma-gpa", caseFileId, () -> tileFromPmaGpaBioethiqueAnalysis(caseFileId));
        // ── SF-167-04 — extension Immigration FR + BE ────────────────────────
        addSafely(tiles, "F-IM-08-oqtf-avec-delai-fr", caseFileId, () -> tileFromOqtfAvecDelaiAnalysis(caseFileId));
        addSafely(tiles, "F-IM-08-oqtf-sans-delai-fr", caseFileId, () -> tileFromOqtfSansDelaiAnalysis(caseFileId));
        addSafely(tiles, "F-IM-08-referes-admin-fr", caseFileId, () -> tileFromReferesAdminAnalysis(caseFileId));
        addSafely(tiles, "F-IM-09-aes-etudiant", caseFileId, () -> tileFromAesEtudiantAnalysis(caseFileId));
        addSafely(tiles, "F-IM-09-aes-famille", caseFileId, () -> tileFromAesFamilleAnalysis(caseFileId));
        addSafely(tiles, "F-IM-09-aes-humanitaire", caseFileId, () -> tileFromAesHumanitaireAnalysis(caseFileId));
        addSafely(tiles, "F-IM-09-aes-metiers-tension", caseFileId, () -> tileFromAesMetiersTensionAnalysis(caseFileId));
        addSafely(tiles, "F-IM-12-asile-avance", caseFileId, () -> tileFromAsileAvanceAnalysis(caseFileId));
        addSafely(tiles, "F-IM-13-naturalisation", caseFileId, () -> tileFromNaturalisationAnalysis(caseFileId));
        addSafely(tiles, "F-IM-17-regime-algerien", caseFileId, () -> tileFromRegimeAlgerienAnalysis(caseFileId));
        addSafely(tiles, "F-IM-19-mineurs", caseFileId, () -> tileFromMineursImmigrationAnalysis(caseFileId));
        addSafely(tiles, "F-IM-20-mesures-eloignement", caseFileId, () -> tileFromMesuresEloignementAnalysis(caseFileId));
        addSafely(tiles, "F-IM-08-annexe13-be", caseFileId, () -> tileFromAnnexe13BeAnalysis(caseFileId));
        addSafely(tiles, "F-IM-14-9bis-humanitaire-be", caseFileId, () -> tileFromBelgian9bisAnalysis(caseFileId));
        addSafely(tiles, "F-IM-14-9ter-medical-be", caseFileId, () -> tileFromBelgian9terAnalysis(caseFileId));
        addSafely(tiles, "F-IM-14-40bis-cohabitant-ue-be", caseFileId, () -> tileFromBelgian40bisAnalysis(caseFileId));
        addSafely(tiles, "F-IM-14-40ter-familial-belge-be", caseFileId, () -> tileFromBelgian40terAnalysis(caseFileId));
        // ── F-207 SF-207-01 — Prescription Travail BE (orphan résorbé F-245 hotfix) ─
        addSafely(tiles, "prescription-be-litige-travail", caseFileId, () -> tileFromPrescriptionBeLitigeTravailAnalysis(caseFileId));
        // ── F-207 SF-207-02 — Checklist C4 ONEM Travail BE (orphan résorbé F-245 hotfix) ─
        addSafely(tiles, "c4-onem-checklist", caseFileId, () -> tileFromC4OnemChecklistAnalysis(caseFileId));
        // ── F-192 SF-192-01 — pistes RETAINED matérialisées ───────────────────
        addSafely(tiles, "F-192-retained-pistes-summary", caseFileId, () -> tileFromRetainedPistesAlignment(caseFileId));
        // ── F-193 SF-193-01 — checks F-96 matérialisés ─────────────────────
        addSafely(tiles, "F-193-procedure-checks-summary", caseFileId, () -> tileFromProcedureChecksAlignment(caseFileId));
        // ── F-194 SF-194-01 — pièces manquantes markables matérialisées ───
        addSafely(tiles, "F-194-pieces-summary", caseFileId, () -> tileFromPiecesManquantesAlignment(caseFileId));
        // ── F-195 SF-195-01 — risques markables matérialisés ───────────────
        addSafely(tiles, "F-195-risques-summary", caseFileId, () -> tileFromRisquesAlignment(caseFileId));
        // ── F-253 SF-253-01 — focus À_CREUSER (rappel curation) ────────────
        addSafely(tiles, "F-253-risques-a-creuser", caseFileId, () -> tileFromRisquesACreuserAlignment(caseFileId));
        // ── F-196 SF-196-01 — questions complémentaires F-94 matérialisées ─
        addSafely(tiles, "F-196-questions-summary", caseFileId, () -> tileFromAiQuestionsAlignment(caseFileId));
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

    /**
     * F-194 SF-194-01 — Tile dashboard agrégeant les pièces manquantes
     * markables matérialisées sur la dernière analyse {@code DONE} du dossier.
     *
     * <ul>
     *   <li>{@code primaryValue} : N total pièces matérialisées</li>
     *   <li>{@code secondaryValue} : "X à demander · Y obtenues · Z non applicables"</li>
     *   <li>{@code alertLevel = WARNING} si ≥ 1 {@code A_DEMANDER} ET dernier run > 7 jours</li>
     *   <li>{@code alertLevel = OK} sinon</li>
     * </ul>
     *
     * <p>Thème {@code DOCUMENTS} (différent de F-192 DIAGNOSTIC, F-193 DELAIS) —
     * les pièces relèvent naturellement du thème DOCUMENTS.</p>
     *
     * <p>Renvoie {@code null} si aucune analyse {@code DONE} ou si l'alignement
     * matérialisé est vide (analyse legacy pré-F-194 ou run dans lequel la
     * matérialisation a échoué fail-open).</p>
     */
    private DashboardTile tileFromPiecesManquantesAlignment(UUID caseFileId) {
        if (pieceManquanteAlignmentService == null || analysisRepository == null) return null;
        var latest = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latest.isEmpty()) return null;
        List<PieceManquanteAlignment> alignments = pieceManquanteAlignmentService
                .deserializeAlignment(latest.get().getPiecesAlignmentJson());
        if (alignments == null || alignments.isEmpty()) return null;

        long aDemander = alignments.stream()
                .filter(a -> PieceManquanteStatus.STATUT_A_DEMANDER.equals(a.statut()))
                .count();
        long obtenues = alignments.stream()
                .filter(a -> PieceManquanteStatus.STATUT_OBTENUE.equals(a.statut()))
                .count();
        long nonApplicables = alignments.stream()
                .filter(a -> PieceManquanteStatus.STATUT_NON_APPLICABLE.equals(a.statut()))
                .count();

        // alertLevel WARNING si ≥ 1 A_DEMANDER ET dernier run > 7j (rappel à l'avocat)
        String alertLevel = "OK";
        if (aDemander > 0) {
            java.time.Instant updatedAt = latest.get().getUpdatedAt();
            if (updatedAt != null && updatedAt.isBefore(java.time.Instant.now().minus(java.time.Duration.ofDays(7)))) {
                alertLevel = "WARNING";
            }
        }

        int total = alignments.size();
        String primary = total + " pièce" + (total > 1 ? "s" : "");
        String secondary = aDemander + " à demander · " + obtenues + " obtenue"
                + (obtenues > 1 ? "s" : "") + " · " + nonApplicables + " non applicable"
                + (nonApplicables > 1 ? "s" : "");

        return new DashboardTile(
                "F-194-pieces-summary",
                "DOCUMENTS",
                "Pièces — état des demandes client",
                primary,
                secondary,
                alertLevel);
    }

    /**
     * F-195 SF-195-01 — Tile dashboard agrégeant les risques markables
     * matérialisés sur la dernière analyse {@code DONE} du dossier.
     *
     * <ul>
     *   <li>{@code primaryValue} : N total risques matérialisés</li>
     *   <li>{@code secondaryValue} : "X validés · Y écartés · Z à creuser"</li>
     *   <li>{@code alertLevel = ALERT} si ≥ 1 VALIDÉ avec keyword critique
     *       (harcèlement / violence / expulsion / dilapidation)</li>
     *   <li>{@code alertLevel = OK} si tous les risques sont écartés</li>
     *   <li>{@code alertLevel = WARNING} sinon (au moins un VALIDÉ ou À_CREUSER)</li>
     * </ul>
     *
     * <p>Thème {@code DIAGNOSTIC} (cohérent F-192 — les risques relèvent du
     * diagnostic, pas des délais ni des documents).</p>
     *
     * <p>Renvoie {@code null} si aucune analyse {@code DONE} ou si l'alignement
     * matérialisé est vide (analyse legacy pré-F-195 ou run dans lequel la
     * matérialisation a échoué fail-open).</p>
     */
    private DashboardTile tileFromRisquesAlignment(UUID caseFileId) {
        if (risqueAlignmentService == null || analysisRepository == null) return null;
        var latest = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latest.isEmpty()) return null;
        List<RisqueAlignment> alignments = risqueAlignmentService
                .deserializeAlignment(latest.get().getRisquesAlignmentJson());
        if (alignments == null || alignments.isEmpty()) return null;

        long valides = alignments.stream()
                .filter(a -> RisqueStatus.STATUT_VALIDE.equals(a.statut()))
                .count();
        long ecartes = alignments.stream()
                .filter(a -> RisqueStatus.STATUT_ECARTE.equals(a.statut()))
                .count();
        long aCreuser = alignments.stream()
                .filter(a -> RisqueStatus.STATUT_A_CREUSER.equals(a.statut()))
                .count();

        // alertLevel ALERT si ≥ 1 VALIDÉ avec keyword critique
        boolean validatedCritical = alignments.stream()
                .filter(a -> RisqueStatus.STATUT_VALIDE.equals(a.statut()))
                .anyMatch(a -> RisqueToolMatcher.isCriticalKeyword(a.risqueLibelle()));

        String alertLevel;
        int total = alignments.size();
        if (validatedCritical) {
            alertLevel = "ALERT";
        } else if (ecartes == total) {
            alertLevel = "OK";
        } else {
            alertLevel = "WARNING";
        }

        String primary = total + " risque" + (total > 1 ? "s" : "");
        String secondary = valides + " validé" + (valides > 1 ? "s" : "")
                + " · " + ecartes + " écarté" + (ecartes > 1 ? "s" : "")
                + " · " + aCreuser + " à creuser";

        return new DashboardTile(
                "F-195-risques-summary",
                "DIAGNOSTIC",
                "Risques — analyse avocat",
                primary,
                secondary,
                alertLevel);
    }

    /**
     * F-253 SF-253-01 — Tile dashboard dédiée au rappel des risques restant
     * à arbitrer (statut {@code A_CREUSER}) sur la dernière analyse {@code DONE}
     * du dossier. Donne un consommateur explicite à un statut F-195 qui était
     * écrit en DB mais lu uniquement à l'écran synthèse.
     *
     * <ul>
     *   <li>{@code primaryValue} : "N à creuser" (pluralisation appliquée)</li>
     *   <li>{@code secondaryValue} : "Curation à compléter" (constant)</li>
     *   <li>{@code alertLevel = WARNING} quand la tile apparaît</li>
     * </ul>
     *
     * <p>Thème {@code DIAGNOSTIC} (cohérent F-195) — les risques relèvent du
     * diagnostic, pas des délais ni des documents.</p>
     *
     * <p><b>Anti-pollution dashboard</b> : la tile retourne {@code null} si
     * compteur {@code A_CREUSER} = 0 (invariant étape 0 bis n°2 — aucune
     * apparition « tous arbitrés ✅ », F-195-risques-summary couvre déjà
     * l'état post-arbitrage).</p>
     *
     * <p>Cohabite avec {@link #tileFromRisquesAlignment(UUID)} (F-195) — F-195
     * affiche la vue globale (V / É / À_C), F-253 met l'accent sur le À_C.</p>
     *
     * <p>Renvoie {@code null} si aucune analyse {@code DONE}, alignement vide
     * (legacy pré-F-195 ou matérialisation fail-open) ou compteur À_C = 0.</p>
     */
    private DashboardTile tileFromRisquesACreuserAlignment(UUID caseFileId) {
        if (risqueAlignmentService == null || analysisRepository == null) return null;
        var latest = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latest.isEmpty()) return null;
        List<RisqueAlignment> alignments = risqueAlignmentService
                .deserializeAlignment(latest.get().getRisquesAlignmentJson());
        if (alignments == null || alignments.isEmpty()) return null;

        long aCreuser = alignments.stream()
                .filter(a -> RisqueStatus.STATUT_A_CREUSER.equals(a.statut()))
                .count();
        if (aCreuser == 0) return null;

        String primary = aCreuser + " à creuser";
        String secondary = "Curation à compléter";

        return new DashboardTile(
                "F-253-risques-a-creuser",
                "DIAGNOSTIC",
                "Risques à arbitrer",
                primary,
                secondary,
                "WARNING");
    }

    /**
     * F-196 SF-196-01 — Tile dashboard agrégeant les questions complémentaires
     * F-94 matérialisées sur la dernière analyse {@code DONE} du dossier.
     *
     * <ul>
     *   <li>{@code primaryValue} : N total questions matérialisées</li>
     *   <li>{@code secondaryValue} : "X répondues · Y en attente"</li>
     *   <li>{@code alertLevel = WARNING} si ≥ 1 question en attente, {@code OK} sinon</li>
     * </ul>
     *
     * <p>Thème {@code DOCUMENTS} (cohérent F-194 — les réponses aux questions
     * "Avez-vous X ?" se matérialisent in fine en pièces).</p>
     *
     * <p>Renvoie {@code null} si aucune analyse {@code DONE} ou si l'alignement
     * matérialisé est vide (analyse legacy pré-F-196 ou run dans lequel la
     * matérialisation a échoué fail-open).</p>
     */
    private DashboardTile tileFromAiQuestionsAlignment(UUID caseFileId) {
        if (aiQuestionAlignmentService == null || analysisRepository == null) return null;
        var latest = analysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, fr.ailegalcase.analysis.AnalysisStatus.DONE);
        if (latest.isEmpty()) return null;
        List<AiQuestionAlignment> alignments = aiQuestionAlignmentService
                .deserializeAlignment(latest.get().getAiQuestionsAlignmentJson());
        if (alignments == null || alignments.isEmpty()) return null;

        long repondues = alignments.stream()
                .filter(a -> a.answerText() != null && !a.answerText().isBlank())
                .count();
        int total = alignments.size();
        long enAttente = total - repondues;

        String alertLevel = enAttente > 0 ? "WARNING" : "OK";
        String primary = total + " question" + (total > 1 ? "s" : "");
        String secondary = repondues + " répondue" + (repondues > 1 ? "s" : "")
                + " · " + enAttente + " en attente";

        return new DashboardTile(
                "F-196-questions-summary",
                "DOCUMENTS",
                "Questions complémentaires — réponses avocat",
                primary,
                secondary,
                alertLevel);
    }

    /**
     * F-167 SF-167-01 — fail-open per tile : un mapper qui crashe ne fait pas
     * planter le dashboard. F-180 SF-180-01 — en plus du WARN, le crash est
     * persisté en base via {@link DashboardTileCrashRecorder} (robuste au
     * redémarrage JVM, historisé 30j) et exploité par l'audit super-admin.
     */
    private void addSafely(List<DashboardTile> tiles, String toolId, UUID caseFileId,
                           Supplier<DashboardTile> supplier) {
        try {
            DashboardTile t = supplier.get();
            if (t != null) {
                tiles.add(t);
            }
        } catch (Exception e) {
            log.warn("F-167 SF-167-01 — fail-open per tile {}: {}", toolId, e.toString());
            crashRecorder.record(toolId, caseFileId, e);
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

    /**
     * F-DT-36 Nullité de procédure de licenciement (FR) — SF-DT-36-03.
     *
     * <p>Correctif de câblage : l'outil calculait et persistait son résultat
     * (table {@code procedure_nullite_licenciement_analyses}) mais n'émettait
     * aucune tuile dashboard — il était orphelin de {@code assembleTiles()}.</p>
     */
    private DashboardTile tileFromProcedureNulliteLicenciementAnalysis(UUID caseFileId) {
        return procedureNulliteLicenciementRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), ProcedureNulliteLicenciementResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                int nbVices = r.vicesDetectes() != null ? r.vicesDetectes().size() : 0;
                String secondary = nbVices + " vice(s) — score " + r.scoreNullite() + "/100";
                return new DashboardTile(
                        "F-DT-36-procedure-nullite-licenciement",
                        "VALIDITE",
                        "Nullité de procédure",
                        verdict != null ? verdict : "—",
                        secondary,
                        mapVerdictNullite(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-DT-38 Rupture de période d'essai (FR) — SF-DT-38-02.
     *
     * <p>Tuile dashboard pour l'outil de qualification d'une rupture pendant
     * la période d'essai. Verdict 4 niveaux : REGULIERE / RISQUE_ABUSIVE /
     * NULLE (rouge avec mention réintégration) / ILLEGALE_REQUALIF_LICENCIEMENT
     * (rouge). Affiche le verdict + le nombre d'anomalies détectées + le score.</p>
     */
    private DashboardTile tileFromRupturePeriodeEssaiAnalysis(UUID caseFileId) {
        return rupturePeriodeEssaiRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), RupturePeriodeEssaiResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                int nbAnomalies = r.anomaliesDetectees() != null ? r.anomaliesDetectees().size() : 0;
                String secondary = nbAnomalies + " anomalie(s) — score " + r.scoreIrregularite() + "/100";
                return new DashboardTile(
                        "F-DT-38-rupture-periode-essai",
                        "VALIDITE",
                        "Rupture période d'essai",
                        verdict != null ? verdict : "—",
                        secondary,
                        mapVerdictRupturePeriodeEssai(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-DT-42 Abandon de poste / présomption de démission (FR) — SF-206-01.
     *
     * <p>Tile diagnostic alignée sur le verdict de solidité de la contestation
     * (SOLIDE / INCERTAINE / DIFFICILE) ; secondary value = score 0-100.</p>
     */
    private DashboardTile tileFromAbandonPostePresomptionDemissionAnalysis(UUID caseFileId) {
        return abandonPostePresomptionDemissionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), AbandonPostePresomptionDemissionResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                String primary = libelleVerdictAbandonPoste(verdict);
                String secondary = "Score " + r.scoreContestation() + "/100";
                return new DashboardTile(
                        "F-DT-42-abandon-poste-presomption-demission",
                        "DIAGNOSTIC",
                        "Abandon de poste",
                        primary,
                        secondary,
                        mapVerdictAbandonPoste(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-DT-75 Congés payés acquis pendant arrêt maladie (FR) — SF-206-03.
     *
     * <p>Tile thème {@code INDEMNITES} (rappel de droits, pas une rupture).
     * Mapping verdict : {@code RAPPEL_SIGNIFICATIF} → WARNING (montant à
     * réclamer / action à engager), {@code RAPPEL_LIMITE} → OK, {@code
     * PAS_DE_RAPPEL} → OK, {@code ACTION_FORCLOSE} → ALERT (délai dépassé).</p>
     */
    private DashboardTile tileFromCongesPayesArretMaladieAnalysis(UUID caseFileId) {
        return congesPayesArretMaladieRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), CongesPayesArretMaladieResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                String primary = libelleVerdictCongesPayesArretMaladie(verdict);
                String secondary = r.joursCpRappel() != null
                        ? r.joursCpRappel().toPlainString() + " j ouvrables de rappel"
                        : "—";
                return new DashboardTile(
                        "F-DT-75-conges-payes-arret-maladie",
                        "INDEMNITES",
                        "Congés payés sur arrêt maladie",
                        primary,
                        secondary,
                        mapVerdictCongesPayesArretMaladie(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-DT-39 Prise d'acte de la rupture aux torts de l'employeur (FR) —
     * SF-206-05. Thème {@code DIAGNOSTIC} (groupe F-169 "Rupture — initiative
     * salarié / torts employeur"). Du point de vue de l'avocat du salarié :
     * un verdict favorable est une opportunité contentieuse (OK), un verdict
     * défavorable est une catastrophe potentielle (ALERT — la prise d'acte
     * notifiée à tort produit les effets d'une démission).
     */
    private DashboardTile tileFromPriseActeRuptureAnalysis(UUID caseFileId) {
        return priseActeRuptureRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), PriseActeRuptureResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                String primary = libelleVerdictPriseActe(verdict);
                String secondary = "Score " + r.scoreSolidite() + "/100";
                return new DashboardTile(
                        "F-DT-39-prise-acte-rupture",
                        "DIAGNOSTIC",
                        "Prise d'acte de la rupture",
                        primary,
                        secondary,
                        mapVerdictPriseActe(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * SF-206-07 — F-DT-40 résiliation judiciaire du contrat de travail aux
     * torts de l'employeur (FR — Cass. soc. 16/03/1989 ; Cass. soc. 20/01/1998 ;
     * art. L.1411-1 CT ; art. 1224, 1227-1228 C.civ.).
     *
     * <p>Thème DIAGNOSTIC. Mapping verdict → alertLevel du point de vue de
     * l'avocat du salarié : un verdict favorable est une opportunité
     * contentieuse (OK), un verdict incertain appelle de l'attention (WARNING),
     * un verdict défavorable est mappé en {@code OK} — choix structurel : la
     * résiliation judiciaire est une <b>voie sans risque de rupture</b> (le
     * rejet ne rompt pas le contrat, contrairement à la prise d'acte qui
     * produit effet immédiat). Un verdict défavorable signifie "demande peu
     * solide à éviter d'engager", ce qui n'est pas une catastrophe — c'est
     * un signal d'orientation, pas d'alerte. Cohérent avec le rappel
     * structurant du calculateur (« voie moins risquée que la prise d'acte »).</p>
     */
    private DashboardTile tileFromResiliationJudiciaireCphAnalysis(UUID caseFileId) {
        return resiliationJudiciaireCphRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), ResiliationJudiciaireCphResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                String primary = libelleVerdictResiliationJud(verdict);
                String secondary = "Score " + r.scoreSolidite() + "/100";
                return new DashboardTile(
                        "F-DT-40-resiliation-judiciaire-cph",
                        "DIAGNOSTIC",
                        "Résiliation judiciaire CPH",
                        primary,
                        secondary,
                        mapVerdictResiliationJud(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-IM-21 JLD rétention administrative (FR) — SF-DT-36-03.
     * Correctif de câblage : outil orphelin du dashboard (résultat persisté
     * dans {@code jld_retention_analyses} mais jamais agrégé).
     */
    private DashboardTile tileFromJldRetentionAnalysis(UUID caseFileId) {
        return jldRetentionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), JldRetentionResult.class);
                String secondary = r.joursRestantsAvantSaisine() + " j. avant saisine JLD";
                return new DashboardTile(
                        "F-IM-21-jld-retention-fr",
                        "DELAIS",
                        "JLD rétention",
                        r.statut() != null ? r.statut() : "—",
                        secondary,
                        mapStatutDelai(r.statut()));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-IM-22 Recours contre transfert Dublin (FR) — SF-DT-36-03.
     * Correctif de câblage : outil orphelin du dashboard.
     */
    private DashboardTile tileFromDublinRecoursAnalysis(UUID caseFileId) {
        return dublinRecoursRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DublinRecoursResult.class);
                String secondary = r.joursRestants() + " j. avant expiration du recours";
                return new DashboardTile(
                        "F-IM-22-dublin-recours-fr",
                        "DELAIS",
                        "Recours Dublin",
                        r.statut() != null ? r.statut() : "—",
                        secondary,
                        mapStatutDelai(r.statut()));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-IM-23 Recours CRRV contre refus de visa (FR) — SF-DT-36-03.
     * Correctif de câblage : outil orphelin du dashboard.
     */
    private DashboardTile tileFromCrrvRefusVisaAnalysis(UUID caseFileId) {
        return crrvRefusVisaRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), CrrvRefusVisaResult.class);
                String secondary = r.joursRestants() + " j. avant expiration du recours CRRV";
                return new DashboardTile(
                        "F-IM-23-crrv-refus-visa-fr",
                        "DELAIS",
                        "Recours CRRV",
                        r.statut() != null ? r.statut() : "—",
                        secondary,
                        mapStatutDelai(r.statut()));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-IM-24 Titre de séjour victime de violences L.425-6 (FR) — SF-DT-36-03.
     * Correctif de câblage : outil orphelin du dashboard.
     */
    private DashboardTile tileFromVictimeViolencesL4256Analysis(UUID caseFileId) {
        return victimeViolencesL4256Repo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getResultData(), VictimeViolencesL4256Result.class);
                String score = r.eligibiliteScore();
                int nbValides = r.criteresValides() != null ? r.criteresValides().size() : 0;
                int nbManquants = r.criteresManquants() != null ? r.criteresManquants().size() : 0;
                String secondary = nbValides + " critère(s) validé(s), "
                        + nbManquants + " manquant(s)";
                String alert = switch (score == null ? "" : score) {
                    case "ELIGIBLE_PLEIN_DROIT" -> "OK";
                    case "ELIGIBLE_SOUS_RESERVE" -> "WARNING";
                    case "NON_ELIGIBLE" -> "ALERT";
                    default -> null;
                };
                return new DashboardTile(
                        "F-IM-24-victime-violences-l4256-fr",
                        "VALIDITE",
                        "Titre L.425-6 violences",
                        score != null ? score : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-IM-25 Étranger malade L.425-9 CESEDA (FR) — SF-214-01.
     * Thème VALIDITE : éligibilité à la protection médicale.
     * Mapping alertLevel :
     * ELIGIBLE_PROBABLE → OK ; ELIGIBLE_SOUS_RESERVE → WARNING ;
     * NON_ELIGIBLE → ALERT ; EN_ATTENTE_AVIS_OFII → null.
     */
    private DashboardTile tileFromEtrangerMaladeAnalysis(UUID caseFileId) {
        return etrangerMaladeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), EtrangerMaladeResult.class);
                String verdict = r.verdict();
                String secondary = r.delaiRecoursTA() != null
                        ? "Délai recours TA : " + r.delaiRecoursTA()
                        : (r.motifRecours() != null ? r.motifRecours() : "—");
                String alert = switch (verdict == null ? "" : verdict) {
                    case "ELIGIBLE_PROBABLE" -> "OK";
                    case "ELIGIBLE_SOUS_RESERVE" -> "WARNING";
                    case "NON_ELIGIBLE" -> "ALERT";
                    default -> null;
                };
                return new DashboardTile(
                        "F-IM-25-etranger-malade-l4259-fr",
                        "VALIDITE",
                        "Étranger malade L.425-9",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * SF-214-03 — F-IM-26 regroupement familial L.434-1+ CESEDA (FR). Expose le
     * verdict d'éligibilité ; seuils chiffrés (ressources/surface requises) en
     * valeur secondaire ; alertLevel mappé sur la sémantique du verdict.
     */
    private DashboardTile tileFromRegroupementFamilialAnalysis(UUID caseFileId) {
        return regroupementFamilialRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RegroupementFamilialResult.class);
                String verdict = r.verdict();
                String secondary = String.format(
                        "Ressources requises : %.2f € — Surface requise : %d m²",
                        r.ressourcesRequises(), r.surfaceRequise());
                String alert = switch (verdict == null ? "" : verdict) {
                    case "ELIGIBLE" -> "OK";
                    case "ELIGIBLE_SOUS_RESERVE" -> "WARNING";
                    case "NON_ELIGIBLE_DELAI", "NON_ELIGIBLE_RESSOURCES", "NON_ELIGIBLE_LOGEMENT" -> "ALERT";
                    default -> null;
                };
                return new DashboardTile(
                        "F-IM-26-regroupement-familial-fr",
                        "VALIDITE",
                        "Regroupement familial L.434-1",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * SF-214-05 — F-IM-27 VPF liens personnels et familiaux L.423-23 CESEDA (FR).
     * Expose le verdict d'éligibilité ; score d'intensité des liens en valeur
     * secondaire ; alertLevel mappé sur la sémantique du verdict.
     */
    private DashboardTile tileFromVpfLiensPersonnelsAnalysis(UUID caseFileId) {
        return vpfLiensPersonnelsRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), VpfLiensPersonnelsResult.class);
                String verdict = r.verdict();
                String secondary = String.format(
                        "Score liens : %d — Durée de résidence : %s",
                        r.score(), r.dureeResidenceRemplie() ? "remplie" : "insuffisante");
                String alert = switch (verdict == null ? "" : verdict) {
                    case "ELIGIBLE_PROBABLE" -> "OK";
                    case "ELIGIBLE_SOUS_RESERVE", "DOSSIER_A_CONSOLIDER" -> "WARNING";
                    case "NON_ELIGIBLE" -> "ALERT";
                    default -> null;
                };
                return new DashboardTile(
                        "F-IM-27-vpf-liens-personnels-l42323-fr",
                        "VALIDITE",
                        "VPF liens personnels L.423-23",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * SF-214-09 — F-IM-29 OQTF catégories L. 611-1 CESEDA (1° à 7°). Tuile
     * informative : rappelle la catégorie identifiée et le délai de recours
     * applicable. Outil FRANCE uniquement.
     *
     * <ul>
     *   <li>{@code ALERT} si OQTF sans délai (fenêtre courte 48 h, catégories 6° / 7°)</li>
     *   <li>{@code WARNING} sinon (délai 30 j à ne pas laisser courir)</li>
     * </ul>
     */
    private DashboardTile tileFromOqtfCategoriesAnalysis(UUID caseFileId) {
        return oqtfCategoriesRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), OqtfCategoriesResult.class);
                boolean sansDelai = r.delaiRecoursHeures() != null;
                String secondary = sansDelai
                        ? "Recours 48 h (L. 614-1)"
                        : "Recours 30 j (L. 614-5)";
                String alert = sansDelai ? "ALERT" : "WARNING";
                return new DashboardTile(
                        "F-IM-29-oqtf-categories-l6111-fr",
                        "VALIDITE",
                        "OQTF catégorie L. 611-1",
                        r.categorieL611() != null ? r.categorieL611().name() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * SF-214-11 — F-IM-30 AES calcul présence prouvée (circulaire Valls 28/11/2012 ;
     * L. 435-1 / L. 435-3 CESEDA). Tuile informative : rappelle l'ancienneté de
     * présence prouvée et la voie AES la plus exigeante atteinte. Outil FRANCE uniquement.
     *
     * <ul>
     *   <li>{@code OK} si au moins une voie AES est atteinte (≥ 3 ans)</li>
     *   <li>{@code WARNING} sinon (ancienneté insuffisante pour toute voie)</li>
     * </ul>
     */
    private DashboardTile tileFromAesPresenceProuveeAnalysis(UUID caseFileId) {
        return aesPresenceProuveeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AesPresenceProuveeResult.class);
                int annees = r.anneesTotalesProuvees();
                java.util.Map<String, Boolean> elig = r.eligibiliteParVoie() != null
                        ? r.eligibiliteParVoie() : java.util.Map.of();
                boolean uneVoie = elig.values().stream().anyMatch(Boolean.TRUE::equals);
                String voies = elig.entrySet().stream()
                        .filter(en -> Boolean.TRUE.equals(en.getValue()))
                        .map(java.util.Map.Entry::getKey)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("aucune voie atteinte");
                String secondary = String.format("Voies éligibles : %s", voies);
                String alert = uneVoie ? "OK" : "WARNING";
                return new DashboardTile(
                        "F-IM-30-aes-presence-prouvee-fr",
                        "VALIDITE",
                        "AES présence prouvée",
                        annees + " an(s) prouvé(s)",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * SF-214-07 — F-IM-28 validation VLS-TS auprès de l'OFII (3 mois à compter de
     * l'entrée en France, art. R. 311-3 CESEDA). Outil FRANCE uniquement.
     *
     * <ul>
     *   <li>{@code OK} si statut {@code VALIDE} ou {@code A_VALIDER} (marge confortable)</li>
     *   <li>{@code WARNING} si statut {@code URGENT} (fenêtre courte ≤ 15 j.)</li>
     *   <li>{@code ALERT} si statut {@code EXPIRE} (délai dépassé, risque d'irrégularité)</li>
     * </ul>
     */
    private DashboardTile tileFromVlsTsValidationAnalysis(UUID caseFileId) {
        return vlsTsValidationRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), VlsTsValidationResult.class);
                VlsTsValidationStatut statut = r.statut();
                String secondary = statut == VlsTsValidationStatut.VALIDE
                        ? "Validation OFII effectuée"
                        : (r.joursRestantsValidation() != null
                                ? r.joursRestantsValidation() + " j. avant l'échéance"
                                : "Échéance non déterminée");
                String alert = switch (statut == null ? VlsTsValidationStatut.A_VALIDER : statut) {
                    case VALIDE, A_VALIDER -> "OK";
                    case URGENT -> "WARNING";
                    case EXPIRE -> "ALERT";
                };
                return new DashboardTile(
                        "F-IM-28-vls-ts-validation-ofii-fr",
                        "DELAIS",
                        "Validation VLS-TS OFII",
                        statut != null ? statut.name() : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** SF-214-13 — F-IM-31 délai de dépôt du renouvellement du titre (R. 433-1 CESEDA, FR). */
    private DashboardTile tileFromRenouvellementDelaiAnalysis(UUID caseFileId) {
        return renouvellementDelaiRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), RenouvellementDelaiResult.class);
                RenouvellementDelaiStatut statut = r.statut();
                RenouvellementDelaiStatut effectif =
                        statut == null ? RenouvellementDelaiStatut.A_DEPOSER : statut;
                String secondary = switch (effectif) {
                    case DEPOSE -> r.alerteRetard()
                            ? "Dépôt effectué hors délai"
                            : "Demande de renouvellement déposée";
                    case EXPIRE -> "Titre expiré sans dépôt";
                    default -> r.joursRestantsAvantOptimal() != null
                            ? r.joursRestantsAvantOptimal() + " j. avant la date optimale de dépôt"
                            : "Échéance non déterminée";
                };
                String alert = switch (effectif) {
                    case EN_AVANCE, DEPOSE -> "OK";
                    case A_DEPOSER -> "OK";
                    case A_DEPOSER_URGENT -> "WARNING";
                    case EXPIRE -> "ALERT";
                };
                return new DashboardTile(
                        "F-IM-31-renouvellement-delai-depot-fr",
                        "DELAIS",
                        "Renouvellement — délai de dépôt",
                        effectif.name(),
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────
    // SF-DT-36-03 — 11 outils Famille BE orphelins du dashboard. Chacun
    // calculait/persistait son résultat sans émettre de tuile. Le verdict est
    // exposé en valeur principale ; alertLevel mappé via mapVerdictDecisionnel
    // pour les verdicts à sémantique universelle, null sinon (cf. mini-spec).
    // ─────────────────────────────────────────────────────────────────────

    /** acceptation-renonciation-succession — option successorale (Cciv 768+). */
    private DashboardTile tileFromAcceptationRenonciationSuccessionAnalysis(UUID caseFileId) {
        return acceptationRenonciationSuccessionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getResultData(), AcceptationRenonciationSuccessionResult.class);
                return new DashboardTile(
                        "acceptation-renonciation-succession",
                        "VALIDITE",
                        "Option successorale",
                        r.optionRecommandee() != null ? r.optionRecommandee() : "—",
                        r.delaiRestantJours() + " j. avant expiration du droit d'option",
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** autorite-parentale-be — autorité parentale belge. */
    private DashboardTile tileFromAutoriteParentaleBeAnalysis(UUID caseFileId) {
        return autoriteParentaleBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), AutoriteParentaleBeResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                return new DashboardTile(
                        "autorite-parentale-be",
                        "VALIDITE",
                        "Autorité parentale (BE)",
                        verdict != null ? verdict : "—",
                        null,
                        mapVerdictDecisionnel(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** contribution-alimentaire-enfants-be — contribution alimentaire enfants (BE). */
    private DashboardTile tileFromContributionAlimentaireEnfantsBeAnalysis(UUID caseFileId) {
        return contributionAlimentaireEnfantsBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), ContributionAlimentaireEnfantsBeResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                String secondary = r.contributionMensuelleNette() != null
                        ? formatEuros(r.contributionMensuelleNette()) + " €/mois"
                        : null;
                return new DashboardTile(
                        "contribution-alimentaire-enfants-be",
                        "INDEMNITES",
                        "Contribution enfants (BE)",
                        verdict != null ? verdict : "—",
                        secondary,
                        mapVerdictDecisionnel(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** contribution-conjoint-be — pension alimentaire entre ex-époux (BE). */
    private DashboardTile tileFromContributionConjointBeAnalysis(UUID caseFileId) {
        return contributionConjointBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), ContributionConjointBeResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                String secondary = r.montantMensuelIndicatif() != null
                        ? formatEuros(r.montantMensuelIndicatif()) + " €/mois"
                        : null;
                return new DashboardTile(
                        "contribution-conjoint-be",
                        "INDEMNITES",
                        "Pension entre ex-époux (BE)",
                        verdict != null ? verdict : "—",
                        secondary,
                        mapVerdictDecisionnel(verdict));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** divorce-dc-be — divorce par consentement mutuel (BE). */
    private DashboardTile tileFromDivorceDcBeAnalysis(UUID caseFileId) {
        return divorceDcBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceDcBeResult.class);
                return new DashboardTile(
                        "divorce-dc-be",
                        "VALIDITE",
                        "Divorce consentement mutuel (BE)",
                        r.verdict() != null ? r.verdict() : "—",
                        null,
                        mapVerdictDecisionnel(r.verdict()));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** divorce-ddi-3voies-be — divorce pour désunion irrémédiable (BE). */
    private DashboardTile tileFromDivorceDdiBeAnalysis(UUID caseFileId) {
        return divorceDdiBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceDdiBeResult.class);
                return new DashboardTile(
                        "divorce-ddi-3voies-be",
                        "DELAIS",
                        "Divorce désunion irrémédiable (BE)",
                        r.voieRecommandee() != null ? r.voieRecommandee() : "—",
                        r.joursSeparation() + " j. depuis la séparation",
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** liquidation-partage-be — liquidation-partage notarial (BE). */
    private DashboardTile tileFromLiquidationPartageBeAnalysis(UUID caseFileId) {
        return liquidationPartageBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), LiquidationPartageBeResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                return new DashboardTile(
                        "liquidation-partage-be",
                        "DELAIS",
                        "Liquidation-partage (BE)",
                        verdict != null ? verdict : "—",
                        r.prochaineEtape(),
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** mediation-familiale-pre-saisine — médiation familiale préalable JAF. */
    private DashboardTile tileFromMediationFamilialePreSaisineAnalysis(UUID caseFileId) {
        return mediationFamilialePreSaisineRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getResultData(), MediationFamilialePreSaisineResult.class);
                return new DashboardTile(
                        "mediation-familiale-pre-saisine",
                        "DOCUMENTS",
                        "Médiation familiale préalable",
                        r.verdict() != null ? r.verdict() : "—",
                        null,
                        mapVerdictDecisionnel(r.verdict()));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** pacte-successoral-be-2018 — pacte successoral global (BE). */
    private DashboardTile tileFromPacteSuccessoralBe2018Analysis(UUID caseFileId) {
        return pacteSuccessoralBe2018Repo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getResultData(), PacteSuccessoralBe2018Result.class);
                return new DashboardTile(
                        "pacte-successoral-be-2018",
                        "VALIDITE",
                        "Pacte successoral (BE)",
                        r.verdict() != null ? r.verdict() : "—",
                        null,
                        mapVerdictDecisionnel(r.verdict()));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** regime-mat-be-communaute-legale — régime de communauté légale (BE). */
    private DashboardTile tileFromRegimeCommunauteLegaleBeAnalysis(UUID caseFileId) {
        return regimeCommunauteLegaleBeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), RegimeCommunauteLegaleBeResponse.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                return new DashboardTile(
                        "regime-mat-be-communaute-legale",
                        "DIAGNOSTIC",
                        "Régime communauté légale (BE)",
                        verdict != null ? verdict : "—",
                        null,
                        null);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /** tribunal-famille-be-mesures-prov — mesures provisoires tribunal famille (BE). */
    private DashboardTile tileFromTribunalFamilleBeMesuresProvisoiresAnalysis(UUID caseFileId) {
        return tribunalFamilleBeMesuresProvisoiresRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getResultData(), TribunalFamilleBeMesuresProvisoiresResult.class);
                return new DashboardTile(
                        "tribunal-famille-be-mesures-prov",
                        "DELAIS",
                        "Mesures provisoires TF (BE)",
                        r.verdict() != null ? r.verdict() : "—",
                        "Score urgence " + r.scoreUrgence() + "/100",
                        null);
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

    /**
     * F-207 SF-207-02 — Checklist C4 ONEM (Travail BE).
     *
     * <p>Verdict {@code CONFORME} / {@code NON_CONFORME} / {@code RISQUE_EXCLUSION_FAUTE_GRAVE}
     * (art. 144 AR 25/11/1991 ; mentions obligatoires + risque exclusion ONEM
     * 4-52 semaines en cas de faute grave). Tuile câblée par F-245 hotfix CI
     * master pour résorber l'orphan détecté par {@code DashboardTileToolIdIntegrityIT}.</p>
     */
    private DashboardTile tileFromC4OnemChecklistAnalysis(UUID caseFileId) {
        return c4OnemChecklistRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), C4OnemChecklistResult.class);
                String verdict = r.verdict() != null ? r.verdict().name() : null;
                String alert = mapVerdictC4Onem(verdict);
                String secondary;
                if (r.exclusionOnemRange() != null) {
                    secondary = "Exclusion ONEM "
                            + r.exclusionOnemRange().minSemaines()
                            + "-" + r.exclusionOnemRange().maxSemaines() + " sem.";
                } else if (r.mentionsManquantes() != null && !r.mentionsManquantes().isEmpty()) {
                    secondary = r.mentionsManquantes().size() + " mention(s) manquante(s)";
                } else {
                    secondary = "Mentions conformes";
                }
                return new DashboardTile(
                        "c4-onem-checklist",
                        "DOCUMENTS",
                        "C4 ONEM",
                        verdict != null ? verdict : "—",
                        secondary,
                        alert);
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    /**
     * F-207 SF-207-01 — Prescription d'une action de droit du travail belge.
     *
     * <p>Verdict {@code PRESCRIT} / {@code IMMINENT} / {@code NON_PRESCRIT}
     * (Loi 03/07/1978 art. 15 + CCT 109 art. 11 ; 5 ans arriérés salaire pendant
     * le contrat). Tuile cablée par F-245 hotfix CI master pour résorber
     * l'orphan détecté par {@code DashboardTileToolIdIntegrityIT}.</p>
     */
    private DashboardTile tileFromPrescriptionBeLitigeTravailAnalysis(UUID caseFileId) {
        return prescriptionBeLitigeTravailRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PrescriptionBeLitigeTravailResult.class);
                String alert = mapVerdictPrescription(r.verdict());
                String secondary = r.dateLimitePrescription() != null
                        ? "Échéance " + r.dateLimitePrescription() + " (" + r.joursRestants() + " j)"
                        : r.joursRestants() + " j restants";
                return new DashboardTile(
                        "prescription-be-litige-travail",
                        "DELAIS",
                        "Prescription Travail BE",
                        r.verdict() != null ? r.verdict() : "—",
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
                        "F-IM-08-oqtf-avec-delai-fr",
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
                        "F-IM-08-oqtf-sans-delai-fr",
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
                        "F-IM-08-referes-admin-fr",
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
                        "F-IM-19-mineurs",
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

    /**
     * SF-DT-36-03 — mapping verdict F-DT-36 → {@code alertLevel} de la tuile
     * dashboard. Distinct de {@link #mapVerdictRisque} : F-DT-36 rend un verdict
     * de <em>gravité</em> (et non de probabilité favorable), où
     * {@code NULLITE_AVEREE} est le résultat critique. Convention couleur figée
     * par SF-DT-36-02 : rouge réservé à {@code NULLITE_AVEREE}.
     */
    private static String mapVerdictNullite(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "NULLITE_AVEREE" -> "ALERT";
            case "NULLITE_PROBABLE" -> "WARNING";
            case "PROCEDURE_REGULIERE" -> "OK";
            default -> null;
        };
    }

    /**
     * SF-DT-38-02 — mapping verdict F-DT-38 → {@code alertLevel} de la tuile
     * dashboard. NULLE et ILLEGALE_REQUALIF_LICENCIEMENT sont les deux états
     * critiques (rouge) ; RISQUE_ABUSIVE est WARNING (or) ; REGULIERE est OK
     * (navy).
     */
    private static String mapVerdictRupturePeriodeEssai(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "NULLE", "ILLEGALE_REQUALIF_LICENCIEMENT" -> "ALERT";
            case "RISQUE_ABUSIVE" -> "WARNING";
            case "REGULIERE" -> "OK";
            default -> null;
        };
    }

    /**
     * SF-206-01 — mapping du verdict F-DT-42 (abandon de poste / présomption
     * de démission, FR) → {@code alertLevel} de la tuile dashboard. Du point
     * de vue de l'avocat du salarié : une contestation solide est une
     * <b>opportunité</b> (verdict OK / vert), une contestation difficile est
     * un signal d'alerte (rouge — la présomption opère).
     */
    private static String mapVerdictAbandonPoste(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "CONTESTATION_SOLIDE" -> "OK";
            case "CONTESTATION_INCERTAINE" -> "WARNING";
            case "CONTESTATION_DIFFICILE" -> "ALERT";
            default -> null;
        };
    }

    /**
     * F-207 SF-207-02 — mapping verdict C4 ONEM → {@code alertLevel} de la tuile
     * dashboard. Convention : {@code RISQUE_EXCLUSION_FAUTE_GRAVE} = ALERT
     * (risque pécuniaire majeur ONEM 4-52 sem.), {@code NON_CONFORME} = WARNING
     * (mentions manquantes — rectification possible), {@code CONFORME} = OK.
     */
    private static String mapVerdictC4Onem(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "CONFORME" -> "OK";
            case "NON_CONFORME" -> "WARNING";
            case "RISQUE_EXCLUSION_FAUTE_GRAVE" -> "ALERT";
            default -> null;
        };
    }

    /**
     * F-207 SF-207-01 — mapping verdict prescription Travail BE → {@code alertLevel}
     * de la tuile dashboard. Convention : {@code PRESCRIT} = ALERT (délai dépassé,
     * action irrecevable), {@code IMMINENT} = WARNING (≤ 30 j restants),
     * {@code NON_PRESCRIT} = OK (délai confortable).
     */
    private static String mapVerdictPrescription(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "NON_PRESCRIT" -> "OK";
            case "IMMINENT" -> "WARNING";
            case "PRESCRIT" -> "ALERT";
            default -> null;
        };
    }

    /** SF-206-01 — libellé court du verdict abandon de poste pour la tile primary. */
    private static String libelleVerdictAbandonPoste(String verdict) {
        if (verdict == null) return "—";
        return switch (verdict) {
            case "CONTESTATION_SOLIDE" -> "Contestation solide";
            case "CONTESTATION_INCERTAINE" -> "Contestation incertaine";
            case "CONTESTATION_DIFFICILE" -> "Contestation difficile";
            default -> "—";
        };
    }

    /**
     * SF-206-05 — mapping du verdict F-DT-39 (prise d'acte de la rupture, FR)
     * → {@code alertLevel} de la tuile dashboard. Du point de vue de l'avocat
     * du salarié : une prise d'acte favorable est une opportunité (OK / vert),
     * une prise d'acte risquée est un signal d'attention (WARNING), une prise
     * d'acte défavorable est une catastrophe (ALERT — la prise d'acte
     * notifiée à tort produit les effets d'une démission, perte des
     * indemnités de licenciement et des allocations chômage).
     */
    private static String mapVerdictPriseActe(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "PRISE_ACTE_FAVORABLE" -> "OK";
            case "PRISE_ACTE_RISQUEE" -> "WARNING";
            case "PRISE_ACTE_DEFAVORABLE" -> "ALERT";
            default -> null;
        };
    }

    /** SF-206-05 — libellé court du verdict prise d'acte pour la tile primary. */
    private static String libelleVerdictPriseActe(String verdict) {
        if (verdict == null) return "—";
        return switch (verdict) {
            case "PRISE_ACTE_FAVORABLE" -> "Prise d'acte favorable";
            case "PRISE_ACTE_RISQUEE" -> "Prise d'acte risquée";
            case "PRISE_ACTE_DEFAVORABLE" -> "Prise d'acte défavorable";
            default -> "—";
        };
    }

    /**
     * SF-206-07 — mapping du verdict F-DT-40 (résiliation judiciaire CPH, FR)
     * → {@code alertLevel} de la tuile dashboard. Du point de vue de l'avocat
     * du salarié : une résiliation favorable est une opportunité (OK / vert),
     * une résiliation incertaine est un signal d'attention (WARNING), une
     * résiliation défavorable est mappée en {@code OK} — choix structurel :
     * contrairement à la prise d'acte (effet immédiat sur le contrat), la
     * résiliation judiciaire est une <b>voie sans risque de rupture</b>. Un
     * rejet ne rompt PAS le contrat, le salarié reste en poste. Le verdict
     * défavorable est donc un signal d'orientation ("ne pas engager
     * l'instance") et non d'alerte rétrospective.
     */
    private static String mapVerdictResiliationJud(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "RESILIATION_FAVORABLE" -> "OK";
            case "RESILIATION_INCERTAINE" -> "WARNING";
            case "RESILIATION_DEFAVORABLE" -> "OK";
            default -> null;
        };
    }

    /** SF-206-07 — libellé court du verdict résiliation judiciaire pour la tile primary. */
    private static String libelleVerdictResiliationJud(String verdict) {
        if (verdict == null) return "—";
        return switch (verdict) {
            case "RESILIATION_FAVORABLE" -> "Résiliation favorable";
            case "RESILIATION_INCERTAINE" -> "Résiliation incertaine";
            case "RESILIATION_DEFAVORABLE" -> "Résiliation défavorable";
            default -> "—";
        };
    }

    /**
     * SF-206-03 — mapping du verdict F-DT-75 (congés payés sur arrêt maladie,
     * FR) → {@code alertLevel} de la tuile dashboard. Du point de vue de
     * l'avocat du salarié : un rappel significatif est une <b>opportunité
     * monétaire</b> mais avec une <b>action à engager rapidement</b>
     * (WARNING — call to action) ; un rappel limité ou pas de rappel n'appelle
     * pas d'action urgente (OK) ; une action forclose est un signal d'alerte
     * (rouge — délai dépassé).
     */
    private static String mapVerdictCongesPayesArretMaladie(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "RAPPEL_SIGNIFICATIF" -> "WARNING";
            case "RAPPEL_LIMITE" -> "OK";
            case "PAS_DE_RAPPEL" -> "OK";
            case "ACTION_FORCLOSE" -> "ALERT";
            default -> null;
        };
    }

    /** SF-206-03 — libellé court du verdict F-DT-75 pour la tile primary. */
    private static String libelleVerdictCongesPayesArretMaladie(String verdict) {
        if (verdict == null) return "—";
        return switch (verdict) {
            case "RAPPEL_SIGNIFICATIF" -> "Rappel significatif";
            case "RAPPEL_LIMITE" -> "Rappel limité";
            case "PAS_DE_RAPPEL" -> "Pas de rappel";
            case "ACTION_FORCLOSE" -> "Action forclose";
            default -> "—";
        };
    }

    /**
     * SF-DT-36-03 — mapping du statut de délai → {@code alertLevel} de la tuile
     * dashboard, commun aux outils contentieux Immigration FR à délai
     * (F-IM-21 JLD rétention, F-IM-22 Dublin, F-IM-23 CRRV). Statut figé par
     * les calculateurs : {@code DISPONIBLE} / {@code RECOURS_FORME} (délai
     * ouvert ou recours déjà formé), {@code URGENT} (échéance proche),
     * {@code EXPIRE} (délai dépassé — critique).
     */
    private static String mapStatutDelai(String statut) {
        if (statut == null) return null;
        return switch (statut) {
            case "DISPONIBLE", "RECOURS_FORME" -> "OK";
            case "URGENT" -> "WARNING";
            case "EXPIRE" -> "ALERT";
            default -> null;
        };
    }

    /**
     * SF-DT-36-03 — mapping {@code alertLevel} pour les verdicts à sémantique
     * universelle des outils Famille BE (validité d'un acte, recevabilité d'une
     * demande). Les verdicts spécifiques à un outil (CONTRIBUTION_DUE,
     * VOIE_2_*, EN_COURS, etc.) ne sont volontairement pas mappés : la tuile
     * reste neutre ({@code null}) plutôt que d'imposer une couleur arbitraire.
     */
    private static String mapVerdictDecisionnel(String verdict) {
        if (verdict == null) return null;
        return switch (verdict) {
            case "VALIDE", "RECEVABLE" -> "OK";
            case "CONTESTABLE" -> "WARNING";
            case "NUL", "IRRECEVABLE" -> "ALERT";
            default -> null;
        };
    }

    /**
     * SF-212-01 — F-DT-36-licenciement-faute-grave-lourde (FRANCE UNIQUEMENT).
     *
     * <p>Thème {@code DIAGNOSTIC} : qualification disciplinaire et impact
     * financier sur les indemnités de rupture.
     * Mapping alertLevel :
     * <ul>
     *   <li>{@code FAUTE_LOURDE} → {@code ALERT} (impact maximal, intention de nuire)</li>
     *   <li>{@code FAUTE_GRAVE} → {@code WARNING} (perte préavis + IL légale)</li>
     *   <li>{@code FAUTE_SIMPLE} → {@code OK} (droits préservés)</li>
     * </ul>
     */
    private DashboardTile tileFromLicenciementFauteGraveLourdAnalysis(UUID caseFileId) {
        return licenciementFauteGraveLourdRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(
                        e.getSnapshotData(), LicenciementFauteGraveLourdResponse.class);
                String qualification = r.qualificationRetenue() != null
                        ? r.qualificationRetenue().name() : null;
                String primary = libelleQualificationFauteGrave(qualification);
                String secondary = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE)
                        .format(Math.round(r.totalIndemnitesDuesEuros())) + " € d'indemnités dues";
                return new DashboardTile(
                        "F-DT-36-licenciement-faute-grave-lourde",
                        "DIAGNOSTIC",
                        "Faute grave / faute lourde",
                        primary,
                        secondary,
                        mapAlertLevelQualificationFaute(qualification));
            } catch (Exception ex) {
                return null;
            }
        }).orElse(null);
    }

    private static String libelleQualificationFauteGrave(String qualification) {
        if (qualification == null) return "Qualification non déterminée";
        return switch (qualification) {
            case "FAUTE_LOURDE" -> "Faute lourde retenue";
            case "FAUTE_GRAVE"  -> "Faute grave retenue";
            case "FAUTE_SIMPLE" -> "Faute simple";
            default             -> qualification;
        };
    }

    private static String mapAlertLevelQualificationFaute(String qualification) {
        if (qualification == null) return null;
        return switch (qualification) {
            case "FAUTE_LOURDE" -> "ALERT";
            case "FAUTE_GRAVE"  -> "WARNING";
            case "FAUTE_SIMPLE" -> "OK";
            default             -> null;
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
