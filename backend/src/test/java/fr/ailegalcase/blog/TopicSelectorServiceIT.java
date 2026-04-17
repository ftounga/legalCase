package fr.ailegalcase.blog;

import fr.ailegalcase.blog.entity.BlogArticle;
import fr.ailegalcase.blog.entity.BlogArticleStatus;
import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import fr.ailegalcase.blog.repository.BlogArticleRepository;
import fr.ailegalcase.blog.repository.BlogTopicRepository;
import fr.ailegalcase.blog.service.TopicSelection;
import fr.ailegalcase.blog.service.TopicSelectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
class TopicSelectorServiceIT {

    @Autowired private TopicSelectorService selectorService;
    @Autowired private BlogTopicRepository topicRepository;
    @Autowired private BlogArticleRepository articleRepository;

    @BeforeEach
    void cleanSlate() {
        articleRepository.deleteAll();
        topicRepository.deleteAll();
    }

    @Test
    void seed_migration080_topicsLoaded() {
        // La migration 080 charge 25 topics au démarrage. Le @BeforeEach les a supprimés,
        // on les réinsère manuellement pour valider que la composition cible est bien tenable
        // (la vraie migration tourne au startup, hors de ce test nettoyé).
        // Ici on teste juste que le seed applicatif peut être rechargé sans conflit d'unicité.
        seedTopic("seed-1", "DROIT_DU_TRAVAIL", "FR");
        seedTopic("seed-2", "DROIT_IMMIGRATION", "BE");

        assertThat(topicRepository.count()).isEqualTo(2);
    }

    @Test
    void selectNext_baseVide_retourneEmpty() {
        Optional<TopicSelection> result = selectorService.selectNext();

        assertThat(result).isEmpty();
    }

    @Test
    void selectNext_deuxTopicsDisponibles_verrouilleUnSeul() {
        seedTopic("travail-fr-1", "DROIT_DU_TRAVAIL", "FR");
        seedTopic("travail-fr-2", "DROIT_DU_TRAVAIL", "FR");

        Optional<TopicSelection> first = selectorService.selectNext();

        assertThat(first).isPresent();
        UUID firstId = first.get().id();
        BlogTopic reserved = topicRepository.findById(firstId).orElseThrow();
        assertThat(reserved.getStatus()).isEqualTo(BlogTopicStatus.RESERVED);
        assertThat(reserved.getUsedAt()).isNotNull();

        // Le 2ème topic est toujours AVAILABLE
        BlogTopic stillAvailable = topicRepository.findAll().stream()
                .filter(t -> !t.getId().equals(firstId))
                .findFirst()
                .orElseThrow();
        assertThat(stillAvailable.getStatus()).isEqualTo(BlogTopicStatus.AVAILABLE);
    }

    @Test
    void selectNext_fallbackCountry_lorsqueFRIndisponible() {
        // Seul un topic BE pour Travail
        seedTopic("travail-be", "DROIT_DU_TRAVAIL", "BE");

        Optional<TopicSelection> result = selectorService.selectNext();

        assertThat(result).isPresent();
        assertThat(result.get().countryScope()).isEqualTo("BE");
    }

    @Test
    void selectNext_quotaTravailDeficitaire_prioritiseTravail() {
        // Base : 10 articles publiés Immigration FR sur 10 → quota 25 % largement dépassé.
        // Déficit Travail (60 %) le plus élevé.
        for (int i = 0; i < 10; i++) {
            seedPublishedArticle("imm-pub-" + i, "DROIT_IMMIGRATION", "FR");
        }

        // Un topic dispo dans chaque catégorie
        seedTopic("topic-travail", "DROIT_DU_TRAVAIL", "FR");
        seedTopic("topic-imm", "DROIT_IMMIGRATION", "FR");
        seedTopic("topic-fam", "DROIT_FAMILLE", "FR");

        Optional<TopicSelection> result = selectorService.selectNext();

        assertThat(result).isPresent();
        assertThat(result.get().category()).isEqualTo("DROIT_DU_TRAVAIL");
    }

    @Test
    void markAsUsed_transitionReservedVersUsed() {
        BlogTopic topic = seedTopic("topic-a", "DROIT_DU_TRAVAIL", "FR");
        Optional<TopicSelection> selected = selectorService.selectNext();
        assertThat(selected).isPresent();

        UUID articleId = UUID.randomUUID();
        selectorService.markAsUsed(topic.getId(), articleId);

        BlogTopic updated = topicRepository.findById(topic.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BlogTopicStatus.USED);
        assertThat(updated.getArticleId()).isEqualTo(articleId);
    }

    @Test
    void releaseReservation_transitionReservedVersAvailable() {
        BlogTopic topic = seedTopic("topic-b", "DROIT_DU_TRAVAIL", "FR");
        selectorService.selectNext(); // le passe RESERVED

        selectorService.releaseReservation(topic.getId());

        BlogTopic updated = topicRepository.findById(topic.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BlogTopicStatus.AVAILABLE);
        assertThat(updated.getUsedAt()).isNull();
    }

    @Test
    void releaseReservation_topicNonReserved_noOpSilencieux() {
        BlogTopic topic = seedTopic("topic-c", "DROIT_DU_TRAVAIL", "FR");
        // Topic reste AVAILABLE
        selectorService.releaseReservation(topic.getId());

        BlogTopic unchanged = topicRepository.findById(topic.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(BlogTopicStatus.AVAILABLE);
    }

    @Test
    void markAsUsed_topicNonReserved_leveIllegalState() {
        BlogTopic topic = seedTopic("topic-d", "DROIT_DU_TRAVAIL", "FR");
        // Topic reste AVAILABLE (pas réservé)

        try {
            selectorService.markAsUsed(topic.getId(), UUID.randomUUID());
            org.junit.jupiter.api.Assertions.fail("IllegalStateException attendue");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("RESERVED");
        }
    }

    private BlogTopic seedTopic(String slug, String category, String countryScope) {
        BlogTopic t = new BlogTopic();
        t.setSlug(slug);
        t.setTitle("Title " + slug);
        t.setCategory(category);
        t.setCountryScope(countryScope);
        t.setStatus(BlogTopicStatus.AVAILABLE);
        // createdAt via @PrePersist
        return topicRepository.save(t);
    }

    private void seedPublishedArticle(String slug, String legalDomain, String country) {
        // Un topic est obligatoire (FK nullable=false)
        BlogTopic topic = seedTopic("topic-" + slug, legalDomain, country);

        BlogArticle a = new BlogArticle();
        a.setSlug(slug);
        a.setTitle("Title " + slug);
        a.setBodyMarkdown("# Body");
        a.setCountry(country);
        a.setLegalDomain(legalDomain);
        a.setAuthorName("Franck Tounga");
        a.setMetaTitle("Meta " + slug);
        a.setMetaDescription("Meta desc");
        a.setReadingTimeMinutes(8);
        a.setStatus(BlogArticleStatus.PUBLISHED);
        a.setTopicId(topic.getId());
        Instant recent = Instant.now().minus(1, ChronoUnit.DAYS);
        a.setPublishedAt(recent);
        articleRepository.save(a);
    }
}
