package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-207-02 : tests unitaires du calculateur de checklist C4 ONEM (9 cas
 * mini-spec — toutes les branches du verdict + lettre rectificative +
 * frontières).
 */
class C4OnemChecklistCalculatorTest {

    private static final LocalDate ENTREE = LocalDate.of(2020, 1, 15);
    private static final LocalDate SORTIE = LocalDate.of(2026, 4, 30);
    private static final BigDecimal SALAIRE = new BigDecimal("3450.75");

    /**
     * Construit une requête complète et conforme (toutes mentions présentes,
     * fauteGraveMentionnee=false). Les tests partent de cette base et ne
     * mutent que les champs qu'ils étudient.
     */
    private static C4OnemChecklistRequest complete() {
        return new C4OnemChecklistRequest(
                "ACME SA",
                "0123456789",
                "Dupont Jean",
                "85010112345",
                ENTREE,
                SORTIE,
                "1",
                "Licenciement pour réorganisation économique",
                false,
                30,
                SALAIRE);
    }

    /**
     * Construit une requête avec un seul champ modifié — wrapper léger pour
     * améliorer la lisibilité des tests.
     */
    private static C4OnemChecklistRequest with(C4OnemChecklistRequest base,
                                               java.util.function.Function<C4OnemChecklistRequest,
                                                       C4OnemChecklistRequest> mutator) {
        return mutator.apply(base);
    }

    // =========================================================================
    // 1. CONFORME — toutes mentions présentes, pas de faute grave
    // =========================================================================

    @Test
    void toutesMentionsPresentes_pasFauteGrave_CONFORME() {
        var result = C4OnemChecklistCalculator.compute(complete());
        assertThat(result.verdict()).isEqualTo(C4OnemChecklistResult.Verdict.CONFORME);
        assertThat(result.mentionsManquantes()).isEmpty();
        assertThat(result.fauteGraveDetectee()).isFalse();
        assertThat(result.exclusionOnemRange()).isNull();
        assertThat(result.lettreRectificativeProposee()).isNull();
        assertThat(result.etapeSuivante()).isEqualTo(C4OnemChecklistResult.EtapeSuivante.AUCUNE);
        assertThat(result.baseJuridique()).contains("25 novembre 1991").contains("art. 92").contains("art. 144");
    }

    // =========================================================================
    // 2. NON_CONFORME — numéro BCE absent
    // =========================================================================

    @Test
    void numeroBceNull_NON_CONFORME_avecNumeroBceDansMentionsManquantes() {
        var req = new C4OnemChecklistRequest(
                "ACME SA", null, "Dupont", null, ENTREE, SORTIE, "1",
                "Licenciement économique", false, 30, SALAIRE);
        var result = C4OnemChecklistCalculator.compute(req);
        assertThat(result.verdict()).isEqualTo(C4OnemChecklistResult.Verdict.NON_CONFORME);
        assertThat(result.mentionsManquantes()).contains(C4OnemChecklistMention.NUMERO_BCE);
        assertThat(result.lettreRectificativeProposee()).isNotNull().contains("numéro BCE");
        assertThat(result.etapeSuivante())
                .isEqualTo(C4OnemChecklistResult.EtapeSuivante.RECTIFICATION_AUPRES_EMPLOYEUR);
    }

    // =========================================================================
    // 3. dateSortie < dateEntree → 400 (IllegalArgumentException côté calculator).
    //     (cohérence dure — au-delà de la mention PERIODE_OCCUPATION_COHERENTE).
    // =========================================================================

    @Test
    void dateSortieAvantDateEntree_leve400() {
        var req = new C4OnemChecklistRequest(
                "ACME SA", "0123456789", "Dupont", "85010112345",
                ENTREE, ENTREE.minusDays(1), "1", "Licenciement éco", false, 30, SALAIRE);
        assertThatThrownBy(() -> C4OnemChecklistCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date de sortie antérieure");
    }

    // =========================================================================
    // 4. RISQUE_EXCLUSION_FAUTE_GRAVE — fauteGraveMentionnee=true + tout OK
    // =========================================================================

    @Test
    void fauteGraveTrue_toutOK_RISQUE_EXCLUSION_avecRange4_52_etCONTESTATION() {
        // Toutes les autres mentions OK ; fauteGraveMentionnee=true.
        // preavisPresteJours peut être null car la mention PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE
        // ne s'applique pas en cas de faute grave.
        var req = new C4OnemChecklistRequest(
                "ACME SA", "0123456789", "Dupont", "85010112345",
                ENTREE, SORTIE, "9",
                "Insubordination répétée le 12/04",
                true, null, SALAIRE);
        var result = C4OnemChecklistCalculator.compute(req);
        assertThat(result.verdict())
                .isEqualTo(C4OnemChecklistResult.Verdict.RISQUE_EXCLUSION_FAUTE_GRAVE);
        assertThat(result.fauteGraveDetectee()).isTrue();
        assertThat(result.exclusionOnemRange()).isNotNull();
        assertThat(result.exclusionOnemRange().minSemaines()).isEqualTo(4);
        assertThat(result.exclusionOnemRange().maxSemaines()).isEqualTo(52);
        assertThat(result.etapeSuivante())
                .isEqualTo(C4OnemChecklistResult.EtapeSuivante.CONTESTATION_C4);
        assertThat(result.lettreRectificativeProposee()).isNull();
    }

    // =========================================================================
    // 5. RISQUE_EXCLUSION_FAUTE_GRAVE — priorité sur autres mentions manquantes
    // =========================================================================

    @Test
    void fauteGraveTrue_autresMentionsManquantes_RISQUE_dominantSansLettre() {
        // numeroBce manquant ET fauteGrave → le verdict reste
        // RISQUE_EXCLUSION_FAUTE_GRAVE (priorité), et lettreRectificativeProposee
        // est null (contestation, pas rectification).
        var req = new C4OnemChecklistRequest(
                null, // raison sociale manquante
                null, // BCE manquant
                "Dupont", null, ENTREE, SORTIE, "9",
                "Insubordination", true, null, SALAIRE);
        var result = C4OnemChecklistCalculator.compute(req);
        assertThat(result.verdict())
                .isEqualTo(C4OnemChecklistResult.Verdict.RISQUE_EXCLUSION_FAUTE_GRAVE);
        assertThat(result.lettreRectificativeProposee()).isNull();
        assertThat(result.etapeSuivante())
                .isEqualTo(C4OnemChecklistResult.EtapeSuivante.CONTESTATION_C4);
        // La liste des mentions manquantes reste populée (traçabilité), mais
        // elle ne déclenche pas de lettre rectificative.
        assertThat(result.mentionsManquantes())
                .contains(C4OnemChecklistMention.RAISON_SOCIALE_EMPLOYEUR,
                        C4OnemChecklistMention.NUMERO_BCE);
    }

    // =========================================================================
    // 6. NON_CONFORME — fauteGraveMentionnee=false + preavisPresteJours=null
    // =========================================================================

    @Test
    void pasFauteGrave_preavisNull_PREAVIS_MENTION_MANQUANTE() {
        var req = new C4OnemChecklistRequest(
                "ACME SA", "0123456789", "Dupont", "85010112345",
                ENTREE, SORTIE, "1", "Licenciement éco", false, null, SALAIRE);
        var result = C4OnemChecklistCalculator.compute(req);
        assertThat(result.verdict()).isEqualTo(C4OnemChecklistResult.Verdict.NON_CONFORME);
        assertThat(result.mentionsManquantes())
                .containsExactly(C4OnemChecklistMention.PREAVIS_PRECISE_SI_NON_FAUTE_GRAVE);
        assertThat(result.lettreRectificativeProposee()).isNotNull();
    }

    // =========================================================================
    // 7. NON_CONFORME — lettre rectificative cite explicitement chaque mention
    // =========================================================================

    @Test
    void nonConforme_lettreRectificative_listeChaqueMentionManquante() {
        var req = new C4OnemChecklistRequest(
                null, null, "Dupont", null, ENTREE, SORTIE, null,
                "Licenciement éco", false, 30, SALAIRE);
        var result = C4OnemChecklistCalculator.compute(req);
        assertThat(result.verdict()).isEqualTo(C4OnemChecklistResult.Verdict.NON_CONFORME);
        String lettre = result.lettreRectificativeProposee();
        assertThat(lettre).isNotNull();
        // Vérifie que chaque mention manquante est citée explicitement (libellé
        // humain dans la lettre).
        assertThat(lettre).contains("raison sociale");
        assertThat(lettre).contains("numéro BCE");
        assertThat(lettre).contains("catégorie ONEM");
        // En-tête + base juridique
        assertThat(lettre).contains("25 novembre 1991");
        assertThat(lettre).contains("art. 92");
    }

    // =========================================================================
    // 8. NON_CONFORME — motifExplicite < 5 caractères
    // =========================================================================

    @Test
    void motifExpliciteTropCourt_MENTION_MANQUANTE() {
        var req = new C4OnemChecklistRequest(
                "ACME SA", "0123456789", "Dupont", "85010112345",
                ENTREE, SORTIE, "1", "OK", false, 30, SALAIRE); // motif 2 car.
        var result = C4OnemChecklistCalculator.compute(req);
        assertThat(result.verdict()).isEqualTo(C4OnemChecklistResult.Verdict.NON_CONFORME);
        assertThat(result.mentionsManquantes())
                .contains(C4OnemChecklistMention.MOTIF_EXPLICITE);
    }

    // =========================================================================
    // 9. CONFORME → lettreRectificativeProposee = null + etapeSuivante = AUCUNE
    //     (recoupé avec le test 1, focus ici sur l'absence stricte de bruit)
    // =========================================================================

    @Test
    void conforme_aucunBruitDansLeResultat() {
        var result = C4OnemChecklistCalculator.compute(complete());
        assertThat(result.verdict()).isEqualTo(C4OnemChecklistResult.Verdict.CONFORME);
        assertThat(result.lettreRectificativeProposee()).isNull();
        assertThat(result.etapeSuivante()).isEqualTo(C4OnemChecklistResult.EtapeSuivante.AUCUNE);
        assertThat(result.exclusionOnemRange()).isNull();
        assertThat(result.mentionsManquantes()).isEmpty();
        assertThat(result.fauteGraveDetectee()).isFalse();
    }

    // =========================================================================
    // BCE format invalide → 400 (séparé des mentions manquantes)
    // =========================================================================

    @Test
    void numeroBceFormatInvalide_leve400() {
        var req = new C4OnemChecklistRequest(
                "ACME SA", "BE12345", "Dupont", null, // 5 chiffres, mal formé
                ENTREE, SORTIE, "1", "Licenciement éco", false, 30, SALAIRE);
        assertThatThrownBy(() -> C4OnemChecklistCalculator.compute(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Format BCE invalide");
    }
}
