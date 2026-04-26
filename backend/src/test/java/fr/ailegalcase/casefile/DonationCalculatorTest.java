package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.DonationCalculator.CodeRisque;
import fr.ailegalcase.casefile.DonationCalculator.FormeDonation;
import fr.ailegalcase.casefile.DonationCalculator.RisqueIdentifie;
import fr.ailegalcase.casefile.DonationCalculator.VerdictValidite;

import java.util.List;

class DonationCalculatorTest {

    private static List<CodeRisque> codes(DonationResult r) {
        return r.risquesRequalification().stream().map(RisqueIdentifie::code).toList();
    }

    private static DonationResult notariee(boolean acteAuthentique,
                                           boolean acceptationExpresse) {
        return DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                acteAuthentique, acceptationExpresse,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE");
    }

    private static DonationResult manuelle(boolean remiseEffective, boolean bienMeuble) {
        return DonationCalculator.compute(
                FormeDonation.DONATION_MANUELLE, "2024-03-15", 50,
                true, true, true, true, true, true, true,
                null, null,
                remiseEffective, bienMeuble,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE");
    }

    // ============================================================
    // Notariée
    // ============================================================

    @Test
    void notarieeValide_returnsValide() {
        DonationResult r = notariee(true, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.risquesRequalification()).isEmpty();
        assertThat(r.scoreEligibilite()).isEqualTo(100);
    }

    @Test
    void notarieeSansActeAuthentique_returnsNul() {
        DonationResult r = notariee(false, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.FORME_NOTARIEE_NON_AUTHENTIQUE);
    }

    @Test
    void notarieeSansAcceptation_returnsNul() {
        DonationResult r = notariee(true, false);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.FORME_NOTARIEE_SANS_ACCEPTATION);
    }

    // ============================================================
    // Manuelle
    // ============================================================

    @Test
    void manuelleValide_returnsValide() {
        DonationResult r = manuelle(true, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.risquesRequalification()).isEmpty();
    }

    @Test
    void manuelleSansRemise_returnsNul() {
        DonationResult r = manuelle(false, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.FORME_MANUELLE_SANS_REMISE);
    }

    @Test
    void manuelleSurImmeuble_returnsNul() {
        DonationResult r = manuelle(true, false);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.FORME_MANUELLE_BIEN_NON_MEUBLE);
    }

    // ============================================================
    // Don indirect
    // ============================================================

    @Test
    void donIndirectAvecIntentionLiberale_returnsValide() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DON_INDIRECT, "2024-03-15", 50,
                true, true, true, true, true, true, true,
                null, null,
                null, null,
                true, true,
                null, null,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
    }

    @Test
    void donIndirectSansIntentionLiberale_returnsContestable() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DON_INDIRECT, "2024-03-15", 50,
                true, true, true, true, true, true, true,
                null, null,
                null, null,
                false, true,
                null, null,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(codes(r)).contains(CodeRisque.DON_INDIRECT_INTENTION_LIBERALE);
    }

    // ============================================================
    // Donation déguisée
    // ============================================================

    @Test
    void deguiseePrixIncoherent_returnsContestable() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_DEGUISEE, "2024-03-15", 50,
                true, true, true, true, true, true, true,
                null, null,
                null, null,
                null, null,
                true, true,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(codes(r)).contains(CodeRisque.REQUALIFICATION_DEGUISEMENT);
        assertThat(codes(r)).contains(CodeRisque.DEGUISEMENT_PRIX_VIL);
    }

    @Test
    void deguiseeApparenceCoherente_returnsValide() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_DEGUISEE, "2024-03-15", 50,
                true, true, true, true, true, true, true,
                null, null,
                null, null,
                null, null,
                true, false,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
    }

    // ============================================================
    // Capacité
    // ============================================================

    @Test
    void mineurMoinsDe16Ans_returnsNul() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 15,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.INCAPACITE_DONATEUR);
    }

    @Test
    void donateurInsanite_returnsNul() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 80,
                false, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.INSANITE_ESPRIT);
    }

    @Test
    void recipiendaireIncapable_returnsNul() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, false, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.INCAPACITE_RECIPIENDAIRE);
    }

    // ============================================================
    // Vices de consentement
    // ============================================================

    @Test
    void viceConsentementDol_returnsNul() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                true, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.VICE_CONSENTEMENT_DOL);
    }

    @Test
    void erreurSubstantielle_returnsNul() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, true, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.VICE_CONSENTEMENT_ERREUR);
    }

    // ============================================================
    // Objet
    // ============================================================

    @Test
    void objetIndetermine_returnsNul() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, false, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeRisque.OBJET_INDETERMINE);
    }

    // ============================================================
    // Quotité disponible / action en réduction
    // ============================================================

    @Test
    void excesQuotiteDisponible_actionReductionTrue() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, false,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.actionEnReductionPossible()).isTrue();
        assertThat(r.delaiContestationAns()).isEqualTo(5);
    }

    // ============================================================
    // Révocation
    // ============================================================

    @Test
    void ingratitude_revocationPossibleTrue() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, true, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.revocationPossible()).isTrue();
        assertThat(r.messages()).anyMatch(m -> m.contains("Ingratitude"));
    }

    @Test
    void inexecutionCharge_revocationPossibleTrue() {
        DonationResult r = DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, true,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.revocationPossible()).isTrue();
        assertThat(r.messages()).anyMatch(m -> m.contains("Inexécution"));
    }

    // ============================================================
    // Validations
    // ============================================================

    @Test
    void countryNull_throws() {
        assertThatThrownBy(() -> DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pays");
    }

    @Test
    void countryBelgique_throwsMentioningJumelle() {
        assertThatThrownBy(() -> DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE")
                .hasMessageContaining("F-FA-24-BE-donation");
    }

    @Test
    void formeNull_throws() {
        assertThatThrownBy(() -> DonationCalculator.compute(
                null, "2024-03-15", 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forme");
    }

    @Test
    void ageInvalide_throws() {
        assertThatThrownBy(() -> DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, "2024-03-15", -1,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Âge");
    }

    @Test
    void dateNulle_throws() {
        assertThatThrownBy(() -> DonationCalculator.compute(
                FormeDonation.DONATION_NOTARIEE, null, 65,
                true, true, true, true, true, true, true,
                true, true,
                null, null,
                null, null,
                null, null,
                false, false, false, false,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date");
    }

    // ============================================================
    // Base juridique et formule
    // ============================================================

    @Test
    void baseJuridiqueContientArticles() {
        DonationResult r = notariee(true, true);
        assertThat(r.baseJuridique())
                .contains("893")
                .contains("902")
                .contains("906")
                .contains("920")
                .contains("931")
                .contains("953");
    }

    @Test
    void formuleContientFormeEtVerdictEtScore() {
        DonationResult r = notariee(true, true);
        assertThat(r.formule())
                .contains("DONATION_NOTARIEE")
                .contains("VALIDE")
                .contains("score 100");
    }

    @Test
    void messagesContiennentBaseJuridique() {
        DonationResult r = notariee(true, true);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Base juridique"));
    }
}
