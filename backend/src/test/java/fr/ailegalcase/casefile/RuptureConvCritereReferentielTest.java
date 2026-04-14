package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuptureConvCritereReferentielTest {

    @Test
    void franceReferentiel_a6Criteres_sommePoids100() {
        List<RuptureConvCritere> fr = RuptureConvCritereReferentiel.getByCountry("FRANCE");
        assertThat(fr).hasSize(6);
        int total = fr.stream().mapToInt(RuptureConvCritere::poids).sum();
        assertThat(total).isEqualTo(100);
    }

    @Test
    void franceReferentiel_contientCodesAttendus() {
        List<String> codes = RuptureConvCritereReferentiel.getByCountry("FRANCE").stream()
                .map(RuptureConvCritere::code).toList();
        assertThat(codes).containsExactlyInAnyOrder(
                "RC_CONSENTEMENT", "RC_DELAI_RETRACTATION", "RC_HOMOLOGATION",
                "RC_ASSISTANCE", "RC_INDEMNITE", "RC_ENTRETIENS");
    }

    @Test
    void franceReferentiel_critere4Bloquants() {
        long bloquants = RuptureConvCritereReferentiel.getByCountry("FRANCE").stream()
                .filter(RuptureConvCritere::bloquant).count();
        assertThat(bloquants).isEqualTo(4);
    }

    @Test
    void getByCode_trouveCritere_caseInsensitive() {
        assertThat(RuptureConvCritereReferentiel.getByCode("RC_CONSENTEMENT")).isNotNull();
        assertThat(RuptureConvCritereReferentiel.getByCode("rc_consentement")).isNotNull();
    }

    @Test
    void getByCode_inconnu_retourneNull() {
        assertThat(RuptureConvCritereReferentiel.getByCode("RC_FANTAISIE")).isNull();
        assertThat(RuptureConvCritereReferentiel.getByCode(null)).isNull();
    }

    @Test
    void isCountryValid_franceSeulement() {
        assertThat(RuptureConvCritereReferentiel.isCountryValid("FRANCE")).isTrue();
        assertThat(RuptureConvCritereReferentiel.isCountryValid("BELGIQUE")).isFalse();
        assertThat(RuptureConvCritereReferentiel.isCountryValid("FOO")).isFalse();
    }

    @Test
    void getByCountry_belgique_retourneVide() {
        assertThat(RuptureConvCritereReferentiel.getByCountry("BELGIQUE")).isEmpty();
    }

    @Test
    void getByCountry_null_retourneVide() {
        assertThat(RuptureConvCritereReferentiel.getByCountry(null)).isEmpty();
    }
}
