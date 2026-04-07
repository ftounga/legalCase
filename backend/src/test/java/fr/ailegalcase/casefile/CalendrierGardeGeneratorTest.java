package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalendrierGardeGeneratorTest {
    @Test void alternee_france_5050() {
        CalendrierGardeResult r = CalendrierGardeGenerator.generate("ALTERNEE_FR", "Marie", "Pierre");
        assertThat(r.joursParAnParentA()).isEqualTo(182); assertThat(r.joursParAnParentB()).isEqualTo(183);
        assertThat(r.commentaire()).contains("Marie").contains("Pierre");
    }
    @Test void dvhClassique_france_6832() {
        CalendrierGardeResult r = CalendrierGardeGenerator.generate("DVH_CLASSIQUE_FR", "Marie", "Pierre");
        assertThat(r.joursParAnParentA()).isEqualTo(249); assertThat(r.joursParAnParentB()).isEqualTo(116);
    }
    @Test void alternee_belgique() {
        CalendrierGardeResult r = CalendrierGardeGenerator.generate("ALTERNEE_BE", "Sophie", "Marc");
        assertThat(r.country()).isEqualTo("BELGIQUE"); assertThat(r.baseJuridique()).contains("374");
    }
    @Test void dvhElargi_belgique() {
        CalendrierGardeResult r = CalendrierGardeGenerator.generate("SECONDAIRE_ELARGI_BE", "A", "B");
        assertThat(r.joursParAnParentA()).isEqualTo(220); assertThat(r.joursParAnParentB()).isEqualTo(145);
    }
    @Test void allModes_generateOk() {
        for (GardeMode g : GardeModeReferentiel.getAll()) {
            CalendrierGardeResult r = CalendrierGardeGenerator.generate(g.code(), "PA", "PB");
            assertThat(r.gardeCode()).isEqualTo(g.code());
            assertThat(r.joursParAnParentA() + r.joursParAnParentB()).isEqualTo(365);
        }
    }
    @Test void unknown_throws() {
        assertThatThrownBy(() -> CalendrierGardeGenerator.generate("X", "A", "B"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
