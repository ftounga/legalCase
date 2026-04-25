package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-15-01 — UT du calculateur des récompenses (art. 1437/1469 Cciv).
 *
 * <p>Couvre :
 * <ul>
 *   <li>Al. 1 (règle générale — récompense = dépense faite) : ≥ 5 cas</li>
 *   <li>Al. 2 (dépenses usuelles — plus faible des 2) : ≥ 5 cas</li>
 *   <li>Al. 3 (acquisition / conservation / amélioration — plus forte des 2) : ≥ 5 cas</li>
 *   <li>Edge cases : régime invalide, 0 opération, opérations multiples, valeurs négatives</li>
 * </ul>
 */
class RecompensesCalculatorTest {

    private static RecompensesRequest.OperationRecompense op(String id,
                                                             String type,
                                                             String nature,
                                                             String depense,
                                                             String vInit,
                                                             String vAct,
                                                             String descr) {
        return new RecompensesRequest.OperationRecompense(
                id, type, nature,
                depense == null ? null : new BigDecimal(depense),
                vInit == null ? null : new BigDecimal(vInit),
                vAct == null ? null : new BigDecimal(vAct),
                descr);
    }

    // ========================================================================
    // Régime matrimonial — validation
    // ========================================================================

    @Test
    void compute_regimeNull_throws() {
        assertThatThrownBy(() -> RecompensesCalculator.compute(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regimeMatrimonial");
    }

    @Test
    void compute_regimeBlank_throws() {
        assertThatThrownBy(() -> RecompensesCalculator.compute("   ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regimeMatrimonial");
    }

    @Test
    void compute_regimeSeparationBiens_throws() {
        assertThatThrownBy(() -> RecompensesCalculator.compute("SEPARATION_BIENS", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("séparation de biens");
    }

    @Test
    void compute_regimeInconnu_throws() {
        assertThatThrownBy(() -> RecompensesCalculator.compute("FOOBAR", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inconnu");
    }

    @Test
    void compute_regimeCommunauteLegale_ok() {
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of());
        assertThat(r.regimeMatrimonial()).isEqualTo("COMMUNAUTE_LEGALE");
        assertThat(r.recompenses()).isEmpty();
        assertThat(r.totalRecompensesDuesParCommunauteEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_regimeParticipationAcquets_ok() {
        RecompensesResult r = RecompensesCalculator.compute("PARTICIPATION_AUX_ACQUETS", List.of());
        assertThat(r.regimeMatrimonial()).isEqualTo("PARTICIPATION_AUX_ACQUETS");
    }

    @Test
    void compute_regimeCommunauteUniverselle_ok() {
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_UNIVERSELLE", List.of());
        assertThat(r.regimeMatrimonial()).isEqualTo("COMMUNAUTE_UNIVERSELLE");
    }

    // ========================================================================
    // Aucune opération
    // ========================================================================

    @Test
    void compute_operationsNull_returnsEmpty() {
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", null);
        assertThat(r.recompenses()).isEmpty();
        assertThat(r.totalRecompensesDuesParCommunauteEur()).isEqualByComparingTo("0.00");
        assertThat(r.totalRecompensesDuesParEpouxEur()).isEqualByComparingTo("0.00");
        assertThat(r.soldeNetPourEpouxEur()).isEqualByComparingTo("0.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("Aucune opération"));
    }

    @Test
    void compute_operationsVides_returnsEmpty() {
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of());
        assertThat(r.recompenses()).isEmpty();
        assertThat(r.soldeNetPourEpouxEur()).isEqualByComparingTo("0.00");
    }

    // ========================================================================
    // Validation d'opérations
    // ========================================================================

    @Test
    void compute_idVide_throws() {
        var bad = op("", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id requis");
    }

    @Test
    void compute_idDuplique_throws() {
        var a = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "100", null, null, null);
        var b = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "200", null, null, null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(a, b)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dupliqué");
    }

    @Test
    void compute_typeInvalide_throws() {
        var bad = op("op-1", "INVALID_TYPE", "AUTRE",
                "100", null, null, null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type invalide");
    }

    @Test
    void compute_natureBienInvalide_throws() {
        var bad = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "INVALID_NATURE",
                "100", null, null, null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("natureBien invalide");
    }

    @Test
    void compute_montantDepenseNull_throws() {
        var bad = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                null, null, null, null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantDepenseEur requis");
    }

    @Test
    void compute_montantDepenseNegatif_throws() {
        var bad = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "-100", null, null, null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ne peut être négatif");
    }

    @Test
    void compute_valeurInitialeNegative_throws() {
        var bad = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AMELIORATION_BIEN_PROPRE",
                "100", "-1", "200", null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeurInitialeBienEur");
    }

    @Test
    void compute_valeurActuelleNegative_throws() {
        var bad = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AMELIORATION_BIEN_PROPRE",
                "100", "200", "-1", null);
        assertThatThrownBy(() -> RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(bad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valeurActuelleBienEur");
    }

    // ========================================================================
    // Al. 1 — Règle générale (AUTRE)
    // ========================================================================

    @Test
    void al1_autre_recompenseEgaleADepense() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "8000", null, null, "Vacances famille");
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses()).hasSize(1);
        var d = r.recompenses().get(0);
        assertThat(d.regleApplicable()).isEqualTo(RecompensesCalculator.REGLE_DEPENSE_FAITE);
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("8000.00");
        assertThat(d.depenseFaiteEur()).isEqualByComparingTo("8000.00");
        assertThat(d.baseJuridiqueOperation()).contains("al. 1");
        assertThat(d.directionRecompense()).isEqualTo(RecompensesCalculator.DIRECTION_COMMUNAUTE_DOIT_PROPRE);
    }

    @Test
    void al1_autre_dépensePropreAuProfitCommunaute_directionCorrecte() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "5000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.directionRecompense()).isEqualTo("COMMUNAUTE_DOIT_PROPRE");
        assertThat(r.totalRecompensesDuesParCommunauteEur()).isEqualByComparingTo("5000.00");
        assertThat(r.totalRecompensesDuesParEpouxEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void al1_autre_dépenseCommunauteAuProfitPropre_directionCorrecte() {
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AUTRE",
                "5000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.directionRecompense()).isEqualTo("PROPRE_DOIT_COMMUNAUTE");
        assertThat(r.totalRecompensesDuesParCommunauteEur()).isEqualByComparingTo("0.00");
        assertThat(r.totalRecompensesDuesParEpouxEur()).isEqualByComparingTo("5000.00");
    }

    @Test
    void al1_autre_montantZero_recompenseZero() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "0", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).montantRecompenseEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void al1_autre_baseJuridiqueRegleGenerale() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).baseJuridiqueOperation())
                .contains("1469").contains("al. 1").contains("générale");
    }

    @Test
    void al1_autre_formuleLisible() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1234", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).formule()).contains("1234").contains("al. 1");
    }

    // ========================================================================
    // Al. 2 — Dépenses usuelles (plus faible des 2 sommes)
    // ========================================================================

    @Test
    void al2_depenseUsuelle_sansValeurs_recompenseEgaleADepense() {
        // Profit subsistant fallback = dépense → min(dépense, dépense) = dépense
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "DEPENSES_USUELLES",
                "5000", null, null, "Dette communauté");
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.regleApplicable()).isEqualTo(RecompensesCalculator.REGLE_PLUS_FAIBLE);
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("5000.00");
        assertThat(d.baseJuridiqueOperation()).contains("al. 2");
    }

    @Test
    void al2_depenseUsuelle_profitInferieur_recompenseEgaleAuProfit() {
        // dépense 5000, vInit 100000, vAct 60000 → ratio = 60000 × 5000 / 100000 = 3000 → min(5000;3000) = 3000
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "DEPENSES_USUELLES",
                "5000", "100000", "60000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("3000.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("3000.00");
    }

    @Test
    void al2_depenseUsuelle_profitSupérieur_recompensePlafonneeADepense() {
        // dépense 5000, vInit 100000, vAct 200000 → ratio = 10000 → min(5000;10000) = 5000
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "DEPENSES_USUELLES",
                "5000", "100000", "200000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("10000.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("5000.00");
    }

    @Test
    void al2_depenseUsuelle_baseJuridiqueAlinea2() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "DEPENSES_USUELLES",
                "1000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).baseJuridiqueOperation())
                .contains("1469").contains("al. 2").contains("usuelle");
    }

    @Test
    void al2_depenseUsuelle_messageMinimumPlafonne() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "DEPENSES_USUELLES",
                "1000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.messages()).anyMatch(m -> m.contains("usuelle") && m.contains("plus faible"));
    }

    @Test
    void al2_depenseUsuelle_directionPropreVersCommunaute() {
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "DEPENSES_USUELLES",
                "5000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).directionRecompense())
                .isEqualTo("PROPRE_DOIT_COMMUNAUTE");
        assertThat(r.totalRecompensesDuesParEpouxEur()).isEqualByComparingTo("5000.00");
    }

    // ========================================================================
    // Al. 3 — Acquisition / Conservation / Amélioration (plus forte des 2)
    // ========================================================================

    @Test
    void al3_amelioration_profitSuperieurADepense_recompenseEgaleAuProfit() {
        // Exemple 1 mini-spec : dépense 50000, vInit 200000, vAct 350000
        // Plus-value = 150000, ratio = 350000 × 50000 / 200000 = 87500
        // Profit retenu = min(150000, 87500) = 87500 → max(50000, 87500) = 87500
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "50000", "200000", "350000", "Travaux agrandissement");
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.regleApplicable()).isEqualTo(RecompensesCalculator.REGLE_PROFIT_SUBSISTANT_PLUS_FORTE);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("87500.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("87500.00");
        assertThat(d.directionRecompense()).isEqualTo("PROPRE_DOIT_COMMUNAUTE");
        assertThat(d.baseJuridiqueOperation()).contains("al. 3");
    }

    @Test
    void al3_amelioration_profitInferieurADepense_recompenseEgaleADepense() {
        // dépense 80000, vInit 200000, vAct 220000
        // plus-value = 20000, ratio = 220000 × 80000 / 200000 = 88000
        // profit retenu = min(20000, 88000) = 20000 → max(80000, 20000) = 80000
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "80000", "200000", "220000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("20000.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("80000.00");
    }

    @Test
    void al3_amelioration_moinsValue_profitZeroEtRecompenseEgaleADepense() {
        // dépense 30000, vInit 200000, vAct 150000
        // plus-value = -50000 → 0 ; profit final = 0 ; max(30000, 0) = 30000
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "30000", "200000", "150000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("0.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("30000.00");
    }

    @Test
    void al3_acquisition_profitProportionnel() {
        // Exemple 2 mini-spec : dépense 60000, vInit 300000 (prix achat), vAct 420000
        // ratio = 420000 × 60000 / 300000 = 84000 → max(60000, 84000) = 84000
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "ACQUISITION_BIEN_PROPRE",
                "60000", "300000", "420000", "Apport partiel sur appartement");
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("84000.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("84000.00");
        assertThat(r.totalRecompensesDuesParEpouxEur()).isEqualByComparingTo("84000.00");
    }

    @Test
    void al3_acquisition_profitInferieurADepense_recompenseEgaleADepense() {
        // dépense 100000, vInit 200000, vAct 100000 (perte de valeur)
        // ratio = 100000 × 100000 / 200000 = 50000 → max(100000, 50000) = 100000
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "ACQUISITION_BIEN_PROPRE",
                "100000", "200000", "100000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("50000.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("100000.00");
    }

    @Test
    void al3_conservation_profitProportionnel() {
        // Exemple 4 mini-spec : dépense 40000, vInit 250000, vAct 300000
        // ratio = 300000 × 40000 / 250000 = 48000, borné à vAct → 48000 → max(40000, 48000) = 48000
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "CONSERVATION_BIEN_PROPRE",
                "40000", "250000", "300000", "Sauvetage saisie");
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("48000.00");
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("48000.00");
    }

    @Test
    void al3_conservation_profitBorneAValeurActuelle() {
        // dépense 500000 sur valeur initiale 100000 vAct 200000 → ratio brut = 1000000, borné à vAct=200000
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "CONSERVATION_BIEN_PROPRE",
                "500000", "100000", "200000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.profitSubsistantEur()).isEqualByComparingTo("200000.00");
        // max(500000, 200000) = 500000
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("500000.00");
    }

    @Test
    void al3_amelioration_sansValeurs_fallbackADepense() {
        // valeurInitiale ou valeurActuelle null → fallback = dépense → max(dépense, dépense) = dépense
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "10000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        var d = r.recompenses().get(0);
        assertThat(d.montantRecompenseEur()).isEqualByComparingTo("10000.00");
    }

    @Test
    void al3_baseJuridiqueAlinea3() {
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "ACQUISITION_BIEN_PROPRE",
                "1000", "10000", "20000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).baseJuridiqueOperation())
                .contains("1469").contains("al. 3");
    }

    @Test
    void al3_messageProfitSubsistantUtilise() {
        var op = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "50000", "200000", "350000", null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.messages()).anyMatch(m -> m.contains("Profit subsistant"));
    }

    // ========================================================================
    // Opérations multiples — agrégation
    // ========================================================================

    @Test
    void compute_operationsMultiples_totauxAgreges() {
        // op1 : amélioration M (DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE) → 87500 dus par M à communauté
        var op1 = op("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "50000", "200000", "350000", null);
        // op2 : autre M (DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE) → 8000 dus par communauté à M
        var op2 = op("op-2", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "8000", null, null, null);
        // op3 : acquisition Mme (DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE) → 84000 dus par Mme à communauté
        var op3 = op("op-3", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "ACQUISITION_BIEN_PROPRE",
                "60000", "300000", "420000", null);

        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op1, op2, op3));

        assertThat(r.recompenses()).hasSize(3);
        assertThat(r.totalRecompensesDuesParCommunauteEur()).isEqualByComparingTo("8000.00");
        assertThat(r.totalRecompensesDuesParEpouxEur()).isEqualByComparingTo("171500.00"); // 87500 + 84000
        assertThat(r.soldeNetPourEpouxEur()).isEqualByComparingTo("-163500.00"); // 8000 - 171500
    }

    @Test
    void compute_soldeNetPositif_quandCommunauteDoit() {
        var op1 = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "10000", null, null, null);
        var op2 = op("op-2", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AUTRE",
                "3000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op1, op2));
        assertThat(r.soldeNetPourEpouxEur()).isEqualByComparingTo("7000.00");
    }

    @Test
    void compute_soldeNetNul_quandEgalite() {
        var op1 = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "5000", null, null, null);
        var op2 = op("op-2", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AUTRE",
                "5000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op1, op2));
        assertThat(r.soldeNetPourEpouxEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_baseJuridiqueGenerale_contient1437et1469() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "100", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.baseJuridiqueGenerale()).contains("1437").contains("1469").contains("Cciv");
    }

    @Test
    void compute_descriptionConservee() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, "Description test");
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).description()).isEqualTo("Description test");
    }

    @Test
    void compute_operationIdRelaye() {
        var op = op("custom-id-42", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "100", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).operationId()).isEqualTo("custom-id-42");
    }

    @Test
    void compute_messageIndicatif_present() {
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.messages()).anyMatch(m -> m.contains("indicatif"));
    }

    @Test
    void compute_nombreUtArrondi2Decimales() {
        // dépense 33333.333 → arrondi 33333.33
        var op = op("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "33333.333", null, null, null);
        RecompensesResult r = RecompensesCalculator.compute("COMMUNAUTE_LEGALE", List.of(op));
        assertThat(r.recompenses().get(0).montantRecompenseEur())
                .isEqualByComparingTo("33333.33");
    }
}
