package fr.ailegalcase.casefile;

/**
 * SF-219-11 : région compétente pour le congé-éducation payé en
 * Belgique post-6e réforme de l'État (Loi spéciale du 06/01/2014,
 * compétence transférée aux Régions au 01/07/2014).
 *
 * <p>Chacune des trois Régions applique son propre régime — la
 * région pertinente est celle du <b>lieu de travail</b> du
 * travailleur (pas son lieu de résidence), conformément aux décrets
 * de transposition régionaux.</p>
 *
 * <ul>
 *   <li>{@link #WALLONIE} — Décret WBR 19/02/2014 + Arrêté GW
 *       19/12/2014. Maintien de l'appellation « congé-éducation payé ».</li>
 *   <li>{@link #FLANDRE} — Décret 12/12/2014 (Vlaams Opleidingsverlof
 *       — VOV), entrée en vigueur 01/09/2019. Refonte complète,
 *       formations agréées par le Vlaams Ministerie van Werk.</li>
 *   <li>{@link #BRUXELLES} — Ordonnance 02/07/2015 + Arrêté GBR
 *       14/04/2016. Maintien de l'appellation « congé-éducation
 *       payé », régime spécifique BCR.</li>
 * </ul>
 */
public enum CongeEducationPayeRegionRegion {
    /** Région wallonne — Décret 19/02/2014 + AGW 19/12/2014. */
    WALLONIE,

    /** Région flamande — Décret 12/12/2014 (Vlaams Opleidingsverlof VOV). */
    FLANDRE,

    /** Région de Bruxelles-Capitale — Ordonnance 02/07/2015 + AGBR 14/04/2016. */
    BRUXELLES
}
