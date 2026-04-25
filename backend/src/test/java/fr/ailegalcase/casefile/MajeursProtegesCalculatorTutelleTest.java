package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-FA-25-04 : tests unitaires pour le régime TUTELLE
 * (art. 440 al. 3 Cciv) — le plus protecteur des 6 régimes.
 *
 * <p>Couvre l'analyse d'éligibilité (eligible + criteresNonRemplis), la
 * priorité de l'arbre de décision (sauvegarde/habilitation conservés en
 * priorité ; tutelle prioritaire sur curatelles si altertationGrave), le
 * critère pivot altertationGrave, les messages spécifiques tutelle
 * (art. 440 al. 3, art. 432 audition), et la backward compatibility de
 * l'API (12-args et 11-args restent valides).
 */
class MajeursProtegesCalculatorTutelleTest {

    private static final LocalDate DATE_CERT = LocalDate.of(2026, 4, 15);

    // -------------------------------------------------------------------------
    // TUTELLE — art. 440 al. 3 Cciv (cas nominal)
    // -------------------------------------------------------------------------

    @Test
    void compute_tutelle_recommandee_ELEVEE() {
        // tous critères tutelle remplis : altération + cert + altertationGrave
        // + incapaciteQuotidienne + ≥ 2 catégories d'actes + sans consentement
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT", "DECISIONS_SANTE"),
                false, true, false,
                true /* incapaciteGestionQuotidienne */,
                true /* altertationGrave */);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("TUTELLE");
        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(8);
    }

    // -------------------------------------------------------------------------
    // TUTELLE — cas dégradés (eligible = false)
    // -------------------------------------------------------------------------

    @Test
    void compute_tutelle_eligible_false_alterationLegere() {
        // altertationGrave = false → tutelle non éligible (basculer curatelle)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true /* incap quotidienne */,
                false /* altertationGrave = false */);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Altération grave").contains("440 al. 3"));
        // sans altertationGrave mais avec incap quotidienne → curatelle
        // renforcée recommandée (autres critères OK)
        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_RENFORCEE");
    }

    @Test
    void compute_tutelle_eligible_false_certManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                false, null,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Certificat médical").contains("431"));
    }

    @Test
    void compute_tutelle_eligible_false_incapAbsente() {
        // sans incapacité quotidienne → tutelle non éligible
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                false /* PAS d'incap */,
                true);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Incapacité").contains("440 al. 3"));
    }

    @Test
    void compute_tutelle_eligible_false_actesInsuffisants() {
        // une seule catégorie d'actes → critère non rempli
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true,
                true);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("2 catégories").contains("440 al. 3"));
    }

    // -------------------------------------------------------------------------
    // Priorité TUTELLE vs CURATELLE_RENFORCEE
    // -------------------------------------------------------------------------

    @Test
    void compute_tutelle_priorite_surCuratelleRenforcee() {
        // même contexte, seule différence = altertationGrave
        // sans altertationGrave → curatelle renforcée recommandée
        MajeursProtegesResult sansGrave = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                false /* altertationGrave = false */);
        // avec altertationGrave → tutelle recommandée
        MajeursProtegesResult avecGrave = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true /* altertationGrave = true */);

        assertThat(sansGrave.regimeOptimalRecommande()).isEqualTo("CURATELLE_RENFORCEE");
        assertThat(avecGrave.regimeOptimalRecommande()).isEqualTo("TUTELLE");
    }

    @Test
    void compute_tutelle_criteresNonRemplis_listeExplicite() {
        // sans incap + sans altGrave + 1 acte → 3 critères manquants
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false,
                false);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Altération grave").contains("440 al. 3"));
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Incapacité"));
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("2 catégories"));
    }

    @Test
    void compute_tutelle_msg_contient_440_al3() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT", "DECISIONS_SANTE"),
                false, false, false,
                true,
                true);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("TUTELLE").contains("440 al. 3"));
    }

    @Test
    void compute_altertationGrave_propagee_dansResult() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        assertThat(r.altertationGrave()).isTrue();
    }

    @Test
    void compute_altertationGrave_null_traiteCommeFalse() {
        // surcharge 12-args (sans altertationGrave) → traité comme false
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true /* incap quotidienne */);

        assertThat(r.altertationGrave()).isFalse();
        // sans altertationGrave, le régime optimal n'est pas tutelle même si
        // les autres critères sont remplis (curatelle renforcée à la place)
        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_RENFORCEE");
    }

    @Test
    void compute_tutelle_eligible_dérivéDeListe() {
        // eligible = (criteresNonRemplis.isEmpty())
        MajeursProtegesResult ok = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);
        assertThat(ok.eligible()).isTrue();
        assertThat(ok.criteresNonRemplis()).isEmpty();

        MajeursProtegesResult ko = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true,
                true);
        assertThat(ko.eligible()).isFalse();
        assertThat(ko.criteresNonRemplis()).isNotEmpty();
    }

    @Test
    void compute_tutelle_inclutDecisionsMedicales_conserve_eligible() {
        // inclure DECISIONS_SANTE comme 3e catégorie ne casse pas l'éligibilité
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT", "DECISIONS_SANTE"),
                false, false, false,
                true,
                true);

        assertThat(r.eligible()).isTrue();
        assertThat(r.regimeOptimalRecommande()).isEqualTo("TUTELLE");
        // DECISIONS_SANTE → expertise psy recommandée (acte irréversible)
        assertThat(r.expertisePsyComplementaireRecommandee()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Priorité arbre de décision — non régression
    // -------------------------------------------------------------------------

    @Test
    void compute_priorite_arbreDecision() {
        // 1) urgence + altération → SAUVEGARDE même si tutelle critères OK
        MajeursProtegesResult urgent = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                true /* urgence */, false, false,
                true,
                true);
        assertThat(urgent.regimeOptimalRecommande()).isEqualTo("SAUVEGARDE_JUSTICE");

        // 2) consentement + famille proche + alt mentale + non urgent →
        //    HABILITATION (l'habilitation suppose le consentement, qui est
        //    incompatible en pratique avec l'altération grave bloquante,
        //    mais l'arbre de décision la place avant la tutelle)
        MajeursProtegesResult habil = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                true /* consentement */,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);
        assertThat(habil.regimeOptimalRecommande()).isEqualTo("HABILITATION_FAMILIALE");

        // 3) tous critères tutelle, sans urgence ni consentement → TUTELLE
        MajeursProtegesResult tutelle = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);
        assertThat(tutelle.regimeOptimalRecommande()).isEqualTo("TUTELLE");
    }

    @Test
    void compute_dureeProcedure_tutelle_8mois() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        // tutelle = régime long → DELAI_LONG_MOIS = 8
        assertThat(r.delaiProcedureMoisPrevisionnel())
                .isEqualTo(MajeursProtegesCalculator.DELAI_LONG_MOIS);
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(8);
    }

    @Test
    void compute_auditionObligatoire_tutelle_true() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        assertThat(r.auditionPersonneObligatoire()).isTrue();
    }

    @Test
    void compute_tutelle_msg_contientArt432_audition() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        // message audition obligatoire art. 432
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Audition").contains("432"));
    }

    // -------------------------------------------------------------------------
    // Backward compatibility — surcharges 11-args et 12-args
    // -------------------------------------------------------------------------

    @Test
    void backwardCompat_surcharge11Args_traitéeCommeFalse() {
        // signature SF-FA-25-01 (11-args) → incap quotidienne ET altGrave = false
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "FRERE_SOEUR",
                List.of(),
                false, false, true /* isolement → branche tutelle compat */);

        assertThat(r.altertationGrave()).isFalse();
        assertThat(r.incapaciteGestionQuotidienne()).isFalse();
        // branche fallback historique altération mentale + isolement → TUTELLE
        assertThat(r.regimeOptimalRecommande()).isEqualTo("TUTELLE");
    }

    @Test
    void compute_tutelle_msg_repassé_grave_critere_pivot() {
        // message global signale altertationGrave si présent
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Altération grave").contains("440 al. 3"));
    }

    @Test
    void formule_contient_eligibleOuiSiAucunCritereManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        assertThat(r.formule()).contains("Éligibilité : OUI");
    }

    @Test
    void formule_contient_eligibleNonSiCritereManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true,
                true);

        assertThat(r.formule()).contains("Éligibilité : NON");
    }
}
