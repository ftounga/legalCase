package fr.ailegalcase.casefile;

/**
 * SF-214-09 : les 7 catégories d'OQTF de l'article L. 611-1 CESEDA
 * (recodification 2021 — anciens L. 511-1 I 1° à 7°).
 *
 * <ul>
 *   <li>{@link #CAT_1} — 1° entrée irrégulière sur le territoire</li>
 *   <li>{@link #CAT_2} — 2° maintien au-delà de la durée de séjour autorisée
 *       (séjour expiré)</li>
 *   <li>{@link #CAT_3} — 3° comportement frauduleux pour l'obtention d'un titre
 *       (fraude au titre)</li>
 *   <li>{@link #CAT_4} — 4° refus de délivrance ou de renouvellement d'un titre</li>
 *   <li>{@link #CAT_5} — 5° retrait d'un titre de séjour, récépissé ou
 *       autorisation provisoire</li>
 *   <li>{@link #CAT_6} — 6° comportement constituant une menace pour l'ordre
 *       public</li>
 *   <li>{@link #CAT_7} — 7° OQTF prise dans le cadre d'une procédure Dublin
 *       (remise à l'État responsable)</li>
 * </ul>
 */
public enum OqtfCategorieL611 {
    CAT_1,
    CAT_2,
    CAT_3,
    CAT_4,
    CAT_5,
    CAT_6,
    CAT_7
}
