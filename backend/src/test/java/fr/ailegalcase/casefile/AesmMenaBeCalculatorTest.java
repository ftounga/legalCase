package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-11 — UT du calculator composite AESM + tutelle MENA BE
 * (Loi 04/05/2007 tutelle MENA + loi 15/12/1980 art. 9bis adapté MENA + circulaire
 * OE 15/09/2005).
 */
class AesmMenaBeCalculatorTest {

    private static final LocalDate ARRIVEE_2022 = LocalDate.of(2022, 9, 1);

    // ===== Cas nominal — FAVORABLE =====

    @Test
    void compute_tousCriteres_avecScolarite30Mois_returnsFavorable_score100_bonus5() {
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true,            // tuteur OK
                true, 30,        // intégration + scolarité 30 mois
                true,            // projet de vie
                true,            // perspective autonomie
                false);          // !menace OP

        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.FAVORABLE);
        assertThat(r.scoreIntegration()).isEqualTo(100);
        assertThat(r.bonus()).isEqualTo(5); // 40+30+20+10+5=105 capé à 100
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.etapeTutelle()).isNull(); // tuteur déjà désigné
        assertThat(r.delaiDesignationTuteur())
                .isEqualTo(ARRIVEE_2022.plusDays(30));
        assertThat(r.baseJuridique()).contains("Loi 04/05/2007");
        assertThat(r.baseJuridique()).contains("art. 9bis");
        assertThat(r.baseJuridique()).contains("circulaire OE 15/09/2005");
        assertThat(r.prochainActe()).contains("Désignation tuteur DGDE");
    }

    @Test
    void compute_tousCriteres_scolarite23Mois_pasDeBonus() {
        // 23 < 24 → pas de bonus
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true, true, 23, true, true, false);

        assertThat(r.scoreIntegration()).isEqualTo(100); // 40+30+20+10
        assertThat(r.bonus()).isZero();
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.FAVORABLE);
    }

    @Test
    void compute_bonusScolarite24Mois_estApplique() {
        // 24 mois exact = seuil bonus inclusif
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                false,           // tuteur non désigné — peu importe le scoring AESM
                true, 24,
                false, false, false); // pas de projet ni autonomie, mais !menace

        // Score : 40 (intégration) + 0 + 0 + 10 (!menace) + 5 (bonus) = 55
        assertThat(r.bonus()).isEqualTo(5);
        assertThat(r.scoreIntegration()).isEqualTo(55);
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.SOUS_RESERVE);
    }

    // ===== SOUS_RESERVE =====

    @Test
    void compute_2critereSur4_avecScolarite12Mois_returnsSousReserve() {
        // 40 (intégration) + 0 + 0 + 10 (!menace) = 50 → SOUS_RESERVE (seuil bas inclusif)
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                14, ARRIVEE_2022,
                true, true, 12, false, false, false);

        assertThat(r.scoreIntegration()).isEqualTo(50);
        assertThat(r.bonus()).isZero();
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.SOUS_RESERVE);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Projet de vie"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Perspective d'autonomie"));
    }

    // ===== DEFAVORABLE =====

    @Test
    void compute_0critere_menaceOrdrePublic_returnsDefavorable_score0() {
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                12, ARRIVEE_2022,
                false, false, null, false, false, true);

        assertThat(r.scoreIntegration()).isZero();
        assertThat(r.bonus()).isZero();
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.DEFAVORABLE);
        assertThat(r.criteresNonRemplis()).hasSize(5); // 4 + tuteur
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Tuteur DGDE"));
    }

    // ===== Priorité urgence =====

    @Test
    void compute_age17_returnsPrioriteUrgenceTrue() {
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                17, ARRIVEE_2022,
                true, true, 30, true, true, false);
        assertThat(r.prioriteUrgence()).isTrue();
    }

    @Test
    void compute_age16_returnsPrioriteUrgenceFalse() {
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                16, ARRIVEE_2022,
                true, true, 30, true, true, false);
        assertThat(r.prioriteUrgence()).isFalse();
    }

    // ===== Volet tutelle =====

    @Test
    void compute_tuteurNonDesigne_etapeTutelleRenseignee() {
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                false,
                true, 24, true, true, false);

        assertThat(r.etapeTutelle()).isNotNull();
        assertThat(r.etapeTutelle()).contains("Service des Tutelles");
        assertThat(r.etapeTutelle()).contains("art. 7");
        assertThat(r.etapeTutelle()).contains("04/05/2007");
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Tuteur DGDE"));
    }

    @Test
    void compute_tuteurDesigne_etapeTutelleNull() {
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true,
                true, 24, true, true, false);

        assertThat(r.etapeTutelle()).isNull();
        assertThat(r.criteresNonRemplis()).noneMatch(c -> c.contains("Tuteur DGDE"));
    }

    @Test
    void compute_delaiDesignationTuteur_egalDateArriveePlus30() {
        LocalDate arrivee = LocalDate.of(2024, 3, 15);
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                14, arrivee,
                false, true, 12, true, true, false);
        assertThat(r.delaiDesignationTuteur()).isEqualTo(LocalDate.of(2024, 4, 14));
    }

    // ===== Cap score 100 =====

    @Test
    void compute_capScore100_avecBonus_neDepassePas100() {
        // 40+30+20+10+5 = 105 → capé à 100
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true, true, 60, true, true, false);

        assertThat(r.scoreIntegration()).isEqualTo(100);
        assertThat(r.bonus()).isEqualTo(5);
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.FAVORABLE);
    }

    // ===== Bord seuils verdict =====

    @Test
    void compute_score70Exact_returnsFavorable_borneBasInclusive() {
        // 40 + 30 + 0 + 0 + 0 = 70
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                14, ARRIVEE_2022,
                true, true, 0, true, false, true);
        // 40 (intégration) + 30 (projet) + 0 (autonomie) + 0 (menace) + 0 (scolarité < 24) = 70
        assertThat(r.scoreIntegration()).isEqualTo(70);
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.FAVORABLE);
    }

    @Test
    void compute_score49_returnsDefavorable_borneHautInclusive() {
        // 0 + 30 (projet) + 0 + 10 (!menace) + 0 = 40 → DEFAVORABLE
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                14, ARRIVEE_2022,
                true, false, null, true, false, false);
        assertThat(r.scoreIntegration()).isEqualTo(40);
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.DEFAVORABLE);
    }

    // ===== Validation =====

    @Test
    void compute_age18_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                18, ARRIVEE_2022,
                true, true, 24, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mineurs");
    }

    @Test
    void compute_ageNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                -1, ARRIVEE_2022,
                true, true, 24, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageActuel");
    }

    @Test
    void compute_dateArriveeFuture_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                15, LocalDate.now().plusDays(1),
                true, true, 24, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateArriveeBelgique");
    }

    @Test
    void compute_integrationScolaireTrueSansDuree_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true, true, null, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Durée scolaire");
    }

    @Test
    void compute_integrationScolaireFalseSansDuree_ok() {
        // Pas d'intégration → durée scolaire pas requise
        AesmMenaBeResult r = AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true, false, null, true, true, false);
        assertThat(r.scoreIntegration()).isEqualTo(60); // 0 + 30 + 20 + 10
        assertThat(r.verdictAESM()).isEqualTo(AesmMenaBeVerdictAesm.SOUS_RESERVE);
    }

    @Test
    void compute_dureeScolaireHorsBorne_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true, true, 121, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeScolaire");
    }

    @Test
    void compute_ageNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                null, ARRIVEE_2022,
                true, true, 24, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageActuel");
    }

    @Test
    void compute_tuteurDesigneNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                null, true, 24, true, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tuteurDesigne");
    }

    @Test
    void compute_menaceOrdrePublicNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> AesmMenaBeCalculator.compute(
                15, ARRIVEE_2022,
                true, true, 24, true, true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("menaceOrdrePublic");
    }
}
