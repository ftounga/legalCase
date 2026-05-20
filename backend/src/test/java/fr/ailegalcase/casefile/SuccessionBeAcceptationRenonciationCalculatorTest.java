package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.ActeHeritierBe;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.DelaiStatutBe;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.EtatPatrimoineSuccessoralBe;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.OptionRecommandeeSuccessionBe;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.QualiteHeritierBe;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.RisqueSuccessionBeCode;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.SeveriteRisqueBe;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.SuccessionBeAcceptationRenonciationVerdict;
import fr.ailegalcase.casefile.SuccessionBeAcceptationRenonciationCalculator.VolonteClientSuccessionBe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-12 : tests unitaires du moteur décisionnel BE d'option successorale.
 */
class SuccessionBeAcceptationRenonciationCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 20);

    /** Base : décès récent, patrimoine solvable, aucun acte, indécis. */
    private SuccessionBeAcceptationRenonciationInput.Builder baseSolvable() {
        return new SuccessionBeAcceptationRenonciationInput.Builder()
                .dateDeces(LocalDate.of(2026, 5, 10)) // 10 jours avant TODAY
                .qualiteHeritier(QualiteHeritierBe.ENFANT)
                .etatPatrimoineSuccessoral(EtatPatrimoineSuccessoralBe.SOLVABLE)
                .actesAccomplis(List.of(ActeHeritierBe.AUCUN))
                .volonteClient(VolonteClientSuccessionBe.INDECIS);
    }

    @Test
    void compute_decesRecentPatrimoineSolvable_optionLibreDelaiOk() {
        var in = baseSolvable().build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(SuccessionBeAcceptationRenonciationVerdict.OPTION_LIBRE_DELAI_OK);
        assertThat(r.delaiStatut()).isEqualTo(DelaiStatutBe.OK);
        assertThat(r.optionRecommandee()).isEqualTo(OptionRecommandeeSuccessionBe.ACCEPTER);
        // dateDeces 2026-05-10 + 4 mois → 2026-09-10
        assertThat(r.dateLimiteOption()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(r.joursRestants()).isEqualTo(113); // 2026-05-20 → 2026-09-10
        assertThat(r.country()).isEqualTo("BELGIQUE");
    }

    @Test
    void compute_patrimoineDouteux_optionRecommandeeBeneficeInventaire() {
        var in = baseSolvable()
                .etatPatrimoineSuccessoral(EtatPatrimoineSuccessoralBe.DOUTEUX)
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE);
        assertThat(r.optionRecommandee()).isEqualTo(
                OptionRecommandeeSuccessionBe.ACCEPTER_BENEFICE_INVENTAIRE);
    }

    @Test
    void compute_patrimoineInconnu_optionRecommandeeBeneficeInventaire() {
        var in = baseSolvable()
                .etatPatrimoineSuccessoral(EtatPatrimoineSuccessoralBe.INCONNU)
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE);
    }

    @Test
    void compute_patrimoineInsolvable_optionRecommandeeRenonciation() {
        var in = baseSolvable()
                .etatPatrimoineSuccessoral(EtatPatrimoineSuccessoralBe.INSOLVABLE)
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.OPTION_RECOMMANDEE_RENONCIATION);
        assertThat(r.optionRecommandee()).isEqualTo(OptionRecommandeeSuccessionBe.RENONCER);
    }

    @Test
    void compute_patrimoineInsolvableEtVolonteAccepter_ajouteRisquePATRIMOINE_INSOLVABLE_ACCEPTATION_DANGEREUSE() {
        var in = baseSolvable()
                .etatPatrimoineSuccessoral(EtatPatrimoineSuccessoralBe.INSOLVABLE)
                .volonteClient(VolonteClientSuccessionBe.ACCEPTER)
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.risques()).anyMatch(risque ->
                risque.code() == RisqueSuccessionBeCode.PATRIMOINE_INSOLVABLE_ACCEPTATION_DANGEREUSE
                        && risque.severite() == SeveriteRisqueBe.HIGH);
    }

    // --- Détection acceptation tacite ---

    @Test
    void compute_encaissementCreance_acceptationTaciteProbable() {
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.ENCAISSEMENT_CREANCE_DEFUNT))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.ACCEPTATION_TACITE_PROBABLE);
        assertThat(r.risques()).anyMatch(risque ->
                risque.code() == RisqueSuccessionBeCode.ACTE_HERITIER_VALANT_ACCEPTATION_TACITE
                        && risque.severite() == SeveriteRisqueBe.HIGH);
    }

    @Test
    void compute_paiementDetteDefunt_acceptationTaciteProbable() {
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.PAIEMENT_DETTE_DEFUNT))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.ACCEPTATION_TACITE_PROBABLE);
    }

    @Test
    void compute_venteBienSuccession_acceptationTaciteProbable() {
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.VENTE_BIEN_SUCCESSION))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.ACCEPTATION_TACITE_PROBABLE);
    }

    @Test
    void compute_prisePossessionBiens_acceptationTaciteProbable() {
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.PRISE_POSSESSION_BIENS))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.ACCEPTATION_TACITE_PROBABLE);
    }

    @Test
    void compute_acteConservatoireNeutreSeul_pasDacceptationTacite() {
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.ACTE_CONSERVATOIRE_NEUTRE))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        // Acte conservatoire neutre ne vaut PAS acceptation tacite → verdict normal.
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.OPTION_LIBRE_DELAI_OK);
        assertThat(r.risques()).noneMatch(risque ->
                risque.code() == RisqueSuccessionBeCode.ACTE_HERITIER_VALANT_ACCEPTATION_TACITE);
    }

    @Test
    void compute_demandeInventaireSeule_pasDacceptationTacite() {
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.DEMANDE_INVENTAIRE))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.OPTION_LIBRE_DELAI_OK);
    }

    @Test
    void compute_acteHeritierEtVolonteRenoncer_basculeBeneficeInventaire() {
        // Acte d'héritier accompli + volonté de renoncer : la renonciation pure
        // est compromise → bascule sur bénéfice d'inventaire (à vérifier).
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.ENCAISSEMENT_CREANCE_DEFUNT))
                .volonteClient(VolonteClientSuccessionBe.RENONCER)
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.optionRecommandee()).isEqualTo(
                OptionRecommandeeSuccessionBe.ACCEPTER_BENEFICE_INVENTAIRE);
    }

    // --- Délais ---

    @Test
    void compute_decesIlYa100Jours_delaiCritique() {
        // 4 mois (~120 j) - 100 j → ~20 jours restants → CRITIQUE (≤ 30)
        var in = baseSolvable()
                .dateDeces(TODAY.minusDays(100))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.delaiStatut()).isEqualTo(DelaiStatutBe.CRITIQUE);
        assertThat(r.verdict()).isEqualTo(SuccessionBeAcceptationRenonciationVerdict.DELAI_CRITIQUE);
        assertThat(r.joursRestants()).isLessThanOrEqualTo(30);
        assertThat(r.joursRestants()).isGreaterThanOrEqualTo(0);
        assertThat(r.risques()).anyMatch(risque ->
                risque.code() == RisqueSuccessionBeCode.DELAI_TRES_COURT);
    }

    @Test
    void compute_decesIlYa150Jours_delaiDepasse() {
        var in = baseSolvable()
                .dateDeces(TODAY.minusDays(150))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.delaiStatut()).isEqualTo(DelaiStatutBe.DEPASSE);
        assertThat(r.verdict()).isEqualTo(SuccessionBeAcceptationRenonciationVerdict.DELAI_DEPASSE);
        assertThat(r.joursRestants()).isNegative();
        assertThat(r.risques()).anyMatch(risque ->
                risque.code() == RisqueSuccessionBeCode.DEVOLUTION_FORCEE_RISQUE
                        && risque.severite() == SeveriteRisqueBe.HIGH);
    }

    @Test
    void compute_miseEnDemeureCreancier1MoisApresDeces_delaiRaccourci() {
        // Décès 2026-05-10 → délai légal 2026-09-10.
        // Mise en demeure 2026-06-10 → délai MED 2026-08-10 (plus court).
        var in = baseSolvable()
                .miseEnDemeureCreancier(true)
                .dateMiseEnDemeureCreancier(LocalDate.of(2026, 6, 10))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.dateLimiteOption()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(r.risques()).anyMatch(risque ->
                risque.code() == RisqueSuccessionBeCode.MISE_EN_DEMEURE_DELAI_REDUIT);
    }

    @Test
    void compute_miseEnDemeureSansDate_qualificationIncomplete() {
        var in = baseSolvable()
                .miseEnDemeureCreancier(true)
                .dateMiseEnDemeureCreancier(null)
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.optionRecommandee()).isNull();
        assertThat(r.dateLimiteOption()).isNull();
        assertThat(r.joursRestants()).isNull();
        assertThat(r.delaiStatut()).isNull();
    }

    @Test
    void compute_delaiDateADate_gereMoisDeLongueursDifferentes() {
        // Décès 2025-10-31 → 4 mois → 2026-02-28 (février court)
        var in = baseSolvable()
                .dateDeces(LocalDate.of(2025, 10, 31))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE",
                LocalDate.of(2026, 2, 28));
        assertThat(r.dateLimiteOption()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(r.joursRestants()).isZero();
        assertThat(r.delaiStatut()).isEqualTo(DelaiStatutBe.CRITIQUE);
    }

    // --- Validation ---

    @Test
    void compute_paysNonBelgique_rejete() {
        var in = baseSolvable().build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "FRANCE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_rejete() {
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(null, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateDecesAbsente_rejete() {
        var in = baseSolvable().dateDeces(null).build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateDecesFuture_rejete() {
        var in = baseSolvable().dateDeces(TODAY.plusDays(1)).build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateMiseEnDemeureAvantDeces_rejete() {
        var in = baseSolvable()
                .miseEnDemeureCreancier(true)
                .dateMiseEnDemeureCreancier(LocalDate.of(2026, 5, 9)) // avant 2026-05-10
                .build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_qualiteHeritierAbsente_rejete() {
        var in = baseSolvable().qualiteHeritier(null).build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_volonteClientAbsente_rejete() {
        var in = baseSolvable().volonteClient(null).build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_etatPatrimoineAbsent_rejete() {
        var in = baseSolvable().etatPatrimoineSuccessoral(null).build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_actesNull_rejete() {
        var in = baseSolvable().actesAccomplis(null).build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_commentaireTropLong_rejete() {
        var in = baseSolvable().commentaire("x".repeat(1001)).build();
        assertThatThrownBy(() ->
                SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Verdict ordering (acte d'héritier prime sur délai/patrimoine) ---

    @Test
    void compute_acteHeritierAvecPatrimoineInsolvable_acteHeritierPrimeSurInsolvable() {
        // Acte d'héritier accompli ET patrimoine insolvable :
        // l'arbre prend ACCEPTATION_TACITE_PROBABLE en premier (mini-spec règle 3).
        var in = baseSolvable()
                .etatPatrimoineSuccessoral(EtatPatrimoineSuccessoralBe.INSOLVABLE)
                .actesAccomplis(List.of(ActeHeritierBe.ENCAISSEMENT_CREANCE_DEFUNT))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.ACCEPTATION_TACITE_PROBABLE);
        // L'option recommandée reste protectrice (renonciation car insolvable + bascule
        // protection).
    }

    @Test
    void compute_delaiDepasseAvecActeHeritier_delaiDepassePrimeSurActe() {
        // Mini-spec règle 2 : DELAI_DEPASSE en premier, AVANT ACCEPTATION_TACITE_PROBABLE.
        var in = baseSolvable()
                .dateDeces(TODAY.minusDays(150))
                .actesAccomplis(List.of(ActeHeritierBe.ENCAISSEMENT_CREANCE_DEFUNT))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.DELAI_DEPASSE);
    }

    // --- Liste exhaustive des actes ---

    @Test
    void compute_listeActesContenantAucun_etActeTacite_acteTaciteDetecte() {
        // Mix AUCUN + acte de disposition → l'acte de disposition prime.
        var in = baseSolvable()
                .actesAccomplis(List.of(ActeHeritierBe.AUCUN, ActeHeritierBe.VENTE_BIEN_SUCCESSION))
                .build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(
                SuccessionBeAcceptationRenonciationVerdict.ACCEPTATION_TACITE_PROBABLE);
    }

    @Test
    void compute_actionsConcretesContiennentDepotGreffe() {
        var in = baseSolvable().build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.actionsConcretes()).anyMatch(a -> a.toLowerCase().contains("greffe"));
    }

    @Test
    void compute_basesJuridiquesNonVides() {
        var in = baseSolvable().build();
        var r = SuccessionBeAcceptationRenonciationCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.basesJuridiques()).isNotEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("CC art. 774"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("CC art. 779"));
    }
}
