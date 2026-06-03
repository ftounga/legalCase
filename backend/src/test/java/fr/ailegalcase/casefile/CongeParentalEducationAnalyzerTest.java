package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-45 : tests unitaires de {@link CongeParentalEducationAnalyzer}
 * (F-DT-78, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.1225-47 à L.1225-60 CT) :
 * <ul>
 *   <li>éligibilité = ancienneteMois &ge; 12 à la date de naissance / adoption
 *       (L.1225-47) ;</li>
 *   <li>date de fin maximale = dateNaissanceOuAdoption + 3 ans (3e anniversaire,
 *       L.1225-48) ;</li>
 *   <li>protection / réintégration (L.1225-55) + mention PreParE (information) ;</li>
 *   <li>champ requis null / ancienneté &lt; 0 / nombreEnfants &lt; 1 →
 *       IllegalArgument.</li>
 * </ul>
 */
class CongeParentalEducationAnalyzerTest {

    private static final LocalDate NAISSANCE = LocalDate.of(2025, 3, 1);

    @Test
    void eligible_anciennete18mois_dateFinMaxPlus3ans() {
        CongeParentalEducationResult r = CongeParentalEducationAnalyzer.analyze(
                18, CongeParentalEducationModalite.TEMPS_PLEIN, 1, NAISSANCE);

        assertThat(r.statut()).isEqualTo(CongeParentalEducationStatut.ELIGIBLE);
        assertThat(r.dateFinMax()).isEqualTo(LocalDate.of(2028, 3, 1));
        assertThat(r.dureeMaxMois()).isEqualTo(36);
        assertThat(r.protectionReintegration()).isTrue();
        assertThat(r.mentionPreparE()).isTrue();
        assertThat(r.baseJuridique()).contains("L.1225-47");
    }

    @Test
    void nonEligible_anciennete8mois_pasDeDateFin() {
        CongeParentalEducationResult r = CongeParentalEducationAnalyzer.analyze(
                8, CongeParentalEducationModalite.TEMPS_PLEIN, 1, NAISSANCE);

        assertThat(r.statut()).isEqualTo(CongeParentalEducationStatut.NON_ELIGIBLE);
        assertThat(r.dateFinMax()).isNull();
        assertThat(r.dureeMaxMois()).isZero();
        assertThat(r.notes()).anyMatch(n -> n.contains("L.1225-47"));
    }

    @Test
    void seuilExact_anciennete12mois_eligible() {
        CongeParentalEducationResult r = CongeParentalEducationAnalyzer.analyze(
                12, CongeParentalEducationModalite.TEMPS_PLEIN, 1, NAISSANCE);

        assertThat(r.statut()).isEqualTo(CongeParentalEducationStatut.ELIGIBLE);
        assertThat(r.dateFinMax()).isEqualTo(LocalDate.of(2028, 3, 1));
    }

    @Test
    void dateFinMax_estLeTroisiemeAnniversaire() {
        CongeParentalEducationResult r = CongeParentalEducationAnalyzer.analyze(
                24, CongeParentalEducationModalite.TEMPS_PLEIN, 1, LocalDate.of(2024, 12, 15));

        assertThat(r.dateFinMax()).isEqualTo(LocalDate.of(2027, 12, 15));
    }

    @Test
    void tempsPartiel_eligibiliteInchangee_modaliteRetenue() {
        CongeParentalEducationResult r = CongeParentalEducationAnalyzer.analyze(
                15, CongeParentalEducationModalite.TEMPS_PARTIEL, 1, NAISSANCE);

        assertThat(r.statut()).isEqualTo(CongeParentalEducationStatut.ELIGIBLE);
        assertThat(r.modaliteRetenue()).isEqualTo(CongeParentalEducationModalite.TEMPS_PARTIEL);
        assertThat(r.notes()).anyMatch(n -> n.contains("temps partiel"));
    }

    @Test
    void naissancesMultiples_noteSpecifique() {
        CongeParentalEducationResult r = CongeParentalEducationAnalyzer.analyze(
                20, CongeParentalEducationModalite.TEMPS_PLEIN, 2, NAISSANCE);

        assertThat(r.statut()).isEqualTo(CongeParentalEducationStatut.ELIGIBLE);
        assertThat(r.nombreEnfants()).isEqualTo(2);
        assertThat(r.notes()).anyMatch(n -> n.contains("multiples"));
    }

    @Test
    void protectionReintegration_etPreparE_toujoursPresents() {
        CongeParentalEducationResult r = CongeParentalEducationAnalyzer.analyze(
                14, CongeParentalEducationModalite.TEMPS_PLEIN, 1, NAISSANCE);

        assertThat(r.protectionReintegration()).isTrue();
        assertThat(r.notes()).anyMatch(n -> n.contains("L.1225-55"));
        assertThat(r.notes()).anyMatch(n -> n.contains("PreParE"));
    }

    @Test
    void ancienneteNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeParentalEducationAnalyzer.analyze(
                null, CongeParentalEducationModalite.TEMPS_PLEIN, 1, NAISSANCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ancienneteNegative_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeParentalEducationAnalyzer.analyze(
                -1, CongeParentalEducationModalite.TEMPS_PLEIN, 1, NAISSANCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nombreEnfantsZero_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeParentalEducationAnalyzer.analyze(
                18, CongeParentalEducationModalite.TEMPS_PLEIN, 0, NAISSANCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modaliteNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeParentalEducationAnalyzer.analyze(
                18, null, 1, NAISSANCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateNull_leveIllegalArgument() {
        assertThatThrownBy(() -> CongeParentalEducationAnalyzer.analyze(
                18, CongeParentalEducationModalite.TEMPS_PLEIN, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
