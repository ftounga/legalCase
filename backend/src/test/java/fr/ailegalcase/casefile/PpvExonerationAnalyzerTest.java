package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-39 : tests unitaires de {@link PpvExonerationAnalyzer} (F-DT-52, outil
 * FRANCE uniquement).
 *
 * <p>Logique déterministe (loi n° 2022-1158 du 16/08/2022 art. 1 + loi
 * n° 2023-1107 du 29/11/2023) :
 * <ul>
 *   <li>plafond social 3 000 € de droit commun ; 6 000 € si accord
 *       d'intéressement OU effectif &lt; 50 ;</li>
 *   <li>montant ≤ plafond → CONFORME, montantImposable = 0 ;</li>
 *   <li>montant &gt; plafond → PLAFOND_DEPASSE, montantImposable = excédent ;</li>
 *   <li>exonération IR si effectif &lt; 50 ET rémunération &lt; 3 SMIC ;</li>
 *   <li>champ requis null / montant ≤ 0 / rémunération ≤ 0 → IllegalArgument.</li>
 * </ul>
 */
class PpvExonerationAnalyzerTest {

    private static final BigDecimal REMUN_BASSE = new BigDecimal("30000"); // < 3 SMIC
    private static final BigDecimal REMUN_HAUTE = new BigDecimal("70000"); // ≥ 3 SMIC

    @Test
    void sousPlafond3000_sansAccord_sansEffectif_conforme() {
        PpvExonerationResult r = PpvExonerationAnalyzer.analyze(
                new BigDecimal("2500"), false, REMUN_HAUTE, false, false);

        assertThat(r.statut()).isEqualTo(PpvExonerationStatut.CONFORME);
        assertThat(r.plafondSocialApplique()).isEqualByComparingTo("3000");
        assertThat(r.montantExonere()).isEqualByComparingTo("2500");
        assertThat(r.montantImposable()).isEqualByComparingTo("0");
        assertThat(r.baseJuridique()).contains("2022-1158").contains("2023-1107");
    }

    @Test
    void plafond6000_viaAccordInteressement_conforme() {
        PpvExonerationResult r = PpvExonerationAnalyzer.analyze(
                new BigDecimal("4500"), true, REMUN_HAUTE, false, false);

        assertThat(r.statut()).isEqualTo(PpvExonerationStatut.CONFORME);
        assertThat(r.plafondSocialApplique()).isEqualByComparingTo("6000");
        assertThat(r.montantExonere()).isEqualByComparingTo("4500");
        assertThat(r.montantImposable()).isEqualByComparingTo("0");
    }

    @Test
    void plafond6000_viaEffectifMoins50_conforme() {
        PpvExonerationResult r = PpvExonerationAnalyzer.analyze(
                new BigDecimal("5500"), false, REMUN_HAUTE, true, false);

        assertThat(r.plafondSocialApplique()).isEqualByComparingTo("6000");
        assertThat(r.statut()).isEqualTo(PpvExonerationStatut.CONFORME);
    }

    @Test
    void depassementPlafond3000_plafondDepasse_montantImposable() {
        PpvExonerationResult r = PpvExonerationAnalyzer.analyze(
                new BigDecimal("4000"), false, REMUN_HAUTE, false, false);

        assertThat(r.statut()).isEqualTo(PpvExonerationStatut.PLAFOND_DEPASSE);
        assertThat(r.plafondSocialApplique()).isEqualByComparingTo("3000");
        assertThat(r.montantExonere()).isEqualByComparingTo("3000");
        assertThat(r.montantImposable()).isEqualByComparingTo("1000");
    }

    @Test
    void exonerationFiscaleIr_effectifMoins50_remunerationSous3Smic() {
        PpvExonerationResult r = PpvExonerationAnalyzer.analyze(
                new BigDecimal("2000"), false, REMUN_BASSE, true, false);

        assertThat(r.exonerationFiscaleIr()).isTrue();
        assertThat(r.notes()).anyMatch(n -> n.contains("impôt sur le revenu"));
    }

    @Test
    void pasExonerationFiscaleIr_effectifNon_ouRemunerationHaute() {
        // effectif >= 50 → pas d'exo IR même si rémunération basse
        PpvExonerationResult r1 = PpvExonerationAnalyzer.analyze(
                new BigDecimal("2000"), false, REMUN_BASSE, false, false);
        assertThat(r1.exonerationFiscaleIr()).isFalse();

        // effectif < 50 mais rémunération >= 3 SMIC → pas d'exo IR
        PpvExonerationResult r2 = PpvExonerationAnalyzer.analyze(
                new BigDecimal("2000"), false, REMUN_HAUTE, true, false);
        assertThat(r2.exonerationFiscaleIr()).isFalse();
    }

    @Test
    void planEpargne_ajouteNoteExonerationIr() {
        PpvExonerationResult r = PpvExonerationAnalyzer.analyze(
                new BigDecimal("2000"), false, REMUN_HAUTE, false, true);

        assertThat(r.versementPlanEpargne()).isTrue();
        assertThat(r.notes()).anyMatch(n -> n.contains("plan d'épargne"));
    }

    @Test
    void champRequisNull_montantNul_remunerationNulle_throwIllegalArgument() {
        assertThatThrownBy(() -> PpvExonerationAnalyzer.analyze(
                null, false, REMUN_HAUTE, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PpvExonerationAnalyzer.analyze(
                BigDecimal.ZERO, false, REMUN_HAUTE, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PpvExonerationAnalyzer.analyze(
                new BigDecimal("2000"), null, REMUN_HAUTE, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PpvExonerationAnalyzer.analyze(
                new BigDecimal("2000"), false, BigDecimal.ZERO, false, false))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PpvExonerationAnalyzer.analyze(
                new BigDecimal("2000"), false, REMUN_HAUTE, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
