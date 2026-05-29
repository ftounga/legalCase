package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-19 : tests unitaires de {@link AjCndaAnalyzer}.
 * Couvre l'éligibilité aux ressources (plafond AJ), le délai de recours CNDA
 * (1 mois normal vs 15 j procédure accélérée, L. 532-4 CESEDA), le délai de
 * demande d'AJ (15 j), les 4 statuts (AJ_A_DEMANDER, AJ_DEPOSEE, HORS_DELAI_AJ,
 * NON_ELIGIBLE_RESSOURCES), les pièces et la validation des entrées — avec une
 * date du jour ({@code today}) injectée pour le déterminisme.
 */
class AjCndaAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── Éligibilité ressources ───────────────────────────────────────────

    @Test
    void analyze_ressourcesSousPlafond_eligibleAj_statutADemander() {
        LocalDate decision = TODAY.minusDays(5);
        AjCndaResult r = AjCndaAnalyzer.analyze(decision, 800.0, false, false, null, TODAY);

        assertThat(r.eligibleAJ()).isTrue();
        assertThat(r.statut()).isEqualTo(AjCndaStatut.AJ_A_DEMANDER);
        assertThat(r.dateEcheanceDemandeAJ()).isEqualTo(decision.plusDays(15));
        assertThat(r.piecesAJ()).isNotEmpty();
        assertThat(r.baseJuridique()).contains("L. 532-4");
    }

    @Test
    void analyze_ressourcesAuPlafond_resteEligible_borneInclusive() {
        AjCndaResult r = AjCndaAnalyzer.analyze(
                TODAY.minusDays(2), AjCndaAnalyzer.PLAFOND_AJ_MENSUEL_EUR, false, false, null, TODAY);

        assertThat(r.eligibleAJ()).isTrue();
        assertThat(r.statut()).isEqualTo(AjCndaStatut.AJ_A_DEMANDER);
    }

    @Test
    void analyze_ressourcesAuDessusPlafond_nonEligible_statutNonEligibleRessources() {
        AjCndaResult r = AjCndaAnalyzer.analyze(
                TODAY.minusDays(2), 2000.0, false, false, null, TODAY);

        assertThat(r.eligibleAJ()).isFalse();
        assertThat(r.statut()).isEqualTo(AjCndaStatut.NON_ELIGIBLE_RESSOURCES);
        assertThat(r.recommandation()).contains("plafond");
    }

    // ── Délais recours CNDA ──────────────────────────────────────────────

    @Test
    void analyze_procedureNormale_delaiRecours1Mois() {
        LocalDate decision = TODAY.minusDays(1);
        AjCndaResult r = AjCndaAnalyzer.analyze(decision, 800.0, false, false, null, TODAY);

        assertThat(r.dateEcheanceRecoursCNDA()).isEqualTo(decision.plusMonths(1));
        assertThat(r.procedureAccelereeDureeReduite()).isFalse();
    }

    @Test
    void analyze_procedureAcceleree_delaiRecours15Jours() {
        LocalDate decision = TODAY.minusDays(1);
        AjCndaResult r = AjCndaAnalyzer.analyze(decision, 800.0, true, false, null, TODAY);

        assertThat(r.dateEcheanceRecoursCNDA()).isEqualTo(decision.plusDays(15));
        assertThat(r.procedureAccelereeDureeReduite()).isTrue();
        assertThat(r.recommandation()).contains("accélérée");
    }

    // ── Statuts AJ ───────────────────────────────────────────────────────

    @Test
    void analyze_demandeAjDeposee_statutAjDeposee() {
        LocalDate decision = TODAY.minusDays(3);
        AjCndaResult r = AjCndaAnalyzer.analyze(decision, 800.0, false, true,
                decision.plusDays(2), TODAY);

        assertThat(r.statut()).isEqualTo(AjCndaStatut.AJ_DEPOSEE);
        assertThat(r.demandeAJDeposee()).isTrue();
    }

    @Test
    void analyze_delaiDemandeAjDepasse_nonDeposee_statutHorsDelai() {
        // décision il y a 20 j → échéance demande AJ (J+15) dépassée
        LocalDate decision = TODAY.minusDays(20);
        AjCndaResult r = AjCndaAnalyzer.analyze(decision, 800.0, false, false, null, TODAY);

        assertThat(r.statut()).isEqualTo(AjCndaStatut.HORS_DELAI_AJ);
        assertThat(r.dateEcheanceDemandeAJ()).isBefore(TODAY);
    }

    @Test
    void analyze_echeanceDemandeAjAujourdhui_resteADemander_borneNonStricte() {
        // décision il y a 15 j → échéance == today → encore dans le délai (pas isAfter)
        LocalDate decision = TODAY.minusDays(15);
        AjCndaResult r = AjCndaAnalyzer.analyze(decision, 800.0, false, false, null, TODAY);

        assertThat(r.dateEcheanceDemandeAJ()).isEqualTo(TODAY);
        assertThat(r.statut()).isEqualTo(AjCndaStatut.AJ_A_DEMANDER);
    }

    @Test
    void analyze_nonEligiblePrimeSurHorsDelai() {
        // ressources > plafond ET hors délai → NON_ELIGIBLE_RESSOURCES prioritaire
        LocalDate decision = TODAY.minusDays(30);
        AjCndaResult r = AjCndaAnalyzer.analyze(decision, 3000.0, false, false, null, TODAY);

        assertThat(r.statut()).isEqualTo(AjCndaStatut.NON_ELIGIBLE_RESSOURCES);
    }

    // ── Pièces ───────────────────────────────────────────────────────────

    @Test
    void analyze_piecesAj_contiennentCerfaRessourcesEtNotificationOfpra() {
        AjCndaResult r = AjCndaAnalyzer.analyze(TODAY.minusDays(2), 800.0, false, false, null, TODAY);

        assertThat(r.piecesAJ()).hasSize(3);
        assertThat(r.piecesAJ()).anyMatch(p -> p.toLowerCase().contains("cerfa"));
        assertThat(r.piecesAJ()).anyMatch(p -> p.toLowerCase().contains("ressources"));
        assertThat(r.piecesAJ()).anyMatch(p -> p.toLowerCase().contains("ofpra"));
    }

    // ── Validation ───────────────────────────────────────────────────────

    @Test
    void analyze_dateDecisionNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AjCndaAnalyzer.analyze(null, 800.0, false, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDecisionOFPRA");
    }

    @Test
    void analyze_dateDecisionFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AjCndaAnalyzer.analyze(
                TODAY.plusDays(1), 800.0, false, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }
}
