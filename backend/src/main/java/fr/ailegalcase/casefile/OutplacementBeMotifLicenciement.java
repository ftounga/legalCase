package fr.ailegalcase.casefile;

/**
 * SF-207-08 : motifs de rupture du contrat de travail conditionnant
 * l'obligation d'outplacement (CCT n°82 ; CCT n°82 bis ; Loi du 5 septembre
 * 2001 art. 13).
 *
 * <p>Substance juridique BE pure — l'obligation employeur d'outplacement
 * n'existe QUE pour un licenciement (hors faute grave). La démission et la
 * faute grave écartent automatiquement l'obligation.</p>
 */
public enum OutplacementBeMotifLicenciement {

    /** Licenciement pour motif économique (restructuration, fermeture). Obligation outplacement applicable. */
    LICENCIEMENT_ECONOMIQUE,

    /** Tout autre licenciement (motif personnel, insuffisance, etc.). Obligation outplacement applicable. */
    LICENCIEMENT_AUTRE,

    /** Licenciement pour faute grave (art. 35 Loi 03/07/1978) — exclut l'obligation employeur d'outplacement. */
    FAUTE_GRAVE,

    /** Démission du salarié — exclut l'obligation employeur d'outplacement. */
    DEMISSION
}
