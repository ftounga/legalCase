package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegalDomainPromptBuilderTest {

    // U-01 : DROIT_DU_TRAVAIL + FRANCE → "droit du travail français"
    @Test
    void domainLabel_travailFrance_returnsFrench() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_DU_TRAVAIL", "FRANCE"))
                .isEqualTo("droit du travail français");
    }

    // U-02 : DROIT_IMMIGRATION + FRANCE → "droit de l'immigration française"
    @Test
    void domainLabel_immigrationFrance_returnsFeminineFrench() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_IMMIGRATION", "FRANCE"))
                .isEqualTo("droit de l'immigration française");
    }

    // U-03 : DROIT_FAMILLE + BELGIQUE → "droit de la famille belge"
    @Test
    void domainLabel_familleBelgique_returnsBelge() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_FAMILLE", "BELGIQUE"))
                .isEqualTo("droit de la famille belge");
    }

    // U-04 : DROIT_IMMIGRATION + BELGIQUE → "droit de l'immigration belge"
    @Test
    void domainLabel_immigrationBelgique_returnsBelge() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_IMMIGRATION", "BELGIQUE"))
                .isEqualTo("droit de l'immigration belge");
    }
}
