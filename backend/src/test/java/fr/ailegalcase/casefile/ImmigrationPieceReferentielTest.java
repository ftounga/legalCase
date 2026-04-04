package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImmigrationPieceReferentielTest {

    @Test
    void visaEtudiant_france_returnsNonEmptyList() {
        var pieces = ImmigrationPieceReferentiel.getPieces("VISA_ETUDIANT", "FRANCE");
        assertThat(pieces).isNotEmpty();
        assertThat(pieces).contains("Passeport en cours de validité");
    }

    @Test
    void visaEtudiant_belgique_isDifferentFromFrance() {
        var france = ImmigrationPieceReferentiel.getPieces("VISA_ETUDIANT", "FRANCE");
        var belgique = ImmigrationPieceReferentiel.getPieces("VISA_ETUDIANT", "BELGIQUE");
        assertThat(france).isNotEmpty();
        assertThat(belgique).isNotEmpty();
        assertThat(france).isNotEqualTo(belgique);
    }

    @Test
    void allTypes_allCountries_nonEmpty() {
        for (String type : new String[]{"VISA_ETUDIANT", "TITRE_SALARIE", "REGROUPEMENT_FAMILIAL", "NATURALISATION"}) {
            for (String country : new String[]{"FRANCE", "BELGIQUE"}) {
                assertThat(ImmigrationPieceReferentiel.getPieces(type, country))
                        .as("type=%s country=%s", type, country)
                        .isNotEmpty();
            }
        }
    }

    @Test
    void unknownType_returnsEmptyList() {
        assertThat(ImmigrationPieceReferentiel.getPieces("TYPE_INCONNU", "FRANCE")).isEmpty();
    }

    @Test
    void unknownCountry_returnsEmptyList() {
        assertThat(ImmigrationPieceReferentiel.getPieces("VISA_ETUDIANT", "ALLEMAGNE")).isEmpty();
    }

    @Test
    void isTitreTypeValid_knownType_returnsTrue() {
        assertThat(ImmigrationPieceReferentiel.isTitreTypeValid("NATURALISATION")).isTrue();
    }

    @Test
    void isTitreTypeValid_unknownType_returnsFalse() {
        assertThat(ImmigrationPieceReferentiel.isTitreTypeValid("VISA_TOURISTE")).isFalse();
    }

    @Test
    void isCountryValid_supportedCountries() {
        assertThat(ImmigrationPieceReferentiel.isCountryValid("FRANCE")).isTrue();
        assertThat(ImmigrationPieceReferentiel.isCountryValid("BELGIQUE")).isTrue();
        assertThat(ImmigrationPieceReferentiel.isCountryValid("ALLEMAGNE")).isFalse();
    }
}
