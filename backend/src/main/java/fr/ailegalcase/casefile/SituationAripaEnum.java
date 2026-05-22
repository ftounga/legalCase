package fr.ailegalcase.casefile;

/**
 * SF-216-07 : situation professionnelle du créancier ou du débiteur d'aliments,
 * utilisée pour orienter la voie de recouvrement ARIPA (art. L. 581-1+ CSS).
 *
 * <p>{@link #INCONNU} n'a de sens que pour le débiteur (créancier toujours
 * identifié dans le dossier de l'avocat).</p>
 */
public enum SituationAripaEnum {
    SALARIE,
    CHOMEUR,
    INDEPENDANT,
    SANS_ACTIVITE,
    INCONNU
}
