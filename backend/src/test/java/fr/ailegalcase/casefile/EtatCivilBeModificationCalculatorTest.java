package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.EtatCivilBeModificationCalculator.EtatCivilBeModificationVerdict;
import fr.ailegalcase.casefile.EtatCivilBeModificationCalculator.TypeModification;

/**
 * SF-223-09 : tests unitaires du moteur décisionnel BE de modification de l'état
 * civil — les 3 procédures (prénom = officier état civil ; nom = SPF Justice ;
 * sexe = loi 25/06/2017 auto-déclaration + seconde déclaration confirmative) ×
 * les 4 verdicts + spécificités (gratuité 1re demande prénom, mineur,
 * auto-déclaration sexe) + gates (pays / type).
 */
class EtatCivilBeModificationCalculatorTest {

    private static EtatCivilBeModificationInput input(
            TypeModification type, boolean majeur, boolean nationaliteResident,
            Boolean motif, Boolean secondePrenom, Boolean sexeReitere, Boolean consentMineur) {
        return new EtatCivilBeModificationInput(
                type, majeur, nationaliteResident, motif, secondePrenom, sexeReitere, consentMineur);
    }

    // -------------------- CHANGEMENT DE PRÉNOM (officier état civil) --------------------

    @Test
    void prenom_majeur_resident_recevable_officier_etat_civil() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_PRENOM, true, true, null, false, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE);
        assertThat(r.autoriteCompetente().toLowerCase()).contains("officier de l'état civil");
        assertThat(r.demarches()).isNotEmpty();
    }

    @Test
    void prenom_premiere_demande_signale_gratuite_tarif_reduit() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_PRENOM, true, true, null, false, null, null), "BELGIQUE");
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("première demande")
                && (m.toLowerCase().contains("réduite") || m.toLowerCase().contains("gratuit")));
    }

    @Test
    void prenom_ni_belge_ni_resident_irrecevable() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_PRENOM, true, false, null, false, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_IRRECEVABLE);
        assertThat(r.demarches()).isEmpty();
    }

    @Test
    void prenom_mineur_sans_consentement_sous_conditions() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_PRENOM, false, true, null, false, null, false), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE_SOUS_CONDITIONS);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("consentement"));
    }

    @Test
    void prenom_mineur_avec_consentement_recevable() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_PRENOM, false, true, null, false, null, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE);
    }

    // -------------------- CHANGEMENT DE NOM (SPF Justice) --------------------

    @Test
    void nom_motif_legitime_recevable_spf_justice() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_NOM, true, true, true, null, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE);
        assertThat(r.autoriteCompetente()).contains("SPF Justice");
    }

    @Test
    void nom_motif_non_legitime_irrecevable() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_NOM, true, true, false, null, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_IRRECEVABLE);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("motif sérieux"));
    }

    @Test
    void nom_motif_absent_qualification_incomplete() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_NOM, true, true, null, null, null, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.demarches()).isEmpty();
    }

    @Test
    void nom_mineur_sans_consentement_sous_conditions() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_NOM, false, true, true, null, null, false), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE_SOUS_CONDITIONS);
    }

    // -------------------- CHANGEMENT DE SEXE (loi 25/06/2017) --------------------

    @Test
    void sexe_majeur_seconde_declaration_reiteree_recevable() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_SEXE, true, true, null, null, true, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("auto-déclaration"));
    }

    @Test
    void sexe_majeur_sans_seconde_declaration_sous_conditions() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_SEXE, true, true, null, null, false, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE_SOUS_CONDITIONS);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("seconde déclaration"));
    }

    @Test
    void sexe_expose_procedure_auto_declaration_et_seconde_declaration() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_SEXE, true, true, null, null, true, null), "BELGIQUE");
        assertThat(r.demarches()).anyMatch(d -> d.toLowerCase().contains("première déclaration"));
        assertThat(r.demarches()).anyMatch(d -> d.toLowerCase().contains("seconde déclaration"));
    }

    @Test
    void sexe_mineur_regime_specifique_sous_conditions() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_SEXE, false, true, null, null, true, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_RECEVABLE_SOUS_CONDITIONS);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("mineur"));
    }

    @Test
    void sexe_ni_belge_ni_resident_irrecevable() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_SEXE, true, false, null, null, true, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(EtatCivilBeModificationVerdict.MODIFICATION_IRRECEVABLE);
    }

    // -------------------- Gates / validation --------------------

    @Test
    void type_absent_leve_400() {
        EtatCivilBeModificationInput in = input(null, true, true, true, null, null, null);
        assertThatThrownBy(() -> EtatCivilBeModificationCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() -> EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_PRENOM, true, true, null, false, null, null), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        EtatCivilBeModificationResult r = EtatCivilBeModificationCalculator.compute(
                input(TypeModification.CHANGEMENT_SEXE, true, true, null, null, true, null), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
        assertThat(r.motifs()).noneMatch(m -> m.toUpperCase().contains("ECLI"));
    }
}
