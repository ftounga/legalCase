package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static fr.ailegalcase.casefile.PriseActeRuptureCalculator.CodeGrief;
import static fr.ailegalcase.casefile.PriseActeRuptureCalculator.EffetProbable;
import static fr.ailegalcase.casefile.PriseActeRuptureCalculator.GriefRetenu;
import static fr.ailegalcase.casefile.PriseActeRuptureCalculator.Verdict;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-206-05 — tests unitaires du calculateur de prise d'acte de la rupture aux
 * torts de l'employeur (FR — Cass. soc. 25/06/2003 n°01-42.679).
 *
 * <p>Couvre les 7 critères d'acceptation de la mini-spec : chaque grief isolé,
 * cumul, modulateur persistance, modulateur impossible-poursuite, bascule
 * LICENCIEMENT_NUL sur harcèlement + sur discrimination, aucun grief coché,
 * bornes 0-100 du score, validation des bornes (montant impayé < 0, pays).</p>
 */
class PriseActeRuptureCalculatorTest {

    /** Input par défaut "rien coché" — aucun grief, verdict défavorable. */
    private static PriseActeRuptureInput aucunGrief() {
        return new PriseActeRuptureInput.Builder()
                .griefsActuelsEtPersistants(true)
                .build();
    }

    @Test
    void aucunGrief_returnsPriseActeDefavorable_effetDemission() {
        var r = PriseActeRuptureCalculator.compute(aucunGrief(), "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_DEFAVORABLE);
        assertThat(r.scoreSolidite()).isZero();
        assertThat(r.griefsRetenus()).isEmpty();
        assertThat(r.effetProbable()).isEqualTo(EffetProbable.DEMISSION);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void defautPaiementSignificatifEtPersistant_pondereMajeur() {
        var in = new PriseActeRuptureInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("3000"))
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus()))
                .containsExactly(CodeGrief.DT39_DEFAUT_PAIEMENT_SALAIRE);
        assertThat(r.griefsRetenus().get(0).poids())
                .isEqualTo(PriseActeRuptureCalculator.POIDS_GRIEF_MAJEUR);
    }

    @Test
    void defautPaiementAncienRegularise_pondereSignificatifEtScoreDivise() {
        var in = new PriseActeRuptureInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("500"))
                .griefsActuelsEtPersistants(false)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus()))
                .containsExactly(CodeGrief.DT39_DEFAUT_PAIEMENT_SALAIRE);
        // poids significatif = 15, divisé par 2 (grief ancien) → 7
        assertThat(r.scoreSolidite()).isEqualTo(7);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_DEFAVORABLE);
    }

    @Test
    void harcelement_basculeEffetLicenciementNul() {
        var in = new PriseActeRuptureInput.Builder()
                .harcelement(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus())).containsExactly(CodeGrief.DT39_HARCELEMENT);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_RISQUEE);
        assertThat(r.scoreSolidite()).isEqualTo(PriseActeRuptureCalculator.POIDS_GRIEF_MAJEUR);
        assertThat(r.effetProbable()).isEqualTo(EffetProbable.LICENCIEMENT_NUL);
    }

    @Test
    void discrimination_basculeEffetLicenciementNul() {
        var in = new PriseActeRuptureInput.Builder()
                .discrimination(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus())).containsExactly(CodeGrief.DT39_DISCRIMINATION);
        assertThat(r.effetProbable()).isEqualTo(EffetProbable.LICENCIEMENT_NUL);
    }

    @Test
    void manquementSecurite_isole_poidsMajeur() {
        var in = new PriseActeRuptureInput.Builder()
                .manquementSecurite(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus())).containsExactly(CodeGrief.DT39_MANQUEMENT_SECURITE);
        assertThat(r.scoreSolidite()).isEqualTo(PriseActeRuptureCalculator.POIDS_GRIEF_MAJEUR);
        assertThat(r.effetProbable()).isEqualTo(EffetProbable.LICENCIEMENT_SANS_CAUSE);
    }

    @Test
    void modificationContrat_isole_poidsSignificatif() {
        var in = new PriseActeRuptureInput.Builder()
                .modificationUnilateraleContrat(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus()))
                .containsExactly(CodeGrief.DT39_MODIFICATION_UNILATERALE_CONTRAT);
        assertThat(r.scoreSolidite())
                .isEqualTo(PriseActeRuptureCalculator.POIDS_GRIEF_SIGNIFICATIF);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_DEFAVORABLE);
    }

    @Test
    void declassement_isole_poidsSignificatif() {
        var in = new PriseActeRuptureInput.Builder()
                .declassement(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus())).containsExactly(CodeGrief.DT39_DECLASSEMENT);
        assertThat(r.scoreSolidite())
                .isEqualTo(PriseActeRuptureCalculator.POIDS_GRIEF_SIGNIFICATIF);
    }

    @Test
    void heuresSupNonPayees_isole_poidsSignificatif() {
        var in = new PriseActeRuptureInput.Builder()
                .heuresSupNonPayees(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus())).containsExactly(CodeGrief.DT39_HEURES_SUP_NON_PAYEES);
        assertThat(r.scoreSolidite())
                .isEqualTo(PriseActeRuptureCalculator.POIDS_GRIEF_SIGNIFICATIF);
    }

    @Test
    void nonRespectDureesRepos_isole_poidsMarginal() {
        var in = new PriseActeRuptureInput.Builder()
                .nonRespectDureesRepos(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(codes(r.griefsRetenus())).containsExactly(CodeGrief.DT39_NON_RESPECT_DUREES_REPOS);
        assertThat(r.scoreSolidite())
                .isEqualTo(PriseActeRuptureCalculator.POIDS_GRIEF_MARGINAL);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_DEFAVORABLE);
    }

    @Test
    void cumulHarcelementEtSecurite_donneFavorable_etLicenciementNul() {
        var in = new PriseActeRuptureInput.Builder()
                .harcelement(true)
                .manquementSecurite(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        // 30 + 30 = 60 → FAVORABLE
        assertThat(r.scoreSolidite()).isEqualTo(60);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_FAVORABLE);
        // Bascule LICENCIEMENT_NUL via harcèlement
        assertThat(r.effetProbable()).isEqualTo(EffetProbable.LICENCIEMENT_NUL);
    }

    @Test
    void griefRendImpossiblePoursuite_majoreScore() {
        var in = new PriseActeRuptureInput.Builder()
                .modificationUnilateraleContrat(true)
                .griefsActuelsEtPersistants(true)
                .griefRendImpossiblePoursuite(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        // 15 + 20 = 35 → RISQUEE (>= 25)
        assertThat(r.scoreSolidite()).isEqualTo(35);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_RISQUEE);
    }

    @Test
    void griefRendImpossiblePoursuite_false_pasDeBonus() {
        var in = new PriseActeRuptureInput.Builder()
                .modificationUnilateraleContrat(true)
                .griefsActuelsEtPersistants(true)
                .griefRendImpossiblePoursuite(false)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(r.scoreSolidite()).isEqualTo(15);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_DEFAVORABLE);
    }

    @Test
    void cumulMassif_scorePlafonneA100() {
        // 4 griefs majeurs (4×30) + 3 significatifs (3×15) + 1 marginal (10)
        // + bonus impossible (20) = 175 → plafond 100
        var in = new PriseActeRuptureInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("5000"))
                .harcelement(true)
                .manquementSecurite(true)
                .modificationUnilateraleContrat(true)
                .declassement(true)
                .discrimination(true)
                .heuresSupNonPayees(true)
                .nonRespectDureesRepos(true)
                .griefsActuelsEtPersistants(true)
                .griefRendImpossiblePoursuite(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(r.scoreSolidite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_FAVORABLE);
        assertThat(r.effetProbable()).isEqualTo(EffetProbable.LICENCIEMENT_NUL);
        assertThat(r.griefsRetenus()).hasSize(8);
    }

    @Test
    void griefsAnciens_diviseScoreParDeux() {
        var inPersistant = new PriseActeRuptureInput.Builder()
                .modificationUnilateraleContrat(true)
                .heuresSupNonPayees(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var inAncien = new PriseActeRuptureInput.Builder()
                .modificationUnilateraleContrat(true)
                .heuresSupNonPayees(true)
                .griefsActuelsEtPersistants(false)
                .build();
        var rPersist = PriseActeRuptureCalculator.compute(inPersistant, "FRANCE");
        var rAncien = PriseActeRuptureCalculator.compute(inAncien, "FRANCE");
        assertThat(rPersist.scoreSolidite()).isEqualTo(30);
        assertThat(rAncien.scoreSolidite()).isEqualTo(15);
    }

    @Test
    void basesJuridiques_contientToujoursArretsFondateurs() {
        var in = new PriseActeRuptureInput.Builder()
                .harcelement(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("25/06/2003"))
                .anyMatch(b -> b.contains("26/03/2014"));
    }

    @Test
    void aucunGrief_griefImpossiblePoursuite_resteDefavorable() {
        // Aucun grief coché mais critère impossible-poursuite à true →
        // PAS de bonus (pas de griefs).
        var in = new PriseActeRuptureInput.Builder()
                .griefRendImpossiblePoursuite(true)
                .griefsActuelsEtPersistants(true)
                .build();
        var r = PriseActeRuptureCalculator.compute(in, "FRANCE");
        assertThat(r.scoreSolidite()).isZero();
        assertThat(r.verdict()).isEqualTo(Verdict.PRISE_ACTE_DEFAVORABLE);
        assertThat(r.effetProbable()).isEqualTo(EffetProbable.DEMISSION);
    }

    @Test
    void paysBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                PriseActeRuptureCalculator.compute(aucunGrief(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void paysVide_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                PriseActeRuptureCalculator.compute(aucunGrief(), ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                PriseActeRuptureCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Input");
    }

    @Test
    void montantImpayesNegatif_throwsIllegalArgument() {
        var in = new PriseActeRuptureInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("-100"))
                .build();
        assertThatThrownBy(() ->
                PriseActeRuptureCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantImpayesEur");
    }

    @Test
    void commentaireTropLong_throwsIllegalArgument() {
        var in = new PriseActeRuptureInput.Builder()
                .griefsCommentaire("x".repeat(2001))
                .build();
        assertThatThrownBy(() ->
                PriseActeRuptureCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoreSolidite_resteDansBornes_0_100() {
        // Test bornes haute et basse via cas extrêmes.
        var inMax = new PriseActeRuptureInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("99999"))
                .harcelement(true)
                .manquementSecurite(true)
                .discrimination(true)
                .modificationUnilateraleContrat(true)
                .griefsActuelsEtPersistants(true)
                .griefRendImpossiblePoursuite(true)
                .build();
        var rMax = PriseActeRuptureCalculator.compute(inMax, "FRANCE");
        assertThat(rMax.scoreSolidite()).isBetween(0, 100).isEqualTo(100);

        var rMin = PriseActeRuptureCalculator.compute(aucunGrief(), "FRANCE");
        assertThat(rMin.scoreSolidite()).isBetween(0, 100).isZero();
    }

    private static List<CodeGrief> codes(List<GriefRetenu> griefs) {
        return griefs.stream().map(GriefRetenu::code).toList();
    }
}
