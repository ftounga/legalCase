package fr.ailegalcase.casefile;

/**
 * SF-219-17 : type de formation visée par la clause d'écolage BE.
 *
 * <p>La Loi du 03/07/1978 sur les contrats de travail, art. 22bis (§ 2),
 * exige que la formation soit <b>spécifique</b> et permette
 * l'acquisition de connaissances ou de compétences nouvelles, qui
 * profitent en premier lieu au travailleur (et qui pourraient donc être
 * monnayées sur le marché du travail). La formation purement <i>obligatoire
 * en vertu d'une disposition légale ou réglementaire</i> (recyclage
 * obligatoire, formation obligatoire de sécurité) ne peut pas faire
 * l'objet d'une clause d'écolage.</p>
 *
 * <h2>Régimes</h2>
 * <ul>
 *   <li>{@link #SPECIFIQUE} — formation spécifique au sens de l'art.
 *       22bis § 2 (clause valable si autres conditions remplies).</li>
 *   <li>{@link #OBLIGATOIRE_LEGALE} — formation imposée par une norme
 *       (KB, AR, CCT sectorielle obligatoire) — clause nulle de plein
 *       droit, l'employeur ne peut pas en réclamer le remboursement.</li>
 *   <li>{@link #INDETERMINE} — qualification non encore opérée — analyse
 *       au cas par cas requise.</li>
 * </ul>
 */
public enum ClauseEcolageBeTypeFormation {
    /** Formation spécifique au sens de l'art. 22bis § 2 — clause valable possible. */
    SPECIFIQUE,

    /** Formation imposée par norme légale ou réglementaire — clause nulle de plein droit. */
    OBLIGATOIRE_LEGALE,

    /** Qualification indéterminée — analyse cas par cas requise. */
    INDETERMINE
}
