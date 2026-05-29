package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-39 : tests unitaires de {@link UeEeeSuisseSejourAnalyzer}.
 * Couvre le droit automatique 3 mois, le droit permanent ≥ 5 ans, la carte
 * membre de famille obligatoire, les conditions selon l'activité, le titre et la
 * base juridique.
 */
class UeEeeSuisseSejourAnalyzerTest {

    // ── Droit de séjour automatique 3 mois ───────────────────────────────

    @Test
    void analyze_citoyenUE_moinsDe5Ans_droitAuto3MoisTrue_permanentFalse() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Italienne", true, false, 24,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_SALARIE);

        assertThat(r.droitSejourAutomatique3Mois()).isTrue();
        assertThat(r.droitSejourPlus5Ans()).isFalse();
        assertThat(r.titreObtenu())
                .isEqualTo(UeEeeSuisseSejourAnalyzer.TITRE_ATTESTATION_ENREGISTREMENT);
        assertThat(r.situationMembreNonUE()).isNull();
    }

    @Test
    void analyze_nonCitoyenUE_droitAuto3MoisFalse() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Algérienne", false, false, 12,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_SALARIE);

        assertThat(r.droitSejourAutomatique3Mois()).isFalse();
    }

    // ── Droit de séjour permanent (≥ 5 ans, art. 16) ─────────────────────

    @Test
    void analyze_citoyenUE_60MoisAvecActivite_droitPermanentTrue() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Espagnole", true, false, 60,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_INDEPENDANT);

        assertThat(r.droitSejourPlus5Ans()).isTrue();
        assertThat(r.conditionsRespectees())
                .anyMatch(c -> c.contains("PERMANENT"));
    }

    @Test
    void analyze_60MoisSansActiviteNiRessources_droitPermanentFalse() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Portugaise", true, false, 72,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_SANS_ACTIVITE_RESSOURCES_SUFFISANTES);

        assertThat(r.droitSejourPlus5Ans()).isFalse();
        assertThat(r.conditionsRespectees())
                .anyMatch(c -> c.contains("n'est pas constitué"));
    }

    // ── Membre de famille ressortissant pays tiers ───────────────────────

    @Test
    void analyze_membreFamilleNonUE_carteObligatoire_etSituationRenseignee() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Marocaine", false, true, 12,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_SANS_ACTIVITE_RESSOURCES_SUFFISANTES);

        assertThat(r.titreObtenu())
                .isEqualTo(UeEeeSuisseSejourAnalyzer.TITRE_CARTE_SEJOUR_MEMBRE_FAMILLE);
        assertThat(r.situationMembreNonUE()).isNotNull();
        assertThat(r.situationMembreNonUE()).contains("art. 10");
    }

    // ── Conditions selon l'activité ──────────────────────────────────────

    @Test
    void analyze_etudiant_conditionRessourcesEtAssuranceMaladie() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Allemande", true, false, 18,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_ETUDIANT);

        assertThat(r.conditionsRespectees())
                .anyMatch(c -> c.contains("Étudiant"));
    }

    @Test
    void analyze_salarie_droitSansConditionRessources() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Belge", true, false, 6,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_SALARIE);

        assertThat(r.conditionsRespectees())
                .anyMatch(c -> c.contains("salariée"));
    }

    // ── Base juridique ───────────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_citeDirective2004_38_etCeseda() {
        UeEeeSuisseSejourResult r = UeEeeSuisseSejourAnalyzer.analyze(
                "Suisse", true, false, 12,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_RETRAITE);

        assertThat(r.baseJuridique())
                .contains("2004/38")
                .contains("L. 233-1");
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_dureeNegative_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> UeEeeSuisseSejourAnalyzer.analyze(
                "Italienne", true, false, -1,
                UeEeeSuisseSejourAnalyzer.ACTIVITE_SALARIE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeSejourMois");
    }

    @Test
    void analyze_activiteInconnue_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> UeEeeSuisseSejourAnalyzer.analyze(
                "Italienne", true, false, 12, "CHOMEUR"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activiteProfessionnelle");
    }
}
