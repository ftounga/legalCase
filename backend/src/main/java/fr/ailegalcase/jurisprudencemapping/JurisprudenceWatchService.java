package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * F-JU-01 / SF-JU-01-02 — orchestrateur du cron veille mensuelle.
 *
 * <p>Pipeline :
 * <ol>
 *   <li>{@link JudilibreApiClient#fetchArretsForPeriod} sur le mois écoulé</li>
 *   <li>Pour chaque mapping non archivé : pré-filtre des arrêts par
 *       chevauchement de mots-clés (matière / juridiction)</li>
 *   <li>{@link ClaudeJurisprudenceEvaluator#evaluate} → {@link ClaudeEvaluation}</li>
 *   <li>Dispatching selon {@link TrustMode} et seuils :
 *       <ul>
 *         <li>confidence ≥ autoActionThreshold + action ∈ {CONFIRM, ADD, REPLACE, ARCHIVE} → action auto</li>
 *         <li>confidence ∈ [pendingThreshold, autoActionThreshold) → flag PENDING</li>
 *         <li>confidence &lt; pendingThreshold → silence (NONE non écrit)</li>
 *       </ul></li>
 *   <li>Garde-fou alerte massive : si {@code AUTO_REPLACE + AUTO_ARCHIVE > alertThresholdPercent} → abort</li>
 *   <li>{@link JurisprudenceWatchEmailService#sendMonthlyRecap} ou {@code sendAbortAlert}</li>
 * </ol>
 *
 * <p>Toutes les écritures passent par {@link JurisprudenceAuditLog} (traçabilité
 * rejouable).</p>
 */
@Service
public class JurisprudenceWatchService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceWatchService.class);
    private static final BigDecimal DEFAULT_AUTO_ACTION_THRESHOLD = new BigDecimal("0.85");
    private static final BigDecimal DEFAULT_ADD_THRESHOLD = new BigDecimal("0.80");
    private static final BigDecimal DEFAULT_PENDING_THRESHOLD = new BigDecimal("0.60");

    private final ToolJurisprudenceMappingRepository mappingRepository;
    private final JurisprudenceWatchFlagRepository flagRepository;
    private final JurisprudenceAuditLogRepository auditLogRepository;
    private final JudilibreApiClient judilibreClient;
    private final ClaudeJurisprudenceEvaluator evaluator;
    private final JurisprudenceWatchEmailService emailService;
    private final TrustMode trustMode;
    private final int alertThresholdPercent;

    public JurisprudenceWatchService(ToolJurisprudenceMappingRepository mappingRepository,
                                     JurisprudenceWatchFlagRepository flagRepository,
                                     JurisprudenceAuditLogRepository auditLogRepository,
                                     JudilibreApiClient judilibreClient,
                                     ClaudeJurisprudenceEvaluator evaluator,
                                     JurisprudenceWatchEmailService emailService,
                                     @Value("${jurisprudence.watch.trust-mode:AUTO_PILOT}") String trustModeValue,
                                     @Value("${jurisprudence.watch.alert-threshold-percent:5}") int alertThresholdPercent) {
        this.mappingRepository = mappingRepository;
        this.flagRepository = flagRepository;
        this.auditLogRepository = auditLogRepository;
        this.judilibreClient = judilibreClient;
        this.evaluator = evaluator;
        this.emailService = emailService;
        this.trustMode = TrustMode.valueOf(trustModeValue.toUpperCase(Locale.ROOT));
        this.alertThresholdPercent = alertThresholdPercent;
    }

    @Transactional
    public JurisprudenceWatchRunSummary runMonthlyWatch() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate end = today.withDayOfMonth(1);
        LocalDate start = end.minusMonths(1);
        return runForPeriod(start, end);
    }

    @Transactional
    public JurisprudenceWatchRunSummary runForPeriod(LocalDate startInclusive, LocalDate endExclusive) {
        log.info("F-JU-01 — JurisprudenceWatchService starting for [{}, {}) trustMode={}",
                startInclusive, endExclusive, trustMode);

        List<JudilibreArret> arrets = judilibreClient.fetchArretsForPeriod(startInclusive, endExclusive);
        List<ToolJurisprudenceMapping> mappings = mappingRepository.findAll().stream()
                .filter(m -> !m.isArchived())
                .toList();

        int total = mappings.size();
        int autoConfirm = 0, autoAdd = 0, autoReplace = 0, autoArchive = 0;
        int flagsPending = 0, skipped = 0;
        boolean aborted = false;
        String abortReason = null;

        for (ToolJurisprudenceMapping mapping : mappings) {
            List<JudilibreArret> candidates = filterCandidates(mapping, arrets);
            if (candidates.isEmpty()) {
                continue;
            }
            ClaudeEvaluation evaluation;
            try {
                evaluation = evaluator.evaluate(mapping, candidates);
            } catch (Exception e) {
                log.warn("F-JU-01 — Claude eval failed for mapping {}: {}", mapping.getId(), e.getMessage());
                skipped++;
                continue;
            }

            DispatchResult dispatch = dispatch(mapping, evaluation);
            switch (dispatch) {
                case AUTO_CONFIRM -> autoConfirm++;
                case AUTO_ADD -> autoAdd++;
                case AUTO_REPLACE -> autoReplace++;
                case AUTO_ARCHIVE -> autoArchive++;
                case FLAG_PENDING -> flagsPending++;
                case SILENCE -> { /* rien à compter */ }
                case ERROR -> skipped++;
            }

            int riskyActions = autoReplace + autoArchive;
            if (total > 0 && riskyActions * 100 > alertThresholdPercent * total) {
                aborted = true;
                abortReason = "Seuil alerte massive franchi : "
                        + riskyActions + " actions REPLACE/ARCHIVE sur " + total
                        + " mappings (> " + alertThresholdPercent + "%)";
                log.warn("F-JU-01 — abort: {}", abortReason);
                break;
            }
        }

        int mappingsEvalues = autoConfirm + autoAdd + autoReplace + autoArchive + flagsPending;
        JurisprudenceWatchRunSummary summary = new JurisprudenceWatchRunSummary(
                startInclusive, endExclusive,
                arrets.size(), mappingsEvalues,
                autoConfirm, autoAdd, autoReplace, autoArchive,
                flagsPending, skipped,
                aborted, abortReason);

        if (aborted) {
            emailService.sendAbortAlert(summary);
        } else {
            emailService.sendMonthlyRecap(summary);
        }
        log.info("F-JU-01 — JurisprudenceWatchService done: {}", summary);
        return summary;
    }

    DispatchResult dispatch(ToolJurisprudenceMapping mapping, ClaudeEvaluation evaluation) {
        if (evaluation.action() == EvaluationAction.NONE) {
            return DispatchResult.SILENCE;
        }
        BigDecimal score = evaluation.confidenceScore() == null ? BigDecimal.ZERO : evaluation.confidenceScore();
        if (score.compareTo(DEFAULT_PENDING_THRESHOLD) < 0) {
            return DispatchResult.SILENCE;
        }

        boolean confidentEnough = score.compareTo(DEFAULT_AUTO_ACTION_THRESHOLD) >= 0;
        boolean addConfident = score.compareTo(DEFAULT_ADD_THRESHOLD) >= 0;

        // En PARANOIA toute décision passe en flag PENDING (jamais auto-action).
        if (trustMode == TrustMode.PARANOIA) {
            createPendingFlag(mapping, evaluation, JurisprudenceWatchFlagSource.CRON);
            return DispatchResult.FLAG_PENDING;
        }

        // EQUILIBRE / AUTO_PILOT
        return switch (evaluation.action()) {
            case CONFIRM -> {
                if (confidentEnough) {
                    applyConfirm(mapping, evaluation);
                    yield DispatchResult.AUTO_CONFIRM;
                }
                createPendingFlag(mapping, evaluation, JurisprudenceWatchFlagSource.CRON);
                yield DispatchResult.FLAG_PENDING;
            }
            case ADD -> {
                if (addConfident && evaluation.arretChoisi() != null) {
                    applyAdd(mapping, evaluation);
                    yield DispatchResult.AUTO_ADD;
                }
                createPendingFlag(mapping, evaluation, JurisprudenceWatchFlagSource.CRON);
                yield DispatchResult.FLAG_PENDING;
            }
            case REPLACE -> {
                if (confidentEnough && evaluation.arretChoisi() != null) {
                    applyReplace(mapping, evaluation);
                    yield DispatchResult.AUTO_REPLACE;
                }
                createPendingFlag(mapping, evaluation, JurisprudenceWatchFlagSource.CRON);
                yield DispatchResult.FLAG_PENDING;
            }
            case ARCHIVE -> {
                if (confidentEnough) {
                    applyArchive(mapping, evaluation);
                    yield DispatchResult.AUTO_ARCHIVE;
                }
                createPendingFlag(mapping, evaluation, JurisprudenceWatchFlagSource.CRON);
                yield DispatchResult.FLAG_PENDING;
            }
            case NONE -> DispatchResult.SILENCE;
        };
    }

    private void applyConfirm(ToolJurisprudenceMapping mapping, ClaudeEvaluation evaluation) {
        mapping.setLastVerifiedAt(Instant.now());
        mappingRepository.save(mapping);
        writeAuditLog(mapping, JurisprudenceAuditAction.AUTO_CONFIRM, evaluation);
    }

    private void applyAdd(ToolJurisprudenceMapping mapping, ClaudeEvaluation evaluation) {
        ToolJurisprudenceMapping added = newMappingFrom(mapping, evaluation.arretChoisi(), evaluation.confidenceScore());
        mappingRepository.save(added);
        // L'ajout est tracé sur le nouveau mapping (pour rendre l'audit log rejouable
        // côté arrêt ajouté). Le mapping existant n'est pas modifié.
        writeAuditLog(added, JurisprudenceAuditAction.AUTO_ADD, evaluation);
    }

    private void applyReplace(ToolJurisprudenceMapping mapping, ClaudeEvaluation evaluation) {
        ToolJurisprudenceMapping added = newMappingFrom(mapping, evaluation.arretChoisi(), evaluation.confidenceScore());
        mapping.setArchived(true);
        mappingRepository.save(mapping);
        mappingRepository.save(added);
        writeAuditLog(added, JurisprudenceAuditAction.AUTO_REPLACE, evaluation);
    }

    private void applyArchive(ToolJurisprudenceMapping mapping, ClaudeEvaluation evaluation) {
        mapping.setArchived(true);
        mappingRepository.save(mapping);
        writeAuditLog(mapping, JurisprudenceAuditAction.AUTO_ARCHIVE, evaluation);
    }

    private void createPendingFlag(ToolJurisprudenceMapping mapping, ClaudeEvaluation evaluation,
                                   JurisprudenceWatchFlagSource source) {
        JurisprudenceWatchFlag flag = new JurisprudenceWatchFlag();
        flag.setToolId(mapping.getToolId());
        flag.setBrancheCalculId(mapping.getBrancheCalculId());
        flag.setArretEntrantRef(evaluation.arretChoisi() == null ? "(action " + evaluation.action() + ")"
                : evaluation.arretChoisi().ref());
        flag.setMappingActuel(mapping);
        flag.setSource(source);
        flag.setConfidenceScore(evaluation.confidenceScore());
        flag.setExplication(evaluation.raison());
        flag.setStatut(JurisprudenceWatchFlagStatut.PENDING);
        flagRepository.save(flag);
    }

    private ToolJurisprudenceMapping newMappingFrom(ToolJurisprudenceMapping base, JudilibreArret arret,
                                                    BigDecimal confidenceScore) {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setToolId(base.getToolId());
        m.setBrancheCalculId(base.getBrancheCalculId());
        m.setArretRef(arret.ref());
        m.setJuridiction(arret.juridiction());
        m.setDateArret(arret.dateArret());
        m.setNumeroPourvoi(arret.numeroPourvoi());
        m.setLienLegifrance(arret.lienLegifrance());
        m.setChapeauOfficiel(arret.chapeauOfficiel());
        m.setLastVerifiedAt(Instant.now());
        m.setConfidenceScore(confidenceScore == null ? BigDecimal.ZERO : confidenceScore);
        m.setArchived(false);
        return m;
    }

    private void writeAuditLog(ToolJurisprudenceMapping mapping, JurisprudenceAuditAction action,
                               ClaudeEvaluation evaluation) {
        JurisprudenceAuditLog logEntry = new JurisprudenceAuditLog();
        logEntry.setMapping(mapping);
        logEntry.setAction(action);
        logEntry.setActor(JurisprudenceAuditActor.CRON);
        logEntry.setClaudeConfidence(evaluation.confidenceScore());
        logEntry.setClaudeReason(evaluation.raison());
        auditLogRepository.save(logEntry);
    }

    /**
     * Pré-filtre les arrêts candidats par chevauchement minimal de juridiction
     * (heuristique simple V1, ex. on ne propose pas un arrêt de chambre civile
     * à un mapping de chambre sociale). Plus de filtrage par mots-clés en V2.
     */
    List<JudilibreArret> filterCandidates(ToolJurisprudenceMapping mapping, List<JudilibreArret> all) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        String mappingJuridiction = mapping.getJuridiction() == null ? "" : mapping.getJuridiction().toLowerCase(Locale.ROOT);
        List<JudilibreArret> filtered = new ArrayList<>();
        for (JudilibreArret arret : all) {
            String aj = arret.juridiction() == null ? "" : arret.juridiction().toLowerCase(Locale.ROOT);
            if (mappingJuridiction.isEmpty() || aj.isEmpty()) {
                filtered.add(arret);
                continue;
            }
            if (mappingJuridiction.contains(extractKey(aj)) || aj.contains(extractKey(mappingJuridiction))) {
                filtered.add(arret);
            }
        }
        // limite pragmatique à 20 candidats par mapping pour borner le coût LLM
        if (filtered.size() > 20) {
            return filtered.subList(0, 20);
        }
        return filtered;
    }

    private static String extractKey(String label) {
        // garde le premier mot signifiant (sociale / civile / commerciale / criminelle / état)
        for (String key : new String[]{"sociale", "civile", "commerciale", "criminelle", "etat", "état", "constitutionnelle"}) {
            if (label.contains(key)) return key;
        }
        return label.length() > 8 ? label.substring(0, 8) : label;
    }

    enum DispatchResult {
        AUTO_CONFIRM, AUTO_ADD, AUTO_REPLACE, AUTO_ARCHIVE, FLAG_PENDING, SILENCE, ERROR
    }
}
