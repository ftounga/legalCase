package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-03 : tests unitaires de {@link RegroupementFamilialAnalyzer}.
 * Couvre les 5 verdicts, le calcul du seuil SMIC pondéré, le calcul de la
 * surface habitable requise, l'exclusion APL et la validation des entrées.
 */
class RegroupementFamilialAnalyzerTest {

    private static final double SMIC = RegroupementFamilialAnalyzer.SMIC_MENSUEL_NET_EUR;

    // ── Verdicts nominaux ────────────────────────────────────────────────

    @Test
    void analyze_tousCriteresRemplis_conjoint_retourne_ELIGIBLE() {
        // 24 mois séjour, ressources largement suffisantes, logement 40 m² pour 2 pers (requis 25)
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                24, 2500.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1);

        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_ELIGIBLE);
        assertThat(r.chipsCriteresNonRemplis()).isEmpty();
        assertThat(r.baseJuridique()).contains("L. 434-1");
    }

    @Test
    void analyze_tousCriteresRemplis_typeAutre_retourne_ELIGIBLE_SOUS_RESERVE() {
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                30, 3000.0, 60, 3, RegroupementFamilialAnalyzer.TYPE_AUTRE, 1);

        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_ELIGIBLE_SOUS_RESERVE);
        assertThat(r.chipsCriteresNonRemplis()).isEmpty();
    }

    @Test
    void analyze_sejourInsuffisant_retourne_NON_ELIGIBLE_DELAI() {
        // 12 mois < 18 → prioritaire sur tout le reste
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                12, 2500.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1);

        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_NON_ELIGIBLE_DELAI);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(RegroupementFamilialAnalyzer.CHIP_SEJOUR_INSUFFISANT);
    }

    @Test
    void analyze_ressourcesInsuffisantes_retourne_NON_ELIGIBLE_RESSOURCES() {
        // Séjour OK, ressources 500 € < SMIC requis, logement OK
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                24, 500.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1);

        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_NON_ELIGIBLE_RESSOURCES);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(RegroupementFamilialAnalyzer.CHIP_RESSOURCES_INSUFFISANTES);
    }

    @Test
    void analyze_logementInsuffisant_retourne_NON_ELIGIBLE_LOGEMENT() {
        // Séjour OK, ressources OK, logement 10 m² pour 4 pers (requis 16 + 27 = 43)
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                24, 3000.0, 10, 4, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1);

        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_NON_ELIGIBLE_LOGEMENT);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(RegroupementFamilialAnalyzer.CHIP_LOGEMENT_INSUFFISANT);
    }

    // ── Calcul du seuil de ressources (R. 434-30) ────────────────────────

    @Test
    void ressourcesRequises_1membre_egal_SMIC() {
        // coefficient = 1.0 + 0.2 × 0 = 1.0
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                24, SMIC, 30, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1);

        assertThat(r.ressourcesRequises()).isEqualTo(Math.round(SMIC * 100.0) / 100.0);
        // ressources = SMIC exactement → suffisant (≥)
        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_ELIGIBLE);
    }

    @Test
    void ressourcesRequises_3membres_applique_pondiration_1_4_SMIC() {
        // coefficient = 1.0 + 0.2 × 2 = 1.4
        double attendu = Math.round(SMIC * 1.4 * 100.0) / 100.0;
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                24, attendu, 60, 4, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 3);

        assertThat(r.ressourcesRequises()).isEqualTo(attendu);
        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_ELIGIBLE);
    }

    // ── Calcul de la surface habitable (R. 434-20) ───────────────────────

    @Test
    void surfaceRequise_progression_9m2_par_personne_supplementaire() {
        // 1 pers → 16 ; 2 pers → 25 ; 4 pers → 43
        assertThat(RegroupementFamilialAnalyzer.surfaceRequise(1)).isEqualTo(16);
        assertThat(RegroupementFamilialAnalyzer.surfaceRequise(2)).isEqualTo(25);
        assertThat(RegroupementFamilialAnalyzer.surfaceRequise(4)).isEqualTo(43);

        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                24, 5000.0, 25, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1);
        assertThat(r.surfaceRequise()).isEqualTo(25);
    }

    // ── Exclusion APL / RSA (CE 22 mars 2010 n° 301750) ──────────────────

    @Test
    void analyze_exclusionAPL_ressourcesHorsAllocations_basculeRessourcesInsuffisantes() {
        // Le requérant a 1000 € de salaire + (implicitement) APL. Le seuil net 1-membre
        // est le SMIC. Comme l'APL doit être exclue, seuls les 1000 € comptent → insuffisant.
        RegroupementFamilialResult r = RegroupementFamilialAnalyzer.analyze(
                24, 1000.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1);

        assertThat(r.verdict()).isEqualTo(RegroupementFamilialAnalyzer.VERDICT_NON_ELIGIBLE_RESSOURCES);
        assertThat(r.ressourcesRequises()).isGreaterThan(1000.0);
        assertThat(r.baseJuridique()).contains("301750");
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_dureeNegative_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RegroupementFamilialAnalyzer.analyze(
                -1, 2000.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejourRegulierMois");
    }

    @Test
    void analyze_ressourcesNulles_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RegroupementFamilialAnalyzer.analyze(
                24, 0.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ressourcesMensuellesNettes");
    }

    @Test
    void analyze_logementNul_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RegroupementFamilialAnalyzer.analyze(
                24, 2000.0, 0, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tailleLogementM2");
    }

    @Test
    void analyze_membresHorsBorne_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RegroupementFamilialAnalyzer.analyze(
                24, 2000.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("membresFamilleARegrouper");
        assertThatThrownBy(() -> RegroupementFamilialAnalyzer.analyze(
                24, 2000.0, 40, 2, RegroupementFamilialAnalyzer.TYPE_CONJOINT, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("membresFamilleARegrouper");
    }

    @Test
    void analyze_typeInconnu_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RegroupementFamilialAnalyzer.analyze(
                24, 2000.0, 40, 2, "FRATRIE", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRegroupement");
    }
}
