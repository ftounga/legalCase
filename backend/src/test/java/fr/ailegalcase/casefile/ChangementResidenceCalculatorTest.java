package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-19-03 : tests unitaires de ChangementResidenceCalculator.
 *
 * <p>Couvre les 5 contributions du score (info préalable, raison, distance,
 * consentement, scolarité), les paliers de distance, les seuils de verdict,
 * les indicateurs dérivés (expertise psy, délai préavis, obligation info)
 * et les erreurs de validation.
 */
class ChangementResidenceCalculatorTest {

    private static final LocalDate D = LocalDate.of(2026, 9, 1);

    @Test
    void compute_nominal_moyenne_score60() {
        // info 30j (+30) + raison TRAVAIL (+20) + distance 250km (+10)
        // + scolarité impactée (0) + pas consentement (0) = 60
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 250, "TRAVAIL",
                false, true, 30,
                "ALTERNEE",
                List.of(8, 12),
                true, true);

        assertThat(r.scoreAcceptabilite()).isEqualTo(60);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
        assertThat(r.obligationInformationRespectee()).isTrue();
        assertThat(r.delaiPreavisLegalOk()).isTrue();
        assertThat(r.baseJuridique()).contains("373-2");
    }

    @Test
    void compute_tousLesCriteres_eleve_score100Plafonne() {
        // info 60j (+30) + TRAVAIL (+20) + 50 km (+20) + consentement (+20)
        // + pas d'impact scolarité (+10) = 100
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                true, true, 60,
                "EXCLUSIVE_DEMANDEUR",
                List.of(5, 7),
                false, false);

        assertThat(r.scoreAcceptabilite()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.expertisePsyEnfantRecommandee()).isFalse();
    }

    @Test
    void compute_aucunCritere_faible_score0() {
        // pas info (0) + AUTRE (0) + 500 km (0) + pas consentement (0)
        // + scolarité impactée (0) = 0
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 500, "AUTRE",
                false, false, 0,
                "ALTERNEE",
                List.of(),
                true, false);

        assertThat(r.scoreAcceptabilite()).isEqualTo(0);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.obligationInformationRespectee()).isFalse();
        assertThat(r.delaiPreavisLegalOk()).isFalse();
        assertThat(r.expertisePsyEnfantRecommandee()).isTrue(); // > 300 km
    }

    @Test
    void compute_distanceProche_lt100km_plus20() {
        // info 30j (30) + AUTRE (0) + 80 km (+20) + pas consentement (0)
        // + scolarité impactée (0) = 50
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 80, "AUTRE",
                false, true, 30,
                "ALTERNEE",
                List.of(8),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(50);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_distanceMoyenne_100to300km_plus10() {
        // info 30j (30) + AUTRE (0) + 250 km (+10) + pas consentement (0)
        // + scolarité impactée (0) = 40
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 250, "AUTRE",
                false, true, 30,
                "ALTERNEE",
                List.of(8),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(40);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_distanceLointaine_gt300km_plus0_etExpertiseRecommandee() {
        // info 30j (30) + AUTRE (0) + 350 km (0) = 30 → FAIBLE
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 350, "AUTRE",
                false, true, 30,
                "ALTERNEE",
                List.of(8),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(30);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.expertisePsyEnfantRecommandee()).isTrue();
    }

    @Test
    void compute_distanceExactly100km_traiteeCommeMoyenne() {
        // 100 km est inclus dans 100-300 → +10
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 100, "AUTRE",
                false, false, 0,
                "ALTERNEE", List.of(),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(10);
    }

    @Test
    void compute_distanceExactly300km_traiteeCommeMoyenne() {
        // 300 km est inclus dans 100-300 → +10
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 300, "AUTRE",
                false, false, 0,
                "ALTERNEE", List.of(),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(10);
    }

    @Test
    void compute_distance301km_traiteeCommeLointaine() {
        // 301 km > 300 → 0
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 301, "AUTRE",
                false, false, 0,
                "ALTERNEE", List.of(),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(0);
    }

    @Test
    void compute_obligationNonRespectee_delaiInsuffisant() {
        // info=true mais 20 jours → obligation non respectée
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, 20,
                "ALTERNEE", List.of(8),
                false, false);
        assertThat(r.obligationInformationRespectee()).isFalse();
        assertThat(r.delaiPreavisLegalOk()).isFalse();
        // pas de +30 sur info
        // raison TRAVAIL (+20) + 50 km (+20) + pas scolarité (+10) = 50
        assertThat(r.scoreAcceptabilite()).isEqualTo(50);
    }

    @Test
    void compute_obligationNonRespectee_pasInformePrealablement() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, false, 90,
                "ALTERNEE", List.of(8),
                false, false);
        // delai 90 mais informePrealablement=false → obligation NOK
        assertThat(r.obligationInformationRespectee()).isFalse();
        assertThat(r.delaiPreavisLegalOk()).isFalse();
    }

    @Test
    void compute_consentement_ajoute20() {
        // info 30j (30) + AUTRE (0) + 350 km (0) + consentement (+20) = 50
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 350, "AUTRE",
                true, true, 30,
                "EXCLUSIVE_DEMANDEUR",
                List.of(),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(50);
    }

    @Test
    void compute_pasImpactScolarite_ajoute10() {
        // info 30j (30) + AUTRE (0) + 500 km (0) + pas consentement (0)
        // + pas scolarité (+10) = 40
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 500, "AUTRE",
                false, true, 30,
                "ALTERNEE",
                List.of(),
                false, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(40);
    }

    @Test
    void compute_expertisePsyRecommandee_distanceGrande() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 400, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(5),
                false, false);
        assertThat(r.expertisePsyEnfantRecommandee()).isTrue();
    }

    @Test
    void compute_expertisePsyRecommandee_tousEnfantsAgesPlusDe10() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(11, 13),
                false, false);
        assertThat(r.expertisePsyEnfantRecommandee()).isTrue();
    }

    @Test
    void compute_expertisePsyNonRecommandee_distanceCourteEtEnfantsJeunes() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 100, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(5, 7),
                false, false);
        assertThat(r.expertisePsyEnfantRecommandee()).isFalse();
    }

    @Test
    void compute_expertisePsyNonRecommandee_unEnfantJeune() {
        // 10 ans NON > 10 → l'enfant de 10 ans empêche la condition
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(10, 14),
                false, false);
        assertThat(r.expertisePsyEnfantRecommandee()).isFalse();
    }

    @Test
    void compute_expertisePsyNonRecommandee_ageEnfantsVide() {
        // pas d'enfants → uniquement la condition distance compte
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 100, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(),
                false, false);
        assertThat(r.expertisePsyEnfantRecommandee()).isFalse();
    }

    @Test
    void compute_seuilEleve_70_borneInferieure() {
        // info 30j (30) + TRAVAIL (+20) + 50 km (+20) = 70 → ELEVEE
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(70);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_seuilMoyenne_40_borneInferieure() {
        // info 30j (30) + AUTRE (0) + 250 km (+10) = 40 → MOYENNE
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 250, "AUTRE",
                false, true, 30,
                "ALTERNEE", List.of(),
                true, false);
        assertThat(r.scoreAcceptabilite()).isEqualTo(40);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_messages_contiennentObligationInformation() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(),
                false, false);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("373-2"));
    }

    @Test
    void compute_messages_alerteSiObligationNonRespectee() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, false, 5,
                "ALTERNEE", List.of(),
                false, false);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("non respectée"));
    }

    @Test
    void compute_messages_alerteSiResidenceAlterneeEtDistance() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 200, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(),
                false, false);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("alternée"));
    }

    @Test
    void compute_formuleContientDistanceEtScore() {
        ChangementResidenceResult r = ChangementResidenceCalculator.compute(
                D, 250, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(8, 12),
                true, true);
        assertThat(r.formule()).contains("250 km").contains("60")
                .contains("MOYENNE");
    }

    @Test
    void compute_dateChangementNull_throws() {
        assertThatThrownBy(() -> ChangementResidenceCalculator.compute(
                null, 50, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateChangementPrevu");
    }

    @Test
    void compute_distanceNegative_throws() {
        assertThatThrownBy(() -> ChangementResidenceCalculator.compute(
                D, -1, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distanceKm");
    }

    @Test
    void compute_raisonInvalide_throws() {
        assertThatThrownBy(() -> ChangementResidenceCalculator.compute(
                D, 50, "INVALIDE",
                false, true, 30,
                "ALTERNEE", List.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raisonChangement");
    }

    @Test
    void compute_modeResidenceInvalide_throws() {
        assertThatThrownBy(() -> ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, 30,
                "INVALIDE", List.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modeResidenceActuel");
    }

    @Test
    void compute_delaiNegatif_throws() {
        assertThatThrownBy(() -> ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, -1,
                "ALTERNEE", List.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delaiInformationJours");
    }

    @Test
    void compute_ageEnfantInvalide_throws() {
        assertThatThrownBy(() -> ChangementResidenceCalculator.compute(
                D, 50, "TRAVAIL",
                false, true, 30,
                "ALTERNEE", List.of(18), false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageEnfants");
    }
}
