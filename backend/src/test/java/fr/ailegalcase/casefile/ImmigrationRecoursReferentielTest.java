package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImmigrationRecoursReferentielTest {

    @Test
    void france_hasAtLeast3Types() {
        List<RecoursType> types = ImmigrationRecoursReferentiel.getTypes("FRANCE");
        assertThat(types).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void belgique_hasAtLeast3Types() {
        List<RecoursType> types = ImmigrationRecoursReferentiel.getTypes("BELGIQUE");
        assertThat(types).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void getTypes_byCountry_returnsOnlyThatCountry() {
        assertThat(ImmigrationRecoursReferentiel.getTypes("FRANCE"))
                .allMatch(t -> "FRANCE".equals(t.country()));
        assertThat(ImmigrationRecoursReferentiel.getTypes("BELGIQUE"))
                .allMatch(t -> "BELGIQUE".equals(t.country()));
    }

    @Test
    void getByCode_knownCode_returnsType() {
        RecoursType type = ImmigrationRecoursReferentiel.getByCode("RECOURS_GRACIEUX_PREFET");
        assertThat(type).isNotNull();
        assertThat(type.country()).isEqualTo("FRANCE");
        assertThat(type.delaiJours()).isEqualTo(60);
    }

    @Test
    void getByCode_unknownCode_returnsNull() {
        assertThat(ImmigrationRecoursReferentiel.getByCode("INCONNU")).isNull();
    }

    @Test
    void allTypes_haveNonEmptyFields() {
        for (RecoursType type : ImmigrationRecoursReferentiel.getAll()) {
            assertThat(type.code()).as("code").isNotBlank();
            assertThat(type.label()).as("label for %s", type.code()).isNotBlank();
            assertThat(type.country()).as("country for %s", type.code()).isIn("FRANCE", "BELGIQUE");
            assertThat(type.delaiJours()).as("delai for %s", type.code()).isPositive();
            assertThat(type.juridiction()).as("juridiction for %s", type.code()).isNotBlank();
            assertThat(type.textesApplicables()).as("textes for %s", type.code()).isNotEmpty();
            assertThat(type.piecesStandard()).as("pieces for %s", type.code()).isNotEmpty();
        }
    }

    @Test
    void isTypeValid_known() {
        assertThat(ImmigrationRecoursReferentiel.isTypeValid("RECOURS_CGRA")).isTrue();
        assertThat(ImmigrationRecoursReferentiel.isTypeValid("INCONNU")).isFalse();
    }

    @Test
    void isCountryValid_supported() {
        assertThat(ImmigrationRecoursReferentiel.isCountryValid("FRANCE")).isTrue();
        assertThat(ImmigrationRecoursReferentiel.isCountryValid("BELGIQUE")).isTrue();
        assertThat(ImmigrationRecoursReferentiel.isCountryValid("ALLEMAGNE")).isFalse();
    }
}
