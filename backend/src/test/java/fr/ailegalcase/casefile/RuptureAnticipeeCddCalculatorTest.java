package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static fr.ailegalcase.casefile.RuptureAnticipeeCddCalculator.AnalyseRuptureAnticipeeCdd;
import static fr.ailegalcase.casefile.RuptureAnticipeeCddInput.AuteurRupture;
import static fr.ailegalcase.casefile.RuptureAnticipeeCddInput.MotifRupture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-17 — tests unitaires du calculateur d'analyse de la rupture anticipée
 * d'un CDD (F-DT-43-rupture-anticipee-cdd, FRANCE).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * LEGITIME / ILLEGITIME_EMPLOYEUR / ILLEGITIME_SALARIE ;
 * calcul indemnités L. 1243-4 + L. 1243-8 ; gate country FRANCE ;
 * matrice (auteur, motif) — 12 combinaisons.</p>
 */
class RuptureAnticipeeCddCalculatorTest {

    private static final LocalDate DATE_RUPTURE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DATE_TERME = LocalDate.of(2026, 7, 1); // 6 mois

    // ── AC1 : rupture employeur sans motif → ILLEGITIME_EMPLOYEUR ───────────

    @Test
    void employeurSansMotif_returnsIllegitimeEmployeurEtIndemnites() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                DATE_TERME, DATE_RUPTURE,
                2500.0,  // salaire mensuel brut
                10000.0  // rémunération totale depuis début CDD
        );
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.ILLEGITIME_EMPLOYEUR);
        // 6 mois × 2500 = 15000
        assertThat(r.indemnitesRuptureAnticipeeEuros()).isEqualTo(15000.0);
        // 10000 × 0.10 = 1000
        assertThat(r.indemnitePrecariteEuros()).isEqualTo(1000.0);
        assertThat(r.totalDuSalarieEuros()).isEqualTo(16000.0);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.moisRestantsContrat()).isEqualTo(6);
    }

    // ── AC2 : rupture employeur faute grave → LEGITIME, indemnités = 0 ──────

    @Test
    void employeurFauteGrave_returnsLegitimeEtZeroIndemnite() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.FAUTE_GRAVE,
                DATE_TERME, DATE_RUPTURE,
                2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.LEGITIME);
        assertThat(r.indemnitesRuptureAnticipeeEuros()).isEqualTo(0.0);
        assertThat(r.indemnitePrecariteEuros()).isEqualTo(0.0);
        assertThat(r.totalDuSalarieEuros()).isEqualTo(0.0);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1243-1"));
    }

    // ── AC3 : accord parties → LEGITIME des deux côtés ──────────────────────

    @Test
    void accordPartiesEmployeur_returnsLegitime() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.ACCORD_PARTIES,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.LEGITIME);
        assertThat(r.totalDuSalarieEuros()).isEqualTo(0.0);
    }

    @Test
    void accordPartiesSalarie_returnsLegitime() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.SALARIE, MotifRupture.ACCORD_PARTIES,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.LEGITIME);
    }

    // ── AC4 : CDI embauche salarié → LEGITIME, pas d'indemnité ──────────────

    @Test
    void cdiEmbaucheSalarie_returnsLegitime() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.SALARIE, MotifRupture.CDI_EMBAUCHE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.LEGITIME);
        assertThat(r.totalDuSalarieEuros()).isEqualTo(0.0);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1243-2"));
    }

    @Test
    void cdiEmbaucheEmployeur_returnsIllegitimeEmployeur() {
        // L'embauche en CDI ailleurs est un motif réservé au salarié.
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.CDI_EMBAUCHE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.ILLEGITIME_EMPLOYEUR);
        assertThat(r.indemnitesRuptureAnticipeeEuros()).isGreaterThan(0.0);
    }

    // ── Rupture salarié sans motif → ILLEGITIME_SALARIE ─────────────────────

    @Test
    void salarieSansMotif_returnsIllegitimeSalarieEtZeroPourSalarie() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.SALARIE, MotifRupture.AUTRE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.ILLEGITIME_SALARIE);
        // Le salarié ne reçoit rien — dommages-intérêts vers l'employeur.
        assertThat(r.indemnitesRuptureAnticipeeEuros()).isEqualTo(0.0);
        assertThat(r.totalDuSalarieEuros()).isEqualTo(0.0);
        assertThat(r.messages()).anyMatch(m -> m.contains("dommages"));
    }

    // ── Force majeure → LEGITIME des deux côtés ─────────────────────────────

    @Test
    void forceMajeureEmployeur_returnsLegitime() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.FORCE_MAJEURE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.LEGITIME);
    }

    @Test
    void forceMajeureSalarie_returnsLegitime() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.SALARIE, MotifRupture.FORCE_MAJEURE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.LEGITIME);
    }

    // ── Inaptitude employeur → LEGITIME ; salarié → ILLEGITIME ──────────────

    @Test
    void inaptitudeEmployeur_returnsLegitime() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.INAPTITUDE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.LEGITIME);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1226-4-2"));
    }

    @Test
    void inaptitudeSalarie_returnsIllegitimeSalarie() {
        // L'inaptitude est un motif côté employeur après médecin du travail.
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.SALARIE, MotifRupture.INAPTITUDE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.ILLEGITIME_SALARIE);
    }

    // ── Faute grave côté salarié → ILLEGITIME_SALARIE ──────────────────────

    @Test
    void fauteGraveSalarie_returnsIllegitimeSalarie() {
        // Le salarié ne peut pas invoquer la faute grave employeur comme motif
        // de rupture anticipée (voie : résiliation judiciaire / prise d'acte).
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.SALARIE, MotifRupture.FAUTE_GRAVE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.analyseRuptureAnticipee()).isEqualTo(AnalyseRuptureAnticipeeCdd.ILLEGITIME_SALARIE);
    }

    // ── Calcul mois restants ─────────────────────────────────────────────────

    @Test
    void moisRestants_calcul3Mois() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 1, 1),
                3000.0, 9000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.moisRestantsContrat()).isEqualTo(3);
        // 3 mois × 3000 = 9000
        assertThat(r.indemnitesRuptureAnticipeeEuros()).isEqualTo(9000.0);
    }

    @Test
    void moisRestants_rupturePresqueAuTerme_returnsZeroOrUn() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 1),
                3000.0, 30000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        // 14 jours / 30 = 0.47 → 0 (rounded)
        assertThat(r.moisRestantsContrat()).isEqualTo(0);
        assertThat(r.indemnitesRuptureAnticipeeEuros()).isEqualTo(0.0);
        // Indemnité de précarité reste due : 30000 × 10 % = 3000
        assertThat(r.indemnitePrecariteEuros()).isEqualTo(3000.0);
    }

    // ── Points d'analyse F-IA-03 ─────────────────────────────────────────────

    @Test
    void pointsAnalyse_couvrentLesTroisCriteres() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.pointsAnalyse())
                .extracting(p -> p.code().name())
                .contains(
                        "DT43_AUTEUR_RUPTURE",
                        "DT43_MOTIF_RUPTURE",
                        "DT43_DATE_TERME");
    }

    // ── Erreurs / pays ───────────────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        assertThatThrownBy(() ->
                RuptureAnticipeeCddCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                RuptureAnticipeeCddCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void auteurNull_throwsIllegalArgument() {
        var input = new RuptureAnticipeeCddInput(
                null, MotifRupture.AUTRE, DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        assertThatThrownBy(() ->
                RuptureAnticipeeCddCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Auteur");
    }

    @Test
    void motifNull_throwsIllegalArgument() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, null, DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        assertThatThrownBy(() ->
                RuptureAnticipeeCddCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motif");
    }

    @Test
    void dateRuptureApresTerme_throwsIllegalArgument() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1),
                2500.0, 10000.0);
        assertThatThrownBy(() ->
                RuptureAnticipeeCddCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postérieure");
    }

    @Test
    void salaireNegatif_throwsIllegalArgument() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                DATE_TERME, DATE_RUPTURE, -100.0, 10000.0);
        assertThatThrownBy(() ->
                RuptureAnticipeeCddCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    // ── Bases juridiques de référence ───────────────────────────────────────

    @Test
    void basesJuridiques_employeurIllegitime_contiennentL1243_4EtL1243_8() {
        var input = new RuptureAnticipeeCddInput(
                AuteurRupture.EMPLOYEUR, MotifRupture.AUTRE,
                DATE_TERME, DATE_RUPTURE, 2500.0, 10000.0);
        var r = RuptureAnticipeeCddCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1243-4"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1243-8"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("Cass. soc."));
    }
}
