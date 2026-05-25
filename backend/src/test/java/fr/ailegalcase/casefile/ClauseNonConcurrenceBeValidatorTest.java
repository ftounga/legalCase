package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-01 : tests unitaires du {@link ClauseNonConcurrenceBeValidator}.
 *
 * <p>Vérifie les 4 verdicts (VALIDE / NULLE x 3 raisons / PARTIELLEMENT_NULLE),
 * le calcul d'indemnité légale ½ × rémunération mensuelle × durée, l'arrondi
 * 2 décimales, la prise en compte du seuil optionnel et la gestion des
 * inputs invalides.</p>
 */
class ClauseNonConcurrenceBeValidatorTest {

    private static ClauseNonConcurrenceBeRequest req(BigDecimal remun, Integer durMois,
                                                     ClauseNonConcurrenceBeZoneEnum zone,
                                                     Boolean activiteInter,
                                                     BigDecimal seuil) {
        return new ClauseNonConcurrenceBeRequest(remun, durMois, zone, activiteInter, seuil);
    }

    @Test
    void validate_remunerationSousLeSeuilDefaut_isNulle_RemunerationInsuffisante() {
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("73000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.NULLE);
        assertThat(r.raisonNullite())
                .isEqualTo(ClauseNonConcurrenceBeRaisonNullite.REMUNERATION_INSUFFISANTE);
    }

    @Test
    void validate_remunerationEgaleAuSeuil_isNulle() {
        // Seuil 2024 = 73 571 €. Strictement supérieur exigé → 73571 = nulle.
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("73571.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.NULLE);
        assertThat(r.raisonNullite())
                .isEqualTo(ClauseNonConcurrenceBeRaisonNullite.REMUNERATION_INSUFFISANTE);
    }

    @Test
    void validate_nominalValide_indemniteEquivalente() {
        // rémun = 100 000 €/an, durée = 6 mois → indemnité = 100000 × 6 / 24 = 25 000 €
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.VALIDE);
        assertThat(r.raisonNullite()).isNull();
        assertThat(r.indemniteLegale()).isEqualByComparingTo("25000.00");
        assertThat(r.indemniteLegaleFormule()).contains("100000.00").contains("6").contains("25000.00");
        assertThat(r.baseJuridique()).contains("Loi 03/07/1978").contains("art. 65").contains("CCT n°13");
    }

    @Test
    void validate_dureeExcessive13_isNulle_DureeExcessive() {
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 13, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.NULLE);
        assertThat(r.raisonNullite()).isEqualTo(ClauseNonConcurrenceBeRaisonNullite.DUREE_EXCESSIVE);
    }

    @Test
    void validate_dureeLimite12_estValide() {
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 12, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.VALIDE);
        // indemnité = 100000 × 12 / 24 = 50000 €
        assertThat(r.indemniteLegale()).isEqualByComparingTo("50000.00");
    }

    @Test
    void validate_zoneInternationaleSansActivite_isPartiellementNulle() {
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_ET_ETRANGER,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.PARTIELLEMENT_NULLE);
        assertThat(r.raisonNullite())
                .isEqualTo(ClauseNonConcurrenceBeRaisonNullite.ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE);
    }

    @Test
    void validate_zoneInternationaleAvecActivite_estValide() {
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_ET_ETRANGER,
                        true, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.VALIDE);
        assertThat(r.raisonNullite()).isNull();
    }

    @Test
    void validate_zoneNonSpecifiee_isNulle() {
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 6, ClauseNonConcurrenceBeZoneEnum.NON_SPECIFIEE,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.NULLE);
        assertThat(r.raisonNullite())
                .isEqualTo(ClauseNonConcurrenceBeRaisonNullite.ZONE_GEOGRAPHIQUE_NON_SPECIFIEE);
    }

    @Test
    void validate_seuilCustomPlusEleve_basculeEnNulle() {
        // Avec seuil par défaut 73 571 : rémun 80 000 → VALIDE
        // Avec seuil custom 90 000 : rémun 80 000 → NULLE
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("80000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, new BigDecimal("90000.00")));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.NULLE);
        assertThat(r.raisonNullite())
                .isEqualTo(ClauseNonConcurrenceBeRaisonNullite.REMUNERATION_INSUFFISANTE);
        assertThat(r.salaireAnnuelSeuil()).isEqualByComparingTo("90000.00");
    }

    @Test
    void validate_indemniteArrondiHalfUp() {
        // 100000.50 × 7 / 24 = 700003.50 / 24 = 29166.8125 → HALF_UP → 29166.81
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.50"), 7, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.VALIDE);
        assertThat(r.indemniteLegale().scale()).isEqualTo(2);
        assertThat(r.indemniteLegale()).isEqualByComparingTo("29166.81");
    }

    @Test
    void validate_indemniteCalculeeMemeSiNulle() {
        // Verdict NULLE mais l'indemnité reste calculée (utile pour simuler le coût en litige).
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 13, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.NULLE);
        // 100000 × 13 / 24 = 54166.6666... → 54166.67
        assertThat(r.indemniteLegale()).isEqualByComparingTo("54166.67");
    }

    @Test
    void validate_avertissementSeuilToujoursPresent() {
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, null));

        assertThat(r.avertissement()).contains("73 571").contains("15 jours");
    }

    @Test
    void validate_remunerationNullOuNegative_throws() {
        assertThatThrownBy(() -> ClauseNonConcurrenceBeValidator.validate(
                req(null, 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT, false, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationAnnuelleBrute");

        assertThatThrownBy(() -> ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("-1"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT, false, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ClauseNonConcurrenceBeValidator.validate(
                req(BigDecimal.ZERO, 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT, false, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_dureeMoisInvalide_throws() {
        assertThatThrownBy(() -> ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 0,
                        ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT, false, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeMois");

        assertThatThrownBy(() -> ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), null,
                        ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT, false, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_zoneNull_throws() {
        assertThatThrownBy(() -> ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 6, null, false, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zoneGeographique");
    }

    @Test
    void validate_activiteInternationaleNull_traiteCommeFalse() {
        // Pour BELGIQUE_ET_ETRANGER avec activiteInternationaleProuvee=null → faux par défaut
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("100000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_ET_ETRANGER,
                        null, null));

        assertThat(r.activiteInternationaleProuvee()).isFalse();
        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.PARTIELLEMENT_NULLE);
    }

    @Test
    void validate_seuilCustomZero_utiliseDefaut() {
        // salaireAnnuelSeuil=0 → traité comme non fourni → seuil 2024 par défaut.
        ClauseNonConcurrenceBeResult r = ClauseNonConcurrenceBeValidator.validate(
                req(new BigDecimal("80000.00"), 6, ClauseNonConcurrenceBeZoneEnum.BELGIQUE_UNIQUEMENT,
                        false, BigDecimal.ZERO));

        assertThat(r.salaireAnnuelSeuil()).isEqualByComparingTo("73571.00");
        assertThat(r.verdict()).isEqualTo(ClauseNonConcurrenceBeVerdict.VALIDE);
    }
}
