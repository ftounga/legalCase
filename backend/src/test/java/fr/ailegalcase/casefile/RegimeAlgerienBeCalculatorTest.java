package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.RegimeAlgerienBeCalculator.LienRattachement;
import fr.ailegalcase.casefile.RegimeAlgerienBeCalculator.NatureActe;
import fr.ailegalcase.casefile.RegimeAlgerienBeCalculator.RegimeAlgerienBeVerdict;

/**
 * SF-223-05 : tests unitaires du moteur décisionnel BE du corridor algérien —
 * mariage reconnu, refus ordre public, dot/mahr, renvoi talaq F-217 + gates.
 */
class RegimeAlgerienBeCalculatorTest {

    private static RegimeAlgerienBeInput input(
            NatureActe nature, Boolean consentement, LienRattachement rattachement) {
        return new RegimeAlgerienBeInput(
                nature, LocalDate.of(2024, 1, 1), consentement, null, null, false, rattachement);
    }

    @Test
    void mariage_consenti_rattache_be_reconnaissance_plein_droit() {
        RegimeAlgerienBeResult r = RegimeAlgerienBeCalculator.compute(
                input(NatureActe.MARIAGE_ALGERIEN, true, LienRattachement.RESIDENCE), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(RegimeAlgerienBeVerdict.RECONNAISSANCE_DE_PLEIN_DROIT);
    }

    @Test
    void mariage_consentement_non_etabli_refus_ordre_public() {
        RegimeAlgerienBeResult r = RegimeAlgerienBeCalculator.compute(
                input(NatureActe.MARIAGE_ALGERIEN, false, LienRattachement.RESIDENCE), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(RegimeAlgerienBeVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("ordre public"));
    }

    @Test
    void mariage_sans_rattachement_be_sous_conditions() {
        RegimeAlgerienBeResult r = RegimeAlgerienBeCalculator.compute(
                input(NatureActe.MARIAGE_ALGERIEN, true, LienRattachement.AUCUN), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(RegimeAlgerienBeVerdict.RECONNAISSANCE_SOUS_CONDITIONS);
    }

    @Test
    void dot_mahr_qualifie_effet_patrimonial() {
        RegimeAlgerienBeInput in = new RegimeAlgerienBeInput(
                NatureActe.DOT_MAHR, LocalDate.of(2023, 6, 1), null, true, 5000d, false,
                LienRattachement.NATIONALITE);
        RegimeAlgerienBeResult r = RegimeAlgerienBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(RegimeAlgerienBeVerdict.RECONNAISSANCE_DE_PLEIN_DROIT);
        assertThat(r.effetsDot().toLowerCase()).contains("patrimonial");
    }

    @Test
    void talaq_renvoie_explicitement_outil_f217() {
        RegimeAlgerienBeResult r = RegimeAlgerienBeCalculator.compute(
                input(NatureActe.TALAQ_ALGERIEN, null, LienRattachement.RESIDENCE), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(RegimeAlgerienBeVerdict.RECONNAISSANCE_SOUS_CONDITIONS);
        // Renvoi explicite mécanique CDIP générale vers F-217.
        assertThat(r.messages()).anyMatch(m -> m.contains("mariage-etranger-be-reconnaissance"));
    }

    @Test
    void bases_renvoient_vers_f217_pour_la_mecanique_generale() {
        RegimeAlgerienBeResult r = RegimeAlgerienBeCalculator.compute(
                input(NatureActe.MARIAGE_ALGERIEN, true, LienRattachement.RESIDENCE), "BELGIQUE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("mariage-etranger-be-reconnaissance"));
    }

    @Test
    void nature_acte_absente_leve_400() {
        RegimeAlgerienBeInput in = new RegimeAlgerienBeInput(
                null, null, true, null, null, false, LienRattachement.RESIDENCE);
        assertThatThrownBy(() -> RegimeAlgerienBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rattachement_absent_leve_400() {
        RegimeAlgerienBeInput in = new RegimeAlgerienBeInput(
                NatureActe.MARIAGE_ALGERIEN, null, true, null, null, false, null);
        assertThatThrownBy(() -> RegimeAlgerienBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void date_future_leve_400() {
        RegimeAlgerienBeInput in = new RegimeAlgerienBeInput(
                NatureActe.MARIAGE_ALGERIEN, LocalDate.now().plusDays(1), true, null, null, false,
                LienRattachement.RESIDENCE);
        assertThatThrownBy(() -> RegimeAlgerienBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void montant_dot_negatif_leve_400() {
        RegimeAlgerienBeInput in = new RegimeAlgerienBeInput(
                NatureActe.DOT_MAHR, null, null, true, -1d, false, LienRattachement.RESIDENCE);
        assertThatThrownBy(() -> RegimeAlgerienBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() -> RegimeAlgerienBeCalculator.compute(
                input(NatureActe.MARIAGE_ALGERIEN, true, LienRattachement.RESIDENCE), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        RegimeAlgerienBeResult r = RegimeAlgerienBeCalculator.compute(
                input(NatureActe.MARIAGE_ALGERIEN, true, LienRattachement.RESIDENCE), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
        assertThat(r.motifs()).noneMatch(m -> m.toUpperCase().contains("ECLI"));
    }
}
