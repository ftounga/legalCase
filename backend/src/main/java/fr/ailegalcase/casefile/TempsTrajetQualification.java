package fr.ailegalcase.casefile;

/**
 * SF-218-51 : verdict de qualification du temps de trajet (F-DT-81). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>TEMPS_TRAVAIL : le déplacement est qualifié de temps de travail effectif
 *       (salarié itinérant sans lieu de travail fixe, CJUE C-266/14).</li>
 *   <li>TRAJET_AVEC_CONTREPARTIE : le trajet dépasse le temps normal et ouvre
 *       droit à une contrepartie (repos ou financière).</li>
 *   <li>TRAJET_SANS_CONTREPARTIE : trajet n'excédant pas le temps normal — pas de
 *       contrepartie due.</li>
 * </ul>
 */
public enum TempsTrajetQualification {
    TEMPS_TRAVAIL,
    TRAJET_AVEC_CONTREPARTIE,
    TRAJET_SANS_CONTREPARTIE
}
