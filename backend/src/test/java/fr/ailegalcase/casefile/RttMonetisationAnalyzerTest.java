package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-37 : tests unitaires de {@link RttMonetisationAnalyzer} (F-DT-51, outil
 * FRANCE uniquement).
 *
 * <p>Logique déterministe (loi n° 2022-1157 du 16/08/2022 art. 5) :
 * <ul>
 *   <li>jours acquis dans la fenêtre → ELIGIBLE + montant brut majoré ;</li>
 *   <li>taux par défaut 25 ; taux &lt; 10 relevé à 10 ; taux &gt; 25 plafonné à
 *       25 ;</li>
 *   <li>hors fenêtre → NON_ELIGIBLE sans montant ;</li>
 *   <li>régime aligné sur les heures supplémentaires ;</li>
 *   <li>champ requis null / jours ≤ 0 / salaire ≤ 0 → IllegalArgument.</li>
 * </ul>
 */
class RttMonetisationAnalyzerTest {

    @Test
    void joursDansFenetre_tauxDefaut_eligible_montantExact() {
        RttMonetisationResult r = RttMonetisationAnalyzer.analyze(
                5, new BigDecimal("200"), null, true);

        assertThat(r.statut()).isEqualTo(RttMonetisationStatut.ELIGIBLE);
        assertThat(r.tauxApplique()).isEqualTo(25d);
        // 5 × 200 × 1.25 = 1250.00
        assertThat(r.montantBrut()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(r.regimeSocialFiscal()).isEqualTo("ALIGNE_HEURES_SUPPLEMENTAIRES");
        assertThat(r.baseJuridique()).contains("2022-1157").contains("31/12/2026");
    }

    @Test
    void tauxBas_releveAuMinimum10() {
        RttMonetisationResult r = RttMonetisationAnalyzer.analyze(
                5, new BigDecimal("200"), 5d, true);

        assertThat(r.statut()).isEqualTo(RttMonetisationStatut.ELIGIBLE);
        assertThat(r.tauxApplique()).isEqualTo(10d);
        // 5 × 200 × 1.10 = 1100.00
        assertThat(r.montantBrut()).isEqualByComparingTo(new BigDecimal("1100.00"));
        assertThat(r.notes()).anyMatch(n -> n.contains("minimum de 10"));
    }

    @Test
    void tauxConventionnel10_applique10() {
        RttMonetisationResult r = RttMonetisationAnalyzer.analyze(
                2, new BigDecimal("150"), 10d, true);

        assertThat(r.tauxApplique()).isEqualTo(10d);
        // 2 × 150 × 1.10 = 330.00
        assertThat(r.montantBrut()).isEqualByComparingTo(new BigDecimal("330.00"));
    }

    @Test
    void tauxHaut_plafonneA25_avecNote() {
        RttMonetisationResult r = RttMonetisationAnalyzer.analyze(
                4, new BigDecimal("100"), 40d, true);

        assertThat(r.statut()).isEqualTo(RttMonetisationStatut.ELIGIBLE);
        assertThat(r.tauxApplique()).isEqualTo(25d);
        // 4 × 100 × 1.25 = 500.00
        assertThat(r.montantBrut()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(r.notes()).anyMatch(n -> n.contains("plafonnée"));
    }

    @Test
    void horsFenetre_nonEligible_sansMontant() {
        RttMonetisationResult r = RttMonetisationAnalyzer.analyze(
                5, new BigDecimal("200"), null, false);

        assertThat(r.statut()).isEqualTo(RttMonetisationStatut.NON_ELIGIBLE);
        assertThat(r.montantBrut()).isNull();
        assertThat(r.joursAcquisDansFenetre()).isFalse();
        assertThat(r.notes()).anyMatch(n -> n.contains("hors de la fenêtre"));
    }

    @Test
    void regimeAligneHeuresSup_noteExoneration() {
        RttMonetisationResult r = RttMonetisationAnalyzer.analyze(
                3, new BigDecimal("180"), 25d, true);

        assertThat(r.regimeSocialFiscal()).isEqualTo("ALIGNE_HEURES_SUPPLEMENTAIRES");
        assertThat(r.notes()).anyMatch(n -> n.contains("exonération"));
    }

    @Test
    void nombreJoursNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RttMonetisationAnalyzer.analyze(
                null, new BigDecimal("200"), null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombreJoursRttRenonces");
    }

    @Test
    void nombreJoursNul_leveIllegalArgument() {
        assertThatThrownBy(() -> RttMonetisationAnalyzer.analyze(
                0, new BigDecimal("200"), null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void salaireNul_leveIllegalArgument() {
        assertThatThrownBy(() -> RttMonetisationAnalyzer.analyze(
                5, BigDecimal.ZERO, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireJournalierBrut");
    }

    @Test
    void joursAcquisDansFenetreNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RttMonetisationAnalyzer.analyze(
                5, new BigDecimal("200"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursAcquisDansFenetre");
    }
}
