package fr.ailegalcase.casefile;

/**
 * SF-219-06 : statut de solvabilité de l'employeur à la date de
 * fermeture — détermine l'intervention du FFE en reprise de créances.
 *
 * <p>Le FFE n'intervient en reprise des créances salariales impayées
 * (salaires, pécule, indemnité de rupture) que si l'employeur est
 * effectivement insolvable ou en faillite — sinon, les créances restent
 * à charge de l'employeur (recouvrement civil).</p>
 *
 * <ul>
 *   <li>{@link #SOLVABLE} — l'employeur honore ses créances : FFE
 *       intervient uniquement pour l'indemnité de fermeture (à charge
 *       de l'employeur, ou subsidiairement du FFE en cas de défaut
 *       postérieur).</li>
 *   <li>{@link #INSOLVABLE_AVERE} — défaut de paiement constaté
 *       (procédure ou jugement), FFE reprend les créances dans la
 *       limite des plafonds légaux.</li>
 *   <li>{@link #FAILLITE_DECLAREE} — faillite déclarée (Tribunal de
 *       l'entreprise) : insolvabilité présumée, FFE intervient
 *       automatiquement.</li>
 * </ul>
 */
public enum LicenciementBeFermetureEntrepriseStatutEmployeur {
    /** Employeur solvable — créances payées, FFE = indemnité seule. */
    SOLVABLE,

    /** Insolvabilité avérée par procédure ou jugement — FFE reprise créances. */
    INSOLVABLE_AVERE,

    /** Faillite déclarée par le Tribunal de l'entreprise — FFE intervient. */
    FAILLITE_DECLAREE
}
