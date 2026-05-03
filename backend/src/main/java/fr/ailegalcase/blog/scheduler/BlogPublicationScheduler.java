package fr.ailegalcase.blog.scheduler;

import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.image.BlogHeroImageResult;
import fr.ailegalcase.blog.image.BlogHeroImageService;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.service.ArticleGenerationResult;
import fr.ailegalcase.blog.service.BlogArticleGeneratorService;
import fr.ailegalcase.blog.service.TopicSelection;
import fr.ailegalcase.blog.service.TopicSelectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * F-120 SF-120-05 — orchestrateur cron quotidien de la production blog.
 *
 * <p>Workflow par run :
 * <ol>
 *   <li>Feature flag {@code blog.scheduler.enabled} — no-op si désactivé.</li>
 *   <li>Circuit breaker journalier + hebdomadaire ({@link BlogCircuitBreaker}).</li>
 *   <li>{@link TopicSelectorService#selectNext()} pour récupérer un topic.</li>
 *   <li>{@link BlogArticleGeneratorService#generateForTopic(UUID)} (Sonnet + Haiku).</li>
 *   <li>Si DRAFT : {@link BlogHeroImageService#generateForArticle(UUID)} + auto-publication.</li>
 *   <li>Si NEEDS_REVIEW : laissé en l'état pour relecture super-admin (SF-120-08).</li>
 * </ol>
 *
 * <p>Toute exception est attrapée pour ne pas casser le scheduler Spring.
 */
@Component
public class BlogPublicationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BlogPublicationScheduler.class);

    private final boolean enabled;
    private final TopicSelectorService topicSelectorService;
    private final BlogArticleGeneratorService articleGenerator;
    private final BlogHeroImageService heroImageService;
    private final BlogArticleRepository articleRepository;
    private final BlogCircuitBreaker circuitBreaker;

    public BlogPublicationScheduler(@Value("${blog.scheduler.enabled:false}") boolean enabled,
                                    TopicSelectorService topicSelectorService,
                                    BlogArticleGeneratorService articleGenerator,
                                    BlogHeroImageService heroImageService,
                                    BlogArticleRepository articleRepository,
                                    BlogCircuitBreaker circuitBreaker) {
        this.enabled = enabled;
        this.topicSelectorService = topicSelectorService;
        this.articleGenerator = articleGenerator;
        this.heroImageService = heroImageService;
        this.articleRepository = articleRepository;
        this.circuitBreaker = circuitBreaker;
    }

    @Scheduled(cron = "${blog.scheduler.cron:0 0 8 * * *}", zone = "UTC")
    public void runDailyPublication() {
        if (!enabled) {
            log.debug("Blog scheduler disabled (blog.scheduler.enabled=false)");
            return;
        }
        try {
            executeRun();
        } catch (Exception e) {
            log.error("Blog publication run failed unexpectedly: {}", e.getMessage(), e);
        }
    }

    void executeRun() {
        CircuitBreakerVerdict verdict = circuitBreaker.canGenerateNow();
        if (!(verdict instanceof CircuitBreakerVerdict.Ok)) {
            log.info("Blog publication skipped: circuit breaker = {}", verdict);
            return;
        }

        Optional<TopicSelection> topicOpt = topicSelectorService.selectNext();
        if (topicOpt.isEmpty()) {
            log.info("No blog topic available, run skipped");
            return;
        }
        TopicSelection topic = topicOpt.get();

        ArticleGenerationResult genResult = articleGenerator.generateForTopic(topic.id());
        if (!(genResult instanceof ArticleGenerationResult.Success success)) {
            log.warn("Blog article generation did not succeed for topic {}: {}", topic.id(), genResult);
            return;
        }

        if (success.status() == BlogArticleStatus.NEEDS_REVIEW) {
            log.info("Blog article {} created in NEEDS_REVIEW, awaiting super-admin review",
                    success.articleId());
            return;
        }

        BlogHeroImageResult heroResult = heroImageService.generateForArticle(success.articleId());
        log.info("Blog hero image result for article {}: {}",
                success.articleId(), heroResult.getClass().getSimpleName());

        publishArticle(success.articleId());
    }

    @Transactional
    public void publishArticle(UUID articleId) {
        articleRepository.findById(articleId).ifPresent(article -> {
            if (article.getStatus() == BlogArticleStatus.DRAFT) {
                article.setStatus(BlogArticleStatus.PUBLISHED);
                article.setPublishedAt(Instant.now());
                articleRepository.save(article);
                log.info("Blog article {} auto-published", articleId);
            } else {
                log.info("Blog article {} not auto-published (status={})", articleId, article.getStatus());
            }
        });
    }
}
