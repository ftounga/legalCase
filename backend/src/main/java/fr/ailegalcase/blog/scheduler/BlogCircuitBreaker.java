package fr.ailegalcase.blog.scheduler;

import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Vérifie qu'on n'a pas atteint la limite quotidienne ou hebdomadaire d'articles
 * publiés. Garde-fou n° 3 de F-120 (cf. PREREQUIS-AVANT-SF-120-03 v2).
 */
@Service
public class BlogCircuitBreaker {

    private final BlogArticleRepository repository;
    private final int dailyCap;
    private final int weeklyCap;

    public BlogCircuitBreaker(BlogArticleRepository repository,
                              @Value("${blog.scheduler.daily-cap:3}") int dailyCap,
                              @Value("${blog.scheduler.weekly-cap:15}") int weeklyCap) {
        this.repository = repository;
        this.dailyCap = dailyCap;
        this.weeklyCap = weeklyCap;
    }

    public CircuitBreakerVerdict canGenerateNow() {
        Instant now = Instant.now();
        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        long publishedToday = repository.countByStatusAndPublishedAtAfter(
                BlogArticleStatus.PUBLISHED, oneDayAgo);
        if (publishedToday >= dailyCap) {
            return new CircuitBreakerVerdict.DailyLimit(publishedToday, dailyCap);
        }

        long publishedThisWeek = repository.countByStatusAndPublishedAtAfter(
                BlogArticleStatus.PUBLISHED, sevenDaysAgo);
        if (publishedThisWeek >= weeklyCap) {
            return new CircuitBreakerVerdict.WeeklyLimit(publishedThisWeek, weeklyCap);
        }

        return CircuitBreakerVerdict.ok();
    }
}
