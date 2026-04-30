package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests F-IM-21 SF-IM-21-01 : référentiel critères binaires validité immigration.
 */
class ImmigrationValidationCriteriaReferentielTest {

    private static final List<String> CODES_FR = List.of(
            "IM21_REGULARITE_SEJOUR_FR",
            "IM21_DELAI_DEPOT_FR",
            "IM21_PIECE_IDENTITE_FR",
            "IM21_JUSTIF_DOMICILE_FR",
            "IM21_ETAT_CIVIL_FR",
            "IM21_PHOTO_FR",
            "IM21_TIMBRE_FISCAL_FR",
            "IM21_PIECES_MARIAGE_FR",
            "IM21_COMMUNAUTE_VIE_FR",
            "IM21_RESSOURCES_FR",
            "IM21_CONVENTION_ACCUEIL_FR"
    );

    private static final List<String> CODES_BE = List.of(
            "IM21_REGULARITE_SEJOUR_BE",
            "IM21_PIECE_IDENTITE_BE",
            "IM21_PIECES_COHABITATION_BE",
            "IM21_RESSOURCES_BE",
            "IM21_LOGEMENT_BE",
            "IM21_ASSURANCE_BE",
            "IM21_EXTRAIT_CASIER_BE"
    );

    @Test
    void resolve_returnsCorrectCriterionForEveryFrCode() {
        for (String code : CODES_FR) {
            ImmigrationValidationCriterion c = ImmigrationValidationCriteriaReferentiel.resolve(code);
            assertThat(c).as("Code %s doit être résolu", code).isNotNull();
            assertThat(c.code()).isEqualTo(code);
            assertThat(c.country()).isEqualTo("FRANCE");
        }
    }

    @Test
    void resolve_returnsCorrectCriterionForEveryBeCode() {
        for (String code : CODES_BE) {
            ImmigrationValidationCriterion c = ImmigrationValidationCriteriaReferentiel.resolve(code);
            assertThat(c).as("Code %s doit être résolu", code).isNotNull();
            assertThat(c.code()).isEqualTo(code);
            assertThat(c.country()).isEqualTo("BELGIQUE");
        }
    }

    @Test
    void forCountry_FRANCE_returns11Criteria() {
        List<ImmigrationValidationCriterion> result = ImmigrationValidationCriteriaReferentiel.forCountry("FRANCE");
        assertThat(result).hasSize(11);
        assertThat(result).allMatch(c -> "FRANCE".equals(c.country()));
    }

    @Test
    void forCountry_BELGIQUE_returns7Criteria() {
        List<ImmigrationValidationCriterion> result = ImmigrationValidationCriteriaReferentiel.forCountry("BELGIQUE");
        assertThat(result).hasSize(7);
        assertThat(result).allMatch(c -> "BELGIQUE".equals(c.country()));
    }

    @Test
    void allCriteria_haveNonEmptyDescription() {
        for (ImmigrationValidationCriterion c : ImmigrationValidationCriteriaReferentiel.all()) {
            assertThat(c.description()).as("Description code %s", c.code()).isNotBlank();
            assertThat(c.description().length()).as("Description code %s minLength", c.code()).isGreaterThanOrEqualTo(20);
        }
    }

    @Test
    void allCriteria_haveNonEmptyBaseJuridique() {
        for (ImmigrationValidationCriterion c : ImmigrationValidationCriteriaReferentiel.all()) {
            assertThat(c.baseJuridique()).as("BaseJuridique code %s", c.code()).isNotBlank();
        }
    }

    @Test
    void allCriteria_haveNonEmptyLabel() {
        for (ImmigrationValidationCriterion c : ImmigrationValidationCriteriaReferentiel.all()) {
            assertThat(c.label()).as("Label code %s", c.code()).isNotBlank();
        }
    }

    @Test
    void resolve_unknownCode_returnsNull() {
        assertThat(ImmigrationValidationCriteriaReferentiel.resolve("IM21_UNKNOWN")).isNull();
        assertThat(ImmigrationValidationCriteriaReferentiel.resolve("FR_CONVOCATION")).isNull();
        assertThat(ImmigrationValidationCriteriaReferentiel.resolve(null)).isNull();
    }

    @Test
    void all_returns18Criteria() {
        assertThat(ImmigrationValidationCriteriaReferentiel.all()).hasSize(18);
    }

    @Test
    void forCountry_unknownCountry_returnsEmpty() {
        assertThat(ImmigrationValidationCriteriaReferentiel.forCountry("ALLEMAGNE")).isEmpty();
        assertThat(ImmigrationValidationCriteriaReferentiel.forCountry(null)).isEmpty();
    }
}
