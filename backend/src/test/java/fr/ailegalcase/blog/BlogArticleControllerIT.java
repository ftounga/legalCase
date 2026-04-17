package fr.ailegalcase.blog;

import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class BlogArticleControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private BlogArticleRepository articleRepository;
    @Autowired private BlogTopicRepository topicRepository;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
        topicRepository.deleteAll();
    }

    @Test
    void getArticles_publicNoAuth_returns200() throws Exception {
        seedPublishedArticle("article-public", "FR", "DROIT_DU_TRAVAIL");

        mockMvc.perform(get("/api/v1/blog/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("article-public"));
    }

    @Test
    void getArticles_filterByCountryFR_returnsOnlyFR() throws Exception {
        seedPublishedArticle("art-fr", "FR", "DROIT_DU_TRAVAIL");
        seedPublishedArticle("art-be", "BE", "DROIT_DU_TRAVAIL");

        mockMvc.perform(get("/api/v1/blog/articles?country=FR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("art-fr"));
    }

    @Test
    void getArticles_filterByLegalDomain_returnsOnlyMatching() throws Exception {
        seedPublishedArticle("art-travail", "FR", "DROIT_DU_TRAVAIL");
        seedPublishedArticle("art-immigration", "FR", "DROIT_IMMIGRATION");

        mockMvc.perform(get("/api/v1/blog/articles?legalDomain=DROIT_IMMIGRATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("art-immigration"));
    }

    @Test
    void getArticles_filterByCountryAndDomain_combines() throws Exception {
        seedPublishedArticle("art-fr-travail", "FR", "DROIT_DU_TRAVAIL");
        seedPublishedArticle("art-fr-famille", "FR", "DROIT_FAMILLE");
        seedPublishedArticle("art-be-travail", "BE", "DROIT_DU_TRAVAIL");

        mockMvc.perform(get("/api/v1/blog/articles?country=FR&legalDomain=DROIT_DU_TRAVAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("art-fr-travail"));
    }

    @Test
    void getArticles_invalidCountry_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/blog/articles?country=US"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getArticles_invalidLegalDomain_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/blog/articles?legalDomain=BUSINESS_LAW"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getArticles_sizeAbove50_silentlyBoundedTo50() throws Exception {
        // We can't easily verify the binding from the JSON output without 50 articles,
        // but we can check the request still returns 200 and not 400.
        mockMvc.perform(get("/api/v1/blog/articles?size=100"))
                .andExpect(status().isOk());
    }

    @Test
    void getArticles_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/blog/articles?page=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getArticleBySlug_publishedArticle_returns200WithDetail() throws Exception {
        seedPublishedArticle("test-slug", "FR", "DROIT_DU_TRAVAIL");

        mockMvc.perform(get("/api/v1/blog/articles/test-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-slug"))
                .andExpect(jsonPath("$.bodyMarkdown").exists())
                .andExpect(jsonPath("$.authorName").value("Franck Tounga"));
    }

    @Test
    void getArticleBySlug_draftArticle_returns404NoLeak() throws Exception {
        seedArticleWithStatus("draft-slug", "FR", "DROIT_DU_TRAVAIL", BlogArticleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/blog/articles/draft-slug"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getArticleBySlug_unpublishedArticle_returns404() throws Exception {
        seedArticleWithStatus("unpub-slug", "FR", "DROIT_DU_TRAVAIL", BlogArticleStatus.UNPUBLISHED);

        mockMvc.perform(get("/api/v1/blog/articles/unpub-slug"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getArticleBySlug_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/blog/articles/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sitemapFragment_returnsOnlyPublished() throws Exception {
        seedPublishedArticle("pub-1", "FR", "DROIT_DU_TRAVAIL");
        seedArticleWithStatus("draft-1", "FR", "DROIT_DU_TRAVAIL", BlogArticleStatus.DRAFT);

        mockMvc.perform(get("/api/v1/blog/articles/sitemap-fragment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("pub-1"));
    }

    private void seedPublishedArticle(String slug, String country, String domain) {
        seedArticleWithStatus(slug, country, domain, BlogArticleStatus.PUBLISHED);
    }

    private void seedArticleWithStatus(String slug, String country, String domain, BlogArticleStatus status) {
        BlogTopic topic = new BlogTopic();
        topic.setSlug(slug + "-topic");
        topic.setTitle("Topic " + slug);
        topic.setCategory(domain);
        topic.setCountryScope(country);
        topic.setStatus(BlogTopicStatus.USED);
        topic.setUsedAt(Instant.now());
        topic = topicRepository.save(topic);

        BlogArticle article = new BlogArticle();
        article.setSlug(slug);
        article.setTitle("Title " + slug);
        article.setSubtitle("Subtitle");
        article.setBodyMarkdown("# Body for " + slug);
        article.setCountry(country);
        article.setLegalDomain(domain);
        article.setAuthorName("Franck Tounga");
        article.setAuthorUrl("https://www.linkedin.com/in/franck-tounga-51a15268/");
        article.setMetaTitle("Meta " + slug);
        article.setMetaDescription("Meta description for " + slug);
        article.setReadingTimeMinutes(8);
        article.setStatus(status);
        article.setTopicId(topic.getId());
        if (status == BlogArticleStatus.PUBLISHED) {
            article.setPublishedAt(Instant.now());
        }
        articleRepository.save(article);

        topic.setArticleId(article.getId());
        topicRepository.save(topic);
    }
}
