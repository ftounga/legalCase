package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.TempsPartielRequalificationCalculator.AnalyseTempsPartielRequalification;
import static fr.ailegalcase.casefile.TempsPartielRequalificationCalculator.CodeCritere;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-33 — tests unitaires du calculateur d'analyse de requalification
 * d'un contrat à temps partiel en temps complet
 * (F-DT-49-temps-partiel-requalification, FRANCE — L. 3123-1 à L. 3123-20 CT,
 * L. 3245-1 CT).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : 3 verdicts
 * (REQUALIFICATION_PROBABLE / REQUALIFICATION_POSSIBLE / PAS_DE_REQUALIFICATION),
 * mentions L. 3123-6 absentes ⇒ présomption, plafond HC 1/3 dépassé, rappel
 * de salaire sur 3 ans, gate country FRANCE, facteurs F-IA-03.</p>
 */
class TempsPartielRequalificationCalculatorTest {

    /** Input baseline — contrat à temps partiel 24h propre + HC sous le seuil. */
    private static TempsPartielRequalificationInput baselineRegulier() {
        return new TempsPartielRequalificationInput(
                24.0,          // durée contractuelle 24h
                true,          // mention durée présente
                true,          // mention répartition présente
                true,          // mention HC présente
                2.0,           // HC moyennes 2h/sem = 8,3% < 10%
                false,         // pas de modification unilatérale
                24,            // 24 mois d'ancienneté
                2000.0         // 2000 € / mois
        );
    }

    // ── AC1 : mentions durée absentes → REQUALIFICATION_PROBABLE ─────────────

    @Test
    void mentionsDureeAbsentes_returnsRequalificationProbable() {
        var input = new TempsPartielRequalificationInput(
                24.0, false, true, true, 2.0, false, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRequalification())
                .isEqualTo(AnalyseTempsPartielRequalification.REQUALIFICATION_PROBABLE);
        assertThat(r.messages()).anyMatch(m -> m.contains("PRÉSOMPTION") || m.contains("présomption"));
        assertThat(r.facteursRequalification())
                .anyMatch(f -> f.code() == CodeCritere.DT49_MENTIONS_DUREE && f.poids() > 0);
    }

    @Test
    void mentionsRepartitionAbsentes_returnsRequalificationProbable() {
        var input = new TempsPartielRequalificationInput(
                24.0, true, false, true, 2.0, false, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRequalification())
                .isEqualTo(AnalyseTempsPartielRequalification.REQUALIFICATION_PROBABLE);
        assertThat(r.facteursRequalification())
                .anyMatch(f -> f.code() == CodeCritere.DT49_MENTIONS_REPARTITION && f.poids() > 0);
    }

    // ── AC2 : HC > 1/3 → REQUALIFICATION_PROBABLE + facteur HC_ABUSIVES ──────

    @Test
    void hcSuperieuresAuTiers_returnsRequalificationProbable_etFacteurHCAbusives() {
        // 24h durée + 10h HC/sem = 41,7% > 1/3 = 33,3%
        var input = new TempsPartielRequalificationInput(
                24.0, true, true, true, 10.0, false, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRequalification())
                .isEqualTo(AnalyseTempsPartielRequalification.REQUALIFICATION_PROBABLE);
        assertThat(r.facteursRequalification())
                .anyMatch(f -> f.code() == CodeCritere.DT49_HC_ABUSIVES && f.poids() > 0);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("HEURES COMPLÉMENTAIRES ABUSIVES") || m.contains("1/3"));
    }

    // ── AC3 : rappel de salaire estimé sur 3 ans si requalification ──────────

    @Test
    void requalificationProbable_calculeRappelSalaire3Ans() {
        var input = new TempsPartielRequalificationInput(
                24.0, false, true, true, 2.0, false, 36, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.rappelSalaire3AnsEstimeEuros()).isNotNull();
        // Δ = (35 - 24) / 24 = 0,458, × 2000 = 916,67 €/mois, × 36 mois = 33000 €
        assertThat(r.rappelSalaire3AnsEstimeEuros()).isGreaterThan(20_000.0);
    }

    @Test
    void requalificationProbable_rappelPlafonneA36Mois() {
        // Ancienneté > 36 mois → rappel plafonné
        var input = new TempsPartielRequalificationInput(
                24.0, false, true, true, 2.0, false, 120, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.rappelSalaire3AnsEstimeEuros()).isNotNull();
        // 36 mois max = même valeur qu'avec 36 mois
        var ref = TempsPartielRequalificationCalculator.compute(
                new TempsPartielRequalificationInput(
                        24.0, false, true, true, 2.0, false, 36, 2000.0),
                "FRANCE");
        assertThat(r.rappelSalaire3AnsEstimeEuros()).isEqualTo(ref.rappelSalaire3AnsEstimeEuros());
    }

    @Test
    void pasDeRequalification_rappelSalaireNull() {
        var r = TempsPartielRequalificationCalculator.compute(baselineRegulier(), "FRANCE");
        assertThat(r.analyseRequalification())
                .isEqualTo(AnalyseTempsPartielRequalification.PAS_DE_REQUALIFICATION);
        assertThat(r.rappelSalaire3AnsEstimeEuros()).isNull();
    }

    // ── AC4 : gate country FRANCE ────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                TempsPartielRequalificationCalculator.compute(baselineRegulier(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void countryNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                TempsPartielRequalificationCalculator.compute(baselineRegulier(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                TempsPartielRequalificationCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    // ── AC5 : modification unilatérale → REQUALIFICATION_POSSIBLE ────────────

    @Test
    void modificationUnilaterale_returnsRequalificationPossible() {
        var input = new TempsPartielRequalificationInput(
                24.0, true, true, true, 2.0, true, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRequalification())
                .isEqualTo(AnalyseTempsPartielRequalification.REQUALIFICATION_POSSIBLE);
        assertThat(r.facteursRequalification())
                .anyMatch(f -> f.code() == CodeCritere.DT49_MODIFICATION_UNILATERALE && f.poids() > 0);
    }

    // ── AC6 : HC entre 1/10 et 1/3 → REQUALIFICATION_POSSIBLE ────────────────

    @Test
    void hcEntreUnDixiemeEtUnTiers_returnsRequalificationPossible() {
        // 24h durée + 6h HC = 25% > 10%, sous 33,3%
        var input = new TempsPartielRequalificationInput(
                24.0, true, true, true, 6.0, false, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRequalification())
                .isEqualTo(AnalyseTempsPartielRequalification.REQUALIFICATION_POSSIBLE);
    }

    // ── AC7 : verdict PAS_DE_REQUALIFICATION quand tout est en règle ────────

    @Test
    void contratRegulier_returnsPasDeRequalification() {
        var r = TempsPartielRequalificationCalculator.compute(baselineRegulier(), "FRANCE");
        assertThat(r.analyseRequalification())
                .isEqualTo(AnalyseTempsPartielRequalification.PAS_DE_REQUALIFICATION);
        assertThat(r.scoreRequalification()).isLessThan(20);
    }

    // ── AC8 : score borné [0, 100] ───────────────────────────────────────────

    @Test
    void scoreBorneEntreZeroEtCent() {
        var maxInput = new TempsPartielRequalificationInput(
                20.0, false, false, false, 15.0, true, 60, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(maxInput, "FRANCE");
        assertThat(r.scoreRequalification()).isBetween(0, 100);

        var minInput = new TempsPartielRequalificationInput(
                24.0, true, true, true, 0.0, false, 0, 0.0);
        var r2 = TempsPartielRequalificationCalculator.compute(minInput, "FRANCE");
        assertThat(r2.scoreRequalification()).isBetween(0, 100);
    }

    // ── AC9 : facteurs F-IA-03 — les 4 codes critères sont présents ──────────

    @Test
    void facteursRequalification_contiennentLes4CodesCriteres() {
        var input = new TempsPartielRequalificationInput(
                24.0, false, false, false, 10.0, true, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.facteursRequalification())
                .extracting(TempsPartielRequalificationCalculator.FacteurRequalification::code)
                .containsExactlyInAnyOrder(
                        CodeCritere.DT49_MENTIONS_DUREE,
                        CodeCritere.DT49_MENTIONS_REPARTITION,
                        CodeCritere.DT49_HC_ABUSIVES,
                        CodeCritere.DT49_MODIFICATION_UNILATERALE);
    }

    // ── AC10 : bases juridiques contiennent L. 3123-9 et L. 3123-6 ──────────

    @Test
    void basesJuridiques_contiennentArticlesCT() {
        var input = new TempsPartielRequalificationInput(
                24.0, false, true, true, 10.0, false, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 3123-6"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 3123-9"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 3245-1"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 3123-1 à L. 3123-20"));
    }

    // ── AC11 : Cass. soc. 22/01/1992 dans les bases quand présomption ───────

    @Test
    void cassationDe1992_presenteQuandPresomption() {
        var input = new TempsPartielRequalificationInput(
                24.0, false, true, true, 2.0, false, 24, 2000.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("22/01/1992"));
    }

    // ── AC12 : durée 0 ou null → ratio HC neutralisé ─────────────────────────

    @Test
    void dureeContractuelleZero_neBloquePas() {
        var input = new TempsPartielRequalificationInput(
                0.0, true, true, true, 0.0, false, 0, 0.0);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        // Pas d'exception, pas de NPE.
        assertThat(r).isNotNull();
    }

    @Test
    void salaireNull_pasDeRappelCalcule() {
        var input = new TempsPartielRequalificationInput(
                24.0, false, true, true, 2.0, false, 24, null);
        var r = TempsPartielRequalificationCalculator.compute(input, "FRANCE");
        assertThat(r.rappelSalaire3AnsEstimeEuros()).isNull();
    }
}
