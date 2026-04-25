package fr.ailegalcase.casefile;

/**
 * SF-DT-20-01 : méthode de calcul des congés payés sur un rappel de salaire
 * (art. L.3141-26 Code du travail).
 *
 * <ul>
 *   <li>{@link #DIX_POURCENT} — règle légale du dixième (10 % du brut), pratique majoritaire.</li>
 *   <li>{@link #MAINTIEN} — méthode du maintien (approximation 1/12 du brut, équivalent
 *       à 1 mois de CP par an de travail).</li>
 *   <li>{@link #AUCUN} — pas d'ICCP appliquée sur le rappel (typiquement CDD avec ICCP
 *       déjà versée séparément à la fin du contrat).</li>
 * </ul>
 */
public enum RappelSalaireMethodeCpSurRappel {
    DIX_POURCENT,
    MAINTIEN,
    AUCUN
}
