package fr.ailegalcase.casefile;

/**
 * SF-219-09 : cycle électoral 4 ans imposé par AR pour les élections
 * sociales en Belgique. Le jour Y (scrutin) est imposé par AR dans
 * une fenêtre nationale de 14 jours — l'employeur n'a pas la
 * liberté de fixer une date hors cycle.
 *
 * <p>Cycle 2024 : 13 au 26 mai 2024 (AR du 14/07/2022).<br>
 * Cycle 2028 : du 11 au 24 mai 2028 (annoncé, AR à venir).<br>
 * Cycle 2032 : prévu mai 2032.</p>
 *
 * <p>Note : la liste des cycles est exhaustive pour la V1 — les
 * cycles passés (2020 et antérieurs) ne sont pas exposés car
 * l'outil sert exclusivement à préparer une procédure à venir ou
 * en cours.</p>
 */
public enum ElectionsSocialesBeCycle {
    /** Cycle 2024 — fenêtre Y : 13 au 26 mai 2024 (AR 14/07/2022). */
    CYCLE_2024,

    /** Cycle 2028 — fenêtre Y annoncée : 11 au 24 mai 2028. */
    CYCLE_2028,

    /** Cycle 2032 — fenêtre Y prévisionnelle : mai 2032. */
    CYCLE_2032
}
