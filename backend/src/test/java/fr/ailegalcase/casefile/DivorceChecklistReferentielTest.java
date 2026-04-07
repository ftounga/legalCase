package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DivorceChecklistReferentielTest {
    @Test void france_hasAtLeast5Etapes() { assertThat(DivorceChecklistReferentiel.getEtapes("FRANCE")).hasSizeGreaterThanOrEqualTo(5); }
    @Test void belgique_hasAtLeast5Etapes() { assertThat(DivorceChecklistReferentiel.getEtapes("BELGIQUE")).hasSizeGreaterThanOrEqualTo(5); }
    @Test void france_hasAtLeast7Pieces() { assertThat(DivorceChecklistReferentiel.getPieces("FRANCE")).hasSizeGreaterThanOrEqualTo(7); }
    @Test void belgique_hasAtLeast6Pieces() { assertThat(DivorceChecklistReferentiel.getPieces("BELGIQUE")).hasSizeGreaterThanOrEqualTo(6); }
    @Test void etapes_orderedAndComplete() {
        for (DivorceEtape e : DivorceChecklistReferentiel.getAllEtapes()) {
            assertThat(e.code()).isNotBlank(); assertThat(e.label()).isNotBlank();
            assertThat(e.ordre()).isPositive(); assertThat(e.description()).isNotBlank();
        }
    }
    @Test void pieces_complete() {
        for (DivorcePiece p : DivorceChecklistReferentiel.getAllPieces()) {
            assertThat(p.code()).isNotBlank(); assertThat(p.label()).isNotBlank();
        }
    }
    @Test void baseJuridique_france() { assertThat(DivorceChecklistReferentiel.getBaseJuridique("FRANCE")).contains("229"); }
    @Test void baseJuridique_belgique() { assertThat(DivorceChecklistReferentiel.getBaseJuridique("BELGIQUE")).contains("1287"); }
}
