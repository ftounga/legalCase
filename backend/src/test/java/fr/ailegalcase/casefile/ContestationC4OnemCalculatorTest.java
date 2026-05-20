package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-03 : tests unitaires du calculateur de contestation C4 ONEM (12 cas
 * mini-spec — Cas A {OUVERT, IMMINENT, PRESCRIT}, Cas B {OUVERT, IMMINENT,
 * PRESCRIT}, bornes de seuils, gestion fin de mois, validations dures).
 *
 * <p>Tous les calculs utilisent {@code dateActionEnvisagee} explicite pour
 * rester déterministes (pas de {@code LocalDate.now()} implicite).</p>
 */
class ContestationC4OnemCalculatorTest {

    private static final LocalDate NOTIF = LocalDate.of(2026, 4, 15);

    // =========================================================================
    // Cas A — palier ADMIN actif (recoursAdminDejaForme=false ou null)
    // =========================================================================

    @Test
    void casA_recoursAdminOuvert_joursRestantsSuperieursA7() {
        // Notification 2026-04-15 + 1 mois = 2026-05-15.
        // Action 2026-04-20 → 25 jours restants (> 7) → RECOURS_ADMIN_OUVERT.
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 4, 20), false, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_ADMIN_OUVERT);
        assertThat(result.etapeSuivante())
                .isEqualTo(ContestationC4OnemResult.EtapeSuivante.RECOURS_ADMIN_DIRECTEUR);
        assertThat(result.paliers()).hasSize(2);

        var admin = result.paliers().get(0);
        assertThat(admin.type()).isEqualTo(ContestationC4OnemResult.PalierType.ADMIN);
        assertThat(admin.dateLimite()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(admin.joursRestants()).isEqualTo(25);
        assertThat(admin.baseJuridique()).contains("art. 144");

        var tribunal = result.paliers().get(1);
        assertThat(tribunal.type()).isEqualTo(ContestationC4OnemResult.PalierType.TRIBUNAL);
        assertThat(tribunal.dateLimite()).isNull();
        assertThat(tribunal.joursRestants()).isNull();
        assertThat(tribunal.baseJuridique()).contains("580");

        assertThat(result.baseJuridique())
                .contains("25 novembre 1991").contains("CJ art. 580");
    }

    @Test
    void casA_recoursAdminImminent_joursRestantsEntre1Et7() {
        // Notification 2026-04-15 + 1 mois = 2026-05-15.
        // Action 2026-05-10 → 5 jours restants (∈ ]0, 7]) → IMMINENT.
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 5, 10), false, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_ADMIN_IMMINENT);
        assertThat(result.etapeSuivante())
                .isEqualTo(ContestationC4OnemResult.EtapeSuivante.RECOURS_ADMIN_DIRECTEUR);
        assertThat(result.paliers().get(0).joursRestants()).isEqualTo(5);
    }

    @Test
    void casA_recoursAdminImminent_borneSuperieure_joursRestantsEgal7() {
        // Notification 2026-04-15 + 1 mois = 2026-05-15.
        // Action 2026-05-08 → 7 jours restants (borne IMMINENT inclusive).
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 5, 8), false, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_ADMIN_IMMINENT);
        assertThat(result.paliers().get(0).joursRestants()).isEqualTo(7);
    }

    @Test
    void casA_recoursAdminOuvert_borneInferieure_joursRestantsEgal8() {
        // 8 jours restants → strictement > 7 → OUVERT (frontière OUVERT/IMMINENT).
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 5, 7), false, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_ADMIN_OUVERT);
        assertThat(result.paliers().get(0).joursRestants()).isEqualTo(8);
    }

    @Test
    void casA_recoursAdminPrescrit_joursRestantsNegatif_etapeSuivanteTribunal() {
        // Notification 2026-04-15 + 1 mois = 2026-05-15.
        // Action 2026-05-20 → -5 jours → PRESCRIT.
        // etapeSuivante = RECOURS_TRIBUNAL_TRAVAIL (saut palier admin).
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 5, 20), false, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_ADMIN_PRESCRIT);
        assertThat(result.etapeSuivante())
                .isEqualTo(ContestationC4OnemResult.EtapeSuivante.RECOURS_TRIBUNAL_TRAVAIL);
        assertThat(result.paliers().get(0).joursRestants()).isEqualTo(-5);
        assertThat(result.paliers().get(1).dateLimite()).isNull();
    }

    @Test
    void casA_recoursAdminPrescrit_borne_joursRestantsEgal0() {
        // Notification 2026-04-15 + 1 mois = 2026-05-15.
        // Action 2026-05-15 → 0 jour → PRESCRIT (frontière inclusive).
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 5, 15), false, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_ADMIN_PRESCRIT);
        assertThat(result.paliers().get(0).joursRestants()).isZero();
    }

    @Test
    void casA_finDeMois_31janvierPlus1Mois_28fevrier() {
        // LocalDate.plusMonths(1) ajuste automatiquement les fins de mois :
        // 31 janvier 2026 + 1 mois → 28 février 2026 (année non bissextile).
        LocalDate notif = LocalDate.of(2026, 1, 31);
        var req = new ContestationC4OnemRequest(
                notif, LocalDate.of(2026, 2, 1), false, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.paliers().get(0).dateLimite())
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    // =========================================================================
    // Cas B — palier TRIBUNAL actif (recoursAdminDejaForme=true)
    // =========================================================================

    @Test
    void casB_recoursTribunalOuvert_joursRestantsSuperieursA14() {
        // Décision Directeur 2026-04-01 + 3 mois = 2026-07-01.
        // Action 2026-04-15 → 77 jours restants → OUVERT.
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 4, 15), true, LocalDate.of(2026, 4, 1));
        // dateNotif < dateDecisionDirecteur car le scénario est : décision ONEM
        // antérieure (15 avril), recours admin formé, décision Directeur encore
        // antérieure dans ce test artificiel → on inverse pour rester cohérent.
        var coherent = new ContestationC4OnemRequest(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 4, 15),
                true,
                LocalDate.of(2026, 4, 1));
        var result = ContestationC4OnemCalculator.compute(coherent);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_OUVERT);
        assertThat(result.etapeSuivante())
                .isEqualTo(ContestationC4OnemResult.EtapeSuivante.RECOURS_TRIBUNAL_TRAVAIL);

        var tribunal = result.paliers().get(1);
        assertThat(tribunal.dateLimite()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(tribunal.joursRestants()).isEqualTo(77);
        // suppression du warning unused
        assertThat(req).isNotNull();
    }

    @Test
    void casB_recoursTribunalImminent_joursRestantsEntre1Et14() {
        // Décision Directeur 2026-03-01 + 3 mois = 2026-06-01.
        // Action 2026-05-25 → 7 jours → IMMINENT (∈ ]0, 14]).
        var req = new ContestationC4OnemRequest(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 25),
                true,
                LocalDate.of(2026, 3, 1));
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_IMMINENT);
        assertThat(result.paliers().get(1).joursRestants()).isEqualTo(7);
    }

    @Test
    void casB_recoursTribunalImminent_borneSuperieure_joursRestantsEgal14() {
        // Décision Directeur 2026-03-01 + 3 mois = 2026-06-01.
        // Action 2026-05-18 → 14 jours → borne IMMINENT inclusive.
        var req = new ContestationC4OnemRequest(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 18),
                true,
                LocalDate.of(2026, 3, 1));
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_IMMINENT);
        assertThat(result.paliers().get(1).joursRestants()).isEqualTo(14);
    }

    @Test
    void casB_recoursTribunalOuvert_borneInferieure_joursRestantsEgal15() {
        // 15 jours → strictement > 14 → OUVERT.
        var req = new ContestationC4OnemRequest(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 17),
                true,
                LocalDate.of(2026, 3, 1));
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_OUVERT);
        assertThat(result.paliers().get(1).joursRestants()).isEqualTo(15);
    }

    @Test
    void casB_recoursTribunalPrescrit_etapeForclusionTotale() {
        // Décision Directeur 2026-01-01 + 3 mois = 2026-04-01.
        // Action 2026-05-01 → -30 jours → PRESCRIT → FORCLUSION_TOTALE.
        var req = new ContestationC4OnemRequest(
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2026, 5, 1),
                true,
                LocalDate.of(2026, 1, 1));
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_TRIBUNAL_PRESCRIT);
        assertThat(result.etapeSuivante())
                .isEqualTo(ContestationC4OnemResult.EtapeSuivante.FORCLUSION_TOTALE);
        assertThat(result.paliers().get(1).joursRestants()).isEqualTo(-30);
    }

    @Test
    void casB_palierAdminInformatif_renvoieDateEtJoursRestants() {
        // En Cas B, le palier ADMIN reste informatif (jours négatifs souvent).
        var req = new ContestationC4OnemRequest(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 1),
                true,
                LocalDate.of(2026, 3, 1));
        var result = ContestationC4OnemCalculator.compute(req);

        var admin = result.paliers().get(0);
        assertThat(admin.type()).isEqualTo(ContestationC4OnemResult.PalierType.ADMIN);
        // Notif 2026-01-15 + 1 mois = 2026-02-15 → action 2026-05-01 → -75 j.
        assertThat(admin.dateLimite()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(admin.joursRestants()).isEqualTo(-75);
    }

    // =========================================================================
    // Validations dures
    // =========================================================================

    @Test
    void dateNotificationNull_leve400() {
        var req = new ContestationC4OnemRequest(null, LocalDate.of(2026, 5, 1), false, null);
        assertThatThrownBy(() -> ContestationC4OnemCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationDecisionOnem est requise");
    }

    @Test
    void dateNotificationFuture_leve400() {
        // Date dans 1 an → future → 400.
        var req = new ContestationC4OnemRequest(
                LocalDate.now(ContestationC4OnemCalculator.ZONE_BRUSSELS).plusYears(1),
                null, false, null);
        assertThatThrownBy(() -> ContestationC4OnemCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void recoursAdminDejaFormeTrue_sansDateDecisionDirecteur_leve400() {
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 5, 1), true, null);
        assertThatThrownBy(() -> ContestationC4OnemCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDecisionDirecteur est requise");
    }

    @Test
    void dateDecisionDirecteurAvantDateNotification_leve400() {
        var req = new ContestationC4OnemRequest(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1),
                true,
                LocalDate.of(2026, 4, 1)); // antérieur à dateNotif
        assertThatThrownBy(() -> ContestationC4OnemCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDecisionDirecteur doit être postérieure");
    }

    @Test
    void dateActionEnvisageeAvantDateNotification_leve400() {
        var req = new ContestationC4OnemRequest(
                NOTIF, NOTIF.minusDays(1), false, null);
        assertThatThrownBy(() -> ContestationC4OnemCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateActionEnvisagee");
    }

    // =========================================================================
    // Cas A — recoursAdminDejaForme null → traité comme false (cas A)
    // =========================================================================

    @Test
    void recoursAdminDejaFormeNull_traiteCommeCasA() {
        // null Boolean → traité comme false (cas A).
        var req = new ContestationC4OnemRequest(
                NOTIF, LocalDate.of(2026, 4, 20), null, null);
        var result = ContestationC4OnemCalculator.compute(req);

        assertThat(result.recoursAdminDejaForme()).isFalse();
        assertThat(result.verdict())
                .isEqualTo(ContestationC4OnemResult.Verdict.RECOURS_ADMIN_OUVERT);
    }
}
