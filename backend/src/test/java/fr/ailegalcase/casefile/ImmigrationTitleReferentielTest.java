package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImmigrationTitleReferentielTest {

    @Test
    void france_hasAtLeast8Titles() {
        List<TitleRecommendation> titles = ImmigrationTitleReferentiel.getTitles("FRANCE");
        assertThat(titles).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void belgique_hasAtLeast8Titles() {
        List<TitleRecommendation> titles = ImmigrationTitleReferentiel.getTitles("BELGIQUE");
        assertThat(titles).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void getTitles_byCountry_returnsOnlyThatCountry() {
        List<TitleRecommendation> fr = ImmigrationTitleReferentiel.getTitles("FRANCE");
        assertThat(fr).allMatch(t -> "FRANCE".equals(t.country()));

        List<TitleRecommendation> be = ImmigrationTitleReferentiel.getTitles("BELGIQUE");
        assertThat(be).allMatch(t -> "BELGIQUE".equals(t.country()));
    }

    @Test
    void getTitles_byCountryAndMotif_filtersCorrectly() {
        List<TitleRecommendation> result = ImmigrationTitleReferentiel.getTitles("FRANCE", "TRAVAIL");
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(t -> "FRANCE".equals(t.country()) && "TRAVAIL".equals(t.motif()));
    }

    @Test
    void getByCode_knownCode_returnsTitle() {
        TitleRecommendation title = ImmigrationTitleReferentiel.getByCode("VLS_TS_ETUDIANT");
        assertThat(title).isNotNull();
        assertThat(title.country()).isEqualTo("FRANCE");
        assertThat(title.motif()).isEqualTo("ETUDES");
    }

    @Test
    void getByCode_unknownCode_returnsNull() {
        assertThat(ImmigrationTitleReferentiel.getByCode("CODE_INCONNU")).isNull();
    }

    @Test
    void allTitles_haveNonEmptyFields() {
        for (TitleRecommendation title : ImmigrationTitleReferentiel.getAll()) {
            assertThat(title.code()).as("code").isNotBlank();
            assertThat(title.label()).as("label for %s", title.code()).isNotBlank();
            assertThat(title.country()).as("country for %s", title.code()).isIn("FRANCE", "BELGIQUE");
            assertThat(title.motif()).as("motif for %s", title.code()).isIn("TRAVAIL", "ETUDES", "FAMILLE", "ASILE", "AUTRE");
            assertThat(title.conditions()).as("conditions for %s", title.code()).isNotBlank();
            assertThat(title.pieces()).as("pieces for %s", title.code()).isNotEmpty();
            assertThat(title.delaiMoyenJours()).as("delai for %s", title.code()).isPositive();
        }
    }

    @Test
    void isCountryValid_supported() {
        assertThat(ImmigrationTitleReferentiel.isCountryValid("FRANCE")).isTrue();
        assertThat(ImmigrationTitleReferentiel.isCountryValid("BELGIQUE")).isTrue();
        assertThat(ImmigrationTitleReferentiel.isCountryValid("ALLEMAGNE")).isFalse();
    }

    @Test
    void isMotifValid_supported() {
        assertThat(ImmigrationTitleReferentiel.isMotifValid("TRAVAIL")).isTrue();
        assertThat(ImmigrationTitleReferentiel.isMotifValid("ETUDES")).isTrue();
        assertThat(ImmigrationTitleReferentiel.isMotifValid("FAMILLE")).isTrue();
        assertThat(ImmigrationTitleReferentiel.isMotifValid("ASILE")).isTrue();
        assertThat(ImmigrationTitleReferentiel.isMotifValid("AUTRE")).isTrue();
        assertThat(ImmigrationTitleReferentiel.isMotifValid("TOURISME")).isFalse();
    }

    @Test
    void eachMotif_hasTitlesForBothCountries() {
        for (String motif : List.of("TRAVAIL", "ETUDES", "FAMILLE", "ASILE")) {
            assertThat(ImmigrationTitleReferentiel.getTitles("FRANCE", motif))
                    .as("FRANCE/%s", motif).isNotEmpty();
            assertThat(ImmigrationTitleReferentiel.getTitles("BELGIQUE", motif))
                    .as("BELGIQUE/%s", motif).isNotEmpty();
        }
    }
}
