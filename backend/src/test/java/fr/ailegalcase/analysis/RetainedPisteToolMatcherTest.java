package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-192 SF-192-01 — UT du mapping baseJuridique / keyword → toolId.
 */
class RetainedPisteToolMatcherTest {

    @Test
    void cesedaTitreArticle_mapsToTitre() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Demander un VPF",
                "Art. L.421-14 CESEDA");
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_TITRE);
    }

    @Test
    void cesedaTitreArticle421_1_mapsToTitre() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Carte de résident",
                "L.421-1 CESEDA");
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_TITRE);
    }

    @Test
    void cesedaRecoursArticleL512_mapsToRecours() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Recours contentieux",
                "L.512-1 CESEDA");
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_RECOURS);
    }

    @Test
    void cesedaRecoursArticleR776_mapsToRecours() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "REP devant le tribunal administratif",
                "R.776-1 CJA");
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_RECOURS);
    }

    @Test
    void rapoKeyword_mapsToRecours() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Engager un RAPO auprès du préfet",
                null);
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_RECOURS);
    }

    @Test
    void recoursHierarchiqueKeyword_mapsToRecours() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Privilégier un recours hiérarchique avant le contentieux",
                null);
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_RECOURS);
    }

    @Test
    void passeportTalentKeyword_mapsToTitre() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Demander un passeport talent",
                null);
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_TITRE);
    }

    @Test
    void vpfKeyword_mapsToTitre() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Solliciter une VPF en remplacement",
                null);
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_TITRE);
    }

    @Test
    void belgianLaw15_12_1980_recours_mapsToRecours() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Introduire un recours CCE",
                "loi du 15/12/1980, article 39/1");
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_RECOURS);
    }

    @Test
    void belgianLaw15_12_1980_titre_mapsToTitre() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Demande de séjour 9bis",
                "loi du 15 décembre 1980");
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_TITRE);
    }

    @Test
    void unrecognizedText_returnsNull() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Saisir le bâtonnier pour conciliation",
                "Article 21 du décret n° 91-1197");
        assertThat(tool).isNull();
    }

    @Test
    void emptyTexte_andBaseJuridique_returnsNull() {
        assertThat(RetainedPisteToolMatcher.resolveToolId("", "")).isNull();
        assertThat(RetainedPisteToolMatcher.resolveToolId(null, null)).isNull();
    }

    @Test
    void cesedaWithoutSpecificArticle_defaultsToTitre() {
        String tool = RetainedPisteToolMatcher.resolveToolId(
                "Régularisation administrative",
                "CESEDA");
        assertThat(tool).isEqualTo(RetainedPisteToolMatcher.TOOL_TITRE);
    }
}
