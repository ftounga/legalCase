package fr.ailegalcase.casefile;

/**
 * SF-219-04 : types d'activités complémentaires autorisés en cumul RCC.
 *
 * <p>L'ONEM permet certaines activités en complément du RCC sous des
 * conditions strictes (déclaration préalable, plafonds de revenus
 * accessoires). Toute reprise d'activité non listée ou non déclarée
 * entraîne la sortie du régime.</p>
 *
 * <ul>
 *   <li>{@link #AUCUNE} — pas d'activité complémentaire (cas standard).</li>
 *   <li>{@link #ACTIVITE_INDEPENDANT_ACCESSOIRE} — activité indépendante
 *       à titre accessoire, soumise à plafond de revenus et déclaration
 *       préalable ONEM.</li>
 *   <li>{@link #ACTIVITE_BENEVOLE_DECLAREE} — activité bénévole
 *       (association, mandat communal), déclarée préalablement à l'ONEM.</li>
 *   <li>{@link #ACTIVITE_NON_DECLAREE} — reprise d'activité non déclarée
 *       à l'ONEM : entraîne la sortie automatique du régime, restitution
 *       des allocations indûment perçues.</li>
 * </ul>
 */
public enum CumulRccAllocationsActiviteAutorisee {
    /** Aucune activité complémentaire — cas standard. */
    AUCUNE,

    /** Indépendant accessoire — soumis à plafond + déclaration ONEM. */
    ACTIVITE_INDEPENDANT_ACCESSOIRE,

    /** Bénévolat ou mandat — déclaration préalable ONEM requise. */
    ACTIVITE_BENEVOLE_DECLAREE,

    /** Activité non déclarée — sortie du régime + récupération ONEM. */
    ACTIVITE_NON_DECLAREE
}
