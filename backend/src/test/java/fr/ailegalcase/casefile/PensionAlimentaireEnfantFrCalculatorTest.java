package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-03 : tests unitaires du calculateur pension alimentaire enfant FR.
 * Couvre les règles art. 371-2 Cciv + barème indicatif Cass. (taux 18/26/30/33/35%).
 */
class PensionAlimentaireEnfantFrCalculatorTest {

    private static PensionAlimentaireEnfantFrRequest req(
            Integer revenusParent1, Integer revenusParent2,
            int nbEnfants, List<Integer> agesEnfants,
            ModeResidenceEnfantEnum mode) {
        return new PensionAlimentaireEnfantFrRequest(
                revenusParent1, revenusParent2, nbEnfants, agesEnfants, mode);
    }

    // ── AC1 : 1 enfant, débiteur 3000 €/mois → montant indicatif 540 € (18%) ──

    @Test
    void ac1_1_enfant_revenus_3000_residence_principale_parent1_montant_540() {
        // Parent 1 héberge → parent 2 (3000 €) est débiteur, taux 18 %.
        PensionAlimentaireEnfantFrRequest r = req(0, 3000, 1, List.of(10),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        // 3000 × 0,18 = 540 € (dans la fourchette 420-540 attendue par la mini-spec).
        assertThat(res.totalMensuelEur()).isEqualTo(540);
        assertThat(res.parentDebiteur()).isEqualTo("PARENT2");
        assertThat(res.tauxApplique()).isEqualTo(0.18);
        assertThat(res.coefficientResidence()).isEqualTo(1.0);
    }

    // ── AC2 : résidence alternée → montant réduit (~ moitié) ─────────────────

    @Test
    void ac2_residence_alternee_taux_reduit() {
        // En alternée, taux 11 % et différentiel entre les revenus.
        // Parent 2 = 3000 €, Parent 1 = 2000 € → débiteur parent 2, base 1000 € × 11 % = 110 €.
        PensionAlimentaireEnfantFrRequest r = req(2000, 3000, 1, List.of(10),
                ModeResidenceEnfantEnum.ALTERNEE);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        assertThat(res.totalMensuelEur()).isEqualTo(110);
        assertThat(res.coefficientResidence()).isEqualTo(0.5);
        assertThat(res.tauxApplique()).isEqualTo(0.11);
        assertThat(res.parentDebiteur()).isEqualTo("PARENT2");
    }

    // ── AC3 : pays BELGIQUE → IllegalArgumentException ──────────────────────

    @Test
    void ac3_pays_belgique_leve_exception() {
        PensionAlimentaireEnfantFrRequest r = req(0, 3000, 1, List.of(10),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        assertThatThrownBy(() -> PensionAlimentaireEnfantFrCalculator.compute(r, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("France");
    }

    // ── 3 enfants → taux barème 30 % ─────────────────────────────────────────

    @Test
    void trois_enfants_taux_30() {
        PensionAlimentaireEnfantFrRequest r = req(0, 4000, 3, List.of(5, 10, 15),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        // 4000 × 0,30 = 1200 € au total, soit 400 € par enfant.
        assertThat(res.totalMensuelEur()).isEqualTo(1200);
        assertThat(res.montantParEnfantMensuelEur()).hasSize(3);
        assertThat(res.montantParEnfantMensuelEur()).containsExactly(400, 400, 400);
        assertThat(res.tauxApplique()).isEqualTo(0.30);
    }

    // ── Revenus débiteur = 0 → montant 0 + alerte ───────────────────────────

    @Test
    void revenus_debiteur_nul_montant_zero_avec_alerte() {
        // Parent 1 héberge → parent 2 débiteur ; parent 2 = 0 € → montant 0 + alerte.
        PensionAlimentaireEnfantFrRequest r = req(2500, 0, 1, List.of(8),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        assertThat(res.totalMensuelEur()).isEqualTo(0);
        assertThat(res.alertes()).anyMatch(a -> a.contains("Revenus du parent débiteur = 0"));
    }

    // ── Garde alternée revenus égaux → débiteur AUCUN ───────────────────────

    @Test
    void garde_alternee_revenus_egaux_pas_de_pension() {
        PensionAlimentaireEnfantFrRequest r = req(2500, 2500, 1, List.of(8),
                ModeResidenceEnfantEnum.ALTERNEE);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        assertThat(res.totalMensuelEur()).isEqualTo(0);
        assertThat(res.parentDebiteur()).isEqualTo("AUCUN");
        assertThat(res.messages()).anyMatch(m -> m.contains("revenus comparables"));
    }

    // ── PRINCIPALE_PARENT2 → débiteur PARENT1 ───────────────────────────────

    @Test
    void principale_parent2_debiteur_parent1() {
        // Parent 2 héberge → parent 1 (2000 €) est débiteur, taux 18 %.
        PensionAlimentaireEnfantFrRequest r = req(2000, 0, 1, List.of(10),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT2);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        assertThat(res.totalMensuelEur()).isEqualTo(360);
        assertThat(res.parentDebiteur()).isEqualTo("PARENT1");
    }

    // ── 5+ enfants → taux plafonne à 35% + message d'avertissement ──────────

    @Test
    void cinq_enfants_taux_plafonne_35() {
        PensionAlimentaireEnfantFrRequest r = req(0, 5000, 5,
                List.of(3, 6, 9, 12, 15),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        // 5000 × 0,35 = 1750 € au total.
        assertThat(res.totalMensuelEur()).isEqualTo(1750);
        assertThat(res.tauxApplique()).isEqualTo(0.35);
        assertThat(res.messages()).anyMatch(m -> m.contains("plafonne"));
    }

    // ── Nombre d'enfants invalide → IllegalArgumentException ─────────────────

    @Test
    void nombre_enfants_zero_leve_exception() {
        PensionAlimentaireEnfantFrRequest r = req(0, 3000, 0, List.of(),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        assertThatThrownBy(() -> PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ages_enfants_inconsistents_leve_exception() {
        PensionAlimentaireEnfantFrRequest r = req(0, 3000, 2, List.of(10),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        assertThatThrownBy(() -> PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mode_residence_null_leve_exception() {
        PensionAlimentaireEnfantFrRequest r = req(0, 3000, 1, List.of(10), null);
        assertThatThrownBy(() -> PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revenus_negatifs_leves_exception() {
        PensionAlimentaireEnfantFrRequest r = req(-100, 3000, 1, List.of(10),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        assertThatThrownBy(() -> PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Enfant majeur → message obligation poursuite études ─────────────────

    @Test
    void enfant_majeur_declenche_message_etudes() {
        PensionAlimentaireEnfantFrRequest r = req(0, 3000, 1, List.of(20),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        assertThat(res.messages()).anyMatch(m -> m.contains("majeurs"));
    }

    // ── Base juridique référence art. 371-2 ─────────────────────────────────

    @Test
    void baseJuridique_contient_art371_2() {
        PensionAlimentaireEnfantFrRequest r = req(0, 3000, 1, List.of(10),
                ModeResidenceEnfantEnum.PRINCIPALE_PARENT1);
        PensionAlimentaireEnfantFrResult res = PensionAlimentaireEnfantFrCalculator.compute(r, "FRANCE");
        assertThat(res.baseJuridique()).contains("371-2");
        assertThat(res.baseJuridique()).contains("cassation");
    }
}
