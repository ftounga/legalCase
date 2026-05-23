package fr.ailegalcase.casefile;

/**
 * SF-216-21 : type de recel successoral envisagé (art. 778 Cciv).
 *
 * <ul>
 *   <li>{@link #DISSIMULATION_BIEN} — dissimulation intentionnelle d'un
 *       bien (mobilier ou immobilier) appartenant à la succession.</li>
 *   <li>{@link #DISSIMULATION_DONATION} — dissimulation d'une donation
 *       rapportable ou réductible reçue du défunt
 *       (art. 778 + 850 Cciv).</li>
 *   <li>{@link #DESTRUCTION_TESTAMENT} — destruction, recel ou
 *       falsification du testament. Volet pénal complémentaire signalé
 *       (art. 441-8 CP, hors scope LegalCase V1).</li>
 *   <li>{@link #RECEL_CREANCE} — dissimulation d'une créance dont le
 *       défunt était titulaire (art. 778 visé par la jurisprudence).</li>
 *   <li>{@link #AUTRE} — autre forme de recel à caractériser dans la
 *       requête.</li>
 * </ul>
 */
public enum TypeRecelEnum {
    DISSIMULATION_BIEN,
    DISSIMULATION_DONATION,
    DESTRUCTION_TESTAMENT,
    RECEL_CREANCE,
    AUTRE
}
