package fr.ailegalcase.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Persistance et lecture des phrases d'explication de source générées par Haiku
 * (SF-IA-03-15a). Fail-open : si la liste est vide ou null, rien n'est persisté.
 */
@Service
public class SourceExplanationService {

    private static final Logger log = LoggerFactory.getLogger(SourceExplanationService.class);
    private static final int MAX_SENTENCE_LENGTH = 300;
    private static final int MAX_LABEL_LENGTH = 255;

    private final SourceExplanationRepository repository;

    public SourceExplanationService(SourceExplanationRepository repository) {
        this.repository = repository;
    }

    /**
     * Persiste une liste d'explications associées à une analyse dossier.
     * Fail-open : les items invalides sont skippés en silence (log warn).
     * L'unicité (case_analysis_id, source_key) est gérée par la contrainte DB.
     * Si la table contient déjà une explication pour ce source_key, elle est préservée.
     */
    @Transactional
    public void persist(CaseAnalysis analysis, List<SourceExplanationData> items) {
        if (items == null || items.isEmpty()) return;

        // SF-IA-03-21 : supprimer les anciennes explications pour cette analyse avant d'insérer.
        repository.deleteByCaseAnalysisId(analysis.getId());

        int persisted = 0;
        for (SourceExplanationData item : items) {
            if (item == null) continue;
            if (item.sourceKey() == null || item.sourceKey().isBlank()) continue;
            if (item.label() == null || item.label().isBlank()) continue;

            String key = item.sourceKey().trim();

            SourceExplanation.SourceType type;
            try {
                type = item.sourceType() != null
                        ? SourceExplanation.SourceType.valueOf(item.sourceType())
                        : SourceExplanation.SourceType.ANALYSIS_DETECTION;
            } catch (IllegalArgumentException e) {
                log.warn("Unknown source_type '{}' for source_key '{}' — skipped", item.sourceType(), key);
                continue;
            }

            SourceExplanation entity = new SourceExplanation();
            entity.setCaseAnalysis(analysis);
            entity.setSourceKey(key);
            entity.setSourceType(type);
            entity.setLabel(truncate(item.label().trim(), MAX_LABEL_LENGTH));
            entity.setSentence(item.sentence() != null ? truncate(item.sentence().trim(), MAX_SENTENCE_LENGTH) : null);
            entity.setSecondaryText(item.secondaryText() != null ? truncate(item.secondaryText().trim(), 500) : null);
            entity.setAnchorDocId(item.anchorDocId());
            entity.setAnchorQuestionId(item.anchorQuestionId());
            entity.setAnchorF96Code(item.anchorF96Code());
            entity.setAnchorChatMessageId(item.anchorChatMessageId());
            entity.setAnchorPieceIndex(item.anchorPieceIndex());
            repository.save(entity);
            persisted++;
        }

        if (persisted > 0) {
            log.info("Persisted {} source explanations for analysis {}", persisted, analysis.getId());
        }
    }

    public Map<String, List<SourceExplanation>> findByCaseAnalysisIdGrouped(UUID caseAnalysisId) {
        return repository.findByCaseAnalysisId(caseAnalysisId).stream()
                .collect(Collectors.groupingBy(SourceExplanation::getSourceKey));
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
