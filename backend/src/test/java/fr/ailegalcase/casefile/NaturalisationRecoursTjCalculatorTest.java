package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-29 : tests unitaires de {@link NaturalisationRecoursTjCalculator}.
 * Couvre échéance 6 mois (Cciv 26-3), statuts RECOURS_POSSIBLE / URGENT /
 * PRESCRIT, motifs par voie, bases juridiques, tribunal compétent et validations.
 */
class NaturalisationRecoursTjCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

    @Test
    void voieMariage_recoursPossible_motifsEtTribunal() {
        // Refus il y a 1 mois → échéance dans ~5 mois → RECOURS_POSSIBLE.
        LocalDate refus = TODAY.minusMonths(1);
        NaturalisationRecoursTjResult r = NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.MARIAGE, refus,
                NaturalisationRecoursTjTypeRefusEnum.REFUS_ENREGISTREMENT, TODAY);

        assertThat(r.statut()).isEqualTo(NaturalisationRecoursTjStatut.RECOURS_POSSIBLE);
        assertThat(r.dateEcheanceRecoursJudicaire()).isEqualTo(refus.plusMonths(6));
        assertThat(r.tribunalCompetent()).contains("Tribunal judiciaire").contains("distinct du TA de Nantes");
        assertThat(r.motifsRecoursDisponibles())
                .anyMatch(m -> m.toLowerCase().contains("communauté de vie"))
                .anyMatch(m -> m.toLowerCase().contains("durée"));
        assertThat(r.messagePrescription()).isNull();
    }

    @Test
    void basesJuridiques_incluentCciv26_3_et_voieMariage() {
        NaturalisationRecoursTjResult r = NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.MARIAGE, TODAY.minusMonths(2),
                NaturalisationRecoursTjTypeRefusEnum.REFUS_ENREGISTREMENT, TODAY);

        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("26-3"))
                .anyMatch(b -> b.contains("21-2"));
    }

    @Test
    void basesJuridiques_ascendant_et_mineur_distinctes() {
        NaturalisationRecoursTjResult asc = NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.ASCENDANT, TODAY.minusMonths(1),
                NaturalisationRecoursTjTypeRefusEnum.CONTESTATION_NATIONALITE, TODAY);
        assertThat(asc.basesJuridiques()).anyMatch(b -> b.contains("21-13"));

        NaturalisationRecoursTjResult min = NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.MINEUR_22_1, TODAY.minusMonths(1),
                NaturalisationRecoursTjTypeRefusEnum.REFUS_ENREGISTREMENT, TODAY);
        assertThat(min.basesJuridiques()).anyMatch(b -> b.contains("22-1"));
    }

    @Test
    void prescrit_plusDe6Mois_motifsVides_messagePrescription() {
        // Refus il y a 7 mois → échéance dépassée → PRESCRIT.
        LocalDate refus = TODAY.minusMonths(7);
        NaturalisationRecoursTjResult r = NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.MARIAGE, refus,
                NaturalisationRecoursTjTypeRefusEnum.REFUS_ENREGISTREMENT, TODAY);

        assertThat(r.statut()).isEqualTo(NaturalisationRecoursTjStatut.PRESCRIT);
        assertThat(r.joursRestants()).isLessThanOrEqualTo(0);
        assertThat(r.motifsRecoursDisponibles()).isEmpty();
        assertThat(r.messagePrescription()).isNotNull().contains("prescrit");
    }

    @Test
    void urgent_moinsDe30Jours() {
        // Refus il y a presque 6 mois → reste < 30 j → URGENT.
        LocalDate refus = TODAY.minusMonths(6).plusDays(20);
        NaturalisationRecoursTjResult r = NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.ASCENDANT, refus,
                NaturalisationRecoursTjTypeRefusEnum.REFUS_ENREGISTREMENT, TODAY);

        assertThat(r.statut()).isEqualTo(NaturalisationRecoursTjStatut.URGENT);
        assertThat(r.joursRestants()).isBetween(1L, 30L);
        assertThat(r.motifsRecoursDisponibles()).isNotEmpty();
    }

    @Test
    void recoursPossible_exactement_marge_confortable() {
        LocalDate refus = TODAY.minusDays(10);
        NaturalisationRecoursTjResult r = NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.MINEUR_22_1, refus,
                NaturalisationRecoursTjTypeRefusEnum.CONTESTATION_NATIONALITE, TODAY);

        assertThat(r.statut()).isEqualTo(NaturalisationRecoursTjStatut.RECOURS_POSSIBLE);
        assertThat(r.joursRestants()).isGreaterThan(30);
    }

    @Test
    void dateRefusFuture_leve_illegalArgument() {
        assertThatThrownBy(() -> NaturalisationRecoursTjCalculator.compute(
                NaturalisationRecoursTjVoieEnum.MARIAGE, TODAY.plusDays(3),
                NaturalisationRecoursTjTypeRefusEnum.REFUS_ENREGISTREMENT, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void voieNull_leve_illegalArgument() {
        assertThatThrownBy(() -> NaturalisationRecoursTjCalculator.compute(
                null, TODAY.minusMonths(1),
                NaturalisationRecoursTjTypeRefusEnum.REFUS_ENREGISTREMENT, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
