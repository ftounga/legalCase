package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-25 : tests unitaires de {@link AnefProcedureCalculator}.
 * Couvre les 4 statuts (NORMAL, URGENT &lt; 30 j, PANNE_EN_COURS, RECOURS_POSSIBLE),
 * la génération des étapes alternatives en cas de panne, le délai de recours pour
 * faute (2 ans) et la validation des entrées — avec une date du jour ({@code today})
 * injectée pour le déterminisme.
 */
class AnefProcedureCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    // ── Statut NORMAL ────────────────────────────────────────────────────

    @Test
    void compute_pasDePanne_echeanceLointaine_statutNormal_avecEtapesStandard() {
        // Expiration dans 120 j, pas de panne → NORMAL
        LocalDate expiration = TODAY.plusDays(120);
        AnefProcedureResult r = AnefProcedureCalculator.compute(
                "Carte de séjour pluriannuelle", expiration, false, null, false, TODAY);

        assertThat(r.statut()).isEqualTo(AnefProcedureStatut.NORMAL);
        assertThat(r.joursAvantExpiration()).isEqualTo(120L);
        assertThat(r.etapesStandard()).isNotEmpty();
        assertThat(r.etapesAlternatives()).isEmpty();
        assertThat(r.delaiRecoursForFauteAnnees()).isEqualTo(2);
        assertThat(r.baseJuridique()).contains("R. 311-2-2");
    }

    // ── Statut URGENT (< 30 j) ──────────────────────────────────────────

    @Test
    void compute_pasDePanne_echeanceDans20Jours_statutUrgent() {
        LocalDate expiration = TODAY.plusDays(20);
        AnefProcedureResult r = AnefProcedureCalculator.compute(
                "VLS-TS salarié", expiration, false, null, false, TODAY);

        assertThat(r.statut()).isEqualTo(AnefProcedureStatut.URGENT);
        assertThat(r.joursAvantExpiration()).isEqualTo(20L);
        assertThat(r.etapesAlternatives()).isEmpty();
        assertThat(r.recommandation()).contains("imminente");
    }

    @Test
    void compute_echeanceExactement30Jours_resteNormal_borneStricte() {
        // Seuil URGENT strict : < 30 j. À exactement 30 j → NORMAL.
        LocalDate expiration = TODAY.plusDays(30);
        AnefProcedureResult r = AnefProcedureCalculator.compute(
                "Titre", expiration, false, null, false, TODAY);

        assertThat(r.statut()).isEqualTo(AnefProcedureStatut.NORMAL);
        assertThat(r.joursAvantExpiration()).isEqualTo(30L);
    }

    // ── Statut PANNE_EN_COURS ────────────────────────────────────────────

    @Test
    void compute_panneSignalee_sansDemandePrefecture_statutPanneEnCours_avecEtapesAlternatives() {
        // Panne signalée, pas encore de démarche préfecture → PANNE_EN_COURS.
        // La panne est prioritaire même si l'échéance serait URGENT.
        LocalDate expiration = TODAY.plusDays(10);
        AnefProcedureResult r = AnefProcedureCalculator.compute(
                "Carte de séjour", expiration, true, TODAY.minusDays(2), false, TODAY);

        assertThat(r.statut()).isEqualTo(AnefProcedureStatut.PANNE_EN_COURS);
        assertThat(r.panneeANEFSignalee()).isTrue();
        assertThat(r.etapesAlternatives()).hasSize(4);
        assertThat(r.etapesAlternatives().get(0)).contains("preuve");
        assertThat(r.etapesAlternatives().get(1)).contains("LRAR");
        assertThat(r.etapesAlternatives().get(2)).contains("physiquement");
        assertThat(r.etapesAlternatives().get(3)).contains("recours pour faute");
        assertThat(r.recommandation()).contains("procédure alternative");
    }

    // ── Statut RECOURS_POSSIBLE ──────────────────────────────────────────

    @Test
    void compute_panneSignalee_avecDemandePrefecture_statutRecoursPossible() {
        LocalDate expiration = TODAY.plusDays(10);
        AnefProcedureResult r = AnefProcedureCalculator.compute(
                "Carte de séjour", expiration, true, TODAY.minusDays(5), true, TODAY);

        assertThat(r.statut()).isEqualTo(AnefProcedureStatut.RECOURS_POSSIBLE);
        assertThat(r.etapesAlternatives()).isNotEmpty();
        assertThat(r.delaiRecoursForFauteAnnees()).isEqualTo(2);
        assertThat(r.recommandation()).contains("recours pour faute");
    }

    // ── Étapes standard toujours présentes ──────────────────────────────

    @Test
    void compute_etapesStandard_toujoursPresentes_memeEnPanne() {
        AnefProcedureResult r = AnefProcedureCalculator.compute(
                "Titre", TODAY.plusDays(5), true, null, false, TODAY);

        assertThat(r.etapesStandard()).hasSize(4);
        assertThat(r.etapesStandard().get(0)).contains("ANEF");
    }

    // ── Expiration dépassée ──────────────────────────────────────────────

    @Test
    void compute_titreDejaExpire_joursNegatifs_statutUrgent() {
        LocalDate expiration = TODAY.minusDays(5);
        AnefProcedureResult r = AnefProcedureCalculator.compute(
                "Titre", expiration, false, null, false, TODAY);

        assertThat(r.joursAvantExpiration()).isEqualTo(-5L);
        assertThat(r.statut()).isEqualTo(AnefProcedureStatut.URGENT);
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void compute_dateExpirationNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AnefProcedureCalculator.compute(
                "Titre", null, false, null, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateExpirationTitre");
    }

    @Test
    void compute_dateTentativeDepotFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> AnefProcedureCalculator.compute(
                "Titre", TODAY.plusDays(60), true, TODAY.plusDays(1), false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateTentativeDepot");
    }
}
