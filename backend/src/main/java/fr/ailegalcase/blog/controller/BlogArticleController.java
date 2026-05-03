package fr.ailegalcase.blog.controller;

import fr.ailegalcase.blog.dto.BlogArticleDetailResponse;
import fr.ailegalcase.blog.dto.BlogArticleSummaryResponse;
import fr.ailegalcase.blog.dto.BlogSitemapEntry;
import fr.ailegalcase.blog.service.BlogArticleQueryService;
import fr.ailegalcase.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blog")
public class BlogArticleController {

    static final int MAX_PAGE_SIZE = 50;
    static final int DEFAULT_PAGE_SIZE = 20;
    private static final String HERO_KEY_PATTERN = "blog/articles/%s/hero.png";

    private final BlogArticleQueryService queryService;
    private final StorageService storageService;

    public BlogArticleController(BlogArticleQueryService queryService,
                                 StorageService storageService) {
        this.queryService = queryService;
        this.storageService = storageService;
    }

    @GetMapping("/articles")
    public Page<BlogArticleSummaryResponse> listArticles(
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "legalDomain", required = false) String legalDomain,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be >= 1");
        }
        int boundedSize = Math.min(size, MAX_PAGE_SIZE);

        try {
            return queryService.findPublished(
                    country,
                    legalDomain,
                    PageRequest.of(page, boundedSize, Sort.by(Sort.Direction.DESC, "publishedAt"))
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/articles/{slug}")
    public ResponseEntity<BlogArticleDetailResponse> getArticleBySlug(@PathVariable String slug) {
        return queryService.findPublishedBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    @GetMapping("/articles/sitemap-fragment")
    public List<BlogSitemapEntry> sitemapFragment() {
        return queryService.sitemapEntries();
    }

    @GetMapping("/articles/{articleId}/hero-image")
    public ResponseEntity<byte[]> getHeroImage(@PathVariable UUID articleId) {
        try {
            byte[] image = storageService.download(HERO_KEY_PATTERN.formatted(articleId));
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .body(image);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hero image not found");
        }
    }
}
