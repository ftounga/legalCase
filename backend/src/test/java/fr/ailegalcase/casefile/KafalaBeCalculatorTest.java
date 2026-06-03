package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.KafalaBeCalculator.KafalaBeVerdict;
import fr.ailegalcase.casefile.KafalaBeCalculator.ObjectifEnvisage;

/**
 * SF-223-03 : tests unitaires du moteur décisionnel BE de qualification du
 * recueil légal (kafala) — effet adoptif exclu + recueil reconnu + acte non
 * officiel + renvois séjour/succession + gates.
 */
class KafalaBeCalculatorTest {

    /** Cas nominal : acte officiel + consentement + objectif recueil/protection. */
    private static KafalaBeInput recueilComplet() {
        return new KafalaBeInput(
                "MA", LocalDate.of(2024, 1, 10),
                true, true, true, true,
                ObjectifEnvisage.RECUEIL_PROTECTION, null);
    }

    @Test
    void objectif_adoption_effet_adoptif_exclu() {
        KafalaBeInput in = new KafalaBeInput(
                "MA", LocalDate.of(2024, 1, 10),
                true, true, true, true,
                ObjectifEnvisage.TENTATIVE_ADOPTION, null);
        KafalaBeResult r = KafalaBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(KafalaBeVerdict.EFFET_ADOPTIF_EXCLU);
        assertThat(r.motifsConditions()).anyMatch(m -> m.toLowerCase().contains("filiation"));
        // Renvoi explicite vers adoption-be / délégation d'autorité parentale.
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("adoption-be")
                || a.toLowerCase().contains("délégation"));
    }

    @Test
    void acte_officiel_et_consentement_recueil_reconnu() {
        KafalaBeResult r = KafalaBeCalculator.compute(recueilComplet(), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(KafalaBeVerdict.RECUEIL_RECONNU_COMME_PROTECTION);
        assertThat(r.motifsConditions()).isEmpty();
    }

    @Test
    void acte_non_officiel_reconnaissance_sous_conditions() {
        KafalaBeInput in = new KafalaBeInput(
                "DZ", LocalDate.of(2023, 6, 1),
                false, true, true, true,
                ObjectifEnvisage.RECUEIL_PROTECTION, null);
        KafalaBeResult r = KafalaBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(KafalaBeVerdict.RECONNAISSANCE_SOUS_CONDITIONS);
        assertThat(r.motifsConditions()).anyMatch(c -> c.toLowerCase().contains("officiel"));
    }

    @Test
    void consentement_absent_reconnaissance_sous_conditions() {
        KafalaBeInput in = new KafalaBeInput(
                "MA", LocalDate.of(2024, 1, 10),
                true, false, true, false,
                ObjectifEnvisage.RECUEIL_PROTECTION, null);
        KafalaBeResult r = KafalaBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(KafalaBeVerdict.RECONNAISSANCE_SOUS_CONDITIONS);
        assertThat(r.motifsConditions()).anyMatch(c -> c.toLowerCase().contains("consentement")
                || c.toLowerCase().contains("autorité"));
    }

    @Test
    void renvois_sejour_et_succession_toujours_presents() {
        KafalaBeResult r = KafalaBeCalculator.compute(recueilComplet(), "BELGIQUE");
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("séjour")
                || a.toLowerCase().contains("immigration"));
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("dip-be-loi-applicable-famille")
                || a.toLowerCase().contains("successor"));
    }

    @Test
    void objectif_absent_leve_400() {
        KafalaBeInput in = new KafalaBeInput(
                "MA", LocalDate.of(2024, 1, 10),
                true, true, true, true, null, null);
        assertThatThrownBy(() -> KafalaBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pays_non_iso2_leve_400() {
        KafalaBeInput in = new KafalaBeInput(
                "Maroc", LocalDate.of(2024, 1, 10),
                true, true, true, true,
                ObjectifEnvisage.RECUEIL_PROTECTION, null);
        assertThatThrownBy(() -> KafalaBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void date_future_leve_400() {
        KafalaBeInput in = new KafalaBeInput(
                "MA", LocalDate.now().plusDays(1),
                true, true, true, true,
                ObjectifEnvisage.RECUEIL_PROTECTION, null);
        assertThatThrownBy(() -> KafalaBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pays_et_date_nulls_acceptes() {
        KafalaBeInput in = new KafalaBeInput(
                null, null, true, true, true, true,
                ObjectifEnvisage.RECUEIL_PROTECTION, null);
        KafalaBeResult r = KafalaBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(KafalaBeVerdict.RECUEIL_RECONNU_COMME_PROTECTION);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() -> KafalaBeCalculator.compute(recueilComplet(), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        KafalaBeResult r = KafalaBeCalculator.compute(recueilComplet(), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
    }
}
