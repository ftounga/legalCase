package fr.ailegalcase.casefile;

import java.util.List;

/**
 * F-294 SF-294-01 — Référentiel statique (fallback Java) des pièces attendues
 * en droit du travail FR, indexées par stade procédural (F-243).
 *
 * <p>Modèle aligné sur {@link DivorceChecklistReferentiel}. Sert de
 * <b>fallback</b> à {@code LegalReferentialService.getExpectedPieces} quand la
 * table {@code legal_referentials} (type {@code EXPECTED_PIECES}) ne retourne
 * aucune entrée — la DB reste la source de vérité (CA11).
 *
 * <p>Les codes / libellés / stades sont alignés <b>1:1</b> avec le seed DB de la
 * migration {@code 607-f294-expected-pieces-travail-fr.xml} (« Valeurs
 * initiales » de la mini-spec). Toute modification doit être répercutée des deux
 * côtés.
 */
public final class TravailPieceReferentiel {

    private TravailPieceReferentiel() {}

    /**
     * Stades procéduraux de PREMIÈRE INSTANCE devant le conseil de prud'hommes
     * (F-243 / {@code ProcedureStageCatalog}, juridiction {@code CPH}).
     *
     * <p>ÉCART ASSUMÉ vs mini-spec : la mini-spec donnait l'exemple
     * {@code CPH_LICENCIEMENT}, qui <b>n'existe pas</b> dans
     * {@code ProcedureStageCatalog} ({@code CPH} y est un code de
     * <i>juridiction</i>, pas de stade). Pour respecter l'invariant « pas de 2ᵉ
     * taxonomie » (mini-spec Note + SF-294-00 invariant #5), les pièces
     * substantielles d'un dossier de licenciement sont rattachées aux <b>vrais
     * codes de stade première instance CPH</b>.
     */
    public static final List<String> STAGES_CPH_PREMIERE_INSTANCE =
            List.of("BCO", "FOND", "REFERE", "DEPARTAGE");

    // ========== PIÈCES TRAVAIL FR ==========
    // Aligné 1:1 avec le seed DB (migration 607). Les pièces "génériques"
    // (stages vide) sont incluses quel que soit le stade procédural.

    private static final List<ExpectedPiece> FR_PIECES = List.of(
            new ExpectedPiece("CONTRAT_TRAVAIL", "Contrat de travail", "FRANCE",
                    STAGES_CPH_PREMIERE_INSTANCE, true, 1),
            new ExpectedPiece("BULLETINS_PAIE_12M", "Bulletins de paie des 12 derniers mois", "FRANCE",
                    STAGES_CPH_PREMIERE_INSTANCE, true, 2),
            new ExpectedPiece("LETTRE_LICENCIEMENT", "Lettre de licenciement", "FRANCE",
                    STAGES_CPH_PREMIERE_INSTANCE, true, 3),
            new ExpectedPiece("CONVOCATION_ENTRETIEN_PREALABLE", "Convocation à l'entretien préalable", "FRANCE",
                    STAGES_CPH_PREMIERE_INSTANCE, true, 4),
            new ExpectedPiece("SOLDE_TOUT_COMPTE", "Reçu pour solde de tout compte", "FRANCE",
                    STAGES_CPH_PREMIERE_INSTANCE, true, 5),
            new ExpectedPiece("CERTIFICAT_TRAVAIL", "Certificat de travail", "FRANCE",
                    STAGES_CPH_PREMIERE_INSTANCE, true, 6),
            new ExpectedPiece("ATTESTATION_FRANCE_TRAVAIL", "Attestation France Travail", "FRANCE",
                    STAGES_CPH_PREMIERE_INSTANCE, true, 7),
            // Générique : socle attendu quel que soit le stade procédural.
            new ExpectedPiece("CONVENTION_COLLECTIVE", "Convention collective applicable", "FRANCE",
                    List.of(), true, 8)
    );

    /**
     * Pièces attendues en droit du travail pour un pays donné.
     *
     * @param country {@code FRANCE} (seule vague seedée en SF-294-01) ; tout
     *                autre pays renvoie une liste vide.
     * @return liste immuable des pièces attendues (tous stades confondus, le
     *         filtrage par stade est fait par le service appelant).
     */
    public static List<ExpectedPiece> getPieces(String country) {
        if (!"FRANCE".equals(country)) return List.of();
        return FR_PIECES;
    }
}
