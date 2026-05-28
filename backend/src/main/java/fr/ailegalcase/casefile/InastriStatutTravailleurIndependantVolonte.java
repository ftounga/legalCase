package fr.ailegalcase.casefile;

/**
 * SF-219-27 : critère 1 sur 4 de la qualification de la nature de la
 * relation de travail au sens de la Loi-programme I du 27/12/2006
 * art. 333 — <b>volonté des parties</b> telle qu'exprimée dans la
 * convention initiale.
 *
 * <p>La volonté exprimée est un indice mais ne lie pas l'auditeur du
 * travail ou le tribunal : la qualification dépend de l'execution
 * réelle de la relation (Cass. BE 23/12/2002 S.02.0021.F doctrine
 * Bart Buysse — primauté de l'execution sur la qualification
 * conventionnelle).</p>
 *
 * <ul>
 *   <li>{@link #INDEPENDANT_EXPLICITE} — convention écrite qualifiant
 *       expressément la relation d'indépendante (contrat d'entreprise,
 *       contrat de prestations, statut indépendant déclaré INASTI).</li>
 *   <li>{@link #SALARIE_EXPLICITE} — convention écrite qualifiant
 *       expressément la relation de salariée (contrat de travail
 *       Loi 03/07/1978).</li>
 *   <li>{@link #IMPLICITE_OU_AMBIGUE} — pas de qualification écrite
 *       claire ; la volonté ne peut être déduite que des
 *       comportements concordants (facturation, statut social, etc).</li>
 * </ul>
 */
public enum InastriStatutTravailleurIndependantVolonte {
    /** Convention écrite expressément indépendante. */
    INDEPENDANT_EXPLICITE,

    /** Convention écrite expressément salariée. */
    SALARIE_EXPLICITE,

    /** Qualification implicite ou ambiguë. */
    IMPLICITE_OU_AMBIGUE
}
