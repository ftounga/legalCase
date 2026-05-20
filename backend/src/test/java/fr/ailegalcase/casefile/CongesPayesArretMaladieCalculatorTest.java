package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static fr.ailegalcase.casefile.CongesPayesArretMaladieCalculator.TypeArret;
import static fr.ailegalcase.casefile.CongesPayesArretMaladieCalculator.Verdict;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-206-03 — tests unitaires du calculateur de rappel de congés payés acquis
 * pendant un arrêt maladie (FR — loi 22/04/2024).
 *
 * <p>Couvre les 6 critères d'acceptation de la mini-spec : plafonnement
 * maladie non pro à 24 j/an ; absence de plafond AT/MP ; rappel net après
 * déduction ; forclusion 24/04/2026 (salarié en poste) ; prescription
 * 3 ans (salarié sorti) ; ACTION_FORCLOSE post-2026-04-24.</p>
 */
class CongesPayesArretMaladieCalculatorTest {

    /** Date de calcul utilisée par défaut dans les tests — avant la forclusion. */
    private static final LocalDate AVANT_FORCLUSION = LocalDate.of(2026, 4, 23);
    /** Date de calcul utilisée pour tester ACTION_FORCLOSE (après 24/04/2026). */
    private static final LocalDate APRES_FORCLUSION = LocalDate.of(2026, 5, 20);

    private static CongesPayesArretMaladieInput.Builder baseBuilder() {
        return new CongesPayesArretMaladieInput.Builder()
                .typeArret(TypeArret.MALADIE_NON_PROFESSIONNELLE)
                .nombreMoisArret(6)
                .salarieEncoreEnPoste(true)
                .joursCpDejaAccordes(BigDecimal.ZERO)
                .salaireBrutMensuel(new BigDecimal("3000"))
                .dateCalcul(AVANT_FORCLUSION);
    }

    @Test
    void maladieNonPro_6mois_acquiert12Jours() {
        var in = baseBuilder().build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("12.00");
        assertThat(r.joursCpRappel()).isEqualByComparingTo("12.00");
        assertThat(r.verdict()).isEqualTo(Verdict.RAPPEL_SIGNIFICATIF);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void maladieNonPro_18mois_plafondAnnuel24JoursAppliqueSurTrancheComplete() {
        // 18 mois = 1 an complet (plafonné à 24) + 6 mois (= 12, sous plafond)
        // → 24 + 12 = 36 j
        var in = baseBuilder().nombreMoisArret(18).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("36.00");
    }

    @Test
    void maladieNonPro_24mois_deuxAnneesPleines48Jours() {
        // 24 mois = 2 tranches de 12 → 2 × 24 = 48 j
        var in = baseBuilder().nombreMoisArret(24).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("48.00");
    }

    @Test
    void maladieNonPro_13mois_uneAnneePleinePlusUnMois() {
        // 13 mois = 12 plafonnés (24) + 1 mois (= 2) → 26 j
        var in = baseBuilder().nombreMoisArret(13).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("26.00");
    }

    @Test
    void atMp_18mois_45JoursSansPlafond() {
        // AT/MP — 2,5 j/mois sans plafond : 18 × 2,5 = 45
        var in = baseBuilder()
                .typeArret(TypeArret.ACCIDENT_TRAVAIL_MALADIE_PRO)
                .nombreMoisArret(18)
                .build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("45.00");
    }

    @Test
    void atMp_36mois_90JoursSansPlafond() {
        var in = baseBuilder()
                .typeArret(TypeArret.ACCIDENT_TRAVAIL_MALADIE_PRO)
                .nombreMoisArret(36)
                .build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("90.00");
    }

    @Test
    void rappelNet_apresDeductionJoursDejaAccordes() {
        // 6 mois maladie non pro → 12 j acquis. Employeur a accordé 5 j → rappel 7.
        var in = baseBuilder().joursCpDejaAccordes(new BigDecimal("5")).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("12.00");
        assertThat(r.joursCpRappel()).isEqualByComparingTo("7");
        assertThat(r.verdict()).isEqualTo(Verdict.RAPPEL_SIGNIFICATIF);
    }

    @Test
    void rappelNet_jamaisNegatif_quandEmployeurADejaAccordePlusQueDu() {
        var in = baseBuilder().joursCpDejaAccordes(new BigDecimal("20")).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpRappel()).isEqualByComparingTo("0");
        assertThat(r.verdict()).isEqualTo(Verdict.PAS_DE_RAPPEL);
    }

    @Test
    void rappel_3jours_verdictRappelLimite() {
        // 1 mois maladie non pro = 2 j. Employeur 0 — 2 j est < 5 → LIMITE
        var in = baseBuilder().nombreMoisArret(1).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.joursCpRappel()).isEqualByComparingTo("2.00");
        assertThat(r.verdict()).isEqualTo(Verdict.RAPPEL_LIMITE);
    }

    @Test
    void salarieEnPoste_dateLimiteEgale_24avril2026() {
        var in = baseBuilder().build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.dateLimiteAction()).isEqualTo(LocalDate.of(2026, 4, 24));
        assertThat(r.actionEncoreOuverte()).isTrue();
    }

    @Test
    void salarieSorti_dateLimiteEgaleDateRupturePlus3Ans() {
        var in = baseBuilder()
                .salarieEncoreEnPoste(false)
                .dateRuptureContrat(LocalDate.of(2025, 3, 1))
                .dateCalcul(LocalDate.of(2026, 1, 15))
                .build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.dateLimiteAction()).isEqualTo(LocalDate.of(2028, 3, 1));
        assertThat(r.actionEncoreOuverte()).isTrue();
    }

    @Test
    void salarieEnPoste_dateCalculApres24Avril2026_actionForclose() {
        // Critère d'acceptation : date du calcul = 2026-05-20 → ACTION_FORCLOSE
        var in = baseBuilder().dateCalcul(APRES_FORCLUSION).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.ACTION_FORCLOSE);
        assertThat(r.actionEncoreOuverte()).isFalse();
    }

    @Test
    void salarieSorti_dateCalculApresPrescription_actionForclose() {
        // Rupture 2021-04-15 → prescrit 2024-04-15. Calcul 2026-05-20 → FORCLOSE.
        var in = baseBuilder()
                .salarieEncoreEnPoste(false)
                .dateRuptureContrat(LocalDate.of(2021, 4, 15))
                .dateCalcul(APRES_FORCLUSION)
                .build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.ACTION_FORCLOSE);
        assertThat(r.actionEncoreOuverte()).isFalse();
    }

    @Test
    void valorisationIndicative_methodeDuDixieme() {
        // 6 mois maladie non pro = 12 j. Salaire 3000 €/mois → salaire journalier
        // méthode dixième = 3000 × 12 / 10 / 24 = 150 €/j. 12 × 150 = 1800 €.
        var in = baseBuilder().build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.valorisationIndicativeEur()).isEqualByComparingTo("1800.00");
    }

    @Test
    void basesJuridiques_contientReferencesLoi2024EtL3141() {
        var r = CongesPayesArretMaladieCalculator.compute(baseBuilder().build(), "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("22 avril 2024"))
                .anyMatch(b -> b.contains("L.3141-5-1"))
                .anyMatch(b -> b.contains("L.3245-1"));
    }

    @Test
    void basesJuridiques_atMp_referenceL3141_5SansLimite() {
        var in = baseBuilder().typeArret(TypeArret.ACCIDENT_TRAVAIL_MALADIE_PRO).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L.3141-5") && b.contains("AT/MP"));
    }

    @Test
    void paysBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                CongesPayesArretMaladieCalculator.compute(baseBuilder().build(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void typeArretManquant_throwsIllegalArgument() {
        var in = baseBuilder().typeArret(null).build();
        assertThatThrownBy(() -> CongesPayesArretMaladieCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeArret");
    }

    @Test
    void nombreMoisNul_throwsIllegalArgument() {
        var in = baseBuilder().nombreMoisArret(0).build();
        assertThatThrownBy(() -> CongesPayesArretMaladieCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombreMoisArret");
    }

    @Test
    void nombreMoisNegatif_throwsIllegalArgument() {
        var in = baseBuilder().nombreMoisArret(-3).build();
        assertThatThrownBy(() -> CongesPayesArretMaladieCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salarieSorti_sansDateRupture_throwsIllegalArgument() {
        var in = baseBuilder().salarieEncoreEnPoste(false).dateRuptureContrat(null).build();
        assertThatThrownBy(() -> CongesPayesArretMaladieCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRuptureContrat");
    }

    @Test
    void salaireBrutNegatif_throwsIllegalArgument() {
        var in = baseBuilder().salaireBrutMensuel(new BigDecimal("-100")).build();
        assertThatThrownBy(() -> CongesPayesArretMaladieCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireBrutMensuel");
    }

    @Test
    void joursDejaAccordesNegatif_throwsIllegalArgument() {
        var in = baseBuilder().joursCpDejaAccordes(new BigDecimal("-1")).build();
        assertThatThrownBy(() -> CongesPayesArretMaladieCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursCpDejaAccordes");
    }

    @Test
    void salaireZero_valorisationNulle() {
        // Salaire 0 → valorisation = 0 (acceptable, n'invalide pas le calcul).
        var in = baseBuilder().salaireBrutMensuel(BigDecimal.ZERO).build();
        var r = CongesPayesArretMaladieCalculator.compute(in, "FRANCE");
        assertThat(r.valorisationIndicativeEur()).isEqualByComparingTo("0.00");
        assertThat(r.joursCpAcquis()).isEqualByComparingTo("12.00");
    }
}
