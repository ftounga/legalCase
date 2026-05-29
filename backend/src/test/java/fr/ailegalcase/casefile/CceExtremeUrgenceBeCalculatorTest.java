package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-15 — UT du calculator du recours en EXTRÊME URGENCE devant le CCE (BE).
 *
 * <p>Source : Loi 15/12/1980 art. 39/82 §4 al. 2-3 — 5 jours OUVRABLES belges
 * (réutilise {@code BelgianBusinessDaysCalculator}). Outil BELGIQUE UNIQUEMENT.
 *
 * <p>Repère : 2026-06-01 est un lundi sans jour férié BE dans la fenêtre +5 j
 * ouvrables → dateLimite = lundi 2026-06-08 (mar 02, mer 03, jeu 04, ven 05,
 * lun 08). 2026-07-21 (Fête nationale) est utilisé pour la couverture jour férié.
 */
class CceExtremeUrgenceBeCalculatorTest {

    private static final CceExtremeUrgenceBeTypeActeEnum OQT =
            CceExtremeUrgenceBeTypeActeEnum.OQT_EXECUTE;

    @Test
    void compute_dateLimite_estActePlus5JoursOuvrables() {
        LocalDate acte = LocalDate.of(2026, 6, 1); // lundi
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, acte);

        // mar 02, mer 03, jeu 04, ven 05, lun 08 → 5 jours ouvrables
        assertThat(r.dateLimiteRecours()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(r.joursOuvrablesRestants()).isEqualTo(5);
        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.DISPONIBLE);
        assertThat(r.actionImmediate()).isNull();
        assertThat(r.baseJuridique()).contains("39/82");
    }

    @Test
    void compute_audienceEstimee_estDateLimitePlus2JoursOuvrables() {
        LocalDate acte = LocalDate.of(2026, 6, 1); // lundi → limite lundi 08
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, acte);

        // limite lundi 08 + 2 j ouvrables → mar 09, mer 10
        assertThat(r.audienceEstimee()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    void compute_joursOuvrablesRestantsSuperieur2_returnsDisponible() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        LocalDate today = LocalDate.of(2026, 6, 3); // mer → reste jeu 04, ven 05, lun 08 = 3
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, today);

        assertThat(r.joursOuvrablesRestants()).isEqualTo(3);
        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.DISPONIBLE);
    }

    @Test
    void compute_borneHaute2jours_returnsCritique() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        LocalDate today = LocalDate.of(2026, 6, 4); // jeu → reste ven 05, lun 08 = 2
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, today);

        assertThat(r.joursOuvrablesRestants()).isEqualTo(2);
        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.CRITIQUE);
        assertThat(r.actionImmediate()).contains("ACTION IMMÉDIATE");
    }

    @Test
    void compute_borneBasse1jour_returnsCritique() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        LocalDate today = LocalDate.of(2026, 6, 5); // ven → reste lun 08 = 1
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, today);

        assertThat(r.joursOuvrablesRestants()).isEqualTo(1);
        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.CRITIQUE);
        assertThat(r.actionImmediate()).contains("suspension d'extrême urgence");
    }

    @Test
    void compute_weekEndNonComptabilise() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        // today = samedi 06 : reste lun 08 = 1 jour ouvrable (sam/dim exclus)
        LocalDate today = LocalDate.of(2026, 6, 6); // samedi
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, today);

        assertThat(r.joursOuvrablesRestants()).isEqualTo(1);
        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.CRITIQUE);
    }

    @Test
    void compute_jourFerieBE_21juillet_estExclu() {
        // acte = vendredi 17/07/2026 ; +5 j ouvrables en sautant le 21/07 (Fête nationale)
        // lun 20, [mar 21 férié], mer 22, jeu 23, ven 24, lun 27 → limite lundi 27/07
        LocalDate acte = LocalDate.of(2026, 7, 17);
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, acte);

        assertThat(r.dateLimiteRecours()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(r.joursOuvrablesRestants()).isEqualTo(5);
        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.DISPONIBLE);
    }

    @Test
    void compute_apresDateLimite_returnsExpire() {
        LocalDate acte = LocalDate.of(2026, 6, 1); // limite lundi 08
        LocalDate today = LocalDate.of(2026, 6, 9); // mardi → limite dépassée
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, false, null, today);

        assertThat(r.joursOuvrablesRestants()).isZero();
        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.EXPIRE);
        assertThat(r.actionImmediate()).contains("ACTION IMMÉDIATE");
    }

    @Test
    void compute_recoursForme_returnsRecoursForme_prioritaireSurExpire() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        LocalDate today = LocalDate.of(2026, 7, 1); // délai expiré...
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, true, LocalDate.of(2026, 6, 3), today);

        assertThat(r.statut()).isEqualTo(CceExtremeUrgenceBeStatut.RECOURS_FORME);
        assertThat(r.recoursForme()).isTrue();
        assertThat(r.dateRecours()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(r.recommandation()).contains("audience CCE");
    }

    @Test
    void compute_acteFutureDansTolerance_estAccepte() {
        // l'acte peut être imminent dans le futur (rapatriement programmé) jusqu'à +7 jours
        LocalDate today = LocalDate.of(2026, 6, 1);
        LocalDate acteFutur = LocalDate.of(2026, 6, 5); // +4 jours
        CceExtremeUrgenceBeResult r = CceExtremeUrgenceBeCalculator.compute(
                acteFutur, OQT, false, null, today);

        assertThat(r.dateActeExecutoire()).isEqualTo(acteFutur);
        assertThat(r.statut()).isNotNull();
    }

    @Test
    void compute_acteFutureAuDelaTolerance_throwsIllegalArgument() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        LocalDate acteTropLoin = LocalDate.of(2026, 6, 30); // +29 jours
        assertThatThrownBy(() -> CceExtremeUrgenceBeCalculator.compute(
                acteTropLoin, OQT, false, null, today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("imminent");
    }

    @Test
    void compute_recoursFormeSansDateRecours_throwsIllegalArgument() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        assertThatThrownBy(() -> CceExtremeUrgenceBeCalculator.compute(
                acte, OQT, true, null, acte))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_typeActeNull_throwsIllegalArgument() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        assertThatThrownBy(() -> CceExtremeUrgenceBeCalculator.compute(
                acte, null, false, null, acte))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeActe");
    }

    @Test
    void compute_dateActeNull_throwsIllegalArgument() {
        LocalDate acte = LocalDate.of(2026, 6, 1);
        assertThatThrownBy(() -> CceExtremeUrgenceBeCalculator.compute(
                null, OQT, false, null, acte))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateActeExecutoire");
    }
}
