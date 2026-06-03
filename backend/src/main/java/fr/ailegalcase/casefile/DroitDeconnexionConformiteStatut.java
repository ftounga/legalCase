package fr.ailegalcase.casefile;

/**
 * SF-218-53 : verdict de conformité à l'obligation relative au droit à la
 * déconnexion (art. L.2242-17 7° CT, F-DT-83). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONFORME : l'obligation de négocier le droit à la déconnexion s'applique
 *       (effectif ≥ 50 ET au moins un délégué syndical) et tous les items de la
 *       checklist sont remplis (accord ou charte, plages définies, actions de
 *       sensibilisation, avis du CSE recueilli en cas de charte).</li>
 *   <li>NON_CONFORME : l'obligation s'applique mais au moins un item de la
 *       checklist n'est pas satisfait.</li>
 *   <li>NON_REQUIS : l'obligation de négocier n'est pas déclenchée (effectif
 *       inférieur à 50 salariés ou absence de délégué syndical) ; l'employeur
 *       reste libre d'adopter une charte.</li>
 * </ul>
 */
public enum DroitDeconnexionConformiteStatut {
    CONFORME,
    NON_CONFORME,
    NON_REQUIS
}
