package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-294 SF-294-02 — Tests unitaires du fallback Java {@link TravailPieceReferentiel}
 * pour le droit du travail belge, alignés sur le seed DB (migration 608).
 */
class TravailPieceReferentielTest {

    /** CA3 — la branche BELGIQUE renvoie exactement les 14 pièces seedées. */
    @Test
    void getPieces_belgique_returns14Pieces() {
        List<ExpectedPiece> pieces = TravailPieceReferentiel.getPieces("BELGIQUE");

        assertThat(pieces).hasSize(14);
        assertThat(pieces).extracting(ExpectedPiece::country).containsOnly("BELGIQUE");
        assertThat(pieces).extracting(ExpectedPiece::code).containsExactly(
                "CONTRAT_TRAVAIL", "FICHES_PAIE", "LETTRE_CONGE", "C4", "CERTIFICAT_TRAVAIL",
                "DECOMPTE_SORTIE", "DECOMPTE_INDEMNITE_RUPTURE", "ATTESTATION_VACANCES",
                "FICHE_FISCALE_281_10", "MOTIVATION_CCT109", "NOTIFICATION_MOTIF_GRAVE",
                "REGLEMENT_TRAVAIL", "CCT_APPLICABLE", "AVERTISSEMENTS_EVALUATIONS");
    }

    /** CA2 — REGLEMENT_TRAVAIL et CCT_APPLICABLE sont génériques (aucun stade). */
    @Test
    void getPieces_belgique_genericPieces_haveNoStage() {
        List<ExpectedPiece> pieces = TravailPieceReferentiel.getPieces("BELGIQUE");

        assertThat(pieces).filteredOn(ExpectedPiece::isGenerique)
                .extracting(ExpectedPiece::code)
                .containsExactly("REGLEMENT_TRAVAIL", "CCT_APPLICABLE");
    }

    /** MOTIVATION_CCT109 et AVERTISSEMENTS_EVALUATIONS ne concernent que le FOND. */
    @Test
    void getPieces_belgique_fondOnlyPieces() {
        List<ExpectedPiece> pieces = TravailPieceReferentiel.getPieces("BELGIQUE");

        assertThat(pieces).filteredOn(p -> p.code().equals("MOTIVATION_CCT109"))
                .singleElement().extracting(ExpectedPiece::stages).isEqualTo(List.of("FOND"));
        assertThat(pieces).filteredOn(p -> p.code().equals("AVERTISSEMENTS_EVALUATIONS"))
                .singleElement().extracting(ExpectedPiece::stages).isEqualTo(List.of("FOND"));
    }

    /**
     * CA8 — chaque stade référencé existe dans {@code ProcedureStageCatalog}
     * (DROIT_DU_TRAVAIL × BELGIQUE), pas de stade fantôme.
     */
    @Test
    void getPieces_belgique_stagesExistInCatalog() {
        Set<String> validStages = ProcedureStageCatalog
                .forDomainAndCountry("DROIT_DU_TRAVAIL", "BELGIQUE")
                .stages().stream()
                .map(ProcedureStageCatalog.Stage::code)
                .collect(java.util.stream.Collectors.toSet());

        for (ExpectedPiece p : TravailPieceReferentiel.getPieces("BELGIQUE")) {
            assertThat(validStages).as("stades de la pièce %s", p.code()).containsAll(p.stages());
        }
    }

    /** Non-régression FR : la branche FRANCE reste inchangée (8 pièces). */
    @Test
    void getPieces_france_unchanged() {
        List<ExpectedPiece> pieces = TravailPieceReferentiel.getPieces("FRANCE");

        assertThat(pieces).hasSize(8);
        assertThat(pieces).extracting(ExpectedPiece::country).containsOnly("FRANCE");
        assertThat(pieces).extracting(ExpectedPiece::code)
                .contains("CONTRAT_TRAVAIL", "LETTRE_LICENCIEMENT", "CONVENTION_COLLECTIVE");
    }

    /** Pays non couvert → liste vide. */
    @Test
    void getPieces_unknownCountry_returnsEmpty() {
        assertThat(TravailPieceReferentiel.getPieces("ALLEMAGNE")).isEmpty();
        assertThat(TravailPieceReferentiel.getPieces(null)).isEmpty();
    }
}
