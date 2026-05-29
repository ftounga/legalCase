package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-05 : tests unitaires de {@link VpfLiensPersonnelsAnalyzer}.
 * Couvre le score d'intensité, le critère de durée (5 ans / 3 ans si entrée
 * mineure), l'impact d'une condamnation pénale, les 4 verdicts, les chips et
 * les recommandations, ainsi que la validation des entrées.
 */
class VpfLiensPersonnelsAnalyzerTest {

    // ── Score d'intensité des liens ──────────────────────────────────────

    @Test
    void analyze_calculeScore_selonBareme() {
        // enfants(+2) + conjoint(+2) + parents(+1) + entreeMineur(+2) + FORT(+1) = 8
        // entrée mineure → seuil durée 36 mois ; 40 mois → rempli
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                40, true, true, true, true, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FORT, false);

        assertThat(r.score()).isEqualTo(8);
        assertThat(r.dureeResidenceRemplie()).isTrue();
        assertThat(r.verdict()).isEqualTo(VpfLiensPersonnelsAnalyzer.VERDICT_ELIGIBLE_PROBABLE);
        assertThat(r.baseJuridique()).contains("L. 423-23");
    }

    @Test
    void analyze_condamnationPenale_retire2Points() {
        // conjoint(+2) + condamnation(-2) = 0
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                72, false, false, true, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_MOYEN, true);

        assertThat(r.score()).isEqualTo(0);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(VpfLiensPersonnelsAnalyzer.CHIP_CONDAMNATION_PENALE);
    }

    // ── Critère de durée ─────────────────────────────────────────────────

    @Test
    void analyze_duree5ans_majeur_estRemplie() {
        // pas entrée mineure → seuil 60 mois ; 60 mois exactement → rempli
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                60, false, true, true, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FORT, false);

        assertThat(r.dureeResidenceRemplie()).isTrue();
        assertThat(r.chipsCriteresNonRemplis())
                .doesNotContain(VpfLiensPersonnelsAnalyzer.CHIP_DUREE_RESIDENCE_INSUFFISANTE);
    }

    @Test
    void analyze_duree3ans_entreeMineure_estRemplie() {
        // entrée mineure → seuil 36 mois ; 36 mois → rempli alors que < 60
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                36, true, true, false, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_MOYEN, false);

        assertThat(r.dureeResidenceRemplie()).isTrue();
    }

    @Test
    void analyze_dureeInsuffisante_majeur_chipEtConsolider() {
        // 24 mois < 60, pas entrée mineure → durée non remplie
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                24, false, true, true, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FORT, false);

        assertThat(r.dureeResidenceRemplie()).isFalse();
        assertThat(r.chipsCriteresNonRemplis())
                .contains(VpfLiensPersonnelsAnalyzer.CHIP_DUREE_RESIDENCE_INSUFFISANTE);
        assertThat(r.verdict()).isEqualTo(VpfLiensPersonnelsAnalyzer.VERDICT_DOSSIER_A_CONSOLIDER);
    }

    // ── Verdicts ─────────────────────────────────────────────────────────

    @Test
    void analyze_dureeOK_scoreIntense_retourne_ELIGIBLE_PROBABLE() {
        // enfants(+2) + conjoint(+2) = 4 ≥ seuil intenses, durée 72 mois OK
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                72, false, true, true, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_MOYEN, false);

        assertThat(r.score()).isEqualTo(4);
        assertThat(r.verdict()).isEqualTo(VpfLiensPersonnelsAnalyzer.VERDICT_ELIGIBLE_PROBABLE);
    }

    @Test
    void analyze_dureeOK_scoreMinimal_retourne_ELIGIBLE_SOUS_RESERVE() {
        // conjoint(+2) = 2 (entre seuil minimal et intenses), durée OK
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                72, false, false, true, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FAIBLE, false);

        assertThat(r.score()).isEqualTo(2);
        assertThat(r.verdict()).isEqualTo(VpfLiensPersonnelsAnalyzer.VERDICT_ELIGIBLE_SOUS_RESERVE);
    }

    @Test
    void analyze_dureeOK_scoreFaible_retourne_DOSSIER_A_CONSOLIDER() {
        // parents(+1) = 1 < seuil minimal, durée OK
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                72, false, false, false, true, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FAIBLE, false);

        assertThat(r.score()).isEqualTo(1);
        assertThat(r.verdict()).isEqualTo(VpfLiensPersonnelsAnalyzer.VERDICT_DOSSIER_A_CONSOLIDER);
        assertThat(r.chipsCriteresNonRemplis())
                .contains(VpfLiensPersonnelsAnalyzer.CHIP_INTENSITE_LIENS_FAIBLE);
    }

    @Test
    void analyze_condamnationSansLienResiduel_retourne_NON_ELIGIBLE() {
        // condamnation(-2) sans aucun lien positif → score -2 ≤ 0
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                72, false, false, false, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FAIBLE, true);

        assertThat(r.score()).isEqualTo(-2);
        assertThat(r.verdict()).isEqualTo(VpfLiensPersonnelsAnalyzer.VERDICT_NON_ELIGIBLE);
    }

    // ── Recommandations + attaches pays d'origine ────────────────────────

    @Test
    void analyze_dossierIncomplet_produitRecommandations() {
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                12, false, true, false, false, "Parents et fratrie restés au pays",
                VpfLiensPersonnelsAnalyzer.NIVEAU_FAIBLE, false);

        assertThat(r.recommandations()).isNotEmpty();
        assertThat(r.recommandations())
                .anySatisfy(s -> assertThat(s).containsIgnoringCase("présence"));
        assertThat(r.chipsCriteresNonRemplis())
                .contains(VpfLiensPersonnelsAnalyzer.CHIP_ATTACHES_PAYS_ORIGINE);
    }

    @Test
    void analyze_dossierComplet_recommandationsNonVides() {
        VpfLiensPersonnelsResult r = VpfLiensPersonnelsAnalyzer.analyze(
                72, false, true, true, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FORT, false);

        assertThat(r.recommandations()).isNotEmpty();
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_dureeNegative_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> VpfLiensPersonnelsAnalyzer.analyze(
                -1, false, true, true, false, null,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FORT, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeResidenceFranceMois");
    }

    @Test
    void analyze_niveauInconnu_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> VpfLiensPersonnelsAnalyzer.analyze(
                72, false, true, true, false, null, "EXCELLENT", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("niveauIntegration");
    }

    @Test
    void analyze_situationTropLongue_lanceIllegalArgumentException() {
        String tropLong = "x".repeat(301);
        assertThatThrownBy(() -> VpfLiensPersonnelsAnalyzer.analyze(
                72, false, true, true, false, tropLong,
                VpfLiensPersonnelsAnalyzer.NIVEAU_FORT, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("situationFamilialeAlEtranger");
    }
}
