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
                                     ChangementStatutRepository changementStatutRepo) {
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
