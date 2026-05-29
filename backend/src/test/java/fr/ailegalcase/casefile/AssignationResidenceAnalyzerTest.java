package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-35 : tests unitaires de {@link AssignationResidenceAnalyzer}.
 * Couvre les 3 statuts (EN_COURS, EXPIRATION_PROCHE &lt; 15 j, EXPIRE), le calcul
 * de l'échéance, {@code renouvellementPossible} (durée &lt; 135 j), la liste des
 * moyens de contestation, le recours TA (48 h) et la validation des entrées
 * (durée &gt; 135 j ou ≤ 0 rejetée) — date du jour injectée pour le déterminisme.
 */
class AssignationResidenceAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── Statut EN_COURS ──────────────────────────────────────────────────

    @Test
    void analyze_echeanceLointaine_statutEnCours_avecEcheanceEtRenouvellement() {
        // Notifiée il y a 10 j pour 45 j → échéance dans 35 j → EN_COURS
        LocalDate notif = TODAY.minusDays(10);
        AssignationResidenceResult r = AssignationResidenceAnalyzer.analyze(
                notif, 45, AssignationResidenceMotifEnum.EXECUTION_OQTF, "Pointage quotidien", TODAY);

        assertThat(r.statut()).isEqualTo(AssignationResidenceStatut.EN_COURS);
        assertThat(r.dateEcheanceAssignation()).isEqualTo(notif.plusDays(45));
        assertThat(r.dureeTotaleAutoriseeJours()).isEqualTo(135);
        assertThat(r.renouvellementPossible()).isTrue();
        assertThat(r.recoursPossible()).isTrue();
        assertThat(r.delaiRecoursTaHeures()).isEqualTo(48);
        assertThat(r.baseJuridique()).contains("L. 731-1");
    }

    // ── Statut EXPIRATION_PROCHE (< 15 j) ────────────────────────────────

    @Test
    void analyze_echeanceDansMoinsDe15Jours_statutExpirationProche() {
        // Notifiée il y a 38 j pour 45 j → échéance dans 7 j → EXPIRATION_PROCHE
        LocalDate notif = TODAY.minusDays(38);
        AssignationResidenceResult r = AssignationResidenceAnalyzer.analyze(
                notif, 45, AssignationResidenceMotifEnum.SURVEILLANCE_MESURE_ELOIGNEMENT, null, TODAY);

        assertThat(r.statut()).isEqualTo(AssignationResidenceStatut.EXPIRATION_PROCHE);
        assertThat(r.dateEcheanceAssignation()).isEqualTo(notif.plusDays(45));
    }

    @Test
    void analyze_echeanceA14Jours_borneExpirationProche() {
        // Notifiée il y a 31 j pour 45 j → échéance dans 14 j (< 15) → EXPIRATION_PROCHE
        LocalDate notif = TODAY.minusDays(31);
        AssignationResidenceResult r = AssignationResidenceAnalyzer.analyze(
                notif, 45, AssignationResidenceMotifEnum.AUTRE, null, TODAY);

        assertThat(r.statut()).isEqualTo(AssignationResidenceStatut.EXPIRATION_PROCHE);
    }

    @Test
    void analyze_echeanceA15Jours_resteEnCours() {
        // Notifiée il y a 30 j pour 45 j → échéance dans 15 j (seuil exclusif) → EN_COURS
        LocalDate notif = TODAY.minusDays(30);
        AssignationResidenceResult r = AssignationResidenceAnalyzer.analyze(
                notif, 45, AssignationResidenceMotifEnum.AUTRE, null, TODAY);

        assertThat(r.statut()).isEqualTo(AssignationResidenceStatut.EN_COURS);
    }

    // ── Statut EXPIRE ────────────────────────────────────────────────────

    @Test
    void analyze_echeanceDepassee_statutExpire() {
        // Notifiée il y a 60 j pour 45 j → échéance il y a 15 j → EXPIRE
        LocalDate notif = TODAY.minusDays(60);
        AssignationResidenceResult r = AssignationResidenceAnalyzer.analyze(
                notif, 45, AssignationResidenceMotifEnum.EXECUTION_OQTF, null, TODAY);

        assertThat(r.statut()).isEqualTo(AssignationResidenceStatut.EXPIRE);
        assertThat(r.dateEcheanceAssignation()).isBefore(TODAY);
    }

    // ── renouvellementPossible ───────────────────────────────────────────

    @Test
    void analyze_dureeMaximale135_renouvellementImpossible() {
        AssignationResidenceResult r = AssignationResidenceAnalyzer.analyze(
                TODAY.minusDays(5), 135, AssignationResidenceMotifEnum.EXECUTION_OQTF, null, TODAY);

        assertThat(r.renouvellementPossible()).isFalse();
        assertThat(r.dureeTotaleAutoriseeJours()).isEqualTo(135);
    }

    // ── motifsContestation ───────────────────────────────────────────────

    @Test
    void analyze_fournitMoyensDeContestation() {
        AssignationResidenceResult r = AssignationResidenceAnalyzer.analyze(
                TODAY.minusDays(5), 45, AssignationResidenceMotifEnum.EXECUTION_OQTF, null, TODAY);

        assertThat(r.motifsContestation()).hasSize(4);
        assertThat(r.motifsContestation())
                .anyMatch(m -> m.toLowerCase().contains("motivation"))
                .anyMatch(m -> m.toLowerCase().contains("fuite"))
                .anyMatch(m -> m.toLowerCase().contains("proportionnalit"))
                .anyMatch(m -> m.toLowerCase().contains("vice de procédure"));
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_dureeSuperieureA135_estRejetee() {
        assertThatThrownBy(() -> AssignationResidenceAnalyzer.analyze(
                TODAY, 136, AssignationResidenceMotifEnum.EXECUTION_OQTF, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("135");
    }

    @Test
    void analyze_dureeNulleOuNegative_estRejetee() {
        assertThatThrownBy(() -> AssignationResidenceAnalyzer.analyze(
                TODAY, 0, AssignationResidenceMotifEnum.EXECUTION_OQTF, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssignationResidenceAnalyzer.analyze(
                TODAY, -5, AssignationResidenceMotifEnum.EXECUTION_OQTF, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_dateNotificationNulle_estRejetee() {
        assertThatThrownBy(() -> AssignationResidenceAnalyzer.analyze(
                null, 45, AssignationResidenceMotifEnum.EXECUTION_OQTF, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_motifNul_estRejete() {
        assertThatThrownBy(() -> AssignationResidenceAnalyzer.analyze(
                TODAY, 45, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
