package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-17 : tests unitaires de {@link IntermittentSpectacleAreAnalyzer}.
 *
 * <p>Valeurs déterministes (règlement Unedic, annexes 8 et 10) :
 * <ul>
 *   <li>seuil d'affiliation = 507 heures sur 12 mois ;</li>
 *   <li>conversion cachet = 12 heures (annexe 10) ;</li>
 *   <li>heures de formation écrêtées au plafond (90 h) ;</li>
 *   <li>marge de ± 10 h autour du seuil → A_VERIFIER ;</li>
 *   <li>date de réexamen = dateFinContrat + 12 mois.</li>
 * </ul>
 */
class IntermittentSpectacleAreAnalyzerTest {

    private static final LocalDate FIN = LocalDate.of(2024, 6, 30);

    @Test
    void annexe8_seuilLargementAtteint_droitsOuverts_excedent() {
        // 600 h > 507 + 10 → OUVERTS, excédent 93.
        IntermittentSpectacleAreResult r = IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_8_TECHNICIENS, FIN, 600, 0, 0);

        assertThat(r.heuresTotalesRetenues()).isEqualTo(600);
        assertThat(r.ouvertureDroits()).isEqualTo(IntermittentSpectacleAreVerdict.OUVERTS);
        assertThat(r.statut()).isEqualTo(IntermittentSpectacleAreStatut.DROITS_OUVERTS);
        assertThat(r.heuresExcedentaires()).isEqualTo(93);
        assertThat(r.heuresManquantes()).isZero();
        assertThat(r.seuilHeures()).isEqualTo(507);
        assertThat(r.baseJuridique()).contains("507");
    }

    @Test
    void annexe10_conversionCachets12h_sousLeSeuil_nonOuverts_heuresManquantes() {
        // 300 h travail + 15 cachets × 12 = 180 → total 480 < 507 - 10 → NON_OUVERTS.
        IntermittentSpectacleAreResult r = IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_10_ARTISTES, FIN, 300, 15, 0);

        assertThat(r.heuresCachets()).isEqualTo(180);
        assertThat(r.heuresTotalesRetenues()).isEqualTo(480);
        assertThat(r.ouvertureDroits()).isEqualTo(IntermittentSpectacleAreVerdict.NON_OUVERTS);
        assertThat(r.statut()).isEqualTo(IntermittentSpectacleAreStatut.DROITS_NON_OUVERTS);
        assertThat(r.heuresManquantes()).isEqualTo(27);
        assertThat(r.heuresExcedentaires()).isZero();
    }

    @Test
    void heuresFormation_ecreteesAuPlafond90() {
        // 400 h + 150 h formation écrêtées à 90 → total 490 < 507 - 10 → NON_OUVERTS,
        // manquantes = 17 (et non négatif : prouve l'écrêtage à 90 et non 150).
        IntermittentSpectacleAreResult r = IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_8_TECHNICIENS, FIN, 400, 0, 150);

        assertThat(r.heuresFormationRetenues()).isEqualTo(90);
        assertThat(r.heuresTotalesRetenues()).isEqualTo(490);
        assertThat(r.heuresManquantes()).isEqualTo(17);
        assertThat(r.ouvertureDroits()).isEqualTo(IntermittentSpectacleAreVerdict.NON_OUVERTS);
    }

    @Test
    void totalDansLaMarge_aVerifier() {
        // 510 h ∈ [497, 517] → A_VERIFIER même si ≥ 507.
        IntermittentSpectacleAreResult r = IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_8_TECHNICIENS, FIN, 510, 0, 0);

        assertThat(r.ouvertureDroits()).isEqualTo(IntermittentSpectacleAreVerdict.A_VERIFIER);
        assertThat(r.statut()).isEqualTo(IntermittentSpectacleAreStatut.A_VERIFIER);
    }

    @Test
    void seuilPileAtteint507_aVerifier_carDansLaMarge() {
        // 507 pile : écart 0 ≤ 10 → A_VERIFIER (borne incluse).
        IntermittentSpectacleAreResult r = IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_8_TECHNICIENS, FIN, 507, 0, 0);

        assertThat(r.ouvertureDroits()).isEqualTo(IntermittentSpectacleAreVerdict.A_VERIFIER);
        assertThat(r.heuresManquantes()).isZero();
        assertThat(r.heuresExcedentaires()).isZero();
    }

    @Test
    void dateProchainExamen_egaleFinContratPlus12Mois() {
        IntermittentSpectacleAreResult r = IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_10_ARTISTES, FIN, 600, 0, 0);

        assertThat(r.dateProchainExamen()).isEqualTo(FIN.plusMonths(12));
        assertThat(r.dateProchainExamen()).isEqualTo(LocalDate.of(2025, 6, 30));
    }

    @Test
    void annexeNulle_leveIllegalArgument() {
        assertThatThrownBy(() -> IntermittentSpectacleAreAnalyzer.analyze(
                null, FIN, 600, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateFinContratFuture_leveIllegalArgument() {
        assertThatThrownBy(() -> IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_8_TECHNICIENS,
                LocalDate.now().plusDays(1), 600, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void heuresNegatives_leveIllegalArgument() {
        assertThatThrownBy(() -> IntermittentSpectacleAreAnalyzer.analyze(
                IntermittentSpectacleAnnexe.ANNEXE_8_TECHNICIENS, FIN, -5, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
