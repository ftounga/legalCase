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
import java.util.UUID;

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
                                     DivorceChecklistRepository divorceRepo) {
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
                buildDivorce(caseFileId)
        );
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
