package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImmigrationWorkRightReferentielTest {

    @Test
    void france_hasAtLeast8Titles() {
        assertThat(ImmigrationWorkRightReferentiel.getByCountry("FRANCE")).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void belgique_hasAtLeast8Titles() {
        assertThat(ImmigrationWorkRightReferentiel.getByCountry("BELGIQUE")).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void getByCountry_returnsOnlyThatCountry() {
        assertThat(ImmigrationWorkRightReferentiel.getByCountry("FRANCE"))
                .allMatch(w -> "FRANCE".equals(w.country()));
        assertThat(ImmigrationWorkRightReferentiel.getByCountry("BELGIQUE"))
                .allMatch(w -> "BELGIQUE".equals(w.country()));
    }

    @Test
    void allEntries_haveDroitTravailValid() {
        for (WorkRightResult w : ImmigrationWorkRightReferentiel.getAll()) {
            assertThat(w.droitTravail())
                    .as("droitTravail for %s", w.titreType())
                    .isIn("OUI", "NON", "CONDITIONNEL");
        }
    }

    @Test
    void allEntries_haveNonEmptyObligations() {
        for (WorkRightResult w : ImmigrationWorkRightReferentiel.getAll()) {
            assertThat(w.obligationsEmployeur())
                    .as("obligations for %s", w.titreType())
                    .isNotEmpty();
        }
    }

    @Test
    void allEntries_haveNonEmptyFields() {
        for (WorkRightResult w : ImmigrationWorkRightReferentiel.getAll()) {
            assertThat(w.titreType()).as("titreType").isNotBlank();
            assertThat(w.titreLabel()).as("label for %s", w.titreType()).isNotBlank();
            assertThat(w.country()).as("country for %s", w.titreType()).isIn("FRANCE", "BELGIQUE");
            assertThat(w.conditions()).as("conditions for %s", w.titreType()).isNotBlank();
            assertThat(w.baseJuridique()).as("base for %s", w.titreType()).isNotBlank();
        }
    }

    @Test
    void getByTitreType_knownType_returnsResult() {
        WorkRightResult result = ImmigrationWorkRightReferentiel.getByTitreType("VLS_TS_SALARIE", "FRANCE");
        assertThat(result).isNotNull();
        assertThat(result.droitTravail()).isEqualTo("OUI");
    }

    @Test
    void getByTitreType_unknownType_returnsNull() {
        assertThat(ImmigrationWorkRightReferentiel.getByTitreType("INCONNU", "FRANCE")).isNull();
    }

    @Test
    void conditionalTitles_existForBothCountries() {
        assertThat(ImmigrationWorkRightReferentiel.getByCountry("FRANCE"))
                .anyMatch(w -> "CONDITIONNEL".equals(w.droitTravail()));
        assertThat(ImmigrationWorkRightReferentiel.getByCountry("BELGIQUE"))
                .anyMatch(w -> "CONDITIONNEL".equals(w.droitTravail()));
    }

    @Test
    void vlsTsEtudiant_isConditional() {
        WorkRightResult result = ImmigrationWorkRightReferentiel.getByTitreType("VLS_TS_ETUDIANT", "FRANCE");
        assertThat(result).isNotNull();
        assertThat(result.droitTravail()).isEqualTo("CONDITIONNEL");
        assertThat(result.conditions()).contains("964 heures");
    }

    @Test
    void carteAEtudes_belgique_isConditional() {
        WorkRightResult result = ImmigrationWorkRightReferentiel.getByTitreType("CARTE_A_ETUDES", "BELGIQUE");
        assertThat(result).isNotNull();
        assertThat(result.droitTravail()).isEqualTo("CONDITIONNEL");
        assertThat(result.conditions()).contains("20 heures");
    }

    @Test
    void carteResident_france_isOui() {
        WorkRightResult result = ImmigrationWorkRightReferentiel.getByTitreType("CARTE_RESIDENT", "FRANCE");
        assertThat(result).isNotNull();
        assertThat(result.droitTravail()).isEqualTo("OUI");
    }

    @Test
    void attestationImmatriculation_belgique_isConditional() {
        WorkRightResult result = ImmigrationWorkRightReferentiel.getByTitreType("ATTESTATION_IMMATRICULATION", "BELGIQUE");
        assertThat(result).isNotNull();
        assertThat(result.droitTravail()).isEqualTo("CONDITIONNEL");
        assertThat(result.conditions()).contains("4 mois");
    }
}
