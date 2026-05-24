package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.ModificationContratRefusCalculator.AnalyseModificationContrat;
import static fr.ailegalcase.casefile.ModificationContratRefusCalculator.TypeConsequence;
import static fr.ailegalcase.casefile.ModificationContratRefusInput.ElementModifie;
import static fr.ailegalcase.casefile.ModificationContratRefusInput.ReponseSalarie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-11 — tests unitaires du calculateur d'analyse de la modification
 * du contrat refusée par le salarié (F-DT-70, FRANCE — Cass. soc. ;
 * L. 1222-6 CT).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * MODIFICATION_CONTRAT vs CHANGEMENT_CONDITIONS_TRAVAIL selon l'élément
 * et la contractualisation ; alerte L. 1222-6 ; conséquences du refus
 * différentes selon motif éco / non éco ; gate country FRANCE.</p>
 */
class ModificationContratRefusCalculatorTest {

    // ── AC1 : Rémunération modifiée + contractualisée → MODIFICATION_CONTRAT ─

    @Test
    void remuneration_returnsModificationContrat() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                false, null, null,
                ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.MODIFICATION_CONTRAT);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.scoreModificationContrat()).isGreaterThanOrEqualTo(90);
    }

    @Test
    void qualification_returnsModificationContrat() {
        var input = new ModificationContratRefusInput(
                ElementModifie.QUALIFICATION,
                true, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.MODIFICATION_CONTRAT);
    }

    @Test
    void dureeTravail_contractualisee_returnsModificationContrat() {
        var input = new ModificationContratRefusInput(
                ElementModifie.DUREE_TRAVAIL,
                true, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.MODIFICATION_CONTRAT);
    }

    @Test
    void dureeTravail_nonContractualisee_returnsIncertain() {
        var input = new ModificationContratRefusInput(
                ElementModifie.DUREE_TRAVAIL,
                false, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.INCERTAIN);
    }

    // ── AC2 : Horaires modifiés + non contractualisés → CHANGEMENT_CONDITIONS

    @Test
    void horaires_nonContractualises_returnsChangementConditionsTravail() {
        var input = new ModificationContratRefusInput(
                ElementModifie.HORAIRES,
                false, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.CHANGEMENT_CONDITIONS_TRAVAIL);
    }

    @Test
    void horaires_contractualises_returnsModificationContrat() {
        var input = new ModificationContratRefusInput(
                ElementModifie.HORAIRES,
                true, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.MODIFICATION_CONTRAT);
    }

    @Test
    void taches_returnsChangementConditionsTravail() {
        var input = new ModificationContratRefusInput(
                ElementModifie.TACHES,
                false, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.CHANGEMENT_CONDITIONS_TRAVAIL);
    }

    @Test
    void lieu_contractualise_returnsModificationContrat() {
        var input = new ModificationContratRefusInput(
                ElementModifie.LIEU_TRAVAIL,
                true, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.MODIFICATION_CONTRAT);
    }

    @Test
    void lieu_nonContractualise_returnsChangementConditionsTravail() {
        var input = new ModificationContratRefusInput(
                ElementModifie.LIEU_TRAVAIL,
                false, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.CHANGEMENT_CONDITIONS_TRAVAIL);
    }

    @Test
    void autre_returnsIncertain() {
        var input = new ModificationContratRefusInput(
                ElementModifie.AUTRE,
                false, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.analyseModification())
                .isEqualTo(AnalyseModificationContrat.INCERTAIN);
    }

    // ── AC3 : Motif éco + notification L. 1222-6 non envoyée → alerteDelai

    @Test
    void motifEco_sansNotification_returnsAlerteDelaiReflexion() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                true,             // motif éco
                false,            // notification absente
                null,
                ReponseSalarie.EN_ATTENTE);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.alerteDelaiReflexion()).isTrue();
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.PROCEDURE_L1222_6_NON_RESPECTEE);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1222-6"));
    }

    @Test
    void motifEco_avecNotificationEtDelai_pasAlerte() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                true,
                true,             // notification ok
                1,                // délai 1 mois
                ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.alerteDelaiReflexion()).isFalse();
    }

    @Test
    void motifEco_notificationNull_alertePrudence() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                true,
                null,             // notification non documentée
                null,
                ReponseSalarie.EN_ATTENTE);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.alerteDelaiReflexion()).isTrue();
    }

    @Test
    void motifEco_delaiInferieurAUnMois_alerte() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                true,
                true,
                0,                // délai 0 mois
                ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.alerteDelaiReflexion()).isTrue();
    }

    // ── AC4 : Conséquences du refus différentes selon motif éco / non éco ──

    @Test
    void refus_motifEco_returnsLicenciementEconomique() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                true,
                true, 1,
                ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.LICENCIEMENT_ECONOMIQUE_L1233_3);
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1233-3"));
    }

    @Test
    void refus_sansMotifEco_returnsRenoncerOuLicencier() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                false,
                null, null,
                ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.EMPLOYEUR_DOIT_RENONCER_OU_LICENCIER)
                .anyMatch(c -> c.type() == TypeConsequence.LICENCIEMENT_SANS_CAUSE_REELLE_SERIEUSE_SI_ILLEGITIME);
    }

    @Test
    void refus_changementConditions_returnsPouvoirDirectionFautif() {
        var input = new ModificationContratRefusInput(
                ElementModifie.HORAIRES,
                false,
                false,
                null, null,
                ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.POUVOIR_DIRECTION_REFUS_FAUTIF);
    }

    @Test
    void acceptation_returnsAccordFormelRequis() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                false,
                null, null,
                ReponseSalarie.ACCEPTATION);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.ACCORD_FORMEL_REQUIS);
    }

    @Test
    void enAttente_returnsAttenteReponseSalarie() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true,
                false,
                null, null,
                ReponseSalarie.EN_ATTENTE);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.consequences())
                .anyMatch(c -> c.type() == TypeConsequence.ATTENTE_REPONSE_SALARIE);
    }

    // ── AC5 : country != FRANCE → exception ─────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true, false, null, null, ReponseSalarie.REFUS);
        assertThatThrownBy(() ->
                ModificationContratRefusCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ModificationContratRefusCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void elementModifieNull_throwsIllegalArgument() {
        var input = new ModificationContratRefusInput(
                null,
                true, false, null, null, ReponseSalarie.REFUS);
        assertThatThrownBy(() ->
                ModificationContratRefusCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Élément");
    }

    @Test
    void reponseSalarieNull_throwsIllegalArgument() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true, false, null, null, null);
        assertThatThrownBy(() ->
                ModificationContratRefusCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Réponse");
    }

    @Test
    void delaiNegatif_throwsIllegalArgument() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true, true, true, -1, ReponseSalarie.REFUS);
        assertThatThrownBy(() ->
                ModificationContratRefusCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    // ── Score borné ─────────────────────────────────────────────────────────

    @Test
    void score_borneEntreZeroEtCent() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true, true, true, 1, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.scoreModificationContrat()).isBetween(0, 100);
    }

    // ── Bases juridiques de référence ───────────────────────────────────────

    @Test
    void basesJuridiques_contiennent_DistinctionCassSoc() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true, false, null, null, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("modification du contrat")
                        && b.contains("changement des conditions"));
    }

    @Test
    void basesJuridiques_motifEco_contient_L1222_6() {
        var input = new ModificationContratRefusInput(
                ElementModifie.REMUNERATION,
                true, true, true, 1, ReponseSalarie.REFUS);
        var r = ModificationContratRefusCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1222-6"));
    }
}
