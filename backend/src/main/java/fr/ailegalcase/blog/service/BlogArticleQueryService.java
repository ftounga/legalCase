package fr.ailegalcase.blog.service;

import fr.ailegalcase.blog.dto.BlogArticleDetailResponse;
import fr.ailegalcase.blog.dto.BlogArticleSummaryResponse;
import fr.ailegalcase.blog.dto.BlogSitemapEntry;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BlogArticleQueryService {

    static final Set<String> ALLOWED_COUNTRIES = Set.of("FR", "BE");
    static final Set<String> ALLOWED_LEGAL_DOMAINS = Set.of(
            "DROIT_DU_TRAVAIL", "DROIT_IMMIGRATION", "DROIT_FAMILLE"
    );

    private final BlogArticleRepository articleRepository;

    public BlogArticleQueryService(BlogArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public Page<BlogArticleSummaryResponse> findPublished(String country,
                                                          String legalDomain,
                                                          Pageable pageable) {
        validateOptional(country, ALLOWED_COUNTRIES, "country");
        validateOptional(legalDomain, ALLOWED_LEGAL_DOMAINS, "legalDomain");
        return articleRepository
                .findPublished(BlogArticleStatus.PUBLISHED, country, legalDomain, pageable)
                .map(BlogArticleSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<BlogArticleDetailResponse> findPublishedBySlug(String slug) {
        return articleRepository
                .findBySlugAndStatus(slug, BlogArticleStatus.PUBLISHED)
                .map(BlogArticleDetailResponse::from);
    }

    @Transactional(readOnly = true)
    public List<BlogSitemapEntry> sitemapEntries() {
        return articleRepository
                .findAllByStatusOrderByPublishedAtDesc(BlogArticleStatus.PUBLISHED)
                .stream()
                .map(BlogSitemapEntry::from)
                .toList();
    }

    private void validateOptional(String value, Set<String> allowed, String paramName) {
        if (value == null) {
            return;
        }
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(
                    "Invalid " + paramName + ": " + value + ". Allowed values: " + allowed
            );
        }
    }
}
