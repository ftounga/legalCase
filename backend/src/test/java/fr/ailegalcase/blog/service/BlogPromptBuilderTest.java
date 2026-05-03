package fr.ailegalcase.blog.service;

import fr.ailegalcase.blog.entity.BlogTopic;
import fr.ailegalcase.blog.entity.BlogTopicStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BlogPromptBuilderTest {

    private final BlogPromptBuilder builder = new BlogPromptBuilder();

    @Test
    void buildSonnetPrompt_includesTopicMetadataAndDomainContext() {
        BlogTopic topic = topic("DROIT_DU_TRAVAIL", "FR",
                "rupture-conventionnelle-frais-avocat",
                "Rupture conventionnelle : qui prend en charge les frais d'avocat ?",
                "Article SEO sur la prise en charge des frais d'avocat lors d'une rupture conventionnelle.");

        BlogPromptBuilder.PromptPair pair = builder.buildSonnetPrompt(topic);

        assertThat(pair.system())
                .contains("Code du travail")
                .contains("France")
                .contains("avocats");
        assertThat(pair.user())
                .contains("rupture-conventionnelle-frais-avocat")
                .contains("Rupture conventionnelle")
                .contains("Pays : France")
                .contains("metaTitle")
                .contains("legalCitations")
                .contains("LEGALCASE_CTA");
    }

    @Test
    void buildSonnetPrompt_belgiumLabelInjected() {
        BlogTopic topic = topic("DROIT_FAMILLE", "BE",
                "divorce-separation-de-fait-belgique",
                "Divorce par séparation de fait en Belgique",
                null);

        BlogPromptBuilder.PromptPair pair = builder.buildSonnetPrompt(topic);

        assertThat(pair.system()).contains("Belgique");
        assertThat(pair.user()).contains("Pays : Belgique");
    }

    @Test
    void buildHaikuPrompt_includesAllCitations() {
        List<BlogPromptBuilder.LegalCitationToVerify> citations = List.of(
                new BlogPromptBuilder.LegalCitationToVerify(
                        "Code du travail, art. L1237-13",
                        "indemnité spécifique de rupture conventionnelle"),
                new BlogPromptBuilder.LegalCitationToVerify(
                        "Cass. soc. 25 mars 2026, n° 24-11.234",
                        "validité de la signature électronique"));

        BlogPromptBuilder.PromptPair pair = builder.buildHaikuPrompt(citations);

        assertThat(pair.system())
                .contains("KNOWN")
                .contains("UNVERIFIABLE")
                .contains("LIKELY_HALLUCINATION");
        assertThat(pair.user())
                .contains("L1237-13")
                .contains("Cass. soc. 25 mars 2026");
    }

    @Test
    void buildHaikuPrompt_emptyCitations_stillProducesValidPrompt() {
        BlogPromptBuilder.PromptPair pair = builder.buildHaikuPrompt(List.of());

        assertThat(pair.system()).isNotEmpty();
        assertThat(pair.user()).contains("(aucune)");
    }

    private BlogTopic topic(String category, String country, String slug, String title, String description) {
        BlogTopic t = new BlogTopic();
        t.setId(UUID.randomUUID());
        t.setSlug(slug);
        t.setTitle(title);
        t.setDescription(description);
        t.setCategory(category);
        t.setCountryScope(country);
        t.setStatus(BlogTopicStatus.RESERVED);
        return t;
    }
}
