package fr.ailegalcase.casefile;

/**
 * SF-FA-13-01 : 5 types de révision post-divorce couverts par l'outil.
 *
 * <ul>
 *   <li>{@link #PENSION_ALIMENTAIRE} — art. 209 Cciv</li>
 *   <li>{@link #RESIDENCE} — art. 373-2-13 Cciv</li>
 *   <li>{@link #DROIT_VISITE} — art. 373-2-9 Cciv</li>
 *   <li>{@link #PRESTATION_COMPENSATOIRE} — art. 279 / 276-3 / 280-1 Cciv</li>
 *   <li>{@link #DEMENAGEMENT_PARENT} — art. 373-2 al. 3 Cciv</li>
 * </ul>
 */
public enum RevisionsPostDivorceTypeRevision {
    PENSION_ALIMENTAIRE,
    RESIDENCE,
    DROIT_VISITE,
    PRESTATION_COMPENSATOIRE,
    DEMENAGEMENT_PARENT
}
