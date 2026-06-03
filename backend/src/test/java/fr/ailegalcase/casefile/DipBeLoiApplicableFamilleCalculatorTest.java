package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.DipBeLoiApplicableFamilleCalculator.DipBeLoiApplicableFamilleVerdict;
import fr.ailegalcase.casefile.DipBeLoiApplicableFamilleCalculator.FondementRattachement;
import fr.ailegalcase.casefile.DipBeLoiApplicableFamilleCalculator.Matiere;

/**
 * SF-223-07 : tests unitaires du moteur décisionnel BE de détermination de la
 * loi applicable en droit de la famille (DIP) — 3 matières (divorce / régime /
 * succession) × échelle de rattachement + professio juris + indéterminable +
 * gates (pays / matière / ISO-2 / date).
 */
class DipBeLoiApplicableFamilleCalculatorTest {

    private static DipBeLoiApplicableFamilleInput input(
            Matiere matiere, String residence, String nationalite, String choix) {
        return new DipBeLoiApplicableFamilleInput(
                matiere, residence, nationalite, choix, null, null);
    }

    // -------------------- DIVORCE (Rome III) --------------------

    @Test
    void divorce_choix_loi_prime() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, "FR", "MA", "DE"), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeLoiApplicableFamilleVerdict.LOI_APPLICABLE_DETERMINEE);
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.CHOIX_LOI_PARTIES);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("DE");
    }

    @Test
    void divorce_a_defaut_residence_habituelle_commune() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, "FR", "MA", null), "BELGIQUE");
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.RESIDENCE_HABITUELLE_COMMUNE);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("FR");
    }

    @Test
    void divorce_a_defaut_nationalite_commune() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, null, "MA", null), "BELGIQUE");
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.NATIONALITE_COMMUNE);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("MA");
    }

    @Test
    void divorce_loi_du_for_belge_par_defaut() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, null, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeLoiApplicableFamilleVerdict.LOI_APPLICABLE_DETERMINEE);
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.LOI_DU_FOR);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("BE");
    }

    // -------------------- RÉGIME MATRIMONIAL (2016/1103) --------------------

    @Test
    void regime_choix_loi_prime() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.REGIME_MATRIMONIAL, "FR", "MA", "IT"), "BELGIQUE");
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.CHOIX_LOI_PARTIES);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("IT");
    }

    @Test
    void regime_a_defaut_premiere_residence_commune() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.REGIME_MATRIMONIAL, "FR", "MA", null), "BELGIQUE");
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.RESIDENCE_HABITUELLE_COMMUNE);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("FR");
    }

    @Test
    void regime_sans_aucun_rattachement_est_indeterminable() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.REGIME_MATRIMONIAL, null, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeLoiApplicableFamilleVerdict.LOI_INDETERMINABLE);
        assertThat(r.loiApplicableDeterminee()).isNull();
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.AUCUN);
    }

    // -------------------- SUCCESSION (650/2012) --------------------

    @Test
    void succession_professio_juris_prime() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.SUCCESSION, "FR", null, "PT"), "BELGIQUE");
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.CHOIX_LOI_PARTIES);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("PT");
    }

    @Test
    void succession_a_defaut_residence_habituelle_defunt() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.SUCCESSION, "FR", null, null), "BELGIQUE");
        assertThat(r.fondementRattachement()).isEqualTo(FondementRattachement.RESIDENCE_HABITUELLE_DEFUNT);
        assertThat(r.loiApplicableDeterminee()).isEqualTo("FR");
    }

    @Test
    void succession_sans_residence_est_indeterminable() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.SUCCESSION, null, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeLoiApplicableFamilleVerdict.LOI_INDETERMINABLE);
    }

    @Test
    void succession_biens_immobiliers_message_non_scission() {
        DipBeLoiApplicableFamilleInput in = new DipBeLoiApplicableFamilleInput(
                Matiere.SUCCESSION, "FR", null, null, null, "ES");
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(in, "BELGIQUE");
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("unique"));
    }

    // -------------------- Gates / validation --------------------

    @Test
    void matiere_absente_leve_400() {
        assertThatThrownBy(() -> DipBeLoiApplicableFamilleCalculator.compute(
                input(null, "FR", null, null), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void iso2_invalide_leve_400() {
        assertThatThrownBy(() -> DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, "FRA", null, null), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, "fr", null, null), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void date_future_leve_400() {
        DipBeLoiApplicableFamilleInput in = new DipBeLoiApplicableFamilleInput(
                Matiere.DIVORCE, "FR", null, null, LocalDate.now().plusDays(1), null);
        assertThatThrownBy(() -> DipBeLoiApplicableFamilleCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() -> DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, "FR", null, null), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        DipBeLoiApplicableFamilleResult r = DipBeLoiApplicableFamilleCalculator.compute(
                input(Matiere.DIVORCE, "FR", null, null), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
        assertThat(r.motifs()).noneMatch(m -> m.toUpperCase().contains("ECLI"));
        // Fondement de rattachement exposé (traçabilité du raisonnement).
        assertThat(r.fondementRattachement()).isNotNull();
    }
}
