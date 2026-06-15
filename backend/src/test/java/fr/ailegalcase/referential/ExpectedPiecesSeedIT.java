package fr.ailegalcase.referential;

import fr.ailegalcase.casefile.ExpectedPiece;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-294 SF-294-01 — IT DB-backed : vérifie que le seed {@code EXPECTED_PIECES}
 * (migration 607) est appliqué par Liquibase et que
 * {@link LegalReferentialService#getExpectedPieces} le résout depuis la base.
 *
 * <ul>
 *   <li><b>CA2</b> : un dossier Travail FR au stade première instance CPH reçoit
 *       au minimum les pièces seedées (résolution DB-first réelle).</li>
 *   <li><b>CA5</b> : sans stade, seules les pièces génériques remontent.</li>
 *   <li><b>CA6</b> : un domaine non seedé (Famille) renvoie un socle vide
 *       (comportement 100 % LLM inchangé).</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
class ExpectedPiecesSeedIT {

    @Autowired
    LegalReferentialService service;

    @Test
    void travailFr_firstInstanceStage_resolvesSeededPiecesFromDb_CA2() {
        List<ExpectedPiece> socle = service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", "FOND");

        assertThat(socle).isNotEmpty();
        assertThat(socle).extracting(ExpectedPiece::label)
                .contains(
                        "Contrat de travail",
                        "Bulletins de paie des 12 derniers mois",
                        "Lettre de licenciement",
                        "Convocation à l'entretien préalable",
                        "Reçu pour solde de tout compte",
                        "Certificat de travail",
                        "Attestation France Travail",
                        "Convention collective applicable");
    }

    @Test
    void travailFr_noStage_onlyGenericPieces_CA5() {
        List<ExpectedPiece> socle = service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", null);

        assertThat(socle).extracting(ExpectedPiece::label)
                .containsExactly("Convention collective applicable");
    }

    @Test
    void famille_notSeeded_returnsEmpty_CA6() {
        assertThat(service.getExpectedPieces("DROIT_FAMILLE", "FRANCE", "FOND")).isEmpty();
    }
}
