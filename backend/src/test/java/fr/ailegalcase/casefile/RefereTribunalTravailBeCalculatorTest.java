package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-05 : tests unitaires du calculateur d'éligibilité au référé tribunal
 * du travail BE (CJ art. 584).
 *
 * <p>Couvre les 3 verdicts (REFERE_ELIGIBLE / REFERE_INCERTAIN /
 * REFERE_NON_ELIGIBLE) sur les bornes du score (0-5), chaque condition
 * individuelle non remplie, la présence/absence du squelette de requête,
 * la règle spécifique du motif AUTRE (description > 30 car.) et la validation
 * défensive des inputs requis.</p>
 */
class RefereTribunalTravailBeCalculatorTest {

    private static final LocalDate FAIT = LocalDate.of(2026, 5, 15);
    private static final LocalDate DEMARCHE = LocalDate.of(2026, 5, 16);

    /**
     * Construit une requête complète remplissant les 5 conditions cumulatives
     * (verdict attendu : REFERE_ELIGIBLE, score = 5). Les tests partent de
     * cette base et ne mutent que les champs étudiés.
     */
    private static RefereTribunalTravailBeRequest eligible() {
        return new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.HARCELEMENT,
                "Harcèlement persistant du supérieur N+1 depuis février 2026",
                FAIT,
                DEMARCHE,
                true,                                                   // preuveUrgenceJointe
                "Cessation immédiate des actes de harcèlement",         // mesureProvisoireDemandee
                true,                                                   // perilEnDemeure
                true                                                    // competenceTerritorialeIdentifiee
        );
    }

    // =========================================================================
    // 1. Score = 5 → REFERE_ELIGIBLE + squelette présent + DEPOSER_REQUETE
    // =========================================================================

    @Test
    void scoreCinq_REFERE_ELIGIBLE_avecSquelette() {
        var result = RefereTribunalTravailBeCalculator.compute(eligible());

        assertThat(result.verdict()).isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_ELIGIBLE);
        assertThat(result.scoreConditions()).isEqualTo(5);
        assertThat(result.conditionsNonRemplies()).isEmpty();
        assertThat(result.requeteSquelette()).isNotNull();
        assertThat(result.requeteSquelette()).contains("REQUÊTE EN RÉFÉRÉ");
        assertThat(result.requeteSquelette()).contains("Tribunal du travail");
        assertThat(result.requeteSquelette()).contains("art. 584");
        assertThat(result.requeteSquelette()).contains("3 juillet 1978");
        assertThat(result.requeteSquelette()).contains(FAIT.toString());
        assertThat(result.requeteSquelette()).contains("Cessation immédiate des actes de harcèlement");
        assertThat(result.etapeSuivante())
                .isEqualTo(RefereTribunalTravailBeResult.EtapeSuivante.DEPOSER_REQUETE);
        assertThat(result.baseJuridique()).contains("art. 584").contains("3 juillet 1978");
    }

    // =========================================================================
    // 2. Score = 4 → REFERE_INCERTAIN (borne haute) + squelette + RENFORCER_DOSSIER
    // =========================================================================

    @Test
    void scoreQuatre_borneHaute_REFERE_INCERTAIN_avecSquelette() {
        // Une condition cassée (preuveUrgenceJointe = false) → score 4.
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.SALAIRE_IMPAYE,
                "Salaires de mars et avril 2026 impayés malgré mise en demeure",
                FAIT, DEMARCHE,
                false,                                                  // preuveUrgenceJointe NON
                "Condamnation au paiement provisionnel des salaires dus",
                true, true);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.verdict()).isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_INCERTAIN);
        assertThat(result.scoreConditions()).isEqualTo(4);
        assertThat(result.conditionsNonRemplies())
                .containsExactly(RefereTribunalTravailBeCondition.PEUVE_URGENCE_JOINTE);
        assertThat(result.requeteSquelette()).isNotNull();
        assertThat(result.etapeSuivante())
                .isEqualTo(RefereTribunalTravailBeResult.EtapeSuivante.RENFORCER_DOSSIER);
    }

    // =========================================================================
    // 3. Score = 3 → REFERE_INCERTAIN (borne basse) + squelette
    // =========================================================================

    @Test
    void scoreTrois_borneBasse_REFERE_INCERTAIN_avecSquelette() {
        // Deux conditions cassées → score 3.
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.MODIFICATION_UNILATERALE,
                "Mutation imposée à 200 km sans avenant",
                FAIT, null,
                false,                                                  // preuveUrgenceJointe NON
                "Suspension de la mutation imposée",
                false,                                                  // perilEnDemeure NON
                true);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.verdict()).isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_INCERTAIN);
        assertThat(result.scoreConditions()).isEqualTo(3);
        assertThat(result.conditionsNonRemplies())
                .containsExactlyInAnyOrder(
                        RefereTribunalTravailBeCondition.PEUVE_URGENCE_JOINTE,
                        RefereTribunalTravailBeCondition.PERIL_EN_DEMEURE);
        assertThat(result.requeteSquelette()).isNotNull();
        assertThat(result.etapeSuivante())
                .isEqualTo(RefereTribunalTravailBeResult.EtapeSuivante.RENFORCER_DOSSIER);
    }

    // =========================================================================
    // 4. Score = 2 → REFERE_NON_ELIGIBLE (borne haute) + squelette NULL
    // =========================================================================

    @Test
    void scoreDeux_borneHauteNonEligible_REFERE_NON_ELIGIBLE_sansSquelette() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.SALAIRE_IMPAYE,
                "Salaire impayé d'avril 2026",
                FAIT, null,
                false,                                                  // preuveUrgenceJointe NON
                "Paiement provisionnel demandé",
                false,                                                  // perilEnDemeure NON
                false);                                                 // competence NON
        // urgenceQualifiable OK (motif != AUTRE) + mesure précise OK → score 2.
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.verdict()).isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_NON_ELIGIBLE);
        assertThat(result.scoreConditions()).isEqualTo(2);
        assertThat(result.requeteSquelette()).isNull();
        assertThat(result.etapeSuivante())
                .isEqualTo(RefereTribunalTravailBeResult.EtapeSuivante.ALTERNATIVE_PROCEDURE_FOND);
    }

    // =========================================================================
    // 5. Score = 0 → REFERE_NON_ELIGIBLE + squelette NULL
    // =========================================================================

    @Test
    void scoreZero_REFERE_NON_ELIGIBLE_sansSquelette_avecToutesConditionsListees() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.AUTRE,
                "trop court",                                           // < 30 car. → URGENCE_QUALIFIABLE KO
                FAIT, null,
                false,
                "trop court",                                           // ≤ 10 car. → MESURE KO
                false, false);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.verdict()).isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_NON_ELIGIBLE);
        assertThat(result.scoreConditions()).isEqualTo(0);
        assertThat(result.conditionsNonRemplies())
                .containsExactlyInAnyOrder(
                        RefereTribunalTravailBeCondition.URGENCE_QUALIFIABLE,
                        RefereTribunalTravailBeCondition.PEUVE_URGENCE_JOINTE,
                        RefereTribunalTravailBeCondition.PERIL_EN_DEMEURE,
                        RefereTribunalTravailBeCondition.MESURE_PROVISOIRE_PRECISE,
                        RefereTribunalTravailBeCondition.COMPETENCE_IDENTIFIEE);
        assertThat(result.requeteSquelette()).isNull();
    }

    // =========================================================================
    // 6. motif AUTRE + description ≤ 30 car. → URGENCE_QUALIFIABLE non remplie
    // =========================================================================

    @Test
    void motifAUTRE_descriptionCourte_URGENCE_QUALIFIABLE_nonRemplie() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.AUTRE,
                "Urgence économique",                                   // 19 car. < 30
                FAIT, null,
                true, "Cessation immédiate de l'acte litigieux",
                true, true);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.scoreConditions()).isEqualTo(4);
        assertThat(result.conditionsNonRemplies())
                .containsExactly(RefereTribunalTravailBeCondition.URGENCE_QUALIFIABLE);
        assertThat(result.verdict())
                .isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_INCERTAIN);
    }

    // =========================================================================
    // 7. motif AUTRE + description circonstanciée > 30 car. → URGENCE_QUALIFIABLE OK
    // =========================================================================

    @Test
    void motifAUTRE_descriptionLongue_URGENCE_QUALIFIABLE_remplie() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.AUTRE,
                "Transfert d'entreprise imposé sans information ni concertation préalable",
                FAIT, null,
                true, "Suspension du transfert jusqu'à information complète",
                true, true);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.scoreConditions()).isEqualTo(5);
        assertThat(result.verdict()).isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_ELIGIBLE);
        assertThat(result.conditionsNonRemplies()).isEmpty();
    }

    // =========================================================================
    // 8. mesureProvisoireDemandee ≤ 10 car. → MESURE_PROVISOIRE_PRECISE non remplie
    // =========================================================================

    @Test
    void mesureCourte_MESURE_PROVISOIRE_PRECISE_nonRemplie() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.HARCELEMENT,
                "Harcèlement persistant du supérieur N+1 documenté",
                FAIT, null,
                true, "stop",                                           // 4 car. ≤ 10
                true, true);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.scoreConditions()).isEqualTo(4);
        assertThat(result.conditionsNonRemplies())
                .containsExactly(RefereTribunalTravailBeCondition.MESURE_PROVISOIRE_PRECISE);
        assertThat(result.verdict())
                .isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_INCERTAIN);
    }

    // =========================================================================
    // 9. competenceTerritorialeIdentifiee = false → COMPETENCE_IDENTIFIEE non remplie
    // =========================================================================

    @Test
    void competenceNonIdentifiee_COMPETENCE_IDENTIFIEE_nonRemplie() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.SALAIRE_IMPAYE,
                "Salaires impayés depuis mars 2026 — mise en demeure restée sans réponse",
                FAIT, DEMARCHE,
                true, "Condamnation provisionnelle au paiement des salaires",
                true, false);                                           // competence NON
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.scoreConditions()).isEqualTo(4);
        assertThat(result.conditionsNonRemplies())
                .containsExactly(RefereTribunalTravailBeCondition.COMPETENCE_IDENTIFIEE);
        assertThat(result.requeteSquelette()).isNotNull();
    }

    // =========================================================================
    // 10. preuveUrgenceJointe = false → PEUVE_URGENCE_JOINTE non remplie (isolé)
    // =========================================================================

    @Test
    void preuveAbsente_PEUVE_URGENCE_JOINTE_nonRemplie() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.MODIFICATION_UNILATERALE,
                "Modification unilatérale du lieu de travail (mutation 200 km)",
                FAIT, null,
                false,                                                  // preuve NON
                "Suspension provisoire de la décision de mutation",
                true, true);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.scoreConditions()).isEqualTo(4);
        assertThat(result.conditionsNonRemplies())
                .containsExactly(RefereTribunalTravailBeCondition.PEUVE_URGENCE_JOINTE);
    }

    // =========================================================================
    // 11. perilEnDemeure = false → PERIL_EN_DEMEURE non remplie (isolé)
    // =========================================================================

    @Test
    void perilAbsent_PERIL_EN_DEMEURE_nonRemplie() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.HARCELEMENT,
                "Harcèlement moral du N+1 — plainte conseiller en prévention",
                FAIT, null,
                true, "Cessation des actes de harcèlement et mesures conservatoires",
                false,                                                  // péril NON
                true);
        var result = RefereTribunalTravailBeCalculator.compute(req);

        assertThat(result.scoreConditions()).isEqualTo(4);
        assertThat(result.conditionsNonRemplies())
                .containsExactly(RefereTribunalTravailBeCondition.PERIL_EN_DEMEURE);
    }

    // =========================================================================
    // 12. Défenses en profondeur — request null + champs requis null
    // =========================================================================

    @Test
    void requestNull_leveIllegalArgument() {
        assertThatThrownBy(() -> RefereTribunalTravailBeCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête référé tribunal du travail BE requise");
    }

    @Test
    void motifUrgenceNull_leveIllegalArgument() {
        var req = new RefereTribunalTravailBeRequest(
                null,
                "Description ok",
                FAIT, null, true, "Mesure assez précise pour passer",
                true, true);
        assertThatThrownBy(() -> RefereTribunalTravailBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifUrgence");
    }

    @Test
    void descriptionBlanche_leveIllegalArgument() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.HARCELEMENT,
                "   ",                                                  // blank → required KO
                FAIT, null, true, "Mesure assez précise pour passer",
                true, true);
        assertThatThrownBy(() -> RefereTribunalTravailBeCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifUrgenceDescription");
    }

    // =========================================================================
    // 13. ELIGIBLE → squelette inclut les sections fait/droit/dispositif
    // =========================================================================

    @Test
    void eligible_squelette_inclutLesQuatreSectionsAttendues() {
        var result = RefereTribunalTravailBeCalculator.compute(eligible());
        String s = result.requeteSquelette();
        assertThat(s).contains("EN FAIT");
        assertThat(s).contains("EN DROIT");
        assertThat(s).contains("PAR CES MOTIFS");
        assertThat(s).contains("art. 584");
        assertThat(s).contains("578 et suivants");
        assertThat(s).contains("Loi du 3 juillet 1978");
    }

    // =========================================================================
    // 14. NON_ELIGIBLE → squelette null même si une condition est remplie
    // =========================================================================

    @Test
    void scoreUn_REFERE_NON_ELIGIBLE_sansSquelette() {
        var req = new RefereTribunalTravailBeRequest(
                RefereTribunalTravailBeMotifUrgence.AUTRE,
                "Court",                                                // URGENCE_QUALIFIABLE KO
                FAIT, null,
                false,                                                  // preuve KO
                "stop",                                                 // MESURE KO
                false,                                                  // peril KO
                true);                                                  // competence OK → score 1
        var result = RefereTribunalTravailBeCalculator.compute(req);
        assertThat(result.scoreConditions()).isEqualTo(1);
        assertThat(result.verdict())
                .isEqualTo(RefereTribunalTravailBeResult.Verdict.REFERE_NON_ELIGIBLE);
        assertThat(result.requeteSquelette()).isNull();
        assertThat(result.etapeSuivante())
                .isEqualTo(RefereTribunalTravailBeResult.EtapeSuivante.ALTERNATIVE_PROCEDURE_FOND);
    }
}
