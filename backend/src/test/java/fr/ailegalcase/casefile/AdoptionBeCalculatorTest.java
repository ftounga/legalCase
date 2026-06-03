package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.AdoptionBeCalculator.AdoptionBeVerdict;
import fr.ailegalcase.casefile.AdoptionBeCalculator.ConsentementParentsBiologiques;
import fr.ailegalcase.casefile.AdoptionBeCalculator.TypeAdoptionBe;

/**
 * SF-223-02 : tests unitaires du moteur décisionnel BE de recevabilité de
 * l'adoption (3 types × verdicts + écart d'âge KO + co-parentale sans lien +
 * agrément manquant + consentements + gates).
 */
class AdoptionBeCalculatorTest {

    /** Cas nominal recevable (toutes conditions réunies) — type paramétrable. */
    private static AdoptionBeInput recevable(TypeAdoptionBe type) {
        return new AdoptionBeInput(
                type, 40, 8, null,
                type == TypeAdoptionBe.CO_PARENTALE ? Boolean.TRUE : null,
                type == TypeAdoptionBe.CO_PARENTALE ? Boolean.TRUE : null,
                true, true, ConsentementParentsBiologiques.OBTENU, null);
    }

    @Test
    void pleniere_conditions_reunies_recevable() {
        AdoptionBeResult r =
                AdoptionBeCalculator.compute(recevable(TypeAdoptionBe.PLENIERE), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.RECEVABLE);
        assertThat(r.motifsConditions()).isEmpty();
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("requête"));
    }

    @Test
    void simple_conditions_reunies_recevable() {
        AdoptionBeResult r =
                AdoptionBeCalculator.compute(recevable(TypeAdoptionBe.SIMPLE), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.RECEVABLE);
    }

    @Test
    void coparentale_avec_lien_recevable() {
        AdoptionBeResult r =
                AdoptionBeCalculator.compute(recevable(TypeAdoptionBe.CO_PARENTALE), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.RECEVABLE);
    }

    @Test
    void ecart_age_insuffisant_irrecevable_avec_motif() {
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.PLENIERE, 30, 22, null,
                null, null, true, true, ConsentementParentsBiologiques.OBTENU, null);
        AdoptionBeResult r = AdoptionBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.IRRECEVABLE);
        assertThat(r.motifsConditions()).anyMatch(m -> m.toLowerCase().contains("écart"));
    }

    @Test
    void ecart_age_saisi_prime_sur_difference_des_ages() {
        // Différence d'âges = 30, mais écart saisi = 10 → insuffisant.
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.PLENIERE, 40, 10, 10,
                null, null, true, true, ConsentementParentsBiologiques.OBTENU, null);
        AdoptionBeResult r = AdoptionBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.IRRECEVABLE);
        assertThat(r.motifsConditions()).anyMatch(m -> m.toLowerCase().contains("écart"));
    }

    @Test
    void coparentale_sans_lien_mariage_ou_cohabitation_irrecevable() {
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.CO_PARENTALE, 40, 8, null,
                true, false, true, true, ConsentementParentsBiologiques.SANS_OBJET, null);
        AdoptionBeResult r = AdoptionBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.IRRECEVABLE);
        assertThat(r.motifsConditions())
                .anyMatch(m -> m.toLowerCase().contains("mariage")
                        || m.toLowerCase().contains("cohabitation légale"));
    }

    @Test
    void age_adoptant_insuffisant_irrecevable() {
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.PLENIERE, 20, 4, null,
                null, null, true, true, ConsentementParentsBiologiques.OBTENU, null);
        AdoptionBeResult r = AdoptionBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.IRRECEVABLE);
        assertThat(r.motifsConditions()).anyMatch(m -> m.toLowerCase().contains("25 ans"));
    }

    @Test
    void agrement_absent_recevable_sous_conditions() {
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.PLENIERE, 40, 8, null,
                null, null, false, true, ConsentementParentsBiologiques.OBTENU, null);
        AdoptionBeResult r = AdoptionBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.RECEVABLE_SOUS_CONDITIONS);
        assertThat(r.motifsConditions()).anyMatch(c -> c.toLowerCase().contains("agrément"));
    }

    @Test
    void consentement_adopte_majeur_12ans_manquant_irrecevable() {
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.SIMPLE, 40, 14, null,
                null, null, true, false, ConsentementParentsBiologiques.OBTENU, null);
        AdoptionBeResult r = AdoptionBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.IRRECEVABLE);
        assertThat(r.motifsConditions()).anyMatch(m -> m.toLowerCase().contains("consentement de l'adopté"));
    }

    @Test
    void consentement_parents_refuse_irrecevable() {
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.PLENIERE, 40, 8, null,
                null, null, true, true, ConsentementParentsBiologiques.REFUSE, null);
        AdoptionBeResult r = AdoptionBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(AdoptionBeVerdict.IRRECEVABLE);
        assertThat(r.motifsConditions()).anyMatch(m -> m.toLowerCase().contains("refusé"));
    }

    @Test
    void type_absent_leve_400() {
        AdoptionBeInput in = new AdoptionBeInput(
                null, 40, 8, null, null, null, true, true,
                ConsentementParentsBiologiques.OBTENU, null);
        assertThatThrownBy(() -> AdoptionBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void age_aberrant_leve_400() {
        AdoptionBeInput in = new AdoptionBeInput(
                TypeAdoptionBe.PLENIERE, 0, 8, null, null, null, true, true,
                ConsentementParentsBiologiques.OBTENU, null);
        assertThatThrownBy(() -> AdoptionBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() ->
                AdoptionBeCalculator.compute(recevable(TypeAdoptionBe.PLENIERE), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        AdoptionBeResult r =
                AdoptionBeCalculator.compute(recevable(TypeAdoptionBe.PLENIERE), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
    }
}
