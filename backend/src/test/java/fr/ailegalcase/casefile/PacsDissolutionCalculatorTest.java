package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.PacsDissolutionCalculator.CreanceType;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.ModeDissolution;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.RegimeBiens;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PacsDissolutionCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_unilateraleAvecNotification_dissolutionValide() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 15),
                ModeDissolution.DECLARATION_UNILATERALE,
                LocalDate.of(2026, 1, 10),
                6,
                RegimeBiens.SEPARATION_BIENS,
                false,
                List.of(CreanceType.AUCUNE),
                0,
                LocalDate.of(2026, 1, 20),
                clockAt(today));

        assertThat(r.dissolutionValide()).isTrue();
        assertThat(r.delaiNotificationOk()).isTrue();
        assertThat(r.dureeUnionEligibleCreances()).isTrue();
        assertThat(r.delaiPrescriptionAnnees()).isEqualTo(5);
        assertThat(r.baseJuridique()).contains("515-7").contains("515-7-1").contains("515-8");
    }

    @Test
    void compute_unilateraleSansNotification_dissolutionInvalide() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 15),
                ModeDissolution.DECLARATION_UNILATERALE,
                LocalDate.of(2026, 1, 10),
                6,
                RegimeBiens.SEPARATION_BIENS,
                false,
                List.of(),
                0,
                null,
                clockAt(today));

        assertThat(r.dissolutionValide()).isFalse();
        assertThat(r.delaiNotificationOk()).isFalse();
    }

    @Test
    void compute_unilateraleNotificationAvantDissolution_dissolutionInvalide() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 15),
                ModeDissolution.DECLARATION_UNILATERALE,
                LocalDate.of(2026, 2, 1),
                6,
                RegimeBiens.SEPARATION_BIENS,
                false,
                List.of(),
                0,
                LocalDate.of(2026, 1, 20),
                clockAt(today));

        assertThat(r.dissolutionValide()).isFalse();
        assertThat(r.delaiNotificationOk()).isFalse();
    }

    @Test
    void compute_conjointe_dissolutionValide_sansNotificationRequise() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2018, 6, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                7,
                RegimeBiens.INDIVISION_AMENAGEE,
                false,
                List.of(),
                0,
                null,
                clockAt(today));

        assertThat(r.dissolutionValide()).isTrue();
        assertThat(r.delaiNotificationOk()).isTrue();
    }

    @Test
    void compute_mariageEtDeces_dissolutionAutomatique() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult mp = PacsDissolutionCalculator.compute(
                LocalDate.of(2018, 6, 1), ModeDissolution.MARIAGE_PARTENAIRES,
                LocalDate.of(2026, 1, 10), 7, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null, clockAt(today));
        PacsDissolutionResult mt = PacsDissolutionCalculator.compute(
                LocalDate.of(2018, 6, 1), ModeDissolution.MARIAGE_TIERS,
                LocalDate.of(2026, 1, 10), 7, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null, clockAt(today));
        PacsDissolutionResult d = PacsDissolutionCalculator.compute(
                LocalDate.of(2018, 6, 1), ModeDissolution.DECES,
                LocalDate.of(2026, 1, 10), 7, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null, clockAt(today));

        assertThat(mp.dissolutionValide()).isTrue();
        assertThat(mt.dissolutionValide()).isTrue();
        assertThat(d.dissolutionValide()).isTrue();
    }

    @Test
    void compute_dureeUnionMoinsUnAn_creancesNonEligibles_etAucuneVisible() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2025, 6, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                0,
                RegimeBiens.INDIVISION_AMENAGEE,
                true,
                List.of(CreanceType.CONTRIBUTION_DESEQUILIBRE,
                        CreanceType.INVESTISSEMENT_BIEN_PROPRE),
                1,
                null,
                clockAt(today));

        assertThat(r.dureeUnionEligibleCreances()).isFalse();
        assertThat(r.creancesPotentielleVisibles()).isEmpty();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("< 1 an"));
    }

    @Test
    void compute_scoreContentieux_tousFacteurs() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // patrimoine (+30) + 2 créances (+25) + durée 5 ans (+20) + indivision (+15) + 1 enfant (+10) = 100
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5,
                RegimeBiens.INDIVISION_AMENAGEE,
                true,
                List.of(CreanceType.CONTRIBUTION_DESEQUILIBRE,
                        CreanceType.INVESTISSEMENT_BIEN_PROPRE),
                1,
                null,
                clockAt(today));

        assertThat(r.scoreCreancesProbables()).isEqualTo(100);
        assertThat(r.verdictRecommandation()).isEqualTo("CONTENTIEUX_INEVITABLE");
    }

    @Test
    void compute_scoreMediation_intervalle40_69() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // patrimoine (+30) + indivision (+15) = 45
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2025, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                1,
                RegimeBiens.INDIVISION_AMENAGEE,
                true,
                List.of(CreanceType.AUCUNE),
                0,
                null,
                clockAt(today));

        assertThat(r.scoreCreancesProbables()).isEqualTo(45);
        assertThat(r.verdictRecommandation()).isEqualTo("MEDIATION_OU_JAF");
    }

    @Test
    void compute_scoreLiquidationAmiable_intervalle1_39() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // 1 enfant (+10) seulement = 10
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2025, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                1,
                RegimeBiens.SEPARATION_BIENS,
                false,
                List.of(),
                1,
                null,
                clockAt(today));

        assertThat(r.scoreCreancesProbables()).isEqualTo(10);
        assertThat(r.verdictRecommandation()).isEqualTo("LIQUIDATION_AMIABLE");
    }

    @Test
    void compute_scoreZero_verdictRien() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2025, 6, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                0,
                RegimeBiens.SEPARATION_BIENS,
                false,
                List.of(CreanceType.AUCUNE),
                0,
                null,
                clockAt(today));

        assertThat(r.scoreCreancesProbables()).isEqualTo(0);
        assertThat(r.verdictRecommandation()).isEqualTo("RIEN_A_FAIRE");
    }

    @Test
    void compute_creancesAvecAucune_filtreesDeListeVisible() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5,
                RegimeBiens.INDIVISION_AMENAGEE,
                false,
                List.of(CreanceType.CONTRIBUTION_DESEQUILIBRE,
                        CreanceType.AUCUNE,
                        CreanceType.CONTRIBUTION_DESEQUILIBRE),
                0,
                null,
                clockAt(today));

        assertThat(r.creancesPotentielleVisibles())
                .containsExactly(CreanceType.CONTRIBUTION_DESEQUILIBRE);
    }

    @Test
    void compute_singleCreance_pasDeBonusMultiple() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        // patrimoine (+30) + durée >=3 ans (+20) + indivision (+15) = 65
        // + 1 créance unique (pas de +25)
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5,
                RegimeBiens.INDIVISION_AMENAGEE,
                true,
                List.of(CreanceType.CONTRIBUTION_DESEQUILIBRE),
                0,
                null,
                clockAt(today));

        assertThat(r.scoreCreancesProbables()).isEqualTo(65);
        assertThat(r.verdictRecommandation()).isEqualTo("MEDIATION_OU_JAF");
    }

    @Test
    void compute_messages_contiennentPrescription5Ans() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5,
                RegimeBiens.INDIVISION_AMENAGEE,
                true,
                List.of(CreanceType.CONTRIBUTION_DESEQUILIBRE),
                1,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Prescription"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("5 ans"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("2224"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("prestation compensatoire"));
    }

    @Test
    void compute_messages_unilateraleSansNotificationContientAvertissement() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_UNILATERALE,
                LocalDate.of(2026, 1, 10),
                5,
                RegimeBiens.SEPARATION_BIENS,
                false,
                List.of(),
                0,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("signification"));
    }

    @Test
    void compute_indivisionParDefaut_messageDedie() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2005, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                21,
                RegimeBiens.INDIVISION_PAR_DEFAUT,
                true,
                List.of(),
                0,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("avant 2007"));
    }

    @Test
    void compute_formule_contientScoreEtVerdict() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        PacsDissolutionResult r = PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5,
                RegimeBiens.INDIVISION_AMENAGEE,
                true,
                List.of(CreanceType.CONTRIBUTION_DESEQUILIBRE,
                        CreanceType.INVESTISSEMENT_BIEN_PROPRE),
                1,
                null,
                clockAt(today));

        assertThat(r.formule()).contains("/100").contains("CONTENTIEUX_INEVITABLE")
                .contains("515-7").contains("DECLARATION_CONJOINTE")
                .contains("2224");
    }

    @Test
    void compute_dateConclusionFuture_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> PacsDissolutionCalculator.compute(
                LocalDate.of(2027, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5,
                RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateConclusionPacs");
    }

    @Test
    void compute_dateDissolutionFuture_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2027, 1, 10),
                5, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDissolution");
    }

    @Test
    void compute_dissolutionAvantConclusion_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 6, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2019, 1, 10),
                5, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("antérieure");
    }

    @Test
    void compute_modeDissolutionNull_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                null,
                LocalDate.of(2026, 1, 10),
                5, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modeDissolution");
    }

    @Test
    void compute_regimeBiensNull_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5, null,
                false, List.of(), 0, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regimeBiens");
    }

    @Test
    void compute_dureeUnionNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                -1, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), 0, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeUnionAnnees");
    }

    @Test
    void compute_enfantsCommunsNegatif_throws() {
        LocalDate today = LocalDate.of(2026, 4, 25);
        assertThatThrownBy(() -> PacsDissolutionCalculator.compute(
                LocalDate.of(2020, 1, 1),
                ModeDissolution.DECLARATION_CONJOINTE,
                LocalDate.of(2026, 1, 10),
                5, RegimeBiens.SEPARATION_BIENS,
                false, List.of(), -1, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enfantsCommuns");
    }
}
