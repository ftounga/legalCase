package fr.ailegalcase.casefile;

/**
 * SF-216-21 : nature de la preuve disponible pour caractériser le recel
 * successoral (art. 778 Cciv).
 *
 * <p>La charge de la preuve repose sur les cohéritiers demandeurs (Cass.
 * 1ère civ. constante). La jurisprudence (Cass. 1ère civ., 14/11/2012,
 * n° 11-20.582) admet un faisceau d'indices.</p>
 *
 * <ul>
 *   <li>{@link #AVEUX} — aveu de l'héritier receleur (preuve la plus
 *       forte).</li>
 *   <li>{@link #DOCUMENT} — pièce écrite probante (relevé bancaire,
 *       acte caché, courriel, etc.).</li>
 *   <li>{@link #TEMOIGNAGE} — témoignage d'un tiers ou d'un cohéritier
 *       sous attestation art. 202 CPC.</li>
 *   <li>{@link #EXPERTISE} — expertise notariale, comptable ou
 *       graphologique.</li>
 *   <li>{@link #FAISCEAU_INDICES} — concordance d'indices admise par
 *       jurisprudence (Cass. 1ère civ., 14/11/2012).</li>
 *   <li>{@link #AUCUNE} — aucune preuve disponible : action vouée à
 *       l'échec.</li>
 * </ul>
 */
public enum PreuveRecelEnum {
    AVEUX,
    DOCUMENT,
    TEMOIGNAGE,
    EXPERTISE,
    FAISCEAU_INDICES,
    AUCUNE
}
