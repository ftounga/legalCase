package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GardeModeReferentielTest {
    @Test void france_hasAtLeast3() { assertThat(GardeModeReferentiel.getByCountry("FRANCE")).hasSizeGreaterThanOrEqualTo(3); }
    @Test void belgique_hasAtLeast3() { assertThat(GardeModeReferentiel.getByCountry("BELGIQUE")).hasSizeGreaterThanOrEqualTo(3); }
    @Test void allHaveFields() {
        for (GardeMode g : GardeModeReferentiel.getAll()) {
            assertThat(g.code()).isNotBlank(); assertThat(g.label()).isNotBlank();
            assertThat(g.baseJuridique()).isNotBlank(); assertThat(g.periodesParentA()).isNotEmpty();
        }
    }
    @Test void getByCode_known() { assertThat(GardeModeReferentiel.getByCode("ALTERNEE_FR")).isNotNull(); }
    @Test void getByCode_unknown() { assertThat(GardeModeReferentiel.getByCode("X")).isNull(); }
}
