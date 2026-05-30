package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-15 : tests unitaires de {@link JournalisteStatutAnalyzer}.
 *
 * <p>Valeurs déterministes (art. L.7112-3 / L.7112-4 / L.7112-5) :
 * <ul>
 *   <li>indemnité de congédiement = 1 mois de salaire par année ou fraction
 *       d'année d'ancienneté, plafonnée à 15 mois ;</li>
 *   <li>commission arbitrale si ancienneté &gt; 15 ans ou faute grave ;</li>
 *   <li>clause de cession / conscience valide si fait générateur constaté.</li>
 * </ul>
 */
class JournalisteStatutAnalyzerTest {

    private static final LocalDate ENTREE = LocalDate.of(2018, 1, 1);
    private static final BigDecimal SALAIRE = new BigDecimal("3000.00");

    @Test
    void clauseCessionValide_5ans_ruptureAssimileeLicenciement_indemnite5mois() {
        // 5 ans pile : ancienneté = 5 années → IL = 5 × 3000 = 15000.
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(5),
                JournalisteStatutTypeRupture.CLAUSE_CESSION,
                SALAIRE, true, true, false);

        assertThat(r.statutJournaliste()).isEqualTo(JournalisteStatutQualification.CONFIRME);
        assertThat(r.clauseValide()).isEqualTo(JournalisteStatutClauseValidite.VALIDE);
        assertThat(r.ancienneteAnnees()).isEqualTo(5);
        assertThat(r.indemniteCongediement()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(r.commissionArbitraleRequise()).isFalse();
        assertThat(r.verdictGlobal()).isEqualTo(JournalisteStatutVerdict.RUPTURE_ASSIMILEE_LICENCIEMENT);
    }

    @Test
    void clauseConscienceValide_changementOrientation_clauseValide() {
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(3),
                JournalisteStatutTypeRupture.CLAUSE_CONSCIENCE,
                SALAIRE, true, false, true);

        assertThat(r.clauseValide()).isEqualTo(JournalisteStatutClauseValidite.VALIDE);
        assertThat(r.verdictGlobal()).isEqualTo(JournalisteStatutVerdict.RUPTURE_ASSIMILEE_LICENCIEMENT);
        assertThat(r.indemniteCongediement()).isEqualByComparingTo(new BigDecimal("9000.00"));
    }

    @Test
    void clauseConscienceSansFaitGenerateur_clauseNonValide_indemniteNonDue() {
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(3),
                JournalisteStatutTypeRupture.CLAUSE_CONSCIENCE,
                SALAIRE, true, false, false);

        assertThat(r.clauseValide()).isEqualTo(JournalisteStatutClauseValidite.NON_VALIDE);
        assertThat(r.motifClause()).containsIgnoringCase("conscience");
        assertThat(r.indemniteCongediement()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.verdictGlobal()).isEqualTo(JournalisteStatutVerdict.INDEMNITE_NON_DUE);
    }

    @Test
    void licenciement_indemniteCongediement1moisParAnnee_fractionCompteAnneeEntiere() {
        // 4 ans + 2 mois → ancienneté = 5 années (fraction comptée) → IL = 5 × 3000.
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(4).plusMonths(2),
                JournalisteStatutTypeRupture.LICENCIEMENT,
                SALAIRE, true, false, false);

        assertThat(r.ancienneteAnnees()).isEqualTo(5);
        assertThat(r.clauseValide()).isEqualTo(JournalisteStatutClauseValidite.SANS_OBJET);
        assertThat(r.indemniteCongediement()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(r.commissionArbitraleRequise()).isFalse();
        assertThat(r.verdictGlobal()).isEqualTo(JournalisteStatutVerdict.INDEMNITE_DUE);
    }

    @Test
    void anciennete18ans_commissionArbitrale_plafond15mois() {
        // 18 ans > 15 ans → commission arbitrale ; IL plafonnée à 15 × 3000 = 45000.
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(18),
                JournalisteStatutTypeRupture.LICENCIEMENT,
                SALAIRE, true, false, false);

        assertThat(r.ancienneteAnnees()).isEqualTo(18);
        assertThat(r.commissionArbitraleRequise()).isTrue();
        assertThat(r.noteCommissionArbitrale()).contains("15 mois");
        assertThat(r.indemniteCongediement()).isEqualByComparingTo(new BigDecimal("45000.00"));
        assertThat(r.verdictGlobal()).isEqualTo(JournalisteStatutVerdict.COMMISSION_ARBITRALE);
    }

    @Test
    void fauteGrave_renvoiCommission_pasIndemniteDeDroit() {
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(6),
                JournalisteStatutTypeRupture.FAUTE_GRAVE,
                SALAIRE, true, false, false);

        assertThat(r.commissionArbitraleRequise()).isTrue();
        assertThat(r.noteCommissionArbitrale()).containsIgnoringCase("faute grave");
        assertThat(r.indemniteCongediement()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.verdictGlobal()).isEqualTo(JournalisteStatutVerdict.COMMISSION_ARBITRALE);
    }

    @Test
    void sansCartePresse_statutAQualifier() {
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(2),
                JournalisteStatutTypeRupture.LICENCIEMENT,
                SALAIRE, false, false, false);

        assertThat(r.statutJournaliste()).isEqualTo(JournalisteStatutQualification.A_QUALIFIER);
        assertThat(r.carteIdentiteProfessionnelle()).isFalse();
    }

    @Test
    void demission_indemniteNonDue() {
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(4),
                JournalisteStatutTypeRupture.DEMISSION,
                SALAIRE, true, false, false);

        assertThat(r.indemniteCongediement()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.commissionArbitraleRequise()).isFalse();
        assertThat(r.verdictGlobal()).isEqualTo(JournalisteStatutVerdict.INDEMNITE_NON_DUE);
    }

    @Test
    void carteParDefaut_confirme() {
        JournalisteStatutResult r = JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(2),
                JournalisteStatutTypeRupture.LICENCIEMENT,
                SALAIRE, null, null, null);

        assertThat(r.statutJournaliste()).isEqualTo(JournalisteStatutQualification.CONFIRME);
        assertThat(r.clauseValide()).isEqualTo(JournalisteStatutClauseValidite.SANS_OBJET);
    }

    @Test
    void dateRuptureAvantDateEntree_leveException() {
        assertThatThrownBy(() -> JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.minusDays(1),
                JournalisteStatutTypeRupture.LICENCIEMENT,
                SALAIRE, true, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salaireNegatif_leveException() {
        assertThatThrownBy(() -> JournalisteStatutAnalyzer.analyze(
                ENTREE, ENTREE.plusYears(2),
                JournalisteStatutTypeRupture.LICENCIEMENT,
                new BigDecimal("-1.00"), true, false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
