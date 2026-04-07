package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LicenciementCritereReferentielTest {

    @Test
    void france_hasAtLeast6Criteres() {
        assertThat(LicenciementCritereReferentiel.getByCountry("FRANCE")).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void belgique_hasAtLeast6Criteres() {
        assertThat(LicenciementCritereReferentiel.getByCountry("BELGIQUE")).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void allCriteres_haveNonEmptyFields() {
        for (LicenciementCritere c : LicenciementCritereReferentiel.getAll()) {
            assertThat(c.code()).as("code").isNotBlank();
            assertThat(c.label()).as("label %s", c.code()).isNotBlank();
            assertThat(c.description()).as("desc %s", c.code()).isNotBlank();
            assertThat(c.poids()).as("poids %s", c.code()).isPositive();
            assertThat(c.baseJuridique()).as("base %s", c.code()).isNotBlank();
        }
    }

    @Test
    void eachCountry_hasBloquantCriteres() {
        assertThat(LicenciementCritereReferentiel.getByCountry("FRANCE"))
                .anyMatch(LicenciementCritere::bloquant);
        assertThat(LicenciementCritereReferentiel.getByCountry("BELGIQUE"))
                .anyMatch(LicenciementCritere::bloquant);
    }

    @Test
    void getByCode_known() {
        assertThat(LicenciementCritereReferentiel.getByCode("FR_MOTIVATION")).isNotNull();
        assertThat(LicenciementCritereReferentiel.getByCode("BE_MOTIVATION")).isNotNull();
    }

    @Test
    void getByCode_unknown() {
        assertThat(LicenciementCritereReferentiel.getByCode("INCONNU")).isNull();
    }
}
