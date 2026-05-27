package fr.ailegalcase.casefile;

/**
 * SF-219-05 : verdict de l'outil de conformité de l'outplacement BE général
 * au titre du régime "préavis ≥ 30 semaines" (Loi du 05/09/2001 art. 11 +
 * AR du 21/10/2007).
 *
 * <p>Outil <b>BE-only</b> distinct de l'outplacement obligatoire 45+ ans
 * (F-207 SF-207-08 — autre régime, qui s'applique quelle que soit la durée
 * du préavis). Aucun équivalent strict en droit français.</p>
 *
 * <p>Hiérarchie d'évaluation (la première condition non remplie déclenche
 * le verdict associé) :
 * <ol>
 *   <li>Pas de licenciement (démission) → {@link #NON_DU_DEMISSION} ;</li>
 *   <li>Licenciement pour motif grave → {@link #NON_DU_MOTIF_GRAVE} ;</li>
 *   <li>Préavis &lt; 30 semaines → {@link #NON_DU_PREAVIS_INSUFFISANT} ;</li>
 *   <li>Pas d'offre ou offre &gt; 15 jours → {@link #NON_CONFORME_OFFRE_TARDIVE} ;</li>
 *   <li>Programme &lt; 60h ou période &gt; 12 mois →
 *       {@link #NON_CONFORME_DUREE_INSUFFISANTE} ;</li>
 *   <li>Forme non conforme (écrit / individuel / contenu minimum) →
 *       {@link #NON_CONFORME_FORME} ;</li>
 *   <li>Toutes conditions remplies → {@link #CONFORME}.</li>
 * </ol>
 * </p>
 */
public enum OutplacementBeGeneral30semVerdict {

    /** Toutes conditions remplies — offre d'outplacement régulière. */
    CONFORME,

    /** Démission : pas d'obligation d'offre d'outplacement (régime fermé). */
    NON_DU_DEMISSION,

    /** Licenciement pour motif grave : exclu du régime (art. 11/3 § 1er). */
    NON_DU_MOTIF_GRAVE,

    /** Préavis &lt; 30 semaines : régime "30 semaines" non applicable. */
    NON_DU_PREAVIS_INSUFFISANT,

    /** Offre absente ou notifiée &gt; 15 jours après fin du contrat. */
    NON_CONFORME_OFFRE_TARDIVE,

    /** Programme &lt; 60h ou période &gt; 12 mois (AR 21/10/2007). */
    NON_CONFORME_DUREE_INSUFFISANTE,

    /** Forme non conforme (non écrite / non individuelle / contenu minimum absent). */
    NON_CONFORME_FORME
}
