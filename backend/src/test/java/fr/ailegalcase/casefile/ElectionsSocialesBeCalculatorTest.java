package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-09 : tests unitaires du {@link ElectionsSocialesBeCalculator}.
 *
 * <p>Couvre : détermination du verdict selon les seuils ETP
 * (&lt; 50 / 50-99 / ≥ 100 + zone limite ±1 autour de 50 et 100),
 * calcul complet du calendrier à rebours du jour Y (X = Y-90, X-60
 * procédure UTE, X-35 listes électeurs, X+35 dépôt candidats, X+40
 * affichage candidats, Y+6 proclamation, Y+45 1re réunion), fenêtre
 * de protection candidats (Loi 19/03/1991, occulte 30 j avant
 * affichage, fin 2 ans non-élus), validation du jour Y dans la
 * fenêtre AR du cycle déclaré (cycles 2024 / 2028 / 2032), validation
 * des inputs et base juridique stable.</p>
 */
class ElectionsSocialesBeCalculatorTest {

    /** Jour Y de référence : 13/05/2024 (premier jour de la fenêtre cycle 2024). */
    private static final LocalDate Y_2024 = LocalDate.of(2024, 5, 13);

    /** Cas de référence : obligation CE + CPPT, 150 ETP, UTE confirmée. */
    private static ElectionsSocialesBeRequest reqObligationCeCppt() {
        return new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024,
                Y_2024,
                150,
                true);
    }

    // ── Verdict OBLIGATION_CE_ET_CPPT ────────────────────────────────────

    @Test
    void analyze_effectif150_returnsObligationCeCppt() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT);
        assertThat(r.conseilEntrepriseObligatoire()).isTrue();
        assertThat(r.comitePreventionProtectionObligatoire()).isTrue();
        assertThat(r.seuilCeApplique()).isEqualTo(100);
        assertThat(r.seuilCpptApplique()).isEqualTo(50);
        assertThat(r.raison()).isEqualTo("OBLIGATION_CE_CPPT_SEUIL_100");
    }

    @Test
    void analyze_effectif500_returnsObligationCeCppt() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 500, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT);
    }

    // ── Verdict OBLIGATION_CPPT_SEUL ─────────────────────────────────────

    @Test
    void analyze_effectif75_returnsObligationCpptSeul() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 75, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CPPT_SEUL);
        assertThat(r.conseilEntrepriseObligatoire()).isFalse();
        assertThat(r.comitePreventionProtectionObligatoire()).isTrue();
        assertThat(r.raison()).isEqualTo("OBLIGATION_CPPT_SEUIL_50");
    }

    // ── Verdict NON_APPLICABLE_SEUIL ─────────────────────────────────────

    @Test
    void analyze_effectif30_returnsNonApplicableSeuil() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 30, false);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.NON_APPLICABLE_SEUIL);
        assertThat(r.conseilEntrepriseObligatoire()).isFalse();
        assertThat(r.comitePreventionProtectionObligatoire()).isFalse();
        assertThat(r.raison()).isEqualTo("EFFECTIF_INFERIEUR_50");
        assertThat(r.synthese()).contains("CCT n° 5");
        // Pas de fenêtre protégée si pas d'obligation.
        assertThat(r.dateDebutPeriodeProtegeeCandidats()).isNull();
        assertThat(r.dateFinPeriodeProtegeeNonElus()).isNull();
    }

    // ── Verdict EFFECTIF_A_RECALCULER (limites ±1 ETP) ───────────────────

    @Test
    void analyze_effectif49_returnsEffectifARecalculer() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 49, false);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
        assertThat(r.raison()).isEqualTo("EFFECTIF_LIMITE_RECALCUL_REQUIS");
        assertThat(r.synthese()).contains("art. 32");
    }

    @Test
    void analyze_effectif50_returnsEffectifARecalculer() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 50, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
    }

    @Test
    void analyze_effectif51_returnsEffectifARecalculer() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 51, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
    }

    @Test
    void analyze_effectif99_returnsEffectifARecalculer() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 99, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
    }

    @Test
    void analyze_effectif100_returnsEffectifARecalculer() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 100, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
    }

    @Test
    void analyze_effectif101_returnsEffectifARecalculer() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 101, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
    }

    @Test
    void analyze_effectif52_returnsObligationCpptSeul() {
        // Sortie de la zone limite ±1 : OK pour CPPT seul.
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 52, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CPPT_SEUL);
    }

    @Test
    void analyze_effectif102_returnsObligationCeCppt() {
        // Sortie de la zone limite ±1 supérieure : OK pour CE + CPPT.
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 102, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT);
    }

    @Test
    void analyze_effectif48_returnsNonApplicableSeuil() {
        // 48 < (50 - 1) → hors zone limite, vraiment sous le seuil.
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 48, false);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.NON_APPLICABLE_SEUIL);
    }

    // ── Calendrier — vérification exhaustive depuis Y = 13/05/2024 ────────

    @Test
    void analyze_calendrier_jourX_equals_Y_moins_90() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // 13/05/2024 - 90 jours = 13/02/2024
        assertThat(r.jourX()).isEqualTo(LocalDate.of(2024, 2, 13));
    }

    @Test
    void analyze_calendrier_dateDebutProcedureUte_equals_X_moins_60() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // X = 13/02/2024 ; X-60 = 15/12/2023
        assertThat(r.dateDebutProcedureUte())
                .isEqualTo(LocalDate.of(2023, 12, 15));
    }

    @Test
    void analyze_calendrier_dateTransmissionListesElecteurs_equals_X_moins_35() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // X = 13/02/2024 ; X-35 = 09/01/2024
        assertThat(r.dateTransmissionListesElecteurs())
                .isEqualTo(LocalDate.of(2024, 1, 9));
    }

    @Test
    void analyze_calendrier_dateDepotListesCandidats_equals_X_plus_35() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // X = 13/02/2024 ; X+35 = 19/03/2024
        assertThat(r.dateDepotListesCandidats())
                .isEqualTo(LocalDate.of(2024, 3, 19));
    }

    @Test
    void analyze_calendrier_dateAffichageCandidats_equals_X_plus_40() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // X = 13/02/2024 ; X+40 = 24/03/2024
        assertThat(r.dateAffichageCandidats())
                .isEqualTo(LocalDate.of(2024, 3, 24));
    }

    @Test
    void analyze_calendrier_dateProclamationResultats_equals_Y_plus_6() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // Y = 13/05/2024 ; Y+6 = 19/05/2024
        assertThat(r.dateProclamationResultats())
                .isEqualTo(LocalDate.of(2024, 5, 19));
    }

    @Test
    void analyze_calendrier_datePremiereReunionInstance_equals_Y_plus_45() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // Y = 13/05/2024 ; Y+45 = 27/06/2024
        assertThat(r.datePremiereReunionInstance())
                .isEqualTo(LocalDate.of(2024, 6, 27));
    }

    // ── Calendrier calculé même si pas d'obligation ──────────────────────

    @Test
    void analyze_calendrier_calculeMemeSiNonApplicable() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 30, false);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        // Calendrier rempli pour conseil prévisionnel.
        assertThat(r.jourX()).isNotNull();
        assertThat(r.dateDebutProcedureUte()).isNotNull();
        assertThat(r.dateAffichageCandidats()).isNotNull();
        // Mais pas de fenêtre protégée.
        assertThat(r.dateDebutPeriodeProtegeeCandidats()).isNull();
        assertThat(r.dateFinPeriodeProtegeeNonElus()).isNull();
    }

    // ── Fenêtre protégée candidats (Loi 19/03/1991) ──────────────────────

    @Test
    void analyze_fenetreProtegee_debut_equals_X_plus_10() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // X = 13/02/2024 ; début protection X+10 = 23/02/2024
        assertThat(r.dateDebutPeriodeProtegeeCandidats())
                .isEqualTo(LocalDate.of(2024, 2, 23));
    }

    @Test
    void analyze_fenetreProtegee_finNonElus_equals_affichage_plus_2_ans() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        // Affichage 24/03/2024 + 2 ans = 24/03/2026
        assertThat(r.dateFinPeriodeProtegeeNonElus())
                .isEqualTo(LocalDate.of(2026, 3, 24));
    }

    @Test
    void analyze_fenetreProtegee_remplie_si_OBLIGATION_CPPT_SEUL() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 75, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.dateDebutPeriodeProtegeeCandidats()).isNotNull();
        assertThat(r.dateFinPeriodeProtegeeNonElus()).isNotNull();
    }

    // ── UTE non confirmée — avertissement dans la synthèse ───────────────

    @Test
    void analyze_uteNonConfirmee_avertissementDansSynthese() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 150, false);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.synthese())
                .contains("UTE non confirmée")
                .contains("X-60");
    }

    @Test
    void analyze_uteConfirmee_pasDavertissementDansSynthese() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        assertThat(r.synthese()).doesNotContain("UTE non confirmée");
    }

    // ── Validation cycle / jour Y dans fenêtre AR ────────────────────────

    @Test
    void analyze_jourY_horsFenetreCycle2024_throws() {
        // 1er mai 2024 < 13/05/2024 (début fenêtre AR cycle 2024).
        ElectionsSocialesBeRequest bad = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024,
                LocalDate.of(2024, 5, 1),
                150, true);

        assertThatThrownBy(() ->
                ElectionsSocialesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hors fenêtre AR")
                .hasMessageContaining("2024");
    }

    @Test
    void analyze_jourY_horsFenetreCycle2028_throws() {
        // 10/05/2028 < 11/05/2028 (début fenêtre AR cycle 2028).
        ElectionsSocialesBeRequest bad = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2028,
                LocalDate.of(2028, 5, 10),
                150, true);

        assertThatThrownBy(() ->
                ElectionsSocialesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hors fenêtre AR")
                .hasMessageContaining("2028");
    }

    @Test
    void analyze_jourY_dansFenetreCycle2028_ok() {
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2028,
                LocalDate.of(2028, 5, 15),
                150, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT);
        assertThat(r.cycleElectoral())
                .isEqualTo(ElectionsSocialesBeCycle.CYCLE_2028);
    }

    @Test
    void analyze_jourY_finFenetreCycle2024_ok() {
        // 26/05/2024 = fin fenêtre cycle 2024 (inclusif).
        ElectionsSocialesBeRequest req = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024,
                LocalDate.of(2024, 5, 26),
                150, true);

        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT);
    }

    // ── Snapshot inputs ──────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        assertThat(r.cycleElectoral())
                .isEqualTo(ElectionsSocialesBeCycle.CYCLE_2024);
        assertThat(r.dateJourY()).isEqualTo(Y_2024);
        assertThat(r.effectifMoyenEtp()).isEqualTo(150);
        assertThat(r.uniteTechniqueExploitationConfirmee()).isTrue();
    }

    // ── Base juridique + avertissement ───────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLois() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        assertThat(r.baseJuridique())
                .contains("04/12/2007")
                .contains("25/05/2012")
                .contains("04/08/1996")
                .contains("19/03/1991")
                .contains("art. 32");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        ElectionsSocialesBeResult r =
                ElectionsSocialesBeCalculator.analyze(reqObligationCeCppt());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("calculateur")
                .contains("4 trimestres");
    }

    // ── Helper determinerVerdict ──────────────────────────────────────────

    @Test
    void determinerVerdict_30_returnsNonApplicableSeuil() {
        assertThat(ElectionsSocialesBeCalculator.determinerVerdict(30))
                .isEqualTo(ElectionsSocialesBeVerdict.NON_APPLICABLE_SEUIL);
    }

    @Test
    void determinerVerdict_75_returnsObligationCpptSeul() {
        assertThat(ElectionsSocialesBeCalculator.determinerVerdict(75))
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CPPT_SEUL);
    }

    @Test
    void determinerVerdict_200_returnsObligationCeCppt() {
        assertThat(ElectionsSocialesBeCalculator.determinerVerdict(200))
                .isEqualTo(ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT);
    }

    @Test
    void determinerVerdict_50_returnsEffectifARecalculer() {
        assertThat(ElectionsSocialesBeCalculator.determinerVerdict(50))
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
    }

    @Test
    void determinerVerdict_100_returnsEffectifARecalculer() {
        assertThat(ElectionsSocialesBeCalculator.determinerVerdict(100))
                .isEqualTo(ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER);
    }

    // ── Validation des inputs ─────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                ElectionsSocialesBeCalculator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_cycleNull_throws() {
        ElectionsSocialesBeRequest bad = new ElectionsSocialesBeRequest(
                null, Y_2024, 150, true);

        assertThatThrownBy(() ->
                ElectionsSocialesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cycleElectoral");
    }

    @Test
    void analyze_dateJourYNull_throws() {
        ElectionsSocialesBeRequest bad = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, null, 150, true);

        assertThatThrownBy(() ->
                ElectionsSocialesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateJourY");
    }

    @Test
    void analyze_effectifNegatif_throws() {
        ElectionsSocialesBeRequest bad = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, -1, true);

        assertThatThrownBy(() ->
                ElectionsSocialesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifMoyenEtp");
    }

    @Test
    void analyze_uteConfirmeeNull_throws() {
        ElectionsSocialesBeRequest bad = new ElectionsSocialesBeRequest(
                ElectionsSocialesBeCycle.CYCLE_2024, Y_2024, 150, null);

        assertThatThrownBy(() ->
                ElectionsSocialesBeCalculator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uniteTechniqueExploitationConfirmee");
    }
}
