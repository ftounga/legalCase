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

    // SF-128-01 : règle critique de classification en tête pour chaque domaine
    @Test
    void domainSpecificInstruction_travail_containsClassificationRule() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("RÈGLE CRITIQUE DE CLASSIFICATION");
        assertThat(instruction).contains("MÉCANISME FACTUEL");
        assertThat(instruction).contains("RUPTURE_CONVENTIONNELLE");
    }

    @Test
    void domainSpecificInstruction_immigration_containsClassificationRule() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        assertThat(instruction).contains("RÈGLE CRITIQUE DE CLASSIFICATION");
        assertThat(instruction).contains("MÉCANISME FACTUEL");
        assertThat(instruction).contains("RECOURS_CNDA");
    }

    @Test
    void domainSpecificInstruction_famille_containsClassificationRule() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("RÈGLE CRITIQUE DE CLASSIFICATION");
        assertThat(instruction).contains("MÉCANISME FACTUEL");
        assertThat(instruction).contains("regime_matrimonial");
    }
}
