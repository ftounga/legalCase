package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-211-03 : tests unitaires de {@link TribunalFamilleBeMesuresProvisoiresCalculator}.
 */
class TribunalFamilleBeMesuresProvisoiresCalculatorTest {

    @Test
    void compute_violenceFamiliale_urgenceHaute() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        true, false, false,
                        true, true, false);

        assertThat(r.scoreUrgence()).isEqualTo(3);
        assertThat(r.urgenceLevel()).isEqualTo("HAUTE");
        assertThat(r.verdict()).isEqualTo("URGENT_REFERE");
        assertThat(r.mesuresRecommandees()).contains("RESIDENCE_SEPAREE", "CONTRIBUTION_ALIMENTAIRE_PROVISOIRE");
        assertThat(r.baseJuridique()).contains("1280");
    }

    @Test
    void compute_deplacementEnfant_urgenceHaute() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        false, true, false,
                        false, false, true);

        assertThat(r.scoreUrgence()).isEqualTo(3);
        assertThat(r.urgenceLevel()).isEqualTo("HAUTE");
        assertThat(r.verdict()).isEqualTo("URGENT_REFERE");
        assertThat(r.mesuresRecommandees()).contains("AUTORITE_PARENTALE_EXCLUSIVE_TEMPORAIRE");
    }

    @Test
    void compute_dilapidationSeule_urgenceMoyenne() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        false, false, true,
                        false, false, false);

        assertThat(r.scoreUrgence()).isEqualTo(2);
        assertThat(r.urgenceLevel()).isEqualTo("MOYENNE");
        assertThat(r.verdict()).isEqualTo("AUCUNE_MESURE");
    }

    @Test
    void compute_dilapidationEtMesure_normalAudience() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        false, false, true,
                        false, true, false);

        assertThat(r.scoreUrgence()).isEqualTo(2);
        assertThat(r.urgenceLevel()).isEqualTo("MOYENNE");
        assertThat(r.verdict()).isEqualTo("NORMAL_AUDIENCE");
        assertThat(r.mesuresRecommandees()).contains("CONTRIBUTION_ALIMENTAIRE_PROVISOIRE");
    }

    @Test
    void compute_violenceEtDeplacement_scoreMax() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        true, true, true,
                        true, true, true);

        assertThat(r.scoreUrgence()).isEqualTo(8);
        assertThat(r.urgenceLevel()).isEqualTo("HAUTE");
        assertThat(r.verdict()).isEqualTo("URGENT_REFERE");
        assertThat(r.mesuresRecommandees()).hasSize(3);
    }

    @Test
    void compute_aucunIndicateur_throws() {
        assertThatThrownBy(() -> TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                false, false, false, false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aucun");
    }

    @Test
    void compute_besoinSeulSansUrgence_normalAudience() {
        // pas d'urgence, juste un besoin → urgence AUCUNE, mais mesure prévue → NORMAL_AUDIENCE
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        false, false, false,
                        true, false, false);

        assertThat(r.scoreUrgence()).isZero();
        assertThat(r.urgenceLevel()).isEqualTo("AUCUNE");
        assertThat(r.verdict()).isEqualTo("NORMAL_AUDIENCE");
        assertThat(r.mesuresRecommandees()).containsExactly("RESIDENCE_SEPAREE");
    }

    @Test
    void compute_messages_inclusViolence() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        true, false, false,
                        false, false, true);

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Violence"));
    }

    @Test
    void compute_messages_inclusDeplacement() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        false, true, false,
                        false, false, true);

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Déplacement"));
    }

    @Test
    void compute_messages_inclusDilapidation() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        false, false, true,
                        false, true, false);

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Dilapidation"));
    }

    @Test
    void compute_formule_contientScoreEtVerdict() {
        TribunalFamilleBeMesuresProvisoiresResult r =
                TribunalFamilleBeMesuresProvisoiresCalculator.compute(
                        true, false, false,
                        false, true, false);

        assertThat(r.formule()).contains("score").contains("HAUTE").contains("URGENT_REFERE");
    }
}
