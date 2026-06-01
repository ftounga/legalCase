package fr.ailegalcase.casefile;

/**
 * SF-218-33 : verdict de régularité de la désignation du délégué syndical (DS)
 * ou du représentant de section syndicale (RSS) (art. L.2143-1 et s.,
 * L.2142-1-1, L.2143-3 CT, F-DT-69). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>REGULIERE : tous les items de désignation sont conformes.</li>
 *   <li>IRREGULIERE : un item d'effectif ou de représentativité n'est pas
 *       conforme — la désignation est contestable / annulable.</li>
 *   <li>A_VERIFIER : désignation d'un DS dont le score personnel n'est pas
 *       renseigné (condition des 10 % à confirmer, L.2143-3).</li>
 * </ul>
 */
public enum DelegationSyndicaleStatutDesignation {
    REGULIERE,
    IRREGULIERE,
    A_VERIFIER
}
