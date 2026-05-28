package fr.ailegalcase.casefile;

/**
 * SF-219-17 : verdict de l'outil <i>clause d'écolage BE — analyseur de
 * validité</i>.
 *
 * <h2>Conditions cumulatives de validité — art. 22bis Loi 03/07/1978
 *     (introduit par la Loi du 27/12/2006, M.B. 28/12/2006)</h2>
 * <ol>
 *   <li><b>Forme écrite obligatoire</b> au plus tard à l'entrée en
 *       formation (art. 22bis § 1er, al. 1er) reprenant la description
 *       de la formation, sa durée, le lieu, le coût réel, la durée
 *       d'efficacité de la clause et le montant dégressif du
 *       remboursement.</li>
 *   <li><b>Coût réel de la formation</b> &gt; à la moitié du <b>revenu
 *       minimum mensuel moyen garanti</b> (RMMMG, fixé par la CCT n° 43
 *       du CNT) du travailleur au moment de la signature
 *       (art. 22bis § 2, al. 3, 2°).</li>
 *   <li><b>Durée d'efficacité maximale</b> 3 ans (art. 22bis § 4, al. 1er) ;
 *       en pratique aussi limitée par la nature de la formation.</li>
 *   <li><b>Dégressivité du remboursement</b> par tiers de la durée
 *       d'efficacité (art. 22bis § 4, al. 2) : 100 % la 1<sup>re</sup>
 *       période, 2/3 (66 %) la 2<sup>e</sup>, 1/3 (33 %) la
 *       3<sup>e</sup>, plus rien au-delà de la durée d'efficacité.</li>
 *   <li><b>Formation spécifique</b> au sens de l'art. 22bis § 2 (pas une
 *       formation obligatoire en vertu d'une norme légale ou
 *       réglementaire).</li>
 *   <li><b>Motif de départ actionnable</b> : démission volontaire du
 *       travailleur OU licenciement pour motif grave imputable au
 *       travailleur. La clause est <b>inopposable</b> en cas de
 *       licenciement sans motif grave par l'employeur, de rupture pour
 *       motif grave dans le chef de l'employeur, ou d'expiration normale
 *       d'un CDD (art. 22bis § 3).</li>
 *   <li><b>Plafonnement absolu du remboursement</b> à 80 % du coût réel
 *       de la formation (art. 22bis § 4, al. 4) — quelle que soit la
 *       formule de dégressivité retenue.</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #INOPPOSABLE_MOTIF_DEPART} — motif de départ exclu par
 *       l'art. 22bis § 3 (licenciement sans motif grave par l'employeur,
 *       rupture pour motif grave dans le chef de l'employeur, expiration
 *       normale d'un CDD) — aucune somme n'est exigible, indépendamment
 *       de la régularité formelle de la clause.</li>
 *   <li>{@link #NULLE_FORME_ECRITE_MANQUANTE} — clause non rédigée par
 *       écrit au plus tard à l'entrée en formation (art. 22bis § 1er,
 *       al. 1er).</li>
 *   <li>{@link #NULLE_FORMATION_OBLIGATOIRE} — la formation est
 *       imposée par une norme légale ou réglementaire (art. 22bis § 2,
 *       al. 1er).</li>
 *   <li>{@link #NULLE_COUT_INSUFFISANT} — coût réel ≤ ½ × RMMMG mensuel
 *       (art. 22bis § 2, al. 3, 2°).</li>
 *   <li>{@link #NULLE_DUREE_EXCESSIVE} — durée d'efficacité &gt; 3 ans
 *       (art. 22bis § 4, al. 1er).</li>
 *   <li>{@link #VALIDE_REMBOURSEMENT_DEGRESSIF} — clause valable ; le
 *       remboursement est calculé selon la dégressivité par tiers et
 *       plafonné à 80 % du coût réel.</li>
 *   <li>{@link #VALIDE_DUREE_EXPIREE} — clause valable, mais la durée
 *       d'efficacité est déjà expirée à la date de départ — aucun
 *       remboursement exigible.</li>
 *   <li>{@link #A_ANALYSER} — qualification du type de formation
 *       indéterminée — analyse cas par cas requise.</li>
 * </ul>
 */
public enum ClauseEcolageBeVerdict {
    /** Motif de départ exclu (art. 22bis § 3) — clause inopposable. */
    INOPPOSABLE_MOTIF_DEPART,

    /** Pas d'écrit au plus tard à l'entrée en formation (art. 22bis § 1er). */
    NULLE_FORME_ECRITE_MANQUANTE,

    /** Formation imposée par norme légale / réglementaire (art. 22bis § 2 al. 1er). */
    NULLE_FORMATION_OBLIGATOIRE,

    /** Coût réel ≤ ½ × RMMMG (art. 22bis § 2 al. 3, 2°). */
    NULLE_COUT_INSUFFISANT,

    /** Durée d'efficacité &gt; 3 ans (art. 22bis § 4 al. 1er). */
    NULLE_DUREE_EXCESSIVE,

    /** Clause valable — remboursement dégressif par tiers, plafonné à 80 %. */
    VALIDE_REMBOURSEMENT_DEGRESSIF,

    /** Clause valable mais durée d'efficacité expirée — aucun remboursement. */
    VALIDE_DUREE_EXPIREE,

    /** Qualification type formation indéterminée — analyse cas par cas. */
    A_ANALYSER
}
