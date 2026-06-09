package fr.ailegalcase.casefile.conclusion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-57 — tests unitaires de l'assemblage déterministe du bordereau de pièces.
 */
class BordereauPiecesBuilderTest {

    private static CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece piece(
            int number, String label, String type) {
        return new CaseConclusionPromptBuilder.ConclusionPromptInput.NumberedPiece(number, label, type);
    }

    @Test
    void buildBordereau_nullList_returnsEmptyString() {
        assertThat(BordereauPiecesBuilder.buildBordereau(null)).isEmpty();
    }

    @Test
    void buildBordereau_emptyList_returnsEmptyString() {
        assertThat(BordereauPiecesBuilder.buildBordereau(List.of())).isEmpty();
    }

    @Test
    void buildBordereau_nonEmpty_formatsSectionWithHeaderAndTypeLabel() {
        String out = BordereauPiecesBuilder.buildBordereau(List.of(
                piece(1, "CDI Dupont", "CONTRAT"),
                piece(2, "Lettre de licenciement", "LETTRE")));

        assertThat(out).startsWith("\n\n## BORDEREAU DE PIÈCES COMMUNIQUÉES\n\n");
        assertThat(out).contains("1. CDI Dupont (Contrat)");
        // « Lettre » déjà présent dans le label → pas de doublon « (Lettre) »
        assertThat(out).endsWith("2. Lettre de licenciement").doesNotContain("(Lettre)");
    }

    @Test
    void buildBordereau_sortsByNumberAscending() {
        String out = BordereauPiecesBuilder.buildBordereau(List.of(
                piece(7, "Bulletin mars", "BULLETIN_PAIE"),
                piece(2, "Bulletin février", "BULLETIN_PAIE"),
                piece(5, "Bulletin janvier", "BULLETIN_PAIE")));

        int p2 = out.indexOf("2. Bulletin février");
        int p5 = out.indexOf("5. Bulletin janvier");
        int p7 = out.indexOf("7. Bulletin mars");
        assertThat(p2).isLessThan(p5);
        assertThat(p5).isLessThan(p7);
    }

    @Test
    void buildBordereau_preservesPersistentNumbers() {
        String out = BordereauPiecesBuilder.buildBordereau(List.of(
                piece(7, "CDI Dupont", "CONTRAT")));

        assertThat(out).contains("7. CDI Dupont");
        assertThat(out).doesNotContain("1. CDI Dupont");
    }

    @Test
    void buildBordereau_antiJargon_neverEmitsRawEnumTokenWhenLabelPresent() {
        String out = BordereauPiecesBuilder.buildBordereau(List.of(
                piece(1, "Avenant 2023", "CONTRAT"),
                piece(2, "Notification rupture", "BULLETIN_PAIE")));

        // jamais le token d'enum brut
        assertThat(out).doesNotContain("CONTRAT").doesNotContain("BULLETIN_PAIE");
        // type traduit en libellé lisible
        assertThat(out).contains("(Contrat)").contains("(Bulletin de paie)");
    }

    @Test
    void buildBordereau_emptyLabel_fallsBackToTypeLabel() {
        String out = BordereauPiecesBuilder.buildBordereau(List.of(
                piece(1, "  ", "BULLETIN_PAIE")));

        assertThat(out).contains("1. Bulletin de paie");
    }

    @Test
    void buildBordereau_noLabelNoType_fallsBackToNeutralLabel() {
        String out = BordereauPiecesBuilder.buildBordereau(List.of(
                piece(1, null, null)));

        assertThat(out).contains("1. Pièce communiquée");
    }

    @Test
    void buildBordereau_autreType_doesNotAppendUninformativeTypeLabel() {
        String out = BordereauPiecesBuilder.buildBordereau(List.of(
                piece(1, "Document divers", "AUTRE")));

        // AUTRE n'enrichit pas un label déjà présent
        assertThat(out).contains("1. Document divers").doesNotContain("(Pièce)");
    }
}
