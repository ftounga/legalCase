package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * F-JU-01 / SF-JU-01-05 — orchestrateur du bootstrap manuel des mappings
 * de jurisprudence par un super-admin.
 *
 * <p>Pour chaque entrée {@link JurisprudenceBootstrapEntry} : récupère les
 * candidats JUDILIBRE → demande à Claude de sélectionner 1-3 arrêts
 * structurants → INSERT dans {@code tool_jurisprudence_mappings} avec audit
 * log {@code AUTO_ADD} actor {@code SUPER_ADMIN}.</p>
 */
@Service
public class JurisprudenceBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceBootstrapService.class);

    /**
     * Sentinelle inscrite dans {@code arretRef} du {@code pseudoMapping} construit par
     * {@link #pseudoMappingFromEntry(JurisprudenceBootstrapEntry)} pour distinguer
     * le mode bootstrap initial (sans mapping pré-existant) du mode dérive
     * (cron veille mensuelle / dérive quotidienne sur un mapping réel).
     *
     * <p>Visibilité package : importée par {@link ClaudeJurisprudenceEvaluator} pour
     * basculer sur {@code SYSTEM_PROMPT_BOOTSTRAP} (SF-JU-01-08).</p>
     */
    static final String BOOTSTRAP_MAPPING_REF_PREFIX = "(bootstrap initial";

    private final JudilibreApiClient judilibreClient;
    private final JurisprudenceBeWebSearchClient beWebSearchClient;
    private final ClaudeJurisprudenceEvaluator evaluator;
    private final ToolJurisprudenceMappingRepository mappingRepository;
    private final JurisprudenceAuditLogRepository auditLogRepository;
    private final JurisprudenceBootstrapJobRepository jobRepository;
    private final TransactionTemplate txTemplate;
    private final TaskExecutor taskExecutor;
    private final JurisprudenceRelevanceGate relevanceGate;
    private final JudilibreQueryEnricher queryEnricher;

    /**
     * SF-JU-06-01 — seuil de confiance minimal pour retenir un mapping au bootstrap
     * (≥ seuil d'affichage 0,60 de SF-JU-01-FIX). En dessous, l'arrêt est rejeté.
     */
    private static final BigDecimal MIN_BOOTSTRAP_CONFIDENCE = new BigDecimal("0.70");

    public JurisprudenceBootstrapService(JudilibreApiClient judilibreClient,
                                         JurisprudenceBeWebSearchClient beWebSearchClient,
                                         ClaudeJurisprudenceEvaluator evaluator,
                                         ToolJurisprudenceMappingRepository mappingRepository,
                                         JurisprudenceAuditLogRepository auditLogRepository,
                                         JurisprudenceBootstrapJobRepository jobRepository,
                                         PlatformTransactionManager txManager,
                                         @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                                         JurisprudenceRelevanceGate relevanceGate,
                                         JudilibreQueryEnricher queryEnricher) {
        this.judilibreClient = judilibreClient;
        this.beWebSearchClient = beWebSearchClient;
        this.evaluator = evaluator;
        this.mappingRepository = mappingRepository;
        this.auditLogRepository = auditLogRepository;
        this.jobRepository = jobRepository;
        this.txTemplate = new TransactionTemplate(txManager);
        this.taskExecutor = taskExecutor;
        this.relevanceGate = relevanceGate;
        this.queryEnricher = queryEnricher;
    }

    /**
     * F-JU-04 SF-JU-04-02 — heuristique de détection des entrées belges.
     *
     * <p>Une entrée est considérée comme BE si son {@code toolId} ou
     * {@code brancheCalculId} contient un marqueur belge connu :
     * {@code -be-} (ex. {@code clause-non-concurrence-be}), {@code onem}
     * (Office national de l'emploi), {@code fedris} (Fonds des accidents
     * du travail BE), ou {@code juridat}.</p>
     */
    static boolean isBelgianEntry(JurisprudenceBootstrapEntry entry) {
        String t = entry.toolId() == null ? "" : entry.toolId().toLowerCase();
        String b = entry.brancheCalculId() == null ? "" : entry.brancheCalculId().toLowerCase();
        return isBeMarker(t) || isBeMarker(b);
    }

    private static boolean isBeMarker(String s) {
        if (s == null || s.isEmpty()) return false;
        // -be- au milieu (ex. rappel-salaire-be-prescription) OU -be à la fin (ex. clause-non-concurrence-be)
        if (s.contains("-be-") || s.endsWith("-be")) return true;
        // Acronymes BE-only sans suffixe -be (ONEM, FEDRIS, Juridat, Juportal).
        return s.contains("onem") || s.contains("fedris")
                || s.contains("juridat") || s.contains("juportal");
    }

    /**
     * SF-JU-01-10 — lance un bootstrap en arrière-plan et retourne immédiatement
     * son identifiant. Le client doit poller {@code GET /bootstrap/jobs/{jobId}}
     * pour suivre la progression jusqu'à un terminal state.
     */
    public JurisprudenceBootstrapJobStarted startBootstrap(JurisprudenceBootstrapRequest request, User triggerUser) {
        int total = request.entries().size();
        UUID userId = triggerUser != null ? triggerUser.getId() : null;
        Instant startedAt = Instant.now();

        UUID jobId = txTemplate.execute(status -> {
            JurisprudenceBootstrapJob job = new JurisprudenceBootstrapJob();
            job.setStatus(JurisprudenceBootstrapJobStatus.RUNNING);
            job.setEntriesTotal(total);
            job.setStartedAt(startedAt);
            job.setTriggeredByUserId(userId);
            jobRepository.save(job);
            return job.getId();
        });

        taskExecutor.execute(() -> runBootstrapJob(jobId, request, triggerUser));

        return new JurisprudenceBootstrapJobStarted(jobId, total, startedAt);
    }

    /**
     * SF-JU-01-10 — récupère l'état courant d'un job (lu via le repository).
     * Pas de transaction explicite : le repository ouvre une read-only.
     */
    public Optional<JurisprudenceBootstrapJob> findJob(UUID jobId) {
        return jobRepository.findById(jobId);
    }

    private void runBootstrapJob(UUID jobId, JurisprudenceBootstrapRequest request, User triggerUser) {
        try {
            JurisprudenceBootstrapResponse resp = runBootstrap(request, triggerUser,
                    progress -> updateJobProgress(jobId, progress));
            updateJobDone(jobId, resp);
        } catch (RuntimeException e) {
            log.error("F-JU-01 — Bootstrap async fatal for jobId={}: {}", jobId, e.getMessage(), e);
            updateJobFailed(jobId, e.getMessage());
        }
    }

    private void updateJobProgress(UUID jobId, BootstrapProgress progress) {
        txTemplate.executeWithoutResult(status -> jobRepository.findById(jobId).ifPresent(job -> {
            job.setEntriesProcessed(progress.processed());
            job.setMappingsCreated(progress.created());
            job.setEntriesSkipped(progress.skipped());
            jobRepository.save(job);
        }));
    }

    private void updateJobDone(UUID jobId, JurisprudenceBootstrapResponse resp) {
        txTemplate.executeWithoutResult(status -> jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JurisprudenceBootstrapJobStatus.DONE);
            job.setEntriesProcessed(resp.entriesProcessed());
            job.setMappingsCreated(resp.mappingsCreated());
            job.setEntriesSkipped(resp.entriesSkipped());
            job.setDurationMs(resp.durationMs());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }));
    }

    private void updateJobFailed(UUID jobId, String errorMessage) {
        txTemplate.executeWithoutResult(status -> jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JurisprudenceBootstrapJobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }));
    }

    /**
     * Version synchrone — utilisée directement par les tests (SF-JU-01-09) et
     * indirectement par {@link #startBootstrap} via le runner async (SF-JU-01-10).
     */
    public JurisprudenceBootstrapResponse runBootstrap(JurisprudenceBootstrapRequest request, User triggerUser) {
        return runBootstrap(request, triggerUser, null);
    }

    /**
     * Variante avec callback de progression invoqué après chaque entrée traitée
     * (SF-JU-01-10). Le callback peut faire un UPDATE de la table jobs ; chaque
     * appel doit être stateless côté caller (on ne porte ni mapping ni audit).
     */
    public JurisprudenceBootstrapResponse runBootstrap(JurisprudenceBootstrapRequest request, User triggerUser,
                                                       Consumer<BootstrapProgress> onProgress) {
        long start = System.currentTimeMillis();
        int processed = 0, created = 0, skipped = 0;
        LocalDate now = LocalDate.now();

        for (JurisprudenceBootstrapEntry entry : request.entries()) {
            processed++;
            LocalDate from = entry.dateMin() != null ? entry.dateMin() : now.minusYears(10);
            // SF-JU-06-03 — requête ciblée optionnelle (fallback sur le mot-clé d'origine).
            String query = request.enrichQueries()
                    ? queryEnricher.enrich(entry.toolId(), entry.brancheCalculId(), entry.motCleRecherche())
                    : entry.motCleRecherche();
            List<JudilibreArret> candidates;
            try {
                // SF-JU-01-13 / SF-JU-04-02 — routage selon pays :
                //   FR (et défaut) → JudilibreApiClient /search PISTE.
                //   BE             → JurisprudenceBeWebSearchClient (Claude+web_search sur JUPORTAL).
                if (isBelgianEntry(entry)) {
                    candidates = beWebSearchClient.fetchArretsByKeyword(
                            query, from, now, 5);
                } else {
                    candidates = judilibreClient.fetchArretsByKeyword(
                            query, from, now, 20);
                }
            } catch (Exception e) {
                log.warn("F-JU-01 — Bootstrap fetchArretsByKeyword failed for {}:{}: {}",
                        entry.toolId(), entry.brancheCalculId(), e.getMessage());
                skipped++;
                notifyProgress(onProgress, processed, created, skipped);
                continue;
            }
            if (candidates.isEmpty()) {
                log.info("F-JU-01 — Bootstrap 0 candidats JUDILIBRE pour {}:{} (query='{}')",
                        entry.toolId(), entry.brancheCalculId(), entry.motCleRecherche());
                skipped++;
                notifyProgress(onProgress, processed, created, skipped);
                continue;
            }
            candidates = filterByJuridiction(candidates, entry.juridictionFiltre());
            if (candidates.size() > 20) {
                candidates = candidates.subList(0, 20);
            }

            ToolJurisprudenceMapping pseudoMapping = pseudoMappingFromEntry(entry);
            ClaudeEvaluation evaluation = evaluator.evaluate(pseudoMapping, candidates);
            if (evaluation.action() == EvaluationAction.NONE || evaluation.arretChoisi() == null) {
                // SF-JU-06-FIX — tracer le NONE de l'évaluateur (sinon skip silencieux,
                // diagnostic aveugle). Inclut la justification renvoyée par l'évaluateur.
                log.info("F-JU-06 — Bootstrap skip (évaluateur NONE) {}:{} (query='{}') — raison: {}",
                        entry.toolId(), entry.brancheCalculId(), query, evaluation.raison());
                skipped++;
                notifyProgress(onProgress, processed, created, skipped);
                continue;
            }
            // SF-JU-06-01 — garde-fous qualité avant persistance (« silence > erreur ») :
            JudilibreArret chosen = evaluation.arretChoisi();
            // (1) chapeau vide/blanc → rejet (sinon citation « » vide affichée à l'avocat)
            if (chosen.chapeauOfficiel() == null || chosen.chapeauOfficiel().isBlank()) {
                log.info("F-JU-06 — Bootstrap rejet (chapeau vide) {}:{} ref={}",
                        entry.toolId(), entry.brancheCalculId(), chosen.ref());
                skipped++;
                notifyProgress(onProgress, processed, created, skipped);
                continue;
            }
            // (2) confiance sous le seuil → rejet
            if (evaluation.confidenceScore() == null
                    || evaluation.confidenceScore().compareTo(MIN_BOOTSTRAP_CONFIDENCE) < 0) {
                log.info("F-JU-06 — Bootstrap rejet (confiance {} < {}) {}:{} ref={}",
                        evaluation.confidenceScore(), MIN_BOOTSTRAP_CONFIDENCE,
                        entry.toolId(), entry.brancheCalculId(), chosen.ref());
                skipped++;
                notifyProgress(onProgress, processed, created, skipped);
                continue;
            }
            // (3) 2ᵉ passe de pertinence sémantique → rejet si l'arrêt ne fonde pas le sujet
            JurisprudenceRelevanceGate.RelevanceVerdict relevance =
                    relevanceGate.assess(entry.motCleRecherche(), chosen);
            if (!relevance.pertinent()) {
                log.info("F-JU-06 — Bootstrap rejet (hors-sujet : {}) {}:{} ref={}",
                        relevance.raison(), entry.toolId(), entry.brancheCalculId(), chosen.ref());
                skipped++;
                notifyProgress(onProgress, processed, created, skipped);
                continue;
            }
            // SF-JU-01-14 — bootstrap idempotent : si l'arrêt choisi par Claude est déjà
            // mappé pour cette (toolId, brancheCalculId), on n'ouvre pas de transaction
            // et on compte l'entrée comme skipped. Évite les DataIntegrityViolationException
            // sur la contrainte uq_tool_jurisprudence_mappings_active (cf. HF-2026-05-27-03).
            String chosenRef = evaluation.arretChoisi().ref();
            if (mappingRepository.existsByToolIdAndBrancheCalculIdAndArretRef(
                    entry.toolId(), entry.brancheCalculId(), chosenRef)) {
                log.info("F-JU-01 — Bootstrap mapping déjà présent pour {}:{} (ref={}), skipped",
                        entry.toolId(), entry.brancheCalculId(), chosenRef);
                skipped++;
                notifyProgress(onProgress, processed, created, skipped);
                continue;
            }
            try {
                final JurisprudenceBootstrapEntry entryRef = entry;
                final ClaudeEvaluation evaluationRef = evaluation;
                final List<JudilibreArret> candidatesRef = candidates;
                txTemplate.executeWithoutResult(status ->
                        persistTopCandidates(entryRef, candidatesRef, evaluationRef, triggerUser));
                created++;
            } catch (RuntimeException e) {
                log.warn("F-JU-01 — Bootstrap persist failed for {}:{}: {}",
                        entry.toolId(), entry.brancheCalculId(), e.getMessage());
                skipped++;
            }
            notifyProgress(onProgress, processed, created, skipped);
        }

        long duration = System.currentTimeMillis() - start;
        log.info("F-JU-01 — Bootstrap done: {} processed, {} created, {} skipped, {} ms",
                processed, created, skipped, duration);
        return new JurisprudenceBootstrapResponse(processed, created, skipped, duration);
    }

    private void notifyProgress(Consumer<BootstrapProgress> onProgress, int processed, int created, int skipped) {
        if (onProgress != null) {
            try {
                onProgress.accept(new BootstrapProgress(processed, created, skipped));
            } catch (RuntimeException e) {
                log.warn("F-JU-01 — Bootstrap progress callback failed: {}", e.getMessage());
            }
        }
    }

    private int persistTopCandidates(JurisprudenceBootstrapEntry entry,
                                     List<JudilibreArret> candidates,
                                     ClaudeEvaluation evaluation,
                                     User triggerUser) {
        // V1 simple : on persiste seulement l'arrêt choisi par Claude (top-1).
        // V2 : Claude pourrait renvoyer un tableau d'arrêts ; tronquer à 3.
        JudilibreArret chosen = evaluation.arretChoisi();
        ToolJurisprudenceMapping mapping = new ToolJurisprudenceMapping();
        mapping.setToolId(entry.toolId());
        mapping.setBrancheCalculId(entry.brancheCalculId());
        mapping.setArretRef(chosen.ref());
        mapping.setJuridiction(chosen.juridiction() == null ? "Cour de cassation" : chosen.juridiction());
        mapping.setDateArret(chosen.dateArret() == null ? LocalDate.now() : chosen.dateArret());
        mapping.setNumeroPourvoi(chosen.numeroPourvoi() == null ? "n/a" : chosen.numeroPourvoi());
        mapping.setLienLegifrance(chosen.lienLegifrance() == null ? "" : chosen.lienLegifrance());
        mapping.setChapeauOfficiel(chosen.chapeauOfficiel() == null ? "" : chosen.chapeauOfficiel());
        mapping.setLastVerifiedAt(Instant.now());
        mapping.setConfidenceScore(evaluation.confidenceScore() == null ? BigDecimal.ZERO : evaluation.confidenceScore());
        mapping.setArchived(false);
        mappingRepository.save(mapping);

        JurisprudenceAuditLog logEntry = new JurisprudenceAuditLog();
        logEntry.setMapping(mapping);
        logEntry.setAction(JurisprudenceAuditAction.AUTO_ADD);
        logEntry.setActor(JurisprudenceAuditActor.SUPER_ADMIN);
        logEntry.setActorUser(triggerUser);
        logEntry.setClaudeConfidence(evaluation.confidenceScore());
        logEntry.setClaudeReason("Bootstrap initial: " + evaluation.raison());
        auditLogRepository.save(logEntry);
        return 1;
    }

    private List<JudilibreArret> filterByJuridiction(List<JudilibreArret> candidates, String filtre) {
        if (filtre == null || filtre.isBlank()) {
            return candidates;
        }
        String f = filtre.toLowerCase();
        List<JudilibreArret> filtered = new ArrayList<>();
        for (JudilibreArret arret : candidates) {
            String j = arret.juridiction() == null ? "" : arret.juridiction().toLowerCase();
            if (j.contains(f)) {
                filtered.add(arret);
            }
        }
        return filtered.isEmpty() ? candidates : filtered;
    }

    private ToolJurisprudenceMapping pseudoMappingFromEntry(JurisprudenceBootstrapEntry entry) {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setToolId(entry.toolId());
        m.setBrancheCalculId(entry.brancheCalculId());
        m.setArretRef(BOOTSTRAP_MAPPING_REF_PREFIX + " — pas de mapping actuel)");
        m.setJuridiction(entry.juridictionFiltre() == null ? "" : entry.juridictionFiltre());
        m.setDateArret(LocalDate.now());
        m.setNumeroPourvoi("");
        m.setLienLegifrance("");
        m.setChapeauOfficiel("Recherche : " + entry.motCleRecherche());
        m.setConfidenceScore(BigDecimal.ZERO);
        return m;
    }

    /**
     * Snapshot intermédiaire transmis au callback {@code onProgress} après
     * chaque entrée. Permet à {@code startBootstrap} d'UPDATE la table
     * {@code jurisprudence_bootstrap_jobs} pour le polling frontend.
     */
    public record BootstrapProgress(int processed, int created, int skipped) {
    }
}
