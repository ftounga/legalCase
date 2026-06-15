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

    /**
     * Stades procéduraux de PREMIÈRE INSTANCE devant le tribunal du travail
     * belge (F-243 / {@code ProcedureStageCatalog}, juridiction {@code TT}) :
     * {@code FOND} et {@code REFERE}. Les pièces substantielles d'un litige du
     * travail BE y sont rattachées.
     */
    public static final List<String> STAGES_TT_PREMIERE_INSTANCE =
            List.of("FOND", "REFERE");

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

    // ========== PIÈCES TRAVAIL BE ==========
    // SF-294-02 — Aligné 1:1 avec le seed DB (migration 608, plage UUID
    // f2940002-...). Pièces de litige rattachées aux stades 1re instance TT
    // (FOND/REFERE) ; AVERTISSEMENTS_EVALUATIONS et MOTIVATION_CCT109 ne
    // concernent que le FOND. REGLEMENT_TRAVAIL / CCT_APPLICABLE sont
    // génériques (stages vide) → inclus quel que soit le stade.

    private static final List<ExpectedPiece> BE_PIECES = List.of(
            new ExpectedPiece("CONTRAT_TRAVAIL", "Contrat de travail", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 1),
            new ExpectedPiece("FICHES_PAIE", "Fiches de paie (dernières)", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 2),
            new ExpectedPiece("LETTRE_CONGE", "Lettre de notification du congé (licenciement)", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 3),
            new ExpectedPiece("C4", "Formulaire C4 (certificat de chômage)", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 4),
            new ExpectedPiece("CERTIFICAT_TRAVAIL", "Certificat de travail", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 5),
            new ExpectedPiece("DECOMPTE_SORTIE", "Décompte de sortie (dernières prestations)", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 6),
            new ExpectedPiece("DECOMPTE_INDEMNITE_RUPTURE", "Décompte de l'indemnité de rupture", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 7),
            new ExpectedPiece("ATTESTATION_VACANCES", "Attestation(s) de vacances", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, true, 8),
            new ExpectedPiece("FICHE_FISCALE_281_10", "Fiche fiscale 281.10", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, false, 9),
            new ExpectedPiece("MOTIVATION_CCT109", "Motivation du licenciement (CCT n°109) — demande + réponse", "BELGIQUE",
                    List.of("FOND"), false, 10),
            new ExpectedPiece("NOTIFICATION_MOTIF_GRAVE", "Notification du motif grave (si applicable)", "BELGIQUE",
                    STAGES_TT_PREMIERE_INSTANCE, false, 11),
            // Génériques : socle attendu quel que soit le stade procédural.
            new ExpectedPiece("REGLEMENT_TRAVAIL", "Règlement de travail", "BELGIQUE",
                    List.of(), false, 12),
            new ExpectedPiece("CCT_APPLICABLE", "CCT / commission paritaire applicable", "BELGIQUE",
                    List.of(), false, 13),
            new ExpectedPiece("AVERTISSEMENTS_EVALUATIONS", "Avertissements / évaluations (dossier disciplinaire)", "BELGIQUE",
                    List.of("FOND"), false, 14)
    );

    /**
     * Pièces attendues en droit du travail pour un pays donné.
     *
     * @param country {@code FRANCE} (SF-294-01) ou {@code BELGIQUE} (SF-294-02) ;
     *                tout autre pays renvoie une liste vide.
     * @return liste immuable des pièces attendues (tous stades confondus, le
     *         filtrage par stade est fait par le service appelant).
     */
    public static List<ExpectedPiece> getPieces(String country) {
        if ("FRANCE".equals(country)) return FR_PIECES;
        if ("BELGIQUE".equals(country)) return BE_PIECES;
        return List.of();
    }
}
