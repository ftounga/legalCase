package fr.ailegalcase.blog.admin;

import fr.ailegalcase.blog.cost.IaCostTracker;
import fr.ailegalcase.blog.cost.IaProvider;
import fr.ailegalcase.blog.dto.BlogArticleSummaryResponse;
import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import fr.ailegalcase.blog.scheduler.BlogPublicationScheduler;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.superadmin.SuperAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * F-120 SF-120-08 — endpoints super-admin pour administrer le blog SEO
 * automatisé : revue des articles {@code NEEDS_REVIEW}, déclenchement manuel
 * du scheduler, dashboard coûts.
 *
 * <p>Tous les endpoints sont gardés par {@link SuperAdminService#assertSuperAdmin}.
 * Le bouton "Run now" est rate-limité à 5 appels/jour pour éviter les abus.
 */
@RestController
@RequestMapping("/api/v1/super-admin/blog")
public class BlogAdminController {

    private static final Logger log = LoggerFactory.getLogger(BlogAdminController.class);
    private static final int RUN_NOW_DAILY_LIMIT = 5;

    private final SuperAdminService superAdminService;
    private final BlogArticleRepository articleRepository;
    private final BlogTopicRepository topicRepository;
    private final BlogPublicationScheduler scheduler;
    private final IaCostTracker costTracker;
    private final ConcurrentMap<LocalDate, AtomicInteger> runNowCallsByDay = new ConcurrentHashMap<>();

    public BlogAdminController(SuperAdminService superAdminService,
                               BlogArticleRepository articleRepository,
                               BlogTopicRepository topicRepository,
                               BlogPublicationScheduler scheduler,
                               IaCostTracker costTracker) {
        this.superAdminService = superAdminService;
        this.articleRepository = articleRepository;
        this.topicRepository = topicRepository;
        this.scheduler = scheduler;
        this.costTracker = costTracker;
    }

    @GetMapping("/articles")
    public List<BlogArticleSummaryResponse> listArticlesByStatus(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @RequestParam(value = "status", defaultValue = "NEEDS_REVIEW") BlogArticleStatus status) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return articleRepository.findAllByStatusOrderByPublishedAtDesc(status)
                .stream()
                .map(BlogArticleSummaryResponse::from)
                .toList();
    }

    @PostMapping("/articles/{articleId}/approve")
    @Transactional
    public ResponseEntity<BlogArticleSummaryResponse> approveArticle(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable UUID articleId) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        BlogArticle article = findArticleOr404(articleId);
        if (article.getStatus() != BlogArticleStatus.NEEDS_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Article must be NEEDS_REVIEW to be approved");
        }
        article.setStatus(BlogArticleStatus.DRAFT);
        articleRepository.save(article);
        log.info("Super-admin approved blog article {}: NEEDS_REVIEW → DRAFT", articleId);
        return ResponseEntity.ok(BlogArticleSummaryResponse.from(article));
    }

    @PostMapping("/articles/{articleId}/reject")
    @Transactional
    public ResponseEntity<Void> rejectArticle(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable UUID articleId) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        BlogArticle article = findArticleOr404(articleId);
        if (article.getStatus() == BlogArticleStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot reject a PUBLISHED article (use unpublish flow instead)");
        }
        // Libère le topic associé pour qu'il puisse être retraité
        UUID topicId = article.getTopicId();
        Optional<BlogTopic> topic = topicRepository.findById(topicId);
        if (topic.isPresent() && topic.get().getStatus() == BlogTopicStatus.USED) {
            topicRepository.compareAndSwapStatus(topicId, BlogTopicStatus.USED,
                    BlogTopicStatus.AVAILABLE, null, null);
        }
        articleRepository.delete(article);
        log.info("Super-admin rejected blog article {}: deleted, topic {} released", articleId, topicId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/run-now")
    public ResponseEntity<RunNowResponse> runNow(@AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        if (!consumeRateLimit()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily rate limit reached (" + RUN_NOW_DAILY_LIMIT + " runs/day)");
        }
        try {
            scheduler.executeRun();
            log.info("Super-admin triggered manual blog publication run");
            return ResponseEntity.ok(new RunNowResponse(true, "Run executed"));
        } catch (Exception e) {
            log.error("Manual run failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RunNowResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/cost-summary")
    public CostSummaryResponse costSummary(@AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        BigDecimal anthropicSpent = costTracker.monthlySpent(IaProvider.ANTHROPIC);
        BigDecimal openaiSpent = costTracker.monthlySpent(IaProvider.OPENAI);
        BigDecimal anthropicProjection = projectMonthlyCost(anthropicSpent);
        BigDecimal openaiProjection = projectMonthlyCost(openaiSpent);
        return new CostSummaryResponse(
                anthropicSpent, anthropicProjection,
                openaiSpent, openaiProjection);
    }

    private BlogArticle findArticleOr404(UUID articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    private boolean consumeRateLimit() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        AtomicInteger counter = runNowCallsByDay.computeIfAbsent(today, k -> new AtomicInteger(0));
        // GC : retirer les jours passés (best-effort)
        runNowCallsByDay.keySet().removeIf(d -> d.isBefore(today));
        return counter.incrementAndGet() <= RUN_NOW_DAILY_LIMIT;
    }

    private static BigDecimal projectMonthlyCost(BigDecimal spent) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int dayOfMonth = today.getDayOfMonth();
        int lengthOfMonth = YearMonth.from(today).lengthOfMonth();
        if (dayOfMonth == 0 || spent.compareTo(BigDecimal.ZERO) == 0) {
            return spent;
        }
        return spent.multiply(BigDecimal.valueOf(lengthOfMonth))
                .divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);
    }

    public record RunNowResponse(boolean success, String message) {}

    public record CostSummaryResponse(BigDecimal anthropicMonthlyEur,
                                      BigDecimal anthropicProjectedEur,
                                      BigDecimal openaiMonthlyEur,
                                      BigDecimal openaiProjectedEur) {}
}
