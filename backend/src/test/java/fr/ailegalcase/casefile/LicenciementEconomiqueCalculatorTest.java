package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.LicenciementEconomiqueCalculator.CritereOrdre;
import fr.ailegalcase.casefile.LicenciementEconomiqueCalculator.MotifEconomique;
import fr.ailegalcase.casefile.LicenciementEconomiqueCalculator.PreuveMotif;
import fr.ailegalcase.casefile.LicenciementEconomiqueCalculator.QualitesProf;
import fr.ailegalcase.casefile.LicenciementEconomiqueCalculator.TentativeReclassement;
import fr.ailegalcase.casefile.LicenciementEconomiqueCalculator.VerdictRisque;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicenciementEconomiqueCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 1);

    private static final List<CritereOrdre> ALL_4_OBLIGATOIRES = List.of(
            CritereOrdre.AGE,
            CritereOrdre.ANCIENNETE,
            CritereOrdre.CHARGES_FAMILLE,
            CritereOrdre.QUALITES_PROFESSIONNELLES
    );

    // ============ Score causalité ============

    @Test
    void causalite_motifAUTRE_returns0() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.AUTRE,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION));
        assertThat(r.scoreCausalite()).isEqualTo(0);
    }

    @Test
    void causalite_motifReconnu_0preuve_returns20() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of());
        assertThat(r.scoreCausalite()).isEqualTo(20);
    }

    @Test
    void causalite_motifReconnu_1preuve_returns20() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES));
        assertThat(r.scoreCausalite()).isEqualTo(20);
    }

    @Test
    void causalite_motifReconnu_2preuves_returns50() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION));
        assertThat(r.scoreCausalite()).isEqualTo(50);
    }

    @Test
    void causalite_motifReconnu_4preuves_returns80() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION,
                        PreuveMotif.BAISSE_COMMANDES, PreuveMotif.BAISSE_TRESORERIE));
        assertThat(r.scoreCausalite()).isEqualTo(80);
    }

    @Test
    void causalite_motifReconnu_4preuves_avecRapportExpert_returns100() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION,
                        PreuveMotif.BAISSE_COMMANDES, PreuveMotif.RAPPORT_EXPERT));
        assertThat(r.scoreCausalite()).isEqualTo(100);
    }

    // ============ Score critères d'ordre ============

    @Test
    void criteresOrdre_4obligatoires_returns100() {
        LicenciementEconomiqueResult r = computeWithCriteres(ALL_4_OBLIGATOIRES);
        assertThat(r.scoreCriteresOrdre()).isEqualTo(100);
        assertThat(r.criteresOrdreObligatoiresOk()).isTrue();
        assertThat(r.criteresOrdreManquants()).isEmpty();
    }

    @Test
    void criteresOrdre_3obligatoires_returns75() {
        LicenciementEconomiqueResult r = computeWithCriteres(List.of(
                CritereOrdre.AGE, CritereOrdre.ANCIENNETE, CritereOrdre.CHARGES_FAMILLE));
        assertThat(r.scoreCriteresOrdre()).isEqualTo(75);
        assertThat(r.criteresOrdreObligatoiresOk()).isFalse();
        assertThat(r.criteresOrdreManquants())
                .containsExactly(CritereOrdre.QUALITES_PROFESSIONNELLES);
    }

    @Test
    void criteresOrdre_1obligatoire_returns25() {
        LicenciementEconomiqueResult r = computeWithCriteres(List.of(CritereOrdre.AGE));
        assertThat(r.scoreCriteresOrdre()).isEqualTo(25);
        assertThat(r.criteresOrdreManquants()).hasSize(3);
    }

    @Test
    void criteresOrdre_0_returns0() {
        LicenciementEconomiqueResult r = computeWithCriteres(List.of());
        assertThat(r.scoreCriteresOrdre()).isEqualTo(0);
        assertThat(r.criteresOrdreManquants()).hasSize(4);
    }

    @Test
    void criteresOrdre_4obligatoires_avecHandicap_returns100_handicapNonPenalise() {
        LicenciementEconomiqueResult r = computeWithCriteres(List.of(
                CritereOrdre.AGE, CritereOrdre.ANCIENNETE, CritereOrdre.CHARGES_FAMILLE,
                CritereOrdre.QUALITES_PROFESSIONNELLES, CritereOrdre.SITUATION_HANDICAP));
        assertThat(r.scoreCriteresOrdre()).isEqualTo(100);
        assertThat(r.criteresOrdreManquants()).isEmpty();
    }

    // ============ Score reclassement ============

    @Test
    void reclassement_0tentative_returns0() {
        LicenciementEconomiqueResult r = computeWithReclassement(List.of(), false);
        assertThat(r.scoreReclassement()).isEqualTo(0);
        assertThat(r.obligationReclassementRespectee()).isFalse();
    }

    @Test
    void reclassement_1tentative_returns40() {
        LicenciementEconomiqueResult r = computeWithReclassement(
                List.of(TentativeReclassement.FORMATION_INTERNE), false);
        assertThat(r.scoreReclassement()).isEqualTo(40);
        assertThat(r.obligationReclassementRespectee()).isFalse();
    }

    @Test
    void reclassement_2tentatives_sansPriorite_returns70() {
        LicenciementEconomiqueResult r = computeWithReclassement(
                List.of(TentativeReclassement.FORMATION_INTERNE, TentativeReclassement.MUTATION_GROUPE),
                false);
        assertThat(r.scoreReclassement()).isEqualTo(70);
        assertThat(r.obligationReclassementRespectee()).isTrue();
    }

    @Test
    void reclassement_2tentatives_avecPriorite_returns100() {
        LicenciementEconomiqueResult r = computeWithReclassement(
                List.of(TentativeReclassement.FORMATION_INTERNE, TentativeReclassement.MUTATION_GROUPE),
                true);
        assertThat(r.scoreReclassement()).isEqualTo(100);
        assertThat(r.obligationReclassementRespectee()).isTrue();
    }

    @Test
    void reclassement_uniquementAUCUNE_returns0() {
        LicenciementEconomiqueResult r = computeWithReclassement(
                List.of(TentativeReclassement.AUCUNE), false);
        assertThat(r.scoreReclassement()).isEqualTo(0);
        assertThat(r.obligationReclassementRespectee()).isFalse();
    }

    @Test
    void reclassement_uniquementAUCUNE_avecPriorite_returnsBonusOnly30() {
        // Cas limite : aucune tentative effective + priorité réembauche → 30 (bonus pur)
        LicenciementEconomiqueResult r = computeWithReclassement(
                List.of(TentativeReclassement.AUCUNE), true);
        assertThat(r.scoreReclassement()).isEqualTo(30);
        assertThat(r.obligationReclassementRespectee()).isFalse();
    }

    // ============ Score global et verdict ============

    @Test
    void scoreGlobal_isArithmeticMeanRounded() {
        // Causalité 50 + critères 100 + reclassement 100 = 250 / 3 = 83.33 → 83
        LicenciementEconomiqueResult r = LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION),
                ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE, TentativeReclassement.MUTATION_GROUPE),
                true, false, DATE, "FRANCE");
        assertThat(r.scoreCausalite()).isEqualTo(50);
        assertThat(r.scoreCriteresOrdre()).isEqualTo(100);
        assertThat(r.scoreReclassement()).isEqualTo(100);
        assertThat(r.scoreGlobal()).isEqualTo(83);
        assertThat(r.verdictRisqueRequalification()).isEqualTo(VerdictRisque.FAIBLE);
    }

    @Test
    void verdict_globalAtLeast70_returnsFAIBLE() {
        LicenciementEconomiqueResult r = LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION,
                        PreuveMotif.BAISSE_COMMANDES, PreuveMotif.RAPPORT_EXPERT),
                ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE, TentativeReclassement.MUTATION_GROUPE),
                true, true, DATE, "FRANCE");
        // 100 + 100 + 100 = 100 → FAIBLE
        assertThat(r.scoreGlobal()).isGreaterThanOrEqualTo(70);
        assertThat(r.verdictRisqueRequalification()).isEqualTo(VerdictRisque.FAIBLE);
    }

    @Test
    void verdict_global40to69_returnsMOYENNE() {
        // Causalité 20 + critères 75 + reclassement 40 = 135/3 = 45 → MOYENNE
        LicenciementEconomiqueResult r = LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(),
                List.of(CritereOrdre.AGE, CritereOrdre.ANCIENNETE, CritereOrdre.CHARGES_FAMILLE),
                52, 180, 2, QualitesProf.BON,
                List.of(TentativeReclassement.FORMATION_INTERNE),
                false, false, DATE, "FRANCE");
        assertThat(r.scoreGlobal()).isBetween(40, 69);
        assertThat(r.verdictRisqueRequalification()).isEqualTo(VerdictRisque.MOYENNE);
    }

    @Test
    void verdict_globalLessThan40_returnsELEVEE() {
        // Motif AUTRE + 0 critère + 0 reclassement = 0/3 = 0 → ELEVEE
        LicenciementEconomiqueResult r = LicenciementEconomiqueCalculator.compute(
                MotifEconomique.AUTRE,
                List.of(),
                List.of(),
                52, 180, 2, QualitesProf.BON,
                List.of(),
                false, false, DATE, "FRANCE");
        assertThat(r.scoreGlobal()).isLessThan(40);
        assertThat(r.verdictRisqueRequalification()).isEqualTo(VerdictRisque.ELEVEE);
    }

    // ============ Messages contextuels ============

    @Test
    void messages_prioriteReembauche_includesValidationL1233_45() {
        LicenciementEconomiqueResult r = LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION),
                ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE, TentativeReclassement.MUTATION_GROUPE),
                true, true, DATE, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Priorité de réembauche") && m.contains("L.1233-45"));
    }

    @Test
    void messages_motifAUTRE_includesAlerteRisqueEleve() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.AUTRE, List.of());
        assertThat(r.messages()).anyMatch(m -> m.contains("Autre"));
    }

    @Test
    void formule_containsAllScoresAndVerdict() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION));
        assertThat(r.formule())
                .contains("Causalité")
                .contains("Critères ordre")
                .contains("Reclassement")
                .contains("global");
    }

    @Test
    void baseJuridique_containsArticles() {
        LicenciementEconomiqueResult r = compute(MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES));
        assertThat(r.baseJuridique())
                .contains("L.1233-3")
                .contains("L.1233-4")
                .contains("L.1233-5")
                .contains("L.1233-45");
    }

    // ============ Validation ============

    @Test
    void compute_motifNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                null, List.of(), ALL_4_OBLIGATOIRES, 52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE), true, false, DATE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motif");
    }

    @Test
    void compute_criteresNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of(), null, 52, 180, 2,
                QualitesProf.EXCELLENT, List.of(TentativeReclassement.FORMATION_INTERNE),
                true, false, DATE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Critères");
    }

    @Test
    void compute_tentativesNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of(), ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT, null, true, false, DATE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tentatives");
    }

    @Test
    void compute_dateNotificationNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of(), ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE), true, false, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notification");
    }

    @Test
    void compute_countryBELGIQUE_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of(), ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE), true, false, DATE, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void compute_countryNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of(), ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE), true, false, DATE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_ageNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of(), ALL_4_OBLIGATOIRES,
                -1, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE), true, false, DATE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Âge");
    }

    @Test
    void compute_ancienneteNegative_throwsIllegalArgument() {
        assertThatThrownBy(() -> LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES, List.of(), ALL_4_OBLIGATOIRES,
                52, -10, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE), true, false, DATE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ancienneté");
    }

    // ============ Helpers ============

    private LicenciementEconomiqueResult compute(MotifEconomique motif, List<PreuveMotif> preuves) {
        return LicenciementEconomiqueCalculator.compute(
                motif, preuves, ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE, TentativeReclassement.MUTATION_GROUPE),
                true, false, DATE, "FRANCE");
    }

    private LicenciementEconomiqueResult computeWithCriteres(List<CritereOrdre> criteres) {
        return LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION),
                criteres,
                52, 180, 2, QualitesProf.EXCELLENT,
                List.of(TentativeReclassement.FORMATION_INTERNE, TentativeReclassement.MUTATION_GROUPE),
                true, false, DATE, "FRANCE");
    }

    private LicenciementEconomiqueResult computeWithReclassement(
            List<TentativeReclassement> tentatives, boolean prioriteReembauche) {
        return LicenciementEconomiqueCalculator.compute(
                MotifEconomique.DIFFICULTES_ECONOMIQUES,
                List.of(PreuveMotif.BAISSE_CHIFFRE_AFFAIRES, PreuveMotif.PERTES_EXPLOITATION),
                ALL_4_OBLIGATOIRES,
                52, 180, 2, QualitesProf.EXCELLENT,
                tentatives,
                prioriteReembauche, false, DATE, "FRANCE");
    }
}
