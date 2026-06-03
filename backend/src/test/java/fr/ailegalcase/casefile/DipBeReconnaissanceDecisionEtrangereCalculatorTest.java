package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.DipBeReconnaissanceDecisionEtrangereCalculator.DipBeReconnaissanceVerdict;
import fr.ailegalcase.casefile.DipBeReconnaissanceDecisionEtrangereCalculator.NatureDecision;

/**
 * SF-223-08 : tests unitaires du moteur décisionnel BE de reconnaissance /
 * exequatur d'une décision familiale étrangère (CDIP art. 22-27) — jugement
 * reconnu de plein droit / exequatur / refus (ordre public, défense, fraude) +
 * mariage religieux non-civil refusé + qualification incomplète + gates
 * (pays / nature / ISO-2 / date).
 */
class DipBeReconnaissanceDecisionEtrangereCalculatorTest {

    private static DipBeReconnaissanceDecisionEtrangereInput jugement(
            Boolean definitive, Boolean defense, boolean ordrePublic, Boolean fraude) {
        return new DipBeReconnaissanceDecisionEtrangereInput(
                NatureDecision.JUGEMENT_ETRANGER_HORS_UE, "MA", LocalDate.of(2023, 1, 1),
                definitive, defense, ordrePublic, fraude, null);
    }

    // -------------------- JUGEMENT ÉTRANGER HORS UE --------------------

    @Test
    void jugement_conforme_reconnaissance_de_plein_droit() {
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                        jugement(true, true, true, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.RECONNAISSANCE_DE_PLEIN_DROIT);
        assertThat(r.actesAProduire()).isNotEmpty();
    }

    @Test
    void jugement_non_definitif_exequatur_requis() {
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                        jugement(false, true, true, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.EXEQUATUR_REQUIS);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("exequatur"));
    }

    @Test
    void jugement_contraire_ordre_public_refuse() {
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                        jugement(true, true, false, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("ordre public"));
    }

    @Test
    void jugement_defense_non_respectee_refuse() {
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                        jugement(true, false, true, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("droits de la défense"));
    }

    @Test
    void jugement_fraude_refuse() {
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                        jugement(true, true, true, false), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("fraude"));
    }

    @Test
    void jugement_champs_fond_manquants_qualification_incomplete() {
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                        jugement(null, true, true, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.actesAProduire()).isEmpty();
    }

    // -------------------- MARIAGE RELIGIEUX NON-CIVIL --------------------

    @Test
    void mariage_religieux_sans_civil_prealable_refuse() {
        DipBeReconnaissanceDecisionEtrangereInput in = new DipBeReconnaissanceDecisionEtrangereInput(
                NatureDecision.MARIAGE_RELIGIEUX_NON_CIVIL, "DZ", null,
                null, null, true, null, false);
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("aucun effet civil"));
    }

    @Test
    void mariage_religieux_avec_civil_prealable_qualification_incomplete() {
        DipBeReconnaissanceDecisionEtrangereInput in = new DipBeReconnaissanceDecisionEtrangereInput(
                NatureDecision.MARIAGE_RELIGIEUX_NON_CIVIL, "DZ", null,
                null, null, true, null, true);
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(DipBeReconnaissanceVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.messages()).anyMatch(m -> m.contains("mariage-etranger-be-reconnaissance"));
    }

    @Test
    void mariage_religieux_mentionne_distinction_avec_mariage_etranger() {
        DipBeReconnaissanceDecisionEtrangereInput in = new DipBeReconnaissanceDecisionEtrangereInput(
                NatureDecision.MARIAGE_RELIGIEUX_NON_CIVIL, null, null,
                null, null, true, null, null);
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(in, "BELGIQUE");
        assertThat(r.conseils()).anyMatch(c -> c.contains("mariage-etranger-be-reconnaissance"));
    }

    // -------------------- Gates / validation --------------------

    @Test
    void nature_absente_leve_400() {
        DipBeReconnaissanceDecisionEtrangereInput in = new DipBeReconnaissanceDecisionEtrangereInput(
                null, "MA", null, true, true, true, true, null);
        assertThatThrownBy(() -> DipBeReconnaissanceDecisionEtrangereCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pays_origine_non_iso2_leve_400() {
        DipBeReconnaissanceDecisionEtrangereInput in = new DipBeReconnaissanceDecisionEtrangereInput(
                NatureDecision.JUGEMENT_ETRANGER_HORS_UE, "MAR", null,
                true, true, true, true, null);
        assertThatThrownBy(() -> DipBeReconnaissanceDecisionEtrangereCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void date_future_leve_400() {
        DipBeReconnaissanceDecisionEtrangereInput in = new DipBeReconnaissanceDecisionEtrangereInput(
                NatureDecision.JUGEMENT_ETRANGER_HORS_UE, "MA", LocalDate.now().plusDays(1),
                true, true, true, true, null);
        assertThatThrownBy(() -> DipBeReconnaissanceDecisionEtrangereCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() -> DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                jugement(true, true, true, true), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        DipBeReconnaissanceDecisionEtrangereResult r =
                DipBeReconnaissanceDecisionEtrangereCalculator.compute(
                        jugement(true, true, true, true), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
        assertThat(r.motifs()).noneMatch(m -> m.toUpperCase().contains("ECLI"));
    }
}
