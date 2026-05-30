package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-21 : tests unitaires de {@link StagiaireGratificationAnalyzer}.
 *
 * <p>Logique déterministe (art. L.124-1 et s. du code de l'éducation) :
 * <ul>
 *   <li>seuil de gratification au-delà de 44 jours de présence ;</li>
 *   <li>gratification minimale = 15 % du plafond horaire SS × heures (1 jour = 7 h) ;</li>
 *   <li>rappel = max(0, due − versée) ;</li>
 *   <li>requalification ELEVE si ≥ 2 indices ou durée &gt; 6 mois ; MODERE si 1
 *       indice ; FAIBLE sinon ;</li>
 *   <li>verdict REQUALIFICATION_PROBABLE / RAPPEL_GRATIFICATION / STAGE_CONFORME.</li>
 * </ul>
 */
class StagiaireGratificationAnalyzerTest {

    @Test
    void stageQuatreMois_quatreVingtsJours_gratifZero_obligatoire_rappelPositif() {
        StagiaireGratificationResult r = StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                80, BigDecimal.ZERO, null, false, false);

        assertThat(r.seuilAtteint()).isTrue();
        assertThat(r.gratificationObligatoire()).isTrue();
        // 80 jours × 7 h × 4.35 €/h = 2 436.00 €.
        assertThat(r.gratificationMinimaleDue()).isEqualByComparingTo("2436.00");
        assertThat(r.rappelGratification()).isGreaterThan(BigDecimal.ZERO);
        assertThat(r.verdictGlobal()).isEqualTo(StagiaireGratificationVerdict.RAPPEL_GRATIFICATION);
        assertThat(r.baseJuridique()).contains("L.124-6");
    }

    @Test
    void stageUnMois_vingtJours_seuilNonAtteint_pasDeGratification() {
        StagiaireGratificationResult r = StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 2, 5),
                20, BigDecimal.ZERO, null, false, false);

        assertThat(r.seuilAtteint()).isFalse();
        assertThat(r.gratificationObligatoire()).isFalse();
        assertThat(r.gratificationMinimaleDue()).isEqualByComparingTo("0.00");
        assertThat(r.rappelGratification()).isEqualByComparingTo("0.00");
        assertThat(r.risqueRequalification()).isEqualTo(StagiaireRisqueRequalification.FAIBLE);
        assertThat(r.verdictGlobal()).isEqualTo(StagiaireGratificationVerdict.STAGE_CONFORME);
    }

    @Test
    void tauxConventionnelPlusFavorable_gratificationCalculeeSurLeTauxConventionnel() {
        StagiaireGratificationResult r = StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                80, BigDecimal.ZERO, new BigDecimal("6.00"), false, false);

        assertThat(r.tauxHoraireApplique()).isEqualByComparingTo("6.00");
        // 80 jours × 7 h × 6.00 €/h = 3 360.00 € (> minimum légal 2 436.00 €).
        assertThat(r.gratificationMinimaleDue()).isEqualByComparingTo("3360.00");
    }

    @Test
    void gratificationVersee_couvreLeMinimum_aucunRappel_stageConforme() {
        // Versée mensuelle 700 € × 5 mois = 3 500 € > 2 436 € dus.
        StagiaireGratificationResult r = StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                80, new BigDecimal("700.00"), null, false, false);

        assertThat(r.gratificationMinimaleDue()).isEqualByComparingTo("2436.00");
        assertThat(r.rappelGratification()).isEqualByComparingTo("0.00");
        assertThat(r.verdictGlobal()).isEqualTo(StagiaireGratificationVerdict.STAGE_CONFORME);
    }

    @Test
    void deuxIndices_posteePermanentEtMissionsHors_risqueEleve_requalificationProbable() {
        StagiaireGratificationResult r = StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                80, BigDecimal.ZERO, null, true, true);

        assertThat(r.missionsHorsProjetPedagogique()).isTrue();
        assertThat(r.posteTravailPermanent()).isTrue();
        assertThat(r.risqueRequalification()).isEqualTo(StagiaireRisqueRequalification.ELEVE);
        assertThat(r.verdictGlobal()).isEqualTo(StagiaireGratificationVerdict.REQUALIFICATION_PROBABLE);
        assertThat(r.motifs()).anySatisfy(m -> assertThat(m).contains("CDI"));
    }

    @Test
    void dureeSuperieureASixMois_risqueEleve_depassementDureeMax() {
        StagiaireGratificationResult r = StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 8, 6),
                120, BigDecimal.ZERO, null, false, false);

        assertThat(r.depassementDureeMax()).isTrue();
        assertThat(r.risqueRequalification()).isEqualTo(StagiaireRisqueRequalification.ELEVE);
        assertThat(r.verdictGlobal()).isEqualTo(StagiaireGratificationVerdict.REQUALIFICATION_PROBABLE);
        assertThat(r.motifs()).anySatisfy(m -> assertThat(m).contains("L.124-5"));
    }

    @Test
    void unSeulIndice_risqueModere() {
        StagiaireGratificationResult r = StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                80, BigDecimal.ZERO, null, true, false);

        assertThat(r.risqueRequalification()).isEqualTo(StagiaireRisqueRequalification.MODERE);
        // Rappel dû mais risque non élevé → RAPPEL_GRATIFICATION.
        assertThat(r.verdictGlobal()).isEqualTo(StagiaireGratificationVerdict.RAPPEL_GRATIFICATION);
    }

    @Test
    void datesIncoherentes_finAvantDebut_leveIllegalArgument() {
        assertThatThrownBy(() -> StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 5, 2), LocalDate.of(2025, 1, 6),
                80, BigDecimal.ZERO, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joursPresenceNegatif_ouMontantNegatif_leveIllegalArgument() {
        assertThatThrownBy(() -> StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                -1, BigDecimal.ZERO, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                80, new BigDecimal("-1"), null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2),
                80, BigDecimal.ZERO, new BigDecimal("-1"), false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void datesRequisesNull_leveIllegalArgument() {
        assertThatThrownBy(() -> StagiaireGratificationAnalyzer.analyze(
                null, LocalDate.of(2025, 5, 2), 80, BigDecimal.ZERO, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), null, 80, BigDecimal.ZERO, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StagiaireGratificationAnalyzer.analyze(
                LocalDate.of(2025, 1, 6), LocalDate.of(2025, 5, 2), null, BigDecimal.ZERO, null, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
