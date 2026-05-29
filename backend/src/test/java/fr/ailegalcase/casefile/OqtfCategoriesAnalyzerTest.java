package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-09 : tests unitaires de {@link OqtfCategoriesAnalyzer}. Couvre les 7
 * catégories L. 611-1, les moyens de défense, le délai de recours
 * (avec/sans délai) et la procédure parallèle (IRTF CAT_6, Dublin CAT_7).
 */
class OqtfCategoriesAnalyzerTest {

    private static final LocalDate HIER = LocalDate.now().minusDays(1);

    // ── Une par catégorie : libellé + base juridique ─────────────────────

    @Test
    void analyze_cat1_entreeIrreguliere() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_1, HIER, "Entrée sans visa");
        assertThat(r.categorieL611()).isEqualTo(OqtfCategorieL611.CAT_1);
        assertThat(r.categorieLibelle()).containsIgnoringCase("entrée irrégulière");
        assertThat(r.baseJuridique()).contains("L. 611-1 1°");
        assertThat(r.moyensDefense()).anySatisfy(m ->
                assertThat(m).containsIgnoringCase("notification"));
    }

    @Test
    void analyze_cat2_sejourExpire() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_2, HIER, null);
        assertThat(r.baseJuridique()).contains("L. 611-1 2°");
        assertThat(r.moyensDefense()).isNotEmpty();
    }

    @Test
    void analyze_cat3_fraudeAuTitre() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_3, HIER, null);
        assertThat(r.baseJuridique()).contains("L. 611-1 3°");
        assertThat(r.moyensDefense()).anySatisfy(m ->
                assertThat(m).containsIgnoringCase("fraude"));
    }

    @Test
    void analyze_cat4_refusDeTitre() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_4, HIER, null);
        assertThat(r.baseJuridique()).contains("L. 611-1 4°");
        assertThat(r.moyensDefense()).isNotEmpty();
    }

    @Test
    void analyze_cat5_retraitDeTitre() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_5, HIER, null);
        assertThat(r.baseJuridique()).contains("L. 611-1 5°");
        assertThat(r.moyensDefense()).anySatisfy(m ->
                assertThat(m).containsIgnoringCase("retrait"));
    }

    // ── CAT_6 : menace ordre public → délai 48 h + IRTF ──────────────────

    @Test
    void analyze_cat6_menaceOrdrePublic_sansDelai_procedureIrtf() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_6, HIER, "Trouble à l'ordre public");
        assertThat(r.baseJuridique()).contains("L. 611-1 6°");
        // OQTF sans délai → recours 48 h
        assertThat(r.delaiRecours()).isEqualTo(OqtfCategoriesAnalyzer.TYPE_RECOURS_SANS_DELAI);
        assertThat(r.delaiRecoursHeures()).isEqualTo(48);
        assertThat(r.delaiRecoursJours()).isNull();
        // moyen de défense : proportionnalité art. 8 CEDH
        assertThat(r.moyensDefense()).anySatisfy(m ->
                assertThat(m).containsIgnoringCase("proportionnalité"));
        // procédure parallèle IRTF L. 612-6
        assertThat(r.procedureParallele()).contains("L. 612-6");
    }

    // ── CAT_7 : Dublin → délai 48 h + renvoi F-IM-22 ─────────────────────

    @Test
    void analyze_cat7_dublin_sansDelai_renvoiFim22() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_7, HIER, null);
        assertThat(r.baseJuridique()).contains("L. 611-1 7°");
        assertThat(r.delaiRecours()).isEqualTo(OqtfCategoriesAnalyzer.TYPE_RECOURS_SANS_DELAI);
        assertThat(r.delaiRecoursHeures()).isEqualTo(48);
        assertThat(r.procedureParallele()).contains("F-IM-22");
        assertThat(r.procedureParallele()).containsIgnoringCase("Dublin");
    }

    // ── Délai avec délai (30 j) pour les catégories ordinaires ───────────

    @Test
    void analyze_cat1_avecDelai_recours30j() {
        OqtfCategoriesResult r = OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_1, HIER, null);
        assertThat(r.delaiRecours()).isEqualTo(OqtfCategoriesAnalyzer.TYPE_RECOURS_AVEC_DELAI);
        assertThat(r.delaiRecoursJours()).isEqualTo(30);
        assertThat(r.delaiRecoursHeures()).isNull();
        assertThat(r.procedureParallele()).isNull();
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_dateFuture_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_1, LocalDate.now().plusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationOqtf");
    }

    @Test
    void analyze_categorieNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> OqtfCategoriesAnalyzer.analyze(
                null, HIER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categorieL611");
    }

    @Test
    void analyze_motifTropLong_lanceIllegalArgumentException() {
        String tropLong = "x".repeat(301);
        assertThatThrownBy(() -> OqtfCategoriesAnalyzer.analyze(
                OqtfCategorieL611.CAT_1, HIER, tropLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifOqtf");
    }
}
