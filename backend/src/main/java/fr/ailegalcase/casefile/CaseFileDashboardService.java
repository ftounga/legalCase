package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
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
                                     CreditTempsBeRepository creditTempsBeRepo) {
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
                buildLicenciement(caseFileId),
                buildIndemnite(caseFileId),
                buildAnciennete(caseFileId),
                buildTitleDecision(caseFileId),
                buildWorkRight(caseFileId),
                buildRecours(caseFileId),
                buildPartage(caseFileId),
                buildGarde(caseFileId),
                buildDivorce(caseFileId),
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
        tiles.sort(Comparator.comparing(DashboardTile::toolId));
        return tiles;
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
        // Réutilise la logique éprouvée de buildIndemnite() pour préserver la
        // priorité RuptureConv > Macron observée par SF-132-02. SF-167-05 fera
        // converger vers une lecture directe.
        var summary = buildIndemnite(caseFileId);
        if (summary == null) {
            return null;
        }
        BigDecimal basse = summary.fourchetteBasse();
        BigDecimal haute = summary.fourhetteHaute();
        String primary = formatEuros(basse) + " – " + formatEuros(haute) + " €";
        return new DashboardTile(
                "F-DT-09-comparateur-indemnites",
                "INDEMNITES",
                "Indemnités",
                primary,
                summary.baremeSource(),
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
     * priorité de {@link #buildIndemnite(UUID)}.
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

    private CaseFileDashboardResponse.LicenciementSummary buildLicenciement(UUID caseFileId) {
        return licenciementRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), LicenciementAnalysisResult.class);
                int nonConformes = (int) r.criteres().stream().filter(c -> "NON".equals(c.reponse())).count();
                return new CaseFileDashboardResponse.LicenciementSummary(r.scoreRisque(), r.verdict(), nonConformes, r.criteres().size());
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    private CaseFileDashboardResponse.IndemniteSummary buildIndemnite(UUID caseFileId) {
        // SF-132-02 : si une analyse "Indemnité rupture conventionnelle" existe,
        // elle prime sur la fourchette Macron (laquelle retournerait 0—0 € sur
        // ce type de dossier). Évite la card dashboard trompeuse "0 — 0 €"
        // observée sur le dossier E28.
        var ruptureConv = ruptureConvIndemniteRepo.findByCaseFileId(caseFileId)
                .map(e -> {
                    try {
                        var r = objectMapper.readValue(e.getResultData(), RuptureConvIndemniteResult.class);
                        return new CaseFileDashboardResponse.IndemniteSummary(
                                "FRANCE",
                                r.indemniteLegaleMinimum(),
                                r.indemniteLegaleMinimum(),
                                "Indemnité légale de licenciement (art. R1234-2)");
                    } catch (Exception ex) { return null; }
                }).orElse(null);
        if (ruptureConv != null) return ruptureConv;

        return indemniteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), IndemniteComparatifResult.class);
                // SF-132-03 : legacy NEGOCIATION_LIBRE (rupture amiable BE) — la
                // card avait affiché "0 — 0 €" à tort avant la refonte. L'outil
                // dédié vit désormais côté frontend (rupture-amiable-info-section).
                if ("NEGOCIATION_LIBRE".equals(r.displayMode())) return null;
                return new CaseFileDashboardResponse.IndemniteSummary(r.country(), r.fourchetteBasseMontant(), r.fourhetteHauteMontant(), r.baremeSource());
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    private CaseFileDashboardResponse.AncienneteSummary buildAnciennete(UUID caseFileId) {
        return ancienneteRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), AncienneteResult.class);
                int ecarts = (int) r.ecarts().stream().filter(ec -> "ECART".equals(ec.verdict())).count();
                return new CaseFileDashboardResponse.AncienneteSummary(r.ancienneteAnnees(), r.ancienneteMois(), r.congesTotalJours(), ecarts);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    private CaseFileDashboardResponse.TitleDecisionSummary buildTitleDecision(UUID caseFileId) {
        return titleDecisionRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var recs = objectMapper.readValue(e.getRecommendedTitles(),
                        objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, TitleRecommendation.class));
                var list = (java.util.List<TitleRecommendation>) recs;
                return new CaseFileDashboardResponse.TitleDecisionSummary(list.size(), list.isEmpty() ? null : list.get(0).label());
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    private CaseFileDashboardResponse.WorkRightSummary buildWorkRight(UUID caseFileId) {
        return workRightRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), WorkRightResult.class);
                return new CaseFileDashboardResponse.WorkRightSummary(r.droitTravail(), r.titreLabel());
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    private CaseFileDashboardResponse.RecoursSummary buildRecours(UUID caseFileId) {
        return recoursRepo.findByCaseFileId(caseFileId).map(e -> {
            var type = ImmigrationRecoursReferentiel.getByCode(e.getRecoursType());
            return new CaseFileDashboardResponse.RecoursSummary(
                    type != null ? type.label() : e.getRecoursType(),
                    e.getDateLimite() != null ? e.getDateLimite().toString() : null,
                    e.getDateLimite() != null && java.time.LocalDate.now().isAfter(e.getDateLimite()));
        }).orElse(null);
    }

    private CaseFileDashboardResponse.PartageSummary buildPartage(UUID caseFileId) {
        return partageRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), PartageImmobilierResult.class);
                return new CaseFileDashboardResponse.PartageSummary(r.soulte(), r.coutTotal());
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    private CaseFileDashboardResponse.GardeSummary buildGarde(UUID caseFileId) {
        return gardeRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), CalendrierGardeResult.class);
                return new CaseFileDashboardResponse.GardeSummary(r.gardeLabel(), r.joursParAnParentA(), r.joursParAnParentB());
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }

    private CaseFileDashboardResponse.DivorceSummary buildDivorce(UUID caseFileId) {
        return divorceRepo.findByCaseFileId(caseFileId).map(e -> {
            try {
                var r = objectMapper.readValue(e.getResultData(), DivorceChecklistResult.class);
                int total = r.etapesTotal() + r.piecesTotal();
                int done = r.etapesCompletees() + r.piecesPresentes();
                int pct = total > 0 ? (done * 100) / total : 0;
                return new CaseFileDashboardResponse.DivorceSummary(r.etapesCompletees(), r.etapesTotal(), r.piecesPresentes(), r.piecesTotal(), pct);
            } catch (Exception ex) { return null; }
        }).orElse(null);
    }
}
