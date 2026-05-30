package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-07 : tests unitaires de {@link SaisieRemunerationCalculator}.
 *
 * <p>Valeurs déterministes calculées à partir du barème 2026 par tranches
 * (art. R. 3252-2 CT : bornes annuelles 4 440 / 8 660 / 12 890 / 17 100 /
 * 21 330 / 25 620 €, fractions 1/20, 1/10, 1/5, 1/4, 1/3, 2/3, totalité au-delà,
 * bornes ramenées au mois), majoration par personne à charge (1 690 €/an —
 * R. 3252-3 CT) et fraction insaisissable RSA (646,52 € — L. 3252-3 CT).
 */
class SaisieRemunerationCalculatorTest {

    private static final double EPS = 0.01;

    @Test
    void remunerationMoyenne_sansPersonneACharge_quotiteSelonBareme() {
        SaisieRemunerationResult r = SaisieRemunerationCalculator.calculate(2000.0, 0, 10_000.0, false);

        assertThat(r.verdict()).isEqualTo(SaisieRemunerationVerdict.SAISISSABLE);
        assertThat(r.quotiteSaisissableMensuelle()).isCloseTo(477.71, org.assertj.core.data.Offset.offset(EPS));
        assertThat(r.montantLaisseAuSalarie()).isCloseTo(1522.29, org.assertj.core.data.Offset.offset(EPS));
    }

    @Test
    void personnesACharge_reduisentLaQuotite() {
        SaisieRemunerationResult sans = SaisieRemunerationCalculator.calculate(2000.0, 0, 10_000.0, false);
        SaisieRemunerationResult avec = SaisieRemunerationCalculator.calculate(2000.0, 3, 10_000.0, false);

        assertThat(avec.quotiteSaisissableMensuelle())
                .isLessThan(sans.quotiteSaisissableMensuelle());
        assertThat(avec.quotiteSaisissableMensuelle()).isCloseTo(283.83, org.assertj.core.data.Offset.offset(EPS));
        assertThat(avec.verdict()).isEqualTo(SaisieRemunerationVerdict.SAISISSABLE);
    }

    @Test
    void remunerationInferieureFractionInsaisissable_verdictInsaisissable() {
        SaisieRemunerationResult r = SaisieRemunerationCalculator.calculate(600.0, 0, 5_000.0, false);

        assertThat(r.verdict()).isEqualTo(SaisieRemunerationVerdict.INSAISISSABLE);
        assertThat(r.quotiteSaisissableMensuelle()).isZero();
        assertThat(r.nombreMoisRecouvrement()).isZero();
        assertThat(r.montantLaisseAuSalarie()).isCloseTo(646.52, org.assertj.core.data.Offset.offset(EPS));
    }

    @Test
    void creanceAlimentaire_verdictPaiementDirect_horsBareme() {
        SaisieRemunerationResult r = SaisieRemunerationCalculator.calculate(2000.0, 0, 5_000.0, true);

        assertThat(r.verdict()).isEqualTo(SaisieRemunerationVerdict.ALIMENTAIRE_PAIEMENT_DIRECT);
        // Paiement direct : tout sauf la fraction insaisissable (RSA).
        assertThat(r.quotiteSaisissableMensuelle()).isCloseTo(1353.48, org.assertj.core.data.Offset.offset(EPS));
        assertThat(r.montantLaisseAuSalarie()).isCloseTo(646.52, org.assertj.core.data.Offset.offset(EPS));
    }

    @Test
    void remunerationHaute_trancheTotaliteAuDela_quotiteEleve() {
        SaisieRemunerationResult r = SaisieRemunerationCalculator.calculate(5000.0, 0, 10_000.0, false);

        assertThat(r.verdict()).isEqualTo(SaisieRemunerationVerdict.SAISISSABLE);
        // La part au-delà de la dernière borne (25 620 €/an = 2 135 €/mois) est
        // saisissable en totalité → quotité élevée.
        assertThat(r.quotiteSaisissableMensuelle()).isCloseTo(3432.71, org.assertj.core.data.Offset.offset(EPS));
    }

    @Test
    void nombreMoisRecouvrement_estCeilCreanceSurQuotite() {
        SaisieRemunerationResult r = SaisieRemunerationCalculator.calculate(2000.0, 0, 10_000.0, false);

        // ceil(10 000 / 477,71) = 21
        assertThat(r.nombreMoisRecouvrement()).isEqualTo(21);
    }

    @Test
    void remunerationNulle_leveIllegalArgument() {
        assertThatThrownBy(() -> SaisieRemunerationCalculator.calculate(0.0, 0, 10_000.0, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creanceNulle_leveIllegalArgument() {
        assertThatThrownBy(() -> SaisieRemunerationCalculator.calculate(2000.0, 0, 0.0, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
