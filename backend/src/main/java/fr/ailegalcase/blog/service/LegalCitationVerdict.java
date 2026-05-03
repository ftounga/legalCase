package fr.ailegalcase.blog.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LegalCitationVerdict(
        @JsonProperty("verdicts") List<Entry> verdicts
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            @JsonProperty("reference") String reference,
            @JsonProperty("verdict") Outcome verdict,
            @JsonProperty("rationale") String rationale
    ) {}

    public enum Outcome {
        KNOWN,
        UNVERIFIABLE,
        LIKELY_HALLUCINATION
    }
}
