package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-05 : tests unitaires du calculateur liquidation de communauté légale FR.
 * Couvre les règles art. 1467-1517 Cciv + art. 1433-1435 Cciv + art. 815-832 Cciv.
 */
class LiquidationCommunauteFrCalculatorTest {

    private static LiquidationCommunauteFrRequest req(
            Integer immeuble1, Integer immeuble2, Integer credit, Integer mobilier, Integer autres,
            Integer recompenses1, Integer recompenses2,
            Integer biensPropres1, Integer biensPropres2,
            OccupationLogementEnum occupation, String regime) {
        return new LiquidationCommunauteFrRequest(immeuble1, immeuble2, credit, mobilier, autres,
                recompenses1, recompenses2, biensPropres1, biensPropres2, occupation, regime);
    }

    // ── AC1 : immeuble 300 000 € + crédit 0 € → quotes-parts = 150 000 € chacun ──

    @Test
    void ac1_immeuble_300k_credit_zero_partage_egal() {
        LiquidationCommunauteFrRequest r = req(300_000, 0, 0, 0, 0,
                0, 0, 0, 0, null, null);
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.masseCommuneEur()).isEqualTo(300_000);
        assertThat(res.quotaPartEpoux1Eur()).isEqualTo(150_000);
        assertThat(res.quotaPartEpoux2Eur()).isEqualTo(150_000);
        assertThat(res.alerteIndivision()).isFalse();
    }

    // ── Immeuble 400 000 € + crédit 100 000 € → masse 300 000 € ──────────────

    @Test
    void immeuble_400k_credit_100k_masse_300k() {
        LiquidationCommunauteFrRequest r = req(400_000, 0, 100_000, 0, 0,
                0, 0, 0, 0, null, "COMMUNAUTE_LEGALE");
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.masseCommuneEur()).isEqualTo(300_000);
        assertThat(res.quotaPartEpoux1Eur()).isEqualTo(150_000);
        assertThat(res.quotaPartEpoux2Eur()).isEqualTo(150_000);
    }

    // ── Récompenses 20k de l'époux 1 → offset des quote-parts ───────────────

    @Test
    void recompenses_epoux1_offset_quote_parts() {
        LiquidationCommunauteFrRequest r = req(300_000, 0, 0, 0, 0,
                20_000, 0, 0, 0, null, null);
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        // Quote-part théorique = 150 000 €. Récompenses1 = 20 000 € due par l'époux 1.
        // Quote-part 1 = 150 000 - 20 000 = 130 000 € ; Quote-part 2 = 150 000 + 20 000 = 170 000 €.
        assertThat(res.quotaPartEpoux1Eur()).isEqualTo(130_000);
        assertThat(res.quotaPartEpoux2Eur()).isEqualTo(170_000);
        assertThat(res.messages())
                .anyMatch(m -> m.contains("Récompenses appliquées"));
    }

    // ── AC2 : régime ≠ COMMUNAUTE_LEGALE → IllegalArgumentException ─────────

    @Test
    void ac2_regime_separation_biens_leve_exception() {
        LiquidationCommunauteFrRequest r = req(300_000, 0, 0, 0, 0,
                0, 0, 0, 0, null, "SEPARATION_BIENS");
        assertThatThrownBy(() -> LiquidationCommunauteFrCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("communauté légale");
    }

    // ── AC3 : pays BELGIQUE → IllegalArgumentException ──────────────────────

    @Test
    void ac3_pays_belgique_leve_exception() {
        LiquidationCommunauteFrRequest r = req(300_000, 0, 0, 0, 0,
                0, 0, 0, 0, null, null);
        assertThatThrownBy(() -> LiquidationCommunauteFrCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Occupation EPOUX1 → soulte indicative due à EPOUX2 ───────────────────

    @Test
    void occupation_logement_epoux1_genere_soulte() {
        LiquidationCommunauteFrRequest r = req(200_000, 0, 0, 0, 0,
                0, 0, 0, 0, OccupationLogementEnum.EPOUX1, null);
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        // Valeur nette logement = 200 000 €. Soulte indicative = 100 000 €.
        assertThat(res.soulteEur()).isEqualTo(100_000);
        assertThat(res.messages()).anyMatch(m -> m.contains("L'époux 1 conserve le logement"));
    }

    // ── Occupation AUCUN → alerte indivision ─────────────────────────────────

    @Test
    void occupation_aucun_declenche_alerte_indivision() {
        LiquidationCommunauteFrRequest r = req(200_000, 0, 0, 0, 0,
                0, 0, 0, 0, OccupationLogementEnum.AUCUN, null);
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.alerteIndivision()).isTrue();
        assertThat(res.alertes()).anyMatch(a -> a.contains("indivision"));
    }

    // ── Passif > actif → masse 0 + alerte déficit ────────────────────────────

    @Test
    void passif_superieur_actif_masse_zero_avec_alerte() {
        LiquidationCommunauteFrRequest r = req(100_000, 0, 200_000, 0, 0,
                0, 0, 0, 0, null, null);
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.masseCommuneEur()).isEqualTo(0);
        assertThat(res.alertes()).anyMatch(a -> a.contains("déficit"));
    }

    // ── Tous champs null → masse 0 + message "aucun actif" ──────────────────

    @Test
    void champs_tous_null_message_aucun_actif() {
        LiquidationCommunauteFrRequest r = req(null, null, null, null, null,
                null, null, null, null, null, null);
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.masseCommuneEur()).isEqualTo(0);
        assertThat(res.quotaPartEpoux1Eur()).isEqualTo(0);
        assertThat(res.quotaPartEpoux2Eur()).isEqualTo(0);
        assertThat(res.messages()).anyMatch(m -> m.contains("Aucun actif commun"));
    }

    // ── Base juridique contient art. 1467 ───────────────────────────────────

    @Test
    void baseJuridique_contient_art1467() {
        LiquidationCommunauteFrRequest r = req(300_000, 0, 0, 0, 0,
                0, 0, 0, 0, null, null);
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.baseJuridique()).contains("1467");
        assertThat(res.baseJuridique()).contains("1433");
    }

    // ── Mobilier + autres actifs cumulés ────────────────────────────────────

    @Test
    void cumul_mobilier_et_autres_actifs() {
        LiquidationCommunauteFrRequest r = req(200_000, 0, 0, 30_000, 20_000,
                0, 0, 0, 0, null, "COMMUNAUTE_LEGALE");
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.masseCommuneEur()).isEqualTo(250_000);
        assertThat(res.quotaPartEpoux1Eur()).isEqualTo(125_000);
        assertThat(res.quotaPartEpoux2Eur()).isEqualTo(125_000);
    }

    // ── Régime COMMUNAUTE_LEGALE explicite accepté ──────────────────────────

    @Test
    void regime_communaute_legale_accepte() {
        LiquidationCommunauteFrRequest r = req(300_000, 0, 0, 0, 0,
                0, 0, 0, 0, null, "COMMUNAUTE_LEGALE");
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.masseCommuneEur()).isEqualTo(300_000);
    }

    // ── Récompenses 2 supérieures à la quote-part → bornage 0 + alerte ─────

    @Test
    void recompenses_excessives_bornage_a_zero_avec_alerte() {
        LiquidationCommunauteFrRequest r = req(100_000, 0, 0, 0, 0,
                0, 200_000, 0, 0, null, null);
        // Quote-part théorique = 50 000 €. Récompenses2 = 200 000 € due par l'époux 2.
        // Quote-part 2 = 50 000 - 200 000 = -150 000 → borné à 0 + alerte.
        LiquidationCommunauteFrResult res = LiquidationCommunauteFrCalculator.compute(r, "FRANCE");
        assertThat(res.quotaPartEpoux2Eur()).isEqualTo(0);
        assertThat(res.alertes()).anyMatch(a -> a.contains("excèdent sa quote-part"));
    }
}
