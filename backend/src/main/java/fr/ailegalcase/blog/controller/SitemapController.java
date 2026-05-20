package fr.ailegalcase.blog.controller;

import fr.ailegalcase.blog.dto.BlogSitemapEntry;
import fr.ailegalcase.blog.service.BlogArticleQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * F-120 SF-120-07 — sitemap.xml dynamique combinant pages statiques + articles
 * blog publiés. Servi sur la racine du domaine pour ingestion par les moteurs
 * de recherche.
 *
 * <p>Format : sitemap.xml v0.9. Charge utile :
 * <ul>
 *   <li>Landing + pages légales (5 URLs statiques)</li>
 *   <li>Liste blog (/blog)</li>
 *   <li>Tous les articles {@code PUBLISHED} (slug, lastmod, priority 0.7)</li>
 * </ul>
 */
@RestController
public class SitemapController {

    private final BlogArticleQueryService queryService;
    private final String publicBaseUrl;

    public SitemapController(BlogArticleQueryService queryService,
                             @Value("${blog.public-base-url:https://legalcase.fr}") String publicBaseUrl) {
        this.queryService = queryService;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @GetMapping(value = "/api/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        List<BlogSitemapEntry> blogEntries = queryService.sitemapEntries();

        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        appendUrl(sb, "/", "weekly", "1.0", null);
        appendUrl(sb, "/blog", "daily", "0.9", null);
        appendUrl(sb, "/contact", "monthly", "0.5", null);
        appendUrl(sb, "/mentions-legales", "monthly", "0.3", null);
        appendUrl(sb, "/privacy", "monthly", "0.3", null);
        appendUrl(sb, "/cgu", "monthly", "0.3", null);

        for (BlogSitemapEntry entry : blogEntries) {
            Instant lastmod = entry.updatedAt() != null ? entry.updatedAt() : entry.publishedAt();
            appendUrl(sb, "/blog/" + entry.slug(), "monthly", "0.7", lastmod);
        }

        sb.append("</urlset>\n");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sb.toString());
    }

    @GetMapping(value = "/api/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String body = ("User-agent: *%n"
                + "Allow: /%n"
                + "Disallow: /api/%n"
                + "Disallow: /case-files/%n"
                + "Disallow: /super-admin/%n"
                + "Disallow: /workspace/%n"
                + "Disallow: /onboarding%n"
                + "Disallow: /dashboard%n%n"
                + "Sitemap: %s/sitemap.xml%n").formatted(publicBaseUrl);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    private void appendUrl(StringBuilder sb, String path, String changefreq, String priority, Instant lastmod) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(publicBaseUrl).append(path).append("</loc>\n");
        if (lastmod != null) {
            sb.append("    <lastmod>").append(DateTimeFormatter.ISO_INSTANT.format(lastmod)).append("</lastmod>\n");
        }
        sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sb.append("    <priority>").append(priority).append("</priority>\n");
        sb.append("  </url>\n");
    }
}
