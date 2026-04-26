package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.TestamentValiditeCalculator.CodeVice;
import fr.ailegalcase.casefile.TestamentValiditeCalculator.FormeTestament;
import fr.ailegalcase.casefile.TestamentValiditeCalculator.VerdictValidite;
import fr.ailegalcase.casefile.TestamentValiditeCalculator.ViceIdentifie;

import java.util.List;

class TestamentValiditeCalculatorTest {

    private static TestamentValiditeResult olographe(boolean manuscrit, boolean date,
                                                     boolean signature, int age,
                                                     boolean saineDEsprit) {
        return TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", age, saineDEsprit, null,
                manuscrit, date, signature,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE");
    }

    private static List<CodeVice> codes(TestamentValiditeResult r) {
        return r.vicesIdentifies().stream().map(ViceIdentifie::code).toList();
    }

    // ============================================================
    // Olographe
    // ============================================================

    @Test
    void olographeValide_returnsValide() {
        TestamentValiditeResult r = olographe(true, true, true, 72, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.vicesIdentifies()).isEmpty();
        assertThat(r.scoreEligibilite()).isEqualTo(100);
    }

    @Test
    void olographeNonManuscrit_returnsNul() {
        TestamentValiditeResult r = olographe(false, true, true, 72, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.FORME_OLOGRAPHE_NON_MANUSCRITE);
    }

    @Test
    void olographeNonDate_returnsNul() {
        TestamentValiditeResult r = olographe(true, false, true, 72, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.FORME_OLOGRAPHE_NON_DATE);
    }

    @Test
    void olographeNonSigne_returnsNul() {
        TestamentValiditeResult r = olographe(true, true, false, 72, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.FORME_OLOGRAPHE_NON_SIGNE);
    }

    // ============================================================
    // Authentique
    // ============================================================

    @Test
    void authentiqueValide_returnsValide() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_AUTHENTIQUE, "2024-03-15", 65, true, null,
                null, null, null,
                true, true, true, true,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.vicesIdentifies()).isEmpty();
    }

    @Test
    void authentiqueDicteeManquante_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_AUTHENTIQUE, "2024-03-15", 65, true, null,
                null, null, null,
                true, false, true, true,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.FORME_AUTHENTIQUE_DICTEE_MANQUANTE);
    }

    @Test
    void authentiqueLectureManquante_returnsContestable() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_AUTHENTIQUE, "2024-03-15", 65, true, null,
                null, null, null,
                true, true, false, true,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(codes(r)).contains(CodeVice.FORME_AUTHENTIQUE_LECTURE_MANQUANTE);
    }

    @Test
    void authentiqueSignaturesIncompletes_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_AUTHENTIQUE, "2024-03-15", 65, true, null,
                null, null, null,
                true, true, true, false,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES);
    }

    // ============================================================
    // Mystique
    // ============================================================

    @Test
    void mystiqueValide_returnsValide() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_MYSTIQUE, "2024-03-15", 70, true, null,
                null, null, null,
                null, null, null, true,
                true, true, true,
                null,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.vicesIdentifies()).isEmpty();
    }

    @Test
    void mystiqueSansPliCachete_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_MYSTIQUE, "2024-03-15", 70, true, null,
                null, null, null,
                null, null, null, true,
                false, true, true,
                null,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.FORME_MYSTIQUE_PLI_NON_CACHE);
    }

    // ============================================================
    // International (Washington 1973)
    // ============================================================

    @Test
    void internationalValide_returnsValide() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_INTERNATIONAL, "2024-03-15", 50, true, null,
                null, null, null,
                null, null, null, true,
                null, null, null,
                true,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
    }

    @Test
    void internationalNonConforme_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_INTERNATIONAL, "2024-03-15", 50, true, null,
                null, null, null,
                null, null, null, true,
                null, null, null,
                false,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.FORME_INTERNATIONAL_WASHINGTON);
    }

    // ============================================================
    // Capacité
    // ============================================================

    @Test
    void mineurMoinsDe16Ans_returnsNul() {
        TestamentValiditeResult r = olographe(true, true, true, 15, true);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.INCAPACITE_MINEUR_MOINS_16_ANS);
    }

    @Test
    void insaniteEsprit_returnsNul() {
        TestamentValiditeResult r = olographe(true, true, true, 80, false);
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.INSANITE_ESPRIT);
    }

    @Test
    void majeurProtegeSansAssistance_returnsContestable() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, false,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.CONTESTABLE);
        assertThat(codes(r)).contains(CodeVice.MAJEUR_PROTEGE_SANS_ASSISTANCE);
    }

    // ============================================================
    // Vices de consentement
    // ============================================================

    @Test
    void viceConsentementDol_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                true, false, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.VICE_CONSENTEMENT_DOL);
    }

    @Test
    void erreurSubstantielle_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, true, false, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.VICE_CONSENTEMENT_ERREUR);
    }

    // ============================================================
    // Révocation
    // ============================================================

    @Test
    void testamentPosterieurContradictoire_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, true, false, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.REVOCATION_TESTAMENT_POSTERIEUR);
    }

    @Test
    void dechirureVolontaire_returnsNul() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, true, false,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.NUL);
        assertThat(codes(r)).contains(CodeVice.REVOCATION_DECHIRURE);
    }

    // ============================================================
    // Quotité disponible / action en réduction
    // ============================================================

    @Test
    void legsExcedeQuotiteDisponible_olographeValide_actionReductionTrue() {
        TestamentValiditeResult r = TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, false, true,
                "FRANCE");
        assertThat(r.verdictValidite()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.actionEnReductionPossible()).isTrue();
        assertThat(r.delaiContestationAns()).isEqualTo(5);
    }

    // ============================================================
    // Validations
    // ============================================================

    @Test
    void countryNull_throws() {
        assertThatThrownBy(() -> TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, false, false,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pays");
    }

    @Test
    void countryBelgique_throwsMentioningJumelle() {
        assertThatThrownBy(() -> TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, false, false,
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE")
                .hasMessageContaining("F-FA-24-BE-testament");
    }

    @Test
    void formeNull_throws() {
        assertThatThrownBy(() -> TestamentValiditeCalculator.compute(
                null, "2024-03-15", 70, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forme");
    }

    @Test
    void ageInvalide_throws() {
        assertThatThrownBy(() -> TestamentValiditeCalculator.compute(
                FormeTestament.TESTAMENT_OLOGRAPHE, "2024-03-15", -1, true, null,
                true, true, true,
                null, null, null, null,
                null, null, null,
                null,
                false, false, false, false, false,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Âge");
    }

    // ============================================================
    // Base juridique et formule
    // ============================================================

    @Test
    void baseJuridiqueContientArticles() {
        TestamentValiditeResult r = olographe(true, true, true, 72, true);
        assertThat(r.baseJuridique())
                .contains("967")
                .contains("970")
                .contains("971")
                .contains("901")
                .contains("1035")
                .contains("920");
    }

    @Test
    void formuleContientFormeEtVerdictEtScore() {
        TestamentValiditeResult r = olographe(true, true, true, 72, true);
        assertThat(r.formule())
                .contains("TESTAMENT_OLOGRAPHE")
                .contains("VALIDE")
                .contains("score 100");
    }

    @Test
    void messagesContiennentBaseJuridique() {
        TestamentValiditeResult r = olographe(true, true, true, 72, true);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Base juridique"));
    }
}
