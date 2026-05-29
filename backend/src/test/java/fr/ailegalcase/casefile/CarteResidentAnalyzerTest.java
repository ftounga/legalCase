package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-23 : tests unitaires de {@link CarteResidentAnalyzer}.
 * Couvre les 6 verdicts (dont INADMISSIBLE prioritaire), les chips de critères
 * non remplis, les atouts et la validation des entrées.
 */
class CarteResidentAnalyzerTest {

    private static final double SMIC = CarteResidentAnalyzer.SMIC_MENSUEL_NET_EUR;

    // ── Verdicts nominaux ────────────────────────────────────────────────

    @Test
    void analyze_tousCriteresRemplis_integrationForte_retourne_ELIGIBLE() {
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                6, "VPF", CarteResidentAnalyzer.INTEGRATION_FORT, 2000.0, false);

        assertThat(r.verdict()).isEqualTo(CarteResidentAnalyzer.VERDICT_ELIGIBLE);
        assertThat(r.chipsCriteresNonRemplis()).isEmpty();
        assertThat(r.baseJuridique()).contains("L. 426-1");
    }

    @Test
    void analyze_tousCriteresRemplis_integrationMoyenne_retourne_ELIGIBLE_SOUS_RESERVE() {
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                7, null, CarteResidentAnalyzer.INTEGRATION_MOYEN, 1800.0, false);

        assertThat(r.verdict()).isEqualTo(CarteResidentAnalyzer.VERDICT_ELIGIBLE_SOUS_RESERVE);
        assertThat(r.chipsCriteresNonRemplis()).isEmpty();
    }

    @Test
    void analyze_sejourQuatreAns_retourne_NON_ELIGIBLE_DELAI() {
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                4, "VPF", CarteResidentAnalyzer.INTEGRATION_FORT, 2000.0, false);

        assertThat(r.verdict()).isEqualTo(CarteResidentAnalyzer.VERDICT_NON_ELIGIBLE_DELAI);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(CarteResidentAnalyzer.CHIP_SEJOUR_INSUFFISANT);
    }

    @Test
    void analyze_integrationFaible_retourne_NON_ELIGIBLE_INTEGRATION() {
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                6, "VPF", CarteResidentAnalyzer.INTEGRATION_FAIBLE, 2000.0, false);

        assertThat(r.verdict()).isEqualTo(CarteResidentAnalyzer.VERDICT_NON_ELIGIBLE_INTEGRATION);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(CarteResidentAnalyzer.CHIP_INTEGRATION_INSUFFISANTE);
    }

    @Test
    void analyze_ressourcesSousSmic_retourne_NON_ELIGIBLE_RESSOURCES() {
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                6, "VPF", CarteResidentAnalyzer.INTEGRATION_FORT, 800.0, false);

        assertThat(r.verdict()).isEqualTo(CarteResidentAnalyzer.VERDICT_NON_ELIGIBLE_RESSOURCES);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(CarteResidentAnalyzer.CHIP_RESSOURCES_INSUFFISANTES);
    }

    @Test
    void analyze_condamnationGrave_retourne_INADMISSIBLE_prioritaire() {
        // Tous les autres critères sont remplis : la condamnation prime.
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                8, "VPF", CarteResidentAnalyzer.INTEGRATION_FORT, 3000.0, true);

        assertThat(r.verdict()).isEqualTo(CarteResidentAnalyzer.VERDICT_INADMISSIBLE);
    }

    // ── Atouts ───────────────────────────────────────────────────────────

    @Test
    void analyze_eligible_listeAtouts_inclutSejourIntegrationRessourcesTitres() {
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                6, "Carte de séjour VPF + salarié", CarteResidentAnalyzer.INTEGRATION_FORT, 2000.0, false);

        assertThat(r.atouts())
                .anyMatch(a -> a.contains("Séjour régulier"))
                .anyMatch(a -> a.contains("Intégration"))
                .anyMatch(a -> a.contains("Ressources"))
                .anyMatch(a -> a.contains("Titres antérieurs"));
    }

    @Test
    void analyze_smicReference_estExposeArrondi() {
        CarteResidentResult r = CarteResidentAnalyzer.analyze(
                6, null, CarteResidentAnalyzer.INTEGRATION_FORT, 2000.0, false);

        assertThat(r.smicMensuelNetReference())
                .isEqualTo(Math.round(SMIC * 100.0) / 100.0);
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_dureeNegative_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> CarteResidentAnalyzer.analyze(
                -1, null, CarteResidentAnalyzer.INTEGRATION_FORT, 2000.0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejourRegulierAnnees");
    }

    @Test
    void analyze_niveauInconnu_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> CarteResidentAnalyzer.analyze(
                6, null, "EXCELLENT", 2000.0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("niveauIntegration");
    }

    @Test
    void analyze_ressourcesNulles_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> CarteResidentAnalyzer.analyze(
                6, null, CarteResidentAnalyzer.INTEGRATION_FORT, 0.0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ressourcesMensuellesNettes");
    }
}
