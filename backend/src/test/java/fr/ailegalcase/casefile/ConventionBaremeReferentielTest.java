package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConventionBaremeReferentielTest {

    @Test
    void france_hasAtLeast5Conventions() {
        assertThat(ConventionBaremeReferentiel.getByCountry("FRANCE")).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void belgique_hasAtLeast3Commissions() {
        assertThat(ConventionBaremeReferentiel.getByCountry("BELGIQUE")).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void getByCountry_returnsOnlyThatCountry() {
        assertThat(ConventionBaremeReferentiel.getByCountry("FRANCE"))
                .allMatch(b -> "FRANCE".equals(b.country()));
        assertThat(ConventionBaremeReferentiel.getByCountry("BELGIQUE"))
                .allMatch(b -> "BELGIQUE".equals(b.country()));
    }

    @Test
    void getByCode_known_returnsBareme() {
        ConventionBareme b = ConventionBaremeReferentiel.getByCode("METALLURGIE");
        assertThat(b).isNotNull();
        assertThat(b.country()).isEqualTo("FRANCE");
        assertThat(b.congesLegauxJours()).isEqualTo(25);
    }

    @Test
    void getByCode_unknown_returnsNull() {
        assertThat(ConventionBaremeReferentiel.getByCode("INCONNU")).isNull();
    }

    @Test
    void allBaremes_haveNonEmptyFields() {
        for (ConventionBareme b : ConventionBaremeReferentiel.getAll()) {
            assertThat(b.code()).as("code").isNotBlank();
            assertThat(b.label()).as("label %s", b.code()).isNotBlank();
            assertThat(b.country()).as("country %s", b.code()).isIn("FRANCE", "BELGIQUE");
            assertThat(b.congesLegauxJours()).as("conges %s", b.code()).isPositive();
            assertThat(b.congesSupplementaires()).as("congesSupp %s", b.code()).isNotEmpty();
            assertThat(b.primesAnciennete()).as("primes %s", b.code()).isNotEmpty();
            assertThat(b.baseJuridique()).as("base %s", b.code()).isNotBlank();
        }
    }

    @Test
    void belgique_has20CongesLegaux() {
        for (ConventionBareme b : ConventionBaremeReferentiel.getByCountry("BELGIQUE")) {
            assertThat(b.congesLegauxJours()).as("conges %s", b.code()).isEqualTo(20);
        }
    }

    @Test
    void france_has25CongesLegaux() {
        for (ConventionBareme b : ConventionBaremeReferentiel.getByCountry("FRANCE")) {
            assertThat(b.congesLegauxJours()).as("conges %s", b.code()).isEqualTo(25);
        }
    }
}
