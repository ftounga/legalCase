package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-07 — UT du calculator naturalisation 12bis BE (Code de la nationalité
 * belge — loi 28/06/1984 consolidée art. 12bis).
 *
 * <p>Source : art. 12bis CNB + AR 14/01/2013 (preuve langue) + AR 27/04/2018
 * (preuve intégration).
 */
class Naturalisation12bisBeCalculatorTest {

    // ===== Voie 5 ans =====

    @Test
    void compute_voie5Ans_toutOK_returnsVoie5Ans_dureeManquante0() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                60,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, true, false, false);

        assertThat(r.voie5Ans()).isTrue();
        assertThat(r.voie10Ans()).isFalse();
        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.VOIE_5_ANS);
        assertThat(r.dureeManquante()).isZero();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.baseJuridique()).contains("art. 12bis");
        assertThat(r.baseJuridique()).contains("AR 14/01/2013");
        assertThat(r.procedureReference()).contains("Chambre des représentants");
    }

    @Test
    void compute_voie5Ans_avecPreuveEmploiSeule_returnsVoie5Ans() {
        // Intégration false mais emploi true → suffit pour la voie 5 ans
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                72,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.SUPERIEUR_A2,
                false, true, false, false);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.VOIE_5_ANS);
        assertThat(r.dureeManquante()).isZero();
    }

    // ===== Voie 10 ans =====

    @Test
    void compute_voie10Ans_sansIntegrationNiEmploi_returnsVoie10Ans() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                120,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                false, false, false, false);

        assertThat(r.voie5Ans()).isFalse();
        assertThat(r.voie10Ans()).isTrue();
        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.VOIE_10_ANS);
        assertThat(r.dureeManquante()).isZero();
        assertThat(r.criteresNonRemplis()).isEmpty();
    }

    @Test
    void compute_voie10Ans_etCondIntegrationOK_prioritseVoie5Ans() {
        // Quand voie5Ans et voie10Ans satisfaites, la priorité va à la voie 5 ans.
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                144,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.SUPERIEUR_A2,
                true, true, false, false);

        assertThat(r.voie5Ans()).isTrue();
        assertThat(r.voie10Ans()).isTrue();
        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.VOIE_5_ANS);
    }

    // ===== Aucune voie — durée =====

    @Test
    void compute_dureeInsuffisante_avecIntegration_returnsAucune_dureeManquanteVoie5() {
        // 50 mois, séjour OK, langue OK, intégration OK, pas de menace
        // → AUCUNE, manque 10 mois pour la voie 5 ans
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                50,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false, false);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isEqualTo(10);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Durée de séjour insuffisante"));
    }

    @Test
    void compute_duree80sansIntegrationNiEmploi_returnsAucune_dureeManquanteVoie10() {
        // 80 mois, séjour OK, langue OK, PAS intégration ni emploi
        // → AUCUNE (pas voie 5 sans intégration, pas voie 10 < 120) — manque 40 mois pour voie 10
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                80,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                false, false, false, false);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isEqualTo(40);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Voie 5 ans"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("preuve d'intégration"));
    }

    // ===== Aucune voie — conditions non temporelles =====

    @Test
    void compute_typeSejourLimite_returnsAucune_critereSejour() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                72,
                NaturalisationBeTypeSejourEnum.LIMITE,
                NaturalisationBeNiveauLangueEnum.SUPERIEUR_A2,
                true, true, false, false);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isZero(); // condition non temporelle → 0
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Séjour limité"));
    }

    @Test
    void compute_niveauLangueInferieurA2_returnsAucune_critereLangue() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                144,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.INFERIEUR_A2,
                true, true, false, false);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isZero();
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Niveau de langue < A2"));
    }

    @Test
    void compute_menaceOrdrePublic_returnsAucune_critereOrdrePublic() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                144,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, true, true, false);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isZero();
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
    }

    @Test
    void compute_condamnationPenale_returnsAucune_critereCondamnation() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                144,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, true, false, true);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isZero();
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Condamnation pénale"));
    }

    @Test
    void compute_duree80avecIntegrationMaisCondamnation_returnsAucune_critereCondamnation() {
        // 80 mois, intégration OK, MAIS condamnation → AUCUNE
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                80,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false, true);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isZero(); // bloquée par condamnation (non temporelle)
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Condamnation pénale"));
    }

    @Test
    void compute_toutInvalide_returnsAucune_4criteresRempliss() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                0,
                NaturalisationBeTypeSejourEnum.LIMITE,
                NaturalisationBeNiveauLangueEnum.INFERIEUR_A2,
                false, false, true, true);

        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.AUCUNE);
        assertThat(r.dureeManquante()).isZero(); // conditions non temp violées
        // 4 critères "non temporels" + 1 critère durée (< 5 ans)
        assertThat(r.criteresNonRemplis()).hasSize(5);
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Séjour limité"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Niveau de langue"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("ordre public"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Condamnation"));
        assertThat(r.criteresNonRemplis()).anyMatch(c -> c.contains("Durée de séjour insuffisante"));
    }

    // ===== Limites inclusives =====

    @Test
    void compute_duree60exact_returnsVoie5Ans_limiteInclusive() {
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                60,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false, false);
        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.VOIE_5_ANS);
    }

    @Test
    void compute_duree120exact_returnsVoie5Ans_quandIntegration() {
        // 120 mois avec intégration → voie 5 ans prioritaire
        Naturalisation12bisBeResult r = Naturalisation12bisBeCalculator.compute(
                120,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, false, false, false);
        assertThat(r.voie5Ans()).isTrue();
        assertThat(r.voie10Ans()).isTrue();
        assertThat(r.voieEligible()).isEqualTo(Naturalisation12bisBeVoieEligible.VOIE_5_ANS);
    }

    // ===== Validation =====

    @Test
    void compute_dureeNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> Naturalisation12bisBeCalculator.compute(
                -1,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejour");
    }

    @Test
    void compute_duree601_throwsIllegalArgument() {
        assertThatThrownBy(() -> Naturalisation12bisBeCalculator.compute(
                601,
                NaturalisationBeTypeSejourEnum.ILLIMITE,
                NaturalisationBeNiveauLangueEnum.A2,
                true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejour");
    }

    @Test
    void compute_typeSejourNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> Naturalisation12bisBeCalculator.compute(
                60, null,
                NaturalisationBeNiveauLangueEnum.A2,
                true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeSejour");
    }

    @Test
    void compute_niveauLangueNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> Naturalisation12bisBeCalculator.compute(
                60, NaturalisationBeTypeSejourEnum.ILLIMITE, null,
                true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("niveauLangue");
    }
}
