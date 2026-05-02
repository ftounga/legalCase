package fr.ailegalcase.backlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.ailegalcase.backlog.BacklogDtos.BacklogSyncResult;
import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedFeature;
import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedMarketingTask;
import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedSubfeature;
import fr.ailegalcase.backlog.BacklogMarkdownParser.ParsedTechnicalBacklog;

/**
 * F-178 SF-178-01 : orchestrate parser + upsert + audit run.
 * Idempotent : 2 appels successifs n'introduisent pas de doublons.
 * Source de vérité = MD ; absence dans MD → is_orphaned=true (pas DELETE).
 */
@Service
public class BacklogSyncService {

    private static final Logger log = LoggerFactory.getLogger(BacklogSyncService.class);

    private final BacklogMarkdownParser parser;
    private final BacklogFeatureRepository featureRepository;
    private final BacklogSubfeatureRepository subfeatureRepository;
    private final BacklogMarketingTaskRepository marketingRepository;
    private final BacklogSyncRunRepository runRepository;
    private final BacklogProperties properties;

    public BacklogSyncService(BacklogMarkdownParser parser,
                              BacklogFeatureRepository featureRepository,
                              BacklogSubfeatureRepository subfeatureRepository,
                              BacklogMarketingTaskRepository marketingRepository,
                              BacklogSyncRunRepository runRepository,
                              BacklogProperties properties) {
        this.parser = parser;
        this.featureRepository = featureRepository;
        this.subfeatureRepository = subfeatureRepository;
        this.marketingRepository = marketingRepository;
        this.runRepository = runRepository;
        this.properties = properties;
    }

    @Transactional
    public BacklogSyncResult sync(SyncTrigger trigger) {
        Instant startedAt = Instant.now();
        BacklogSyncRunEntity run = new BacklogSyncRunEntity();
        run.setStartedAt(startedAt);
        run.setSuccess(true);
        run.setTriggeredBy(trigger);
        run = runRepository.save(run);

        try {
            String techContent = readFile(properties.productSpecPath());
            String marketingContent = readFile(properties.marketingBacklogPath());

            ParsedTechnicalBacklog tech = parser.parseTechnicalBacklog(techContent, properties.productSpecPath());
            List<ParsedMarketingTask> marketing = parser.parseMarketingBacklog(marketingContent, properties.marketingBacklogPath());

            int orphans = 0;
            orphans += upsertFeatures(tech.features(), startedAt);
            orphans += upsertSubfeatures(tech.subfeatures(), startedAt);
            orphans += upsertMarketingTasks(marketing, startedAt);

            run.setFeaturesCount(tech.features().size());
            run.setSubfeaturesCount(tech.subfeatures().size());
            run.setMarketingCount(marketing.size());
            run.setOrphansMarked(orphans);
            run.setFinishedAt(Instant.now());
            run.setDurationMs(run.getFinishedAt().toEpochMilli() - startedAt.toEpochMilli());
            run.setSuccess(true);
            run = runRepository.save(run);

            log.info("Backlog sync OK ({}): {} features, {} subfeatures, {} marketing, {} orphans in {}ms",
                    trigger, run.getFeaturesCount(), run.getSubfeaturesCount(),
                    run.getMarketingCount(), run.getOrphansMarked(), run.getDurationMs());

            return new BacklogSyncResult(
                    run.getId(), run.getDurationMs(),
                    run.getFeaturesCount(), run.getSubfeaturesCount(), run.getMarketingCount(),
                    run.getOrphansMarked(), true);

        } catch (Exception e) {
            run.setSuccess(false);
            run.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            run.setFinishedAt(Instant.now());
            run.setDurationMs(run.getFinishedAt().toEpochMilli() - startedAt.toEpochMilli());
            runRepository.save(run);
            log.error("Backlog sync FAILED ({}): {}", trigger, e.getMessage(), e);
            throw new BacklogSyncException("Backlog sync failed: " + e.getMessage(), e);
        }
    }

    private int upsertFeatures(List<ParsedFeature> parsed, Instant parsedAt) {
        // Dédupliquer en gardant la dernière occurrence — cohérent avec le
        // warning "last occurrence wins" émis par le parser sur les doublons
        // de codes dans le MD source. Sans ça, deux entrées avec le même code
        // dans la même transaction provoquent une violation de uk_..._code.
        Map<String, ParsedFeature> deduplicated = new LinkedHashMap<>();
        for (ParsedFeature p : parsed) deduplicated.put(p.code(), p);

        Set<String> seenCodes = new HashSet<>();
        for (ParsedFeature p : deduplicated.values()) {
            seenCodes.add(p.code());
            BacklogFeatureEntity entity = featureRepository.findByCode(p.code())
                    .orElseGet(BacklogFeatureEntity::new);
            entity.setCode(p.code());
            entity.setTitle(p.title());
            entity.setTargetVersion(p.targetVersion());
            entity.setStatus(p.status());
            entity.setDescription(p.description());
            entity.setDomain(p.domain());
            entity.setPriority(p.priority());
            entity.setSourceFile(p.sourceFile());
            entity.setSourceLine(p.sourceLine());
            entity.setParsedAt(parsedAt);
            entity.setOrphaned(false);
            featureRepository.save(entity);
        }
        return markOrphans(featureRepository.findByOrphanedFalse(), seenCodes,
                BacklogFeatureEntity::getCode, featureRepository::save);
    }

    private int upsertSubfeatures(List<ParsedSubfeature> parsed, Instant parsedAt) {
        Map<String, ParsedSubfeature> deduplicated = new LinkedHashMap<>();
        for (ParsedSubfeature p : parsed) deduplicated.put(p.code(), p);

        Set<String> seenCodes = new HashSet<>();
        Map<String, UUID> featureIdByCode = featureRepository.findAll().stream()
                .collect(Collectors.toMap(BacklogFeatureEntity::getCode, BacklogFeatureEntity::getId, (a, b) -> a));

        for (ParsedSubfeature p : deduplicated.values()) {
            UUID parentId = featureIdByCode.get(p.parentFeatureCode());
            if (parentId == null) {
                log.debug("Subfeature {} references unknown parent {} — skip", p.code(), p.parentFeatureCode());
                continue;
            }
            seenCodes.add(p.code());
            BacklogSubfeatureEntity entity = subfeatureRepository.findByCode(p.code())
                    .orElseGet(BacklogSubfeatureEntity::new);
            entity.setCode(p.code());
            entity.setParentFeatureId(parentId);
            entity.setTitle(p.title());
            entity.setStatus(p.status());
            entity.setDescription(p.description());
            entity.setSourceFile(p.sourceFile());
            entity.setSourceLine(p.sourceLine());
            entity.setParsedAt(parsedAt);
            entity.setOrphaned(false);
            subfeatureRepository.save(entity);
        }
        return markOrphans(subfeatureRepository.findByOrphanedFalse(), seenCodes,
                BacklogSubfeatureEntity::getCode, subfeatureRepository::save);
    }

    private int upsertMarketingTasks(List<ParsedMarketingTask> parsed, Instant parsedAt) {
        Map<String, ParsedMarketingTask> deduplicated = new LinkedHashMap<>();
        for (ParsedMarketingTask p : parsed) deduplicated.put(p.code(), p);

        Set<String> seenCodes = new HashSet<>();
        for (ParsedMarketingTask p : deduplicated.values()) {
            seenCodes.add(p.code());
            BacklogMarketingTaskEntity entity = marketingRepository.findByCode(p.code())
                    .orElseGet(BacklogMarketingTaskEntity::new);
            entity.setCode(p.code());
            entity.setTitle(p.title());
            entity.setStatus(p.status());
            entity.setDescription(p.description());
            entity.setCategory(p.category());
            entity.setSourceFile(p.sourceFile());
            entity.setSourceLine(p.sourceLine());
            entity.setParsedAt(parsedAt);
            entity.setOrphaned(false);
            marketingRepository.save(entity);
        }
        return markOrphans(marketingRepository.findByOrphanedFalse(), seenCodes,
                BacklogMarketingTaskEntity::getCode, marketingRepository::save);
    }

    private <T> int markOrphans(List<T> existing, Set<String> seenCodes,
                                Function<T, String> codeExtractor,
                                Function<T, T> saver) {
        int marked = 0;
        for (T entity : existing) {
            if (!seenCodes.contains(codeExtractor.apply(entity))) {
                if (entity instanceof BacklogFeatureEntity f) f.setOrphaned(true);
                else if (entity instanceof BacklogSubfeatureEntity s) s.setOrphaned(true);
                else if (entity instanceof BacklogMarketingTaskEntity m) m.setOrphaned(true);
                saver.apply(entity);
                marked++;
            }
        }
        return marked;
    }

    private String readFile(String path) throws IOException {
        Path filePath = Path.of(path);
        if (!Files.exists(filePath)) {
            throw new IOException("Backlog source file not found: " + filePath.toAbsolutePath());
        }
        return Files.readString(filePath);
    }

    public static class BacklogSyncException extends RuntimeException {
        public BacklogSyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
