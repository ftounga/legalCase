package fr.ailegalcase.casefile;

/**
 * SF-219-07 : tranche de taille de l'entreprise au sens de la Loi du
 * 13/02/1998 (loi Renault) — détermine le seuil de licenciements
 * déclenchant l'application de la procédure de licenciement collectif.
 *
 * <p>La Loi 13/02/1998 (art. 2) définit le licenciement collectif comme
 * tout licenciement pour motif non inhérent à la personne du travailleur
 * touchant, sur une période de 60 jours, un nombre de travailleurs qui
 * dépend de l'effectif moyen de l'entreprise calculé sur les 4
 * trimestres civils précédents :</p>
 *
 * <ul>
 *   <li>{@link #PME_20_99} — entreprise occupant en moyenne plus de 20
 *       et moins de 100 travailleurs : seuil = au moins 10 licenciements
 *       sur 60 jours.</li>
 *   <li>{@link #GRANDE_100_299} — entreprise occupant en moyenne au moins
 *       100 et moins de 300 travailleurs : seuil = au moins 10 % de
 *       l'effectif sur 60 jours (= 10 à 30 licenciements selon la taille
 *       exacte).</li>
 *   <li>{@link #TRES_GRANDE_300_PLUS} — entreprise occupant en moyenne
 *       au moins 300 travailleurs : seuil = au moins 30 licenciements
 *       sur 60 jours.</li>
 *   <li>{@link #PETITE_MOINS_20} — entreprise &lt; 20 travailleurs : la
 *       Loi Renault ne s'applique pas (procédure d'information CCT n° 9
 *       générale subsiste, mais hors scope de cet outil).</li>
 * </ul>
 */
public enum LicenciementBeCollectifRenaultTailleEntreprise {
    /** Effectif < 20 — procédure Renault non applicable. */
    PETITE_MOINS_20,

    /** Effectif 20-99 — seuil 10 licenciements / 60 jours. */
    PME_20_99,

    /** Effectif 100-299 — seuil 10 % de l'effectif / 60 jours. */
    GRANDE_100_299,

    /** Effectif ≥ 300 — seuil 30 licenciements / 60 jours. */
    TRES_GRANDE_300_PLUS
}
