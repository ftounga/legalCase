package fr.ailegalcase.blog.service;

import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import fr.ailegalcase.blog.repository.CategoryCountryCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Moteur interne de sélection de sujets pour le blog SEO.
 *
 * {@link #selectNext()} retourne le prochain topic à générer en respectant
 * les quotas éditoriaux (60/25/15 catégorie, 70/30 pays) sur une fenêtre glissante 4 semaines.
 *
 * L'acquisition d'un topic se fait via un UPDATE conditionnel atomique
 * (pattern compare-and-swap) qui garantit l'idempotence entre appels concurrents.
 *
 * L'appelant (SF-120-03 génération, SF-120-05 scheduler) doit appeler
 * {@link #markAsUsed(UUID, UUID)} après génération réussie, ou
 * {@link #releaseReservation(UUID)} en cas d'échec.
 */
@Service
public class TopicSelectorService {

    private static final Logger log = LoggerFactory.getLogger(TopicSelectorService.class);

    static final Duration QUOTA_WINDOW = Duration.ofDays(28);

    private final BlogTopicRepository topicRepository;
    private final BlogArticleRepository articleRepository;
    private final QuotaCalculator quotaCalculator;

    public TopicSelectorService(BlogTopicRepository topicRepository,
                                BlogArticleRepository articleRepository) {
        this.topicRepository = topicRepository;
        this.articleRepository = articleRepository;
        this.quotaCalculator = new QuotaCalculator();
    }

    /**
     * Retourne le prochain topic AVAILABLE à traiter, en le verrouillant à RESERVED.
     * Respecte la priorité : (1) catégorie la plus en déficit, (2) pays le plus en déficit.
     * Fallback : si aucun topic disponible dans la combinaison prioritaire, essaie
     * les catégories/pays suivants, puis n'importe quel topic AVAILABLE.
     *
     * @return le topic verrouillé, ou {@link Optional#empty()} si aucun topic disponible
     */
    @Transactional
    public Optional<TopicSelection> selectNext() {
        Map<String, Map<String, Long>> countsMatrix = loadQuotaMatrix();
        List<String> categoriesByPriority = quotaCalculator.prioritizedCategories(
                flattenByCategory(countsMatrix));

        for (String category : categoriesByPriority) {
            List<String> countriesByPriority = quotaCalculator.prioritizedCountriesForCategory(
                    category, countsMatrix.getOrDefault(category, Map.of()));
            for (String country : countriesByPriority) {
                Optional<BlogTopic> acquired = tryAcquire(
                        () -> topicRepository
                                .findFirstByStatusAndCategoryAndCountryScopeOrderByCreatedAtAsc(
                                        BlogTopicStatus.AVAILABLE, category, country));
                if (acquired.isPresent()) {
                    return acquired.map(t -> logAndReturn(t, category, country));
                }
            }
            // Fallback pays : n'importe quel pays dans cette catégorie
            Optional<BlogTopic> acquired = tryAcquire(
                    () -> topicRepository.findFirstByStatusAndCategoryOrderByCreatedAtAsc(
                            BlogTopicStatus.AVAILABLE, category));
            if (acquired.isPresent()) {
                return acquired.map(t -> logAndReturn(t, category, "FALLBACK"));
            }
        }

        // Fallback ultime : n'importe quel topic AVAILABLE
        Optional<BlogTopic> acquired = tryAcquire(
                () -> topicRepository.findFirstByStatusOrderByCreatedAtAsc(BlogTopicStatus.AVAILABLE));
        return acquired.map(t -> logAndReturn(t, "FALLBACK", "FALLBACK"));
    }

    /**
     * Passe un topic RESERVED à USED et l'associe à l'article généré.
     *
     * @throws IllegalStateException si le topic n'est pas en état RESERVED
     */
    @Transactional
    public void markAsUsed(UUID topicId, UUID articleId) {
        int updated = topicRepository.compareAndSwapStatus(
                topicId, BlogTopicStatus.RESERVED, BlogTopicStatus.USED,
                Instant.now(), articleId);
        if (updated == 0) {
            throw new IllegalStateException(
                    "Cannot mark topic " + topicId + " as USED: not in RESERVED state");
        }
        log.info("Topic marked as USED: topicId={}, articleId={}", topicId, articleId);
    }

    /**
     * Libère une réservation : passe un topic RESERVED à AVAILABLE. No-op si le
     * statut est déjà différent (idempotent).
     */
    @Transactional
    public void releaseReservation(UUID topicId) {
        int updated = topicRepository.compareAndSwapStatus(
                topicId, BlogTopicStatus.RESERVED, BlogTopicStatus.AVAILABLE,
                null, null);
        if (updated > 0) {
            log.info("Topic reservation released: topicId={}", topicId);
        }
    }

    private Optional<BlogTopic> tryAcquire(java.util.function.Supplier<Optional<BlogTopic>> candidate) {
        Optional<BlogTopic> found = candidate.get();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        BlogTopic topic = found.get();
        int acquired = topicRepository.compareAndSwapStatus(
                topic.getId(), BlogTopicStatus.AVAILABLE, BlogTopicStatus.RESERVED,
                Instant.now(), null);
        if (acquired == 0) {
            // Race perdue : le topic a été pris par un autre thread entre le SELECT et l'UPDATE.
            // Ne pas retenter ici — l'appelant peut rappeler selectNext() si besoin.
            log.debug("Race lost on topic {}, skipping", topic.getId());
            return Optional.empty();
        }
        return Optional.of(topic);
    }

    private TopicSelection logAndReturn(BlogTopic topic, String chosenCategory, String chosenCountry) {
        log.info("Topic selected: slug={}, category={}, country={}, chosenCategory={}, chosenCountry={}",
                topic.getSlug(), topic.getCategory(), topic.getCountryScope(),
                chosenCategory, chosenCountry);
        return TopicSelection.from(topic);
    }

    /**
     * Charge la matrice des comptes publiés sur la fenêtre glissante, indexée par {@code category → (country → count)}.
     */
    private Map<String, Map<String, Long>> loadQuotaMatrix() {
        Instant since = Instant.now().minus(QUOTA_WINDOW);
        List<CategoryCountryCount> rows = articleRepository
                .countPublishedByCategoryAndCountrySince(BlogArticleStatus.PUBLISHED, since);
        Map<String, Map<String, Long>> matrix = new HashMap<>();
        for (CategoryCountryCount row : rows) {
            matrix.computeIfAbsent(row.getCategory(), k -> new HashMap<>())
                    .put(row.getCountry(), row.getCount());
        }
        return matrix;
    }

    private Map<String, Long> flattenByCategory(Map<String, Map<String, Long>> matrix) {
        Map<String, Long> flat = new HashMap<>();
        matrix.forEach((cat, byCountry) ->
                flat.put(cat, byCountry.values().stream().mapToLong(Long::longValue).sum()));
        return flat;
    }
}
