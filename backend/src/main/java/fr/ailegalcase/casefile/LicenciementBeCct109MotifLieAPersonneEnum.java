package fr.ailegalcase.casefile;

/**
 * SF-213-10 : qualification du motif de licenciement lié à la personne du
 * travailleur (conduite, aptitude, comportement) au sens de l'art. 8 CCT
 * n° 109 du 12/02/2014.
 *
 * <h2>Sémantique</h2>
 * <ul>
 *   <li>{@link #SANS_MOTIF} — aucun motif n'a été communiqué ou un motif
 *       totalement vide. Bascule au minimum 3 semaines.</li>
 *   <li>{@link #MOTIF_VAGUE} — un motif a été donné mais reste imprécis
 *       (« perte de confiance », « réorganisation » sans détail). Reste au
 *       minimum 3 semaines.</li>
 *   <li>{@link #MOTIF_PRECIS_DISPUTE} — un motif précis est invoqué mais le
 *       salarié le conteste, et au moins un facteur aggravant mineur est
 *       constaté (procédure incomplète, motif partiellement étayé).
 *       Bascule à 8 semaines.</li>
 *   <li>{@link #MOTIF_PROUVE_VALIDE} — un motif valable au sens de l'art. 8
 *       CCT 109 est prouvé (faute du salarié documentée, raisons économiques
 *       reconnues, etc.). Si les procédures sont également respectées →
 *       NON_DERAISONNABLE, pas d'indemnité.</li>
 * </ul>
 */
public enum LicenciementBeCct109MotifLieAPersonneEnum {
    SANS_MOTIF,
    MOTIF_VAGUE,
    MOTIF_PRECIS_DISPUTE,
    MOTIF_PROUVE_VALIDE
}
