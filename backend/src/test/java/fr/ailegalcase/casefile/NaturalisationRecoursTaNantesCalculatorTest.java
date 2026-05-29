package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-31 : tests unitaires de {@link NaturalisationRecoursTaNantesCalculator}.
 * Couvre échéance 2 mois (CJA L. 213-1), statuts RECOURS_POSSIBLE / URGENT /
 * PRESCRIT, motifs, bases juridiques, tribunal compétent (TA Nantes) et validations.
 */
class NaturalisationRecoursTaNantesCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

    @Test
    void delai2Mois_recoursPossible_tribunalTaNantes_etMotifs() {
        // Refus il y a 10 jours → échéance dans ~1,5 mois → RECOURS_POSSIBLE.
        LocalDate refus = TODAY.minusDays(10);
        NaturalisationRecoursTaNantesResult r = NaturalisationRecoursTaNantesCalculator.compute(
                refus, "Intégration insuffisante", false, TODAY);

        assertThat(r.statut()).isEqualTo(NaturalisationRecoursTaNantesStatut.RECOURS_POSSIBLE);
        assertThat(r.dateEcheanceRecoursTa()).isEqualTo(refus.plusMonths(2));
        assertThat(r.tribunalCompetent())
                .contains("Tribunal administratif de Nantes")
                .contains("compétence exclusive nationale");
        assertThat(r.motifsRecoursDisponibles())
                .anyMatch(m -> m.toLowerCase().contains("motivation"))
                .anyMatch(m -> m.toLowerCase().contains("excès de pouvoir"))
                .anyMatch(m -> m.toLowerCase().contains("intégration"));
        assertThat(r.messagePrescription()).isNull();
        assertThat(r.joursRestants()).isGreaterThan(15);
    }

    @Test
    void basesJuridiques_incluentCjaL213_1_etCciv21_15() {
        NaturalisationRecoursTaNantesResult r = NaturalisationRecoursTaNantesCalculator.compute(
                TODAY.minusDays(20), null, true, TODAY);

        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 213-1"))
                .anyMatch(b -> b.contains("21-15"));
        // recoursPrerequis transmis tel quel
        assertThat(r.recoursPrerequis()).isTrue();
    }

    @Test
    void urgent_moinsDe15Jours() {
        // Refus il y a presque 2 mois → reste ≤ 15 j → URGENT.
        LocalDate refus = TODAY.minusMonths(2).plusDays(10);
        NaturalisationRecoursTaNantesResult r = NaturalisationRecoursTaNantesCalculator.compute(
                refus, null, false, TODAY);

        assertThat(r.statut()).isEqualTo(NaturalisationRecoursTaNantesStatut.URGENT);
        assertThat(r.joursRestants()).isBetween(1L, 15L);
        assertThat(r.motifsRecoursDisponibles()).isNotEmpty();
    }

    @Test
    void prescrit_plusDe2Mois_motifsVides_messagePrescription() {
        // Refus il y a 3 mois → échéance dépassée → PRESCRIT.
        LocalDate refus = TODAY.minusMonths(3);
        NaturalisationRecoursTaNantesResult r = NaturalisationRecoursTaNantesCalculator.compute(
                refus, "Motif quelconque", false, TODAY);

        assertThat(r.statut()).isEqualTo(NaturalisationRecoursTaNantesStatut.PRESCRIT);
        assertThat(r.joursRestants()).isLessThanOrEqualTo(0);
        assertThat(r.motifsRecoursDisponibles()).isEmpty();
        assertThat(r.messagePrescription()).isNotNull().contains("prescrit");
    }

    @Test
    void motivationRefusTropLongue_leve_illegalArgument() {
        String tropLong = "x".repeat(501);
        assertThatThrownBy(() -> NaturalisationRecoursTaNantesCalculator.compute(
                TODAY.minusDays(5), tropLong, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateRefusFuture_leve_illegalArgument() {
        assertThatThrownBy(() -> NaturalisationRecoursTaNantesCalculator.compute(
                TODAY.plusDays(3), null, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateRefusNull_leve_illegalArgument() {
        assertThatThrownBy(() -> NaturalisationRecoursTaNantesCalculator.compute(
                null, null, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
