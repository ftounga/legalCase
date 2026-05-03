package fr.ailegalcase.blog.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainContextMapperTest {

    @Test
    void contextFor_droitTravail_mentionsCodeDuTravailAndCCT() {
        String context = DomainContextMapper.contextFor("DROIT_DU_TRAVAIL");
        assertThat(context).contains("Code du travail").contains("CCT");
    }

    @Test
    void contextFor_droitImmigration_mentionsCESEDAandLoi1980() {
        String context = DomainContextMapper.contextFor("DROIT_IMMIGRATION");
        assertThat(context).contains("CESEDA").contains("15 décembre 1980");
    }

    @Test
    void contextFor_droitFamille_mentionsCodeCivil() {
        String context = DomainContextMapper.contextFor("DROIT_FAMILLE");
        assertThat(context).contains("Code civil").contains("divorce");
    }

    @Test
    void contextFor_unknownCategory_throws() {
        assertThatThrownBy(() -> DomainContextMapper.contextFor("DROIT_INCONNU"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DROIT_INCONNU");
    }
}
