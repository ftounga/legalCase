package fr.ailegalcase.blog.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedArticleJson(
        @JsonProperty("metaTitle") String metaTitle,
        @JsonProperty("metaDescription") String metaDescription,
        @JsonProperty("subtitle") String subtitle,
        @JsonProperty("introduction") String introduction,
        @JsonProperty("sections") List<Section> sections,
        @JsonProperty("conclusion") String conclusion,
        @JsonProperty("ctaBlock") String ctaBlock,
        @JsonProperty("legalCitations") List<LegalCitation> legalCitations
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(
            @JsonProperty("heading") String heading,
            @JsonProperty("content") String content
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegalCitation(
            @JsonProperty("reference") String reference,
            @JsonProperty("context") String context
    ) {}
}
