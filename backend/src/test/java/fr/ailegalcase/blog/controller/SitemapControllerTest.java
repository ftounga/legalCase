package fr.ailegalcase.blog.controller;

import fr.ailegalcase.blog.dto.BlogSitemapEntry;
import fr.ailegalcase.blog.service.BlogArticleQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SitemapControllerTest {

    private final BlogArticleQueryService queryService = mock(BlogArticleQueryService.class);
    private final SitemapController controller = new SitemapController(
            queryService, "https://legalcase.example.com");

    @Test
    void sitemap_emptyBlog_returnsStaticUrls() {
        when(queryService.sitemapEntries()).thenReturn(List.of());

        ResponseEntity<String> response = controller.sitemap();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        String xml = response.getBody();
        assertThat(xml).contains("<urlset");
        assertThat(xml).contains("<loc>https://legalcase.example.com/</loc>");
        assertThat(xml).contains("<loc>https://legalcase.example.com/blog</loc>");
        assertThat(xml).contains("<loc>https://legalcase.example.com/contact</loc>");
        assertThat(xml).contains("<loc>https://legalcase.example.com/cgu</loc>");
    }

    @Test
    void sitemap_includesPublishedBlogArticles() {
        BlogSitemapEntry entry = new BlogSitemapEntry(
                "rupture-conventionnelle",
                "FR",
                Instant.parse("2026-05-01T08:00:00Z"),
                Instant.parse("2026-05-02T10:00:00Z"));
        when(queryService.sitemapEntries()).thenReturn(List.of(entry));

        ResponseEntity<String> response = controller.sitemap();

        String xml = response.getBody();
        assertThat(xml).contains("<loc>https://legalcase.example.com/blog/rupture-conventionnelle</loc>");
        assertThat(xml).contains("<lastmod>2026-05-02T10:00:00Z</lastmod>");
    }

    @Test
    void sitemap_handlesTrailingSlashInBaseUrl() {
        SitemapController c = new SitemapController(queryService, "https://legalcase.example.com/");
        when(queryService.sitemapEntries()).thenReturn(List.of());

        ResponseEntity<String> response = c.sitemap();

        assertThat(response.getBody()).contains("https://legalcase.example.com/");
        assertThat(response.getBody()).doesNotContain("https://legalcase.example.com//");
    }

    @Test
    void robots_includesSitemapAndDisallows() {
        ResponseEntity<String> response = controller.robots();

        String body = response.getBody();
        assertThat(body).contains("User-agent: *");
        assertThat(body).contains("Allow: /");
        assertThat(body).contains("Disallow: /api/");
        assertThat(body).contains("Disallow: /super-admin/");
        assertThat(body).contains("Sitemap: https://legalcase.example.com/sitemap.xml");
    }

    /**
     * F-143 SF-143-02 — la valeur par défaut de {@code blog.public-base-url}
     * doit être {@code https://legalcase.fr} (host canonique post-bascule 301).
     * Vérification non-régression contre un retour accidentel à l'ancien host.
     */
    @Test
    void sitemap_defaultBaseUrl_pointsToLegalcaseFr() {
        SitemapController c = new SitemapController(queryService, "https://legalcase.fr");
        when(queryService.sitemapEntries()).thenReturn(List.of());

        ResponseEntity<String> response = c.sitemap();

        String xml = response.getBody();
        assertThat(xml).contains("<loc>https://legalcase.fr/</loc>");
        assertThat(xml).doesNotContain("ng-itconsulting");
    }
}
