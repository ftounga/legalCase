package fr.ailegalcase.casefile;

/**
 * SF-213-09 : nature de la modification unilatérale imposée par l'employeur
 * dans l'analyse de l'acte équipollent à rupture (Loi 03/07/1978 art. 20,
 * BELGIQUE).
 *
 * <p>Les éléments suivants peuvent — selon leur ampleur et leur caractère
 * essentiel — caractériser un acte équipollent à rupture imputable à
 * l'employeur (Cass. BE 23/12/1957 et jurisprudence postérieure).</p>
 */
public enum TypeModificationUnilateraleEnum {
    /** Modification du salaire (taux, structure, primes contractuelles). */
    SALAIRE,
    /** Modification du lieu de travail (hors clause de mobilité contractuelle). */
    LIEU_TRAVAIL,
    /** Modification de la fonction (responsabilités, qualification). */
    FONCTION,
    /** Modification de l'horaire de travail. */
    HORAIRE,
    /** Autre élément du contrat. */
    AUTRE
}
