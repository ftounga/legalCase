package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-210-01 : tests unitaires de {@link MediationFamilialePreSaisineCalculator}.
 */
class MediationFamilialePreSaisineCalculatorTest {

    @Test
    void motif_inscope_sans_mediation_ni_exception_renvoie_irrecevable() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.AUTORITE_PARENTALE,
                false,
                null,
                MediationFamilialePreSaisineException.AUCUNE,
                null);
        assertThat(r.verdict()).isEqualTo("IRRECEVABLE");
        assertThat(r.motifInScope()).isTrue();
        assertThat(r.dispenseApplicable()).isFalse();
        assertThat(r.piecesAJoindre()).isNotEmpty();
    }

    @Test
    void motif_inscope_avec_mediation_tentee_renvoie_recevable() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.CONTRIBUTION_ENTRETIEN,
                true,
                LocalDate.of(2026, 4, 15),
                MediationFamilialePreSaisineException.AUCUNE,
                null);
        assertThat(r.verdict()).isEqualTo("RECEVABLE");
        assertThat(r.dispenseApplicable()).isFalse();
        assertThat(r.piecesAJoindre()).anyMatch(s -> s.contains("Attestation"));
    }

    @Test
    void motif_inscope_avec_exception_violence_renvoie_recevable_par_dispense() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.AUTORITE_PARENTALE,
                false,
                null,
                MediationFamilialePreSaisineException.VIOLENCE,
                "Plainte du 10/04/2026");
        assertThat(r.verdict()).isEqualTo("RECEVABLE");
        assertThat(r.dispenseApplicable()).isTrue();
        assertThat(r.piecesAJoindre()).isNotEmpty();
    }

    @Test
    void motif_inscope_avec_urgence_renvoie_recevable_par_dispense() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.RESIDENCE_ENFANT,
                false,
                null,
                MediationFamilialePreSaisineException.URGENCE,
                null);
        assertThat(r.verdict()).isEqualTo("RECEVABLE");
        assertThat(r.dispenseApplicable()).isTrue();
    }

    @Test
    void motif_hors_scope_divorce_renvoie_non_concerne() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.DIVORCE_CONTENTIEUX,
                false,
                null,
                MediationFamilialePreSaisineException.AUCUNE,
                null);
        assertThat(r.verdict()).isEqualTo("NON_CONCERNE");
        assertThat(r.motifInScope()).isFalse();
        assertThat(r.piecesAJoindre()).isEmpty();
    }

    @Test
    void motif_hors_scope_succession_renvoie_non_concerne() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.SUCCESSION,
                false,
                null,
                MediationFamilialePreSaisineException.AUCUNE,
                null);
        assertThat(r.verdict()).isEqualTo("NON_CONCERNE");
    }

    @Test
    void motif_null_throws() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                MediationFamilialePreSaisineCalculator.compute(
                        null, false, null, MediationFamilialePreSaisineException.AUCUNE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void droit_visite_inscope() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.DROIT_VISITE,
                false,
                null,
                MediationFamilialePreSaisineException.AUCUNE,
                null);
        assertThat(r.verdict()).isEqualTo("IRRECEVABLE");
        assertThat(r.motifInScope()).isTrue();
    }

    @Test
    void exception_accord_prealable_dispense() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.AUTORITE_PARENTALE,
                false,
                null,
                MediationFamilialePreSaisineException.ACCORD_PREALABLE,
                "Mail commun des deux parents");
        assertThat(r.verdict()).isEqualTo("RECEVABLE");
        assertThat(r.dispenseApplicable()).isTrue();
    }

    @Test
    void formule_irrecevable_mentionne_motif_et_recommandation() {
        MediationFamilialePreSaisineResult r = MediationFamilialePreSaisineCalculator.compute(
                MediationFamilialePreSaisineMotif.AUTORITE_PARENTALE,
                false,
                null,
                MediationFamilialePreSaisineException.AUCUNE,
                null);
        assertThat(r.formule()).contains("IRRECEVABLE");
        assertThat(r.formule()).contains("autorité parentale");
        assertThat(r.baseJuridique()).contains("373-2-10");
    }
}
