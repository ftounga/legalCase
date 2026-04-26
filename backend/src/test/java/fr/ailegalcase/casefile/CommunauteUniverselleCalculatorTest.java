package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.CommunauteUniverselleCalculator.DispositifAnalyse;
import fr.ailegalcase.casefile.CommunauteUniverselleCalculator.VerdictValidite;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunauteUniverselleCalculatorTest {

    private static final double VALEUR = 800_000.0;

    // ============ Verdict VALIDE — VALIDITE_CONVENTION ============

    @Test
    void validite_tousCriteres_returnsVALIDE() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, true,
                null, null, null, "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.scoreValidite()).isGreaterThanOrEqualTo(90);
        assertThat(r.actionRetranchementPossible()).isFalse();
        assertThat(r.partAttributionConjointPct()).isEqualTo(0); // pas calculé pour ce dispositif
    }

    // ============ Verdict NUL — contrat non notarié ============

    @Test
    void contratNonNotarie_returnsNUL() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                false, true, true, true,
                null, null, null, "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(r.scoreValidite()).isEqualTo(0);
        assertThat(r.partAttributionConjointPct()).isEqualTo(0);
        assertThat(r.messages()).anyMatch(m -> m.contains("NON notarié")
                || m.contains("1394"));
    }

    @Test
    void liquidation_contratNonNotarie_returnsNUL() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                false, null, null, null,
                true, true, VALEUR, "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(r.partAttributionConjointPct()).isEqualTo(0);
        assertThat(r.valeurAttributionEur()).isEqualTo(0.0);
    }

    // ============ Verdict CONTESTABLE — vice du consentement ============

    @Test
    void validite_viceConsentement_returnsCONTESTABLE() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, false, true,
                null, null, null, "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(r.scoreValidite()).isLessThan(90);
    }

    @Test
    void validite_reserveHereditaireNonRespectee_returnsCONTESTABLE() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, false,
                null, null, null, "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
    }

    // ============ LIQUIDATION_DECES — avec CAI + enfants non communs ============

    @Test
    void liquidation_caiAvecEnfantsNonCommuns_actionRetranchement() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                true, true, VALEUR, "FRANCE");
        assertThat(r.actionRetranchementPossible()).isTrue();
        assertThat(r.partAttributionConjointPct()).isEqualTo(100);
        assertThat(r.valeurAttributionEur()).isEqualTo(VALEUR);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("retranchement"));
    }

    // ============ LIQUIDATION_DECES — avec CAI + enfants tous communs ============

    @Test
    void liquidation_caiAvecEnfantsCommuns_pasDeRetranchement() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                true, false, VALEUR, "FRANCE");
        assertThat(r.actionRetranchementPossible()).isFalse();
        assertThat(r.partAttributionConjointPct()).isEqualTo(100);
        assertThat(r.valeurAttributionEur()).isEqualTo(VALEUR);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
    }

    // ============ LIQUIDATION_DECES — sans CAI ============

    @Test
    void liquidation_sansCai_partage50_50() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                false, false, VALEUR, "FRANCE");
        assertThat(r.partAttributionConjointPct()).isEqualTo(50);
        assertThat(r.valeurAttributionEur()).isEqualTo(VALEUR / 2.0);
        assertThat(r.actionRetranchementPossible()).isFalse();
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
    }

    @Test
    void liquidation_sansCai_avecEnfantsNonCommuns_pasDeRetranchement() {
        // Sans CAI il n'y a pas de risque de retranchement (régime classique)
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                false, true, VALEUR, "FRANCE");
        assertThat(r.actionRetranchementPossible()).isFalse();
        assertThat(r.partAttributionConjointPct()).isEqualTo(50);
    }

    // ============ Valeur attribution ============

    @Test
    void liquidation_valeurZero_attributionZero() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                true, false, 0.0, "FRANCE");
        assertThat(r.valeurAttributionEur()).isEqualTo(0.0);
    }

    // ============ Base juridique ============

    @Test
    void baseJuridique_contient_1526_et_1527() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, true,
                null, null, null, "FRANCE");
        assertThat(r.baseJuridique()).contains("1526");
        assertThat(r.baseJuridique()).contains("1527");
    }

    // ============ Country ============

    @Test
    void country_FRANCE_normalized() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, true,
                null, null, null, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, true,
                null, null, null, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    // ============ Validations ============

    @Test
    void validation_dispositif_null_throws() {
        assertThatThrownBy(() -> CommunauteUniverselleCalculator.compute(
                null, true, true, true, true,
                null, null, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dispositif");
    }

    @Test
    void validation_contratNotarie_null_throws() {
        assertThatThrownBy(() -> CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                null, true, true, true,
                null, null, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contrat notarié");
    }

    @Test
    void validation_validite_inscriptionEtatCivil_null_throws() {
        assertThatThrownBy(() -> CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, null, true, true,
                null, null, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Inscription");
    }

    @Test
    void validation_liquidation_cai_null_throws() {
        assertThatThrownBy(() -> CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                null, true, VALEUR, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attribution intégrale");
    }

    @Test
    void validation_liquidation_valeurNegative_throws() {
        assertThatThrownBy(() -> CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                true, false, -1000.0, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("≥ 0");
    }

    @Test
    void validation_country_null_throws() {
        assertThatThrownBy(() -> CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, true,
                null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Messages ============

    @Test
    void messages_validite_contiennent_dispositif_libelle() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, true,
                null, null, null, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("Validité"));
    }

    @Test
    void messages_liquidation_contiennent_part_pct() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                true, false, VALEUR, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("100 %"));
    }

    @Test
    void formule_contient_score_et_verdict() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.VALIDITE_CONVENTION,
                true, true, true, true,
                null, null, null, "FRANCE");
        assertThat(r.formule()).contains("score");
        assertThat(r.formule()).contains("VALIDE");
    }

    @Test
    void risques_caiEtEnfantsNonCommuns_listeRisqueRetranchement() {
        CommunauteUniverselleResult r = CommunauteUniverselleCalculator.compute(
                DispositifAnalyse.LIQUIDATION_DECES,
                true, null, null, null,
                true, true, VALEUR, "FRANCE");
        assertThat(r.risquesIdentifies()).anyMatch(
                m -> m.toLowerCase().contains("retranchement"));
    }
}
