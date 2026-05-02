package fr.ailegalcase.backlog;

import fr.ailegalcase.backlog.BacklogDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BacklogQueryService {

    private final BacklogFeatureRepository featureRepository;
    private final BacklogSubfeatureRepository subfeatureRepository;
    private final BacklogMarketingTaskRepository marketingRepository;
    private final BacklogSyncRunRepository runRepository;
    private final BacklogProperties properties;

    public BacklogQueryService(BacklogFeatureRepository featureRepository,
                               BacklogSubfeatureRepository subfeatureRepository,
                               BacklogMarketingTaskRepository marketingRepository,
                               BacklogSyncRunRepository runRepository,
                               BacklogProperties properties) {
        this.featureRepository = featureRepository;
        this.subfeatureRepository = subfeatureRepository;
        this.marketingRepository = marketingRepository;
        this.runRepository = runRepository;
        this.properties = properties;
    }

    public Page<BacklogFeatureSummary> searchFeatures(BacklogStatus status,
                                                     BacklogDomain domain,
                                                     BacklogPriority priority,
                                                     String search,
                                                     Pageable pageable) {
        return featureRepository.search(status, domain, priority, toSearchPattern(search), pageable)
                .map(this::toSummary);
    }

    public BacklogFeatureDetail getFeatureDetail(String code) {
        BacklogFeatureEntity feature = featureRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found: " + code));
        List<BacklogSubfeatureDto> subfeatures = subfeatureRepository
                .findByParentFeatureIdOrderByCodeAsc(feature.getId())
                .stream()
                .map(this::toSubfeatureDto)
                .toList();
        return new BacklogFeatureDetail(
                feature.getId(),
                feature.getCode(),
                feature.getTitle(),
                feature.getTargetVersion(),
                feature.getStatus(),
                feature.getDescription(),
                feature.getDomain(),
                feature.getPriority(),
                feature.getSourceFile(),
                feature.getSourceLine(),
                feature.getParsedAt(),
                feature.getUpdatedAt(),
                subfeatures
        );
    }

    public Page<BacklogMarketingTaskSummary> searchMarketingTasks(BacklogMarketingStatus status,
                                                                  String search,
                                                                  Pageable pageable) {
        return marketingRepository.search(status, toSearchPattern(search), pageable)
                .map(this::toMarketingSummary);
    }

    /**
     * Pré-formate le terme de recherche en pattern LIKE pour PostgreSQL.
     *
     * <p>Évite le bug PostgreSQL "function lower(bytea) does not exist" : quand
     * Hibernate binde un paramètre {@code String} null sans type explicite via
     * JDBC, PostgreSQL infère {@code bytea} et l'expression
     * {@code LOWER(CONCAT('%', ?, '%'))} échoue au type-check parce que
     * {@code lower} n'accepte pas bytea. En pré-formattant côté Java, le
     * paramètre est toujours bindé comme texte non-null (ou null géré par le
     * test {@code :searchPattern IS NULL} en amont).</p>
     */
    private static String toSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase() + "%";
    }

    public List<BacklogSyncRunSummary> recentSyncRuns(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return runRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toRunSummary)
                .toList();
    }

    public BacklogFreshness getFreshness() {
        Instant now = Instant.now();
        var lastRunOpt = runRepository.findFirstByOrderByStartedAtDesc();
        var lastSuccessOpt = runRepository.findFirstBySuccessTrueOrderByStartedAtDesc();

        if (lastRunOpt.isEmpty()) {
            return new BacklogFreshness(null, null, "STALE", null);
        }
        BacklogSyncRunEntity lastRun = lastRunOpt.get();
        Instant lastSuccessAt = lastSuccessOpt.map(BacklogSyncRunEntity::getStartedAt).orElse(null);
        long minutes = Duration.between(lastRun.getStartedAt(), now).toMinutes();

        String status;
        if (!lastRun.isSuccess()) {
            status = "ERROR";
        } else if (minutes > properties.staleAfterMinutes()) {
            status = "STALE";
        } else {
            status = "OK";
        }
        return new BacklogFreshness(lastRun.getStartedAt(), lastSuccessAt, status, minutes);
    }

    private BacklogFeatureSummary toSummary(BacklogFeatureEntity f) {
        return new BacklogFeatureSummary(
                f.getId(), f.getCode(), f.getTitle(), f.getTargetVersion(),
                f.getStatus(), f.getDomain(), f.getPriority(), f.getUpdatedAt()
        );
    }

    private BacklogSubfeatureDto toSubfeatureDto(BacklogSubfeatureEntity s) {
        return new BacklogSubfeatureDto(
                s.getId(), s.getCode(), s.getTitle(), s.getStatus(),
                s.getDescription(), s.getSourceLine(), s.getUpdatedAt()
        );
    }

    private BacklogMarketingTaskSummary toMarketingSummary(BacklogMarketingTaskEntity t) {
        return new BacklogMarketingTaskSummary(
                t.getId(), t.getCode(), t.getTitle(), t.getStatus(),
                t.getCategory(), t.getUpdatedAt()
        );
    }

    private BacklogSyncRunSummary toRunSummary(BacklogSyncRunEntity r) {
        return new BacklogSyncRunSummary(
                r.getId(), r.getStartedAt(), r.getFinishedAt(), r.getDurationMs(),
                r.isSuccess(), r.getFeaturesCount(), r.getSubfeaturesCount(),
                r.getMarketingCount(), r.getOrphansMarked(), r.getTriggeredBy(), r.getErrorMessage()
        );
    }
}
