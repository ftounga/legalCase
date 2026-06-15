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
 *   <li><b>SF-294-03 / CA1</b> : un dossier Famille FR reçoit comme socle les
 *       pièces {@code DIVORCE_PIECES} FR mappées en {@link ExpectedPiece}
 *       (réutilisation, sans seed {@code EXPECTED_PIECES} dédié).</li>
 *   <li><b>CA6</b> : un domaine non couvert (Immigration) renvoie un socle vide
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
    void travailBe_firstInstanceStage_resolvesSeededPiecesFromDb_CA1() {
        // SF-294-02 — Travail BE au stade FOND (tribunal du travail) : socle seedé
        // (migration 608) résolu DB-first, filtré par stade.
        List<ExpectedPiece> socle = service.getExpectedPieces("DROIT_DU_TRAVAIL", "BELGIQUE", "FOND");

        assertThat(socle).isNotEmpty();
        assertThat(socle).extracting(ExpectedPiece::label)
                .contains(
                        "Contrat de travail",
                        "Fiches de paie (dernières)",
                        "Lettre de notification du congé (licenciement)",
                        "Formulaire C4 (certificat de chômage)",
                        "Règlement de travail",
                        "CCT / commission paritaire applicable",
                        "Motivation du licenciement (CCT n°109) — demande + réponse");
        assertThat(socle).extracting(ExpectedPiece::country).containsOnly("BELGIQUE");
    }

    @Test
    void travailBe_noStage_onlyGenericPieces_CA2() {
        // SF-294-02 / CA2 — sans stade, seules les pièces génériques BE remontent.
        List<ExpectedPiece> socle = service.getExpectedPieces("DROIT_DU_TRAVAIL", "BELGIQUE", null);

        assertThat(socle).extracting(ExpectedPiece::label)
                .containsExactly("Règlement de travail", "CCT / commission paritaire applicable");
    }

    @Test
    void travailFr_noStage_onlyGenericPieces_CA5() {
        List<ExpectedPiece> socle = service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", null);

        assertThat(socle).extracting(ExpectedPiece::label)
                .containsExactly("Convention collective applicable");
    }

    @Test
    void familleFr_reusesDivorcePieces_CA1() {
        // SF-294-03 : Famille délègue à getDivorcePieces (DB-first + fallback Java),
        // pas de seed EXPECTED_PIECES dédié. Pièces génériques → incluses quel que
        // soit le stade.
        List<ExpectedPiece> socle = service.getExpectedPieces("DROIT_FAMILLE", "FRANCE", "FOND");

        assertThat(socle).isNotEmpty();
        assertThat(socle).extracting(ExpectedPiece::label)
                .contains(
                        // Libellés EXACTS du seed DB DIVORCE_PIECES (migration 067, source de vérité).
                        // ⚠ Le fallback Java DivorceChecklistReferentiel diverge ("de l'acte", "des deux
                        // époux") — incohérence Java↔DB pré-existante signalée comme dette ; sans impact
                        // prod (DB toujours seedée). On asserte la réalité prod = DB.
                        "Copie intégrale acte de mariage",
                        "Actes de naissance des époux",
                        "Pièces d'identité des époux");
        assertThat(socle).extracting(ExpectedPiece::country).containsOnly("FRANCE");
    }

    @Test
    void immigration_notCovered_returnsEmpty_CA6() {
        assertThat(service.getExpectedPieces("DROIT_IMMIGRATION", "FRANCE", "FOND")).isEmpty();
    }
}
