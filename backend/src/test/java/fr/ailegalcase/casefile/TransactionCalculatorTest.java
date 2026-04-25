package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.TransactionCalculator.ConcessionEmployeur;
import fr.ailegalcase.casefile.TransactionCalculator.ConcessionSalarie;
import fr.ailegalcase.casefile.TransactionCalculator.RupturePrealable;
import fr.ailegalcase.casefile.TransactionCalculator.VerdictValidite;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionCalculatorTest {

    private static final LocalDate SIGNATURE = LocalDate.of(2026, 4, 15);
    private static final BigDecimal SALAIRE = new BigDecimal("2500.00");
    private static final BigDecimal INDEMNITE = new BigDecimal("15000.00");

    private TransactionInput.Builder baseBuilder() {
        return new TransactionInput.Builder()
                .dateSignature(SIGNATURE)
                .salaireMensuelBrutEur(SALAIRE)
                .ancienneteAnnees(8)
                .indemniteTransactionnelleEur(INDEMNITE);
    }

    // ---- Concessions réciproques ----

    @Test
    void compute_concessionsReciproques_oneEachSide_isCharacterized() {
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.concessionsReciproquesCaracterisees()).isTrue();
        assertThat(r.scoreValidite()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void compute_aucuneConcessionEmployeur_reciproquesNonCaracterisees() {
        var input = baseBuilder()
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.concessionsReciproquesCaracterisees()).isFalse();
        assertThat(r.risqueNulliteRetenu()).isTrue();
    }

    @Test
    void compute_aucuneConcessionSalarie_reciproquesNonCaracterisees() {
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.concessionsReciproquesCaracterisees()).isFalse();
        assertThat(r.risqueNulliteRetenu()).isTrue();
    }

    @Test
    void compute_aucuneConcession_returnsNulle() {
        var input = baseBuilder().build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.concessionsReciproquesCaracterisees()).isFalse();
        assertThat(r.verdictValiditeContrat()).isEqualTo(VerdictValidite.NULLE);
    }

    // ---- Score 0-100 ----

    @Test
    void compute_allConditionsMet_returnsScore100Valide() {
        // Plancher Macron : 0.5 × 2500 × 8 = 10000 → indemnité 15000 > Macron OK
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .delaiReflexion15jOk(true)
                .presenceAvocatAssistance(true)
                .viceConsentementAllegue(false)
                .renonciationActionExpresse(true)
                .rupturePrealable(RupturePrealable.LICENCIEMENT_PERSONNEL)
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.scoreValidite()).isEqualTo(100);
        assertThat(r.verdictValiditeContrat()).isEqualTo(VerdictValidite.VALIDE);
        assertThat(r.risqueNulliteRetenu()).isFalse();
    }

    @Test
    void compute_score70_returnsValide() {
        // réciproques 30 + délai 20 + indemnite > Macron 20 = 70 (sans avocat, sans vice = absence vice +15 = 85)
        // Avec : réciproques + délai + absence vice = 30+20+15 = 65 → CONTESTABLE
        // Pour avoir exactement 70 : réciproques 30 + délai 20 + avocat 15 + (sans absence vice impossible) → vice allégué
        // → réciproques + délai + avocat = 65 + indemnite > Macron = 85 / sans = 65
        // Test : réciproques + délai + absence vice + indemnité = 30+20+15+20 = 85 / sans indemnité = 65
        // Pour avoir exactement >= 70 sans 100 : (réciproques + délai + avocat + absence vice) = 30+20+15+15 = 80 → VALIDE
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .delaiReflexion15jOk(true)
                .presenceAvocatAssistance(true)
                .indemniteTransactionnelleEur(new BigDecimal("5000.00")) // < Macron 10000
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.scoreValidite()).isEqualTo(80);
        assertThat(r.verdictValiditeContrat()).isEqualTo(VerdictValidite.VALIDE);
    }

    @Test
    void compute_score60_returnsContestable() {
        // réciproques 30 + délai 20 + absence vice 15 (sans avocat, indemnité < Macron) = 65
        // → ajustement : sans absence vice (vice allégué) = 30+20 = 50 + indemnité 20 = 70 → VALIDE
        // → réciproques + avocat + absence vice = 30+15+15 = 60 → CONTESTABLE
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .presenceAvocatAssistance(true)
                .indemniteTransactionnelleEur(new BigDecimal("5000.00")) // < Macron 10000
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.scoreValidite()).isEqualTo(60);
        assertThat(r.verdictValiditeContrat()).isEqualTo(VerdictValidite.CONTESTABLE);
    }

    @Test
    void compute_score35_returnsNulle() {
        // absence vice 15 + indemnité > Macron 20 = 35 (pas de réciproques, pas de délai, pas avocat)
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .build(); // pas de concession salarié → pas de réciproques
        var r = TransactionCalculator.compute(input, "FRANCE");
        // absence vice 15 + indemnité 15000 > 10000 → +20 = 35
        assertThat(r.scoreValidite()).isEqualTo(35);
        assertThat(r.verdictValiditeContrat()).isEqualTo(VerdictValidite.NULLE);
    }

    @Test
    void compute_viceConsentementAllegue_setsRisqueNullite() {
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .delaiReflexion15jOk(true)
                .presenceAvocatAssistance(true)
                .viceConsentementAllegue(true)
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.risqueNulliteRetenu()).isTrue();
    }

    @Test
    void compute_delai15jRespecte_addsBonus() {
        var inputWith = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .delaiReflexion15jOk(true)
                .indemniteTransactionnelleEur(new BigDecimal("5000.00"))
                .build();
        var inputWithout = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .delaiReflexion15jOk(false)
                .indemniteTransactionnelleEur(new BigDecimal("5000.00"))
                .build();
        var rWith = TransactionCalculator.compute(inputWith, "FRANCE");
        var rWithout = TransactionCalculator.compute(inputWithout, "FRANCE");
        assertThat(rWith.scoreValidite() - rWithout.scoreValidite()).isEqualTo(20);
    }

    @Test
    void compute_avocatPresent_addsBonus() {
        var input = baseBuilder()
                .presenceAvocatAssistance(true)
                .indemniteTransactionnelleEur(new BigDecimal("5000.00"))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        // absence vice 15 + avocat 15 = 30
        assertThat(r.scoreValidite()).isEqualTo(30);
    }

    @Test
    void compute_indemniteEqualMacron_noBonus() {
        // 0.5 × 2500 × 8 = 10000
        var input = baseBuilder()
                .indemniteTransactionnelleEur(new BigDecimal("10000.00"))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.indemniteTransactionnelleSuperieureMacron()).isFalse();
        // absence vice 15 only
        assertThat(r.scoreValidite()).isEqualTo(15);
    }

    @Test
    void compute_indemniteSuperieureMacron_addsBonus() {
        var input = baseBuilder()
                .indemniteTransactionnelleEur(new BigDecimal("11000.00"))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.indemniteTransactionnelleSuperieureMacron()).isTrue();
    }

    // ---- Ratio concessions ----

    @Test
    void compute_ratioConcessionsEmployeur_zeroIfNoEmployeur() {
        var input = baseBuilder()
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.ratioConcessionsEmployeurPct()).isEqualByComparingTo("0.0");
    }

    @Test
    void compute_ratioConcessionsEmployeur_hundredIfOnlyEmployeur() {
        var input = baseBuilder()
                .concessionsEmployeur(List.of(
                        ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE,
                        ConcessionEmployeur.LETTRE_RECOMMANDATION))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.ratioConcessionsEmployeurPct()).isEqualByComparingTo("100.0");
    }

    @Test
    void compute_ratioConcessionsEmployeur_fiftyIfBalanced() {
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.ratioConcessionsEmployeurPct()).isEqualByComparingTo("50.0");
    }

    @Test
    void compute_ratioConcessionsEmployeur_emptyAllZero() {
        var input = baseBuilder().build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.ratioConcessionsEmployeurPct()).isEqualByComparingTo("0.0");
    }

    // ---- Base juridique + formule ----

    @Test
    void compute_baseJuridique_includesArticles() {
        var input = baseBuilder().build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.baseJuridique())
                .contains("2044")
                .contains("2052")
                .contains("16/12/2010");
    }

    @Test
    void compute_formule_includesScoreAndVerdict() {
        var input = baseBuilder()
                .concessionsEmployeur(List.of(ConcessionEmployeur.INDEMNITE_TRANSACTIONNELLE))
                .concessionsSalarie(List.of(ConcessionSalarie.RENONCIATION_ACTION_PRUDHOMALE))
                .delaiReflexion15jOk(true)
                .presenceAvocatAssistance(true)
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.formule())
                .contains("Score 100/100")
                .contains("VALIDE")
                .contains("concessions réciproques");
    }

    @Test
    void compute_messages_warnsIfNoReciproques() {
        var input = baseBuilder().build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("concessions réciproques") && m.contains("NULLE"));
    }

    // ---- Cas d'erreur ----

    @Test
    void compute_dateSignatureNull_throws() {
        var input = baseBuilder().dateSignature(null).build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void compute_salaireZero_throws() {
        var input = baseBuilder().salaireMensuelBrutEur(BigDecimal.ZERO).build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_salaireNull_throws() {
        var input = baseBuilder().salaireMensuelBrutEur(null).build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_ancienneteNegative_throws() {
        var input = baseBuilder().ancienneteAnnees(-1).build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ancienneté");
    }

    @Test
    void compute_ancienneteNull_throws() {
        var input = baseBuilder().ancienneteAnnees(null).build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_indemniteNegative_throws() {
        var input = baseBuilder().indemniteTransactionnelleEur(new BigDecimal("-1")).build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Indemnité");
    }

    @Test
    void compute_indemniteNull_throws() {
        var input = baseBuilder().indemniteTransactionnelleEur(null).build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysBELGIQUE_throws() {
        var input = baseBuilder().build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void compute_paysNull_throws() {
        var input = baseBuilder().build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysBlank_throws() {
        var input = baseBuilder().build();
        assertThatThrownBy(() -> TransactionCalculator.compute(input, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_throws() {
        assertThatThrownBy(() -> TransactionCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_ancienneteCappedAt30_forMacron() {
        // 0.5 × 2500 × min(40, 30) = 0.5 × 2500 × 30 = 37500
        var input = baseBuilder()
                .ancienneteAnnees(40)
                .indemniteTransactionnelleEur(new BigDecimal("38000.00"))
                .build();
        var r = TransactionCalculator.compute(input, "FRANCE");
        assertThat(r.indemniteTransactionnelleSuperieureMacron()).isTrue();
    }
}
