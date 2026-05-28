package fr.ailegalcase.casefile;

/**
 * SF-219-26 : présence d'éléments de subordination caractérisant le
 * salariat au sens de l'art. 1<sup>er</sup> de la Loi du 27/06/1969
 * révisant l'arrêté-loi du 28/12/1944 et de l'art. 328 § 1
 * C. pén. soc. (présomption simple de salariat applicable à toute
 * personne pour laquelle aucune DIMONA n'a été effectuée).
 *
 * <p>La présomption de salariat est <b>simple</b> (Cass. BE
 * 06/12/2010 S.10.0067.F) et renversable par tout moyen de preuve.
 * L'avocat doit caractériser concrètement l'existence ou l'absence
 * de subordination juridique (horaires imposés, instructions, lieu
 * de travail imposé, exclusivité, intégration dans l'organisation
 * de l'employeur, fourniture du matériel).</p>
 */
public enum TravailNoirBeDimonaElementsSubordination {
    /** Éléments de subordination clairement caractérisés — salariat. */
    OUI,

    /** Aucun élément de subordination — relation indépendante. */
    NON,

    /** Indéterminé en l'état — analyse complémentaire avocat requise. */
    INDETERMINE
}
