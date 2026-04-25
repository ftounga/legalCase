package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevisionsPostDivorceCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 25);
    private static final LocalDate D_22_MOIS = LocalDate.of(2024, 6, 15);   // ~22 mois
    private static final LocalDate D_3_MOIS = LocalDate.of(2026, 1, 15);    // ~3 mois
    private static final LocalDate D_15_MOIS = LocalDate.of(2025, 1, 15);   // ~15 mois
    private static final String LONG_MOTIF =
            "Perte d'emploi du débiteur depuis 6 mois, baisse de 60% de ses revenus mensuels.";

    // =========================================================================
    // PENSION_ALIMENTAIRE — 4 tests
    // =========================================================================

    @Test
    void pension_baisseRevenus60Pct_modificationSubstantielle_elevee() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("4000"), new BigDecimal("1600"),
                new BigDecimal("2000"), new BigDecimal("2200"),
                2, List.of(10, 14),
                "EXCLUSIVE_MERE", "ALTERNEE",
                null, null, TODAY);
        assertThat(r.ecartRevenusPct()).isEqualTo(-60);
        assertThat(r.modificationSubstantielle()).isTrue();
        assertThat(r.motivationSuffisante()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100); // 50 + 25 + 15 + 10
        assertThat(r.verdictRevisionPossible()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("Art. 209 Cciv");
    }

    @Test
    void pension_baisseRevenus10Pct_nonSubstantielle_faibleMoyenne() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("4000"), new BigDecimal("3600"),
                null, null,
                2, List.of(10),
                null, null,
                null, null, TODAY);
        assertThat(r.ecartRevenusPct()).isEqualTo(-10);
        assertThat(r.modificationSubstantielle()).isFalse();
        // 0 (modif non substantielle) + 25 (motiv) + 15 (anc) + 10 (1+ enfant) = 50
        assertThat(r.scoreGlobal()).isEqualTo(50);
        assertThat(r.verdictRevisionPossible()).isEqualTo("MOYENNE");
    }

    @Test
    void pension_baisseRevenus20Pct_estSubstantielle() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("5000"), new BigDecimal("4000"),
                null, null,
                1, List.of(8),
                null, null,
                null, null, TODAY);
        assertThat(r.ecartRevenusPct()).isEqualTo(-20);
        assertThat(r.modificationSubstantielle()).isTrue();
    }

    @Test
    void pension_baseJuridique_correcte() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("4000"), new BigDecimal("1600"), null, null,
                2, List.of(10), null, null, null, null, TODAY);
        assertThat(r.baseJuridique()).contains("209");
    }

    // =========================================================================
    // RESIDENCE — 4 tests
    // =========================================================================

    @Test
    void residence_changementMode_substantielle_elevee() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "RESIDENCE", D_22_MOIS,
                "Le père a déménagé à 200km, il n'est plus possible d'exercer la résidence alternée.",
                null, null, null, null,
                2, List.of(8, 12),
                "ALTERNEE", "EXCLUSIVE_MERE",
                null, null, TODAY);
        assertThat(r.modificationSubstantielle()).isTrue();
        // 50 (changement mode) + 25 (motiv) + 15 (anc) + 10 (enfant ≥ 6 ans) = 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictRevisionPossible()).isEqualTo("ELEVEE");
    }

    @Test
    void residence_memeMode_nonSubstantielle() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "RESIDENCE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                1, List.of(10),
                "ALTERNEE", "ALTERNEE",
                null, null, TODAY);
        assertThat(r.modificationSubstantielle()).isFalse();
    }

    @Test
    void residence_enfantsTropJeunes_critereSpecifiqueAbsent() {
        // 50 + 25 + 15 + 0 (aucun enfant ≥ 6 ans) = 90 → ELEVEE quand même
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "RESIDENCE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                1, List.of(3),
                "ALTERNEE", "EXCLUSIVE_MERE",
                null, null, TODAY);
        assertThat(r.modificationSubstantielle()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(90);
    }

    @Test
    void residence_baseJuridique_correcte() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "RESIDENCE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                1, List.of(10),
                "ALTERNEE", "EXCLUSIVE_MERE",
                null, null, TODAY);
        assertThat(r.baseJuridique()).contains("373-2-13");
    }

    // =========================================================================
    // DROIT_VISITE — 4 tests
    // =========================================================================

    @Test
    void droitVisite_enfantPreado_substantielle_elevee() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DROIT_VISITE", D_22_MOIS,
                "L'adolescent de 14 ans souhaite être entendu et demande à voir son père moins souvent.",
                null, null, null, null,
                1, List.of(14),
                null, null,
                null, null, TODAY);
        assertThat(r.modificationSubstantielle()).isTrue();
        // 50 + 25 + 15 + 10 = 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictRevisionPossible()).isEqualTo("ELEVEE");
    }

    @Test
    void droitVisite_enfantsJeunes_pasSubstantielSansChangementMode() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DROIT_VISITE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                1, List.of(7),
                null, null,
                null, null, TODAY);
        assertThat(r.modificationSubstantielle()).isFalse();
    }

    @Test
    void droitVisite_changementModeMemeJeune_estSubstantielle() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DROIT_VISITE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                1, List.of(7),
                "ALTERNEE", "EXCLUSIVE_MERE",
                null, null, TODAY);
        assertThat(r.modificationSubstantielle()).isTrue();
    }

    @Test
    void droitVisite_baseJuridique_inclutAuditionEnfant() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DROIT_VISITE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                1, List.of(13),
                null, null,
                null, null, TODAY);
        assertThat(r.baseJuridique()).contains("373-2-9").contains("388-1");
    }

    // =========================================================================
    // PRESTATION_COMPENSATOIRE — 4 tests
    // =========================================================================

    @Test
    void prestation_baisseRevenus40Pct_substantielle_elevee() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PRESTATION_COMPENSATOIRE", D_22_MOIS,
                "Mise en invalidité du débiteur — perte irréversible de 40% des revenus.",
                new BigDecimal("5000"), new BigDecimal("3000"),
                null, null,
                0, null,
                null, null,
                null, null, TODAY);
        assertThat(r.ecartRevenusPct()).isEqualTo(-40);
        assertThat(r.modificationSubstantielle()).isTrue();
        // 50 + 25 + 15 + 10 (anc ≥ 12 mois) = 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictRevisionPossible()).isEqualTo("ELEVEE");
    }

    @Test
    void prestation_decisionTropRecente_critereSpecifiqueAbsent() {
        // 22 mois = ≥12 → critère ok ; ici 3 mois → KO
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PRESTATION_COMPENSATOIRE", D_3_MOIS, LONG_MOTIF,
                new BigDecimal("5000"), new BigDecimal("3000"), null, null,
                0, null, null, null, null, null, TODAY);
        // 50 + 25 + 0 (anc < 6) + 0 (anc < 12) = 75 → ELEVEE limite
        assertThat(r.scoreGlobal()).isEqualTo(75);
    }

    @Test
    void prestation_pasEcart_pasSubstantielle() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PRESTATION_COMPENSATOIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("4000"), new BigDecimal("4000"), null, null,
                0, null, null, null, null, null, TODAY);
        assertThat(r.ecartRevenusPct()).isEqualTo(0);
        assertThat(r.modificationSubstantielle()).isFalse();
    }

    @Test
    void prestation_baseJuridique_inclut279() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PRESTATION_COMPENSATOIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("5000"), new BigDecimal("3000"), null, null,
                0, null, null, null, null, null, TODAY);
        assertThat(r.baseJuridique()).contains("279");
    }

    // =========================================================================
    // DEMENAGEMENT_PARENT — 4 tests
    // =========================================================================

    @Test
    void demenagement_distance200km_substantielle_elevee() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DEMENAGEMENT_PARENT", D_22_MOIS,
                "Mutation professionnelle du père dans une autre région — déménagement à 200 km.",
                null, null, null, null,
                2, List.of(8, 11),
                "ALTERNEE", null,
                true, 200, TODAY);
        assertThat(r.modificationSubstantielle()).isTrue();
        // 50 + 25 + 15 + 10 = 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictRevisionPossible()).isEqualTo("ELEVEE");
    }

    @Test
    void demenagement_pasInformationPrealable_substantielle() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DEMENAGEMENT_PARENT", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                2, List.of(8, 11),
                "ALTERNEE", null,
                false, 30, TODAY);
        assertThat(r.modificationSubstantielle()).isTrue();
    }

    @Test
    void demenagement_distance10km_avecInfo_pasSubstantielle() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DEMENAGEMENT_PARENT", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                1, List.of(10),
                "ALTERNEE", null,
                true, 10, TODAY);
        assertThat(r.modificationSubstantielle()).isFalse();
    }

    @Test
    void demenagement_baseJuridique_correcte() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "DEMENAGEMENT_PARENT", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                2, List.of(8, 11),
                "ALTERNEE", null,
                true, 200, TODAY);
        assertThat(r.baseJuridique()).contains("373-2");
    }

    // =========================================================================
    // Edges & verdict thresholds
    // =========================================================================

    @Test
    void verdict_threshold_50_estMoyenne() {
        // Modification substantielle 50 + autres KO ⇒ score 50 → MOYENNE
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_3_MOIS, "court",
                new BigDecimal("5000"), new BigDecimal("3000"), null, null,
                0, null, null, null, null, null, TODAY);
        assertThat(r.scoreGlobal()).isEqualTo(50);
        assertThat(r.verdictRevisionPossible()).isEqualTo("MOYENNE");
    }

    @Test
    void verdict_threshold_75_estElevee() {
        // 50 + 25 = 75 (motiv ok, ancienneté KO, critère KO)
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_3_MOIS, LONG_MOTIF,
                new BigDecimal("5000"), new BigDecimal("3000"), null, null,
                0, null, null, null, null, null, TODAY);
        assertThat(r.scoreGlobal()).isEqualTo(75);
        assertThat(r.verdictRevisionPossible()).isEqualTo("ELEVEE");
    }

    @Test
    void verdict_score49_estFaible() {
        // Pas de modif substantielle + motiv ok + ancienneté ok + critère ok = 0 + 25 + 15 + 10 = 50
        // Pour < 50 : pas de modif + motiv ko + ancienneté ko + critère ok = 10 → FAIBLE
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_3_MOIS, "x",
                new BigDecimal("5000"), new BigDecimal("4900"), null, null,
                1, List.of(8), null, null, null, null, TODAY);
        assertThat(r.scoreGlobal()).isLessThan(50);
        assertThat(r.verdictRevisionPossible()).isEqualTo("FAIBLE");
    }

    @Test
    void anciennete_calculee_correctement() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_15_MOIS, LONG_MOTIF,
                new BigDecimal("5000"), new BigDecimal("3000"), null, null,
                1, null, null, null, null, null, TODAY);
        assertThat(r.ancienneteDecisionMois()).isBetween(14, 16);
    }

    @Test
    void motivation_courte_estInsuffisante() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, "trop court",
                new BigDecimal("5000"), new BigDecimal("3000"), null, null,
                1, null, null, null, null, null, TODAY);
        assertThat(r.motivationSuffisante()).isFalse();
    }

    @Test
    void motivation_null_estInsuffisante() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, null,
                new BigDecimal("5000"), new BigDecimal("3000"), null, null,
                1, null, null, null, null, null, TODAY);
        assertThat(r.motivationSuffisante()).isFalse();
    }

    @Test
    void formule_contient_typeEtScore() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("4000"), new BigDecimal("1600"), null, null,
                2, List.of(10), null, null, null, null, TODAY);
        assertThat(r.formule()).contains("PENSION_ALIMENTAIRE").contains("100/100");
    }

    @Test
    void messages_contiennent_baseJuridique_pension() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("4000"), new BigDecimal("1600"), null, null,
                2, List.of(10), null, null, null, null, TODAY);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("209"));
    }

    @Test
    void revenusActuelsBaseSurCreancier_quandDebiteurAbsent() {
        RevisionsPostDivorceResult r = RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                null, null,
                new BigDecimal("2000"), new BigDecimal("3000"),
                1, null, null, null, null, null, TODAY);
        assertThat(r.ecartRevenusPct()).isEqualTo(50);
        assertThat(r.modificationSubstantielle()).isTrue();
    }

    // =========================================================================
    // Erreurs
    // =========================================================================

    @Test
    void typeRevisionInconnu_throws() {
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                "AUTRE_INCONNU", D_22_MOIS, LONG_MOTIF,
                null, null, null, null, 0, null, null, null, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRevision inconnu");
    }

    @Test
    void typeRevisionNull_throws() {
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                null, D_22_MOIS, LONG_MOTIF,
                null, null, null, null, 0, null, null, null, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRevision");
    }

    @Test
    void modeResidenceInconnu_throws() {
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                "RESIDENCE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null, 1, List.of(10),
                "INCONNU", "ALTERNEE",
                null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modeResidence");
    }

    @Test
    void dateFuture_throws() {
        LocalDate futur = TODAY.plusYears(1);
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", futur, LONG_MOTIF,
                null, null, null, null, 0, null, null, null, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void revenusNegatifs_throws() {
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                new BigDecimal("-1"), new BigDecimal("100"), null, null,
                0, null, null, null, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenus");
    }

    @Test
    void nbEnfantsACharge_negatif_throws() {
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                "PENSION_ALIMENTAIRE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null,
                -1, null, null, null, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nbEnfantsACharge");
    }

    @Test
    void distanceDemenagement_negatif_throws() {
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                "DEMENAGEMENT_PARENT", D_22_MOIS, LONG_MOTIF,
                null, null, null, null, 1, List.of(8),
                null, null, true, -10, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distanceDemenagementKm");
    }

    @Test
    void ageEnfantNegatif_throws() {
        assertThatThrownBy(() -> RevisionsPostDivorceCalculator.compute(
                "DROIT_VISITE", D_22_MOIS, LONG_MOTIF,
                null, null, null, null, 1, List.of(-1),
                null, null, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageEnfants");
    }
}
