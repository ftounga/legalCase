package fr.ailegalcase.blog;

import fr.ailegalcase.blog.dto.BlogArticleDetailResponse;
import fr.ailegalcase.blog.dto.BlogArticleSummaryResponse;
import fr.ailegalcase.blog.dto.BlogSitemapEntry;
import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.service.BlogArticleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogArticleQueryServiceTest {

    @Mock
    private BlogArticleRepository repository;

    private BlogArticleQueryService service;

    @BeforeEach
    void setUp() {
        service = new BlogArticleQueryService(repository);
    }

    @Test
    void findPublished_nominal_returnsMappedPage() {
        BlogArticle article = sampleArticle("le-licenciement-economique", "FR", "DROIT_DU_TRAVAIL");
        Page<BlogArticle> page = new PageImpl<>(List.of(article));
        when(repository.findPublished(eq(BlogArticleStatus.PUBLISHED), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<BlogArticleSummaryResponse> result = service.findPublished("FR", "DROIT_DU_TRAVAIL",
                PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).slug()).isEqualTo("le-licenciement-economique");
        assertThat(result.getContent().get(0).country()).isEqualTo("FR");
    }

    @Test
    void findPublished_passesNullFiltersWhenAbsent() {
        when(repository.findPublished(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findPublished(null, null, PageRequest.of(0, 20));

        ArgumentCaptor<String> countryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> domainCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).findPublished(eq(BlogArticleStatus.PUBLISHED),
                countryCaptor.capture(), domainCaptor.capture(), any(Pageable.class));
        assertThat(countryCaptor.getValue()).isNull();
        assertThat(domainCaptor.getValue()).isNull();
    }

    @Test
    void findPublished_filtersByCountryOnly() {
        when(repository.findPublished(any(), eq("BE"), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findPublished("BE", null, PageRequest.of(0, 20));

        verify(repository).findPublished(eq(BlogArticleStatus.PUBLISHED), eq("BE"), eq(null), any(Pageable.class));
    }

    @Test
    void findPublished_invalidCountry_throws() {
        assertThatThrownBy(() -> service.findPublished("US", null, PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country");
    }

    @Test
    void findPublished_invalidLegalDomain_throws() {
        assertThatThrownBy(() -> service.findPublished(null, "BUSINESS_LAW", PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("legalDomain");
    }

    @Test
    void findPublishedBySlug_nominal_returnsDetailResponse() {
        BlogArticle article = sampleArticle("test-slug", "FR", "DROIT_DU_TRAVAIL");
        when(repository.findBySlugAndStatus("test-slug", BlogArticleStatus.PUBLISHED))
                .thenReturn(Optional.of(article));

        Optional<BlogArticleDetailResponse> result = service.findPublishedBySlug("test-slug");

        assertThat(result).isPresent();
        assertThat(result.get().slug()).isEqualTo("test-slug");
        assertThat(result.get().bodyMarkdown()).isEqualTo("# Body");
    }

    @Test
    void findPublishedBySlug_unknownSlug_returnsEmpty() {
        when(repository.findBySlugAndStatus("unknown", BlogArticleStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        Optional<BlogArticleDetailResponse> result = service.findPublishedBySlug("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findPublishedBySlug_draftArticle_returnsEmpty() {
        // The repo query filters by status=PUBLISHED, so a DRAFT row won't match.
        when(repository.findBySlugAndStatus("draft-article", BlogArticleStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        Optional<BlogArticleDetailResponse> result = service.findPublishedBySlug("draft-article");

        assertThat(result).isEmpty();
    }

    @Test
    void sitemapEntries_returnsOnlyPublished() {
        BlogArticle a1 = sampleArticle("article-1", "FR", "DROIT_DU_TRAVAIL");
        BlogArticle a2 = sampleArticle("article-2", "BE", "DROIT_FAMILLE");
        when(repository.findAllByStatusOrderByPublishedAtDesc(BlogArticleStatus.PUBLISHED))
                .thenReturn(List.of(a1, a2));

        List<BlogSitemapEntry> entries = service.sitemapEntries();

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).slug()).isEqualTo("article-1");
        assertThat(entries.get(0).country()).isEqualTo("FR");
        assertThat(entries.get(1).country()).isEqualTo("BE");
    }

    @Test
    void allValidLegalDomainsAccepted() {
        when(repository.findPublished(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        for (String domain : List.of("DROIT_DU_TRAVAIL", "DROIT_IMMIGRATION", "DROIT_FAMILLE")) {
            service.findPublished(null, domain, PageRequest.of(0, 20));
        }
    }

    private BlogArticle sampleArticle(String slug, String country, String domain) {
        BlogArticle a = new BlogArticle();
        a.setId(UUID.randomUUID());
        a.setSlug(slug);
        a.setTitle("Title " + slug);
        a.setSubtitle("Subtitle");
        a.setBodyMarkdown("# Body");
        a.setCountry(country);
        a.setLegalDomain(domain);
        a.setAuthorName("Franck Tounga");
        a.setAuthorUrl("https://www.linkedin.com/in/franck-tounga-51a15268/");
        a.setMetaTitle("Meta " + slug);
        a.setMetaDescription("Meta desc");
        a.setReadingTimeMinutes(8);
        a.setStatus(BlogArticleStatus.PUBLISHED);
        a.setTopicId(UUID.randomUUID());
        Instant now = Instant.now();
        a.setPublishedAt(now);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        return a;
    }
}
