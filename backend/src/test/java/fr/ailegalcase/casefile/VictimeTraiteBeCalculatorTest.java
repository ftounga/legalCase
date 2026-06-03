package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-221-06 — tests unitaires du calculateur titre victime de la traite des êtres humains
 * BE (art. 61/2 et s. Loi 15/12/1980 ; circulaire du 26/09/2008). Couvre les 5 verdicts,
 * les 3 phases, les conditions rupture / accompagnement / coopération, l'orientation vers
 * les centres spécialisés et les validations.
 */
class VictimeTraiteBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 3);

    @Test
    void delaiReflexion_phaseReflexion_avecConditions() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.REFLEXION_45J, true, false, true, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.DELAI_REFLEXION);
        assertThat(r.etapeProcedure()).contains("réflexion");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("61/2"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("26/09/2008"));
        assertThat(r.messages()).anyMatch(m -> m.contains("PAG-ASA"));
    }

    @Test
    void eligibleTitreTemporaire_declarationFaite_ruptureEtAccompagnement() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.DECLARATION_FAITE, true, false, true, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.ELIGIBLE_TITRE_TEMPORAIRE);
        assertThat(r.etapeProcedure()).isEqualTo("Déclaration faite");
    }

    @Test
    void eligibleTitreTemporaire_procedurePenale_sansCooperationDocumentee() {
        // Procédure pénale en cours, rupture + accompagnement mais coopération non documentée
        // -> titre temporaire (pas le titre lié à l'utilité de la déclaration).
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.PROCEDURE_PENALE_EN_COURS, true, false, true, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.ELIGIBLE_TITRE_TEMPORAIRE);
        assertThat(r.etapeProcedure()).isEqualTo("Procédure pénale en cours");
    }

    @Test
    void eligibleSousProcedurePenale_cooperation_procedurePenale() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.PROCEDURE_PENALE_EN_COURS, true, true, true, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.ELIGIBLE_SOUS_PROCEDURE_PENALE);
        assertThat(r.etapeProcedure()).isEqualTo("Procédure pénale en cours");
        assertThat(r.cooperationJudiciaire()).isTrue();
    }

    @Test
    void conditionsNonReunies_pasDeRupture() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.DECLARATION_FAITE, false, true, true, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.CONDITIONS_NON_REUNIES);
        assertThat(r.messages()).anyMatch(m -> m.contains("rupture"));
    }

    @Test
    void conditionsNonReunies_pasDAccompagnement() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.PROCEDURE_PENALE_EN_COURS, true, true, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.CONDITIONS_NON_REUNIES);
        assertThat(r.messages()).anyMatch(m -> m.contains("centre spécialisé"));
    }

    @Test
    void aOrienterCentre_phaseAucune_default() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.AUCUNE, false, false, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.A_ORIENTER_CENTRE);
        assertThat(r.messages()).anyMatch(m -> m.contains("PAG-ASA"));
    }

    @Test
    void aOrienterCentre_primeMemeAvecConditions() {
        // Phase AUCUNE prime : orienter d'abord vers un centre, même si rupture/accompagnement true.
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.AUCUNE, true, true, true, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.A_ORIENTER_CENTRE);
    }

    @Test
    void dateDebutAccompagnement_inclueDansMessages() {
        LocalDate debut = TODAY.minusDays(20);
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.DECLARATION_FAITE, true, false, true, debut, TODAY);

        assertThat(r.dateDebutAccompagnement()).isEqualTo(debut);
        assertThat(r.messages()).anyMatch(m -> m.contains(debut.toString()));
    }

    @Test
    void nullBooleans_traitesCommeFalse_declaration_conditionsNonReunies() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.DECLARATION_FAITE, null, null, null, null, TODAY);

        assertThat(r.verdict()).isEqualTo(VictimeTraiteBeVerdict.CONDITIONS_NON_REUNIES);
        assertThat(r.ruptureAvecReseau()).isFalse();
        assertThat(r.accompagnementCentreSpecialise()).isFalse();
    }

    @Test
    void validation_phaseNull_jette() {
        assertThatThrownBy(() -> VictimeTraiteBeCalculator.compute(
                null, true, true, true, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phaseProcedure");
    }

    @Test
    void validation_dateAccompagnementFuture_jette() {
        assertThatThrownBy(() -> VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.DECLARATION_FAITE, true, true, true, TODAY.plusDays(1), TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void distinctDuRegimeFR_messageRappele() {
        VictimeTraiteBeResult r = VictimeTraiteBeCalculator.compute(
                VictimeTraiteBePhase.DECLARATION_FAITE, true, false, true, null, TODAY);

        assertThat(r.messages()).anyMatch(m -> m.contains("F-IM-35"));
    }
}
