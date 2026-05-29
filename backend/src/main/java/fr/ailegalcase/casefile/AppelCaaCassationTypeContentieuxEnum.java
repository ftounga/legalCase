package fr.ailegalcase.casefile;

/**
 * SF-214-33 : nature du contentieux des étrangers porté devant le TA, déterminant
 * le régime de délai et l'application du filtre d'admission des pourvois en cassation.
 *
 * <ul>
 *   <li>OQTF : obligation de quitter le territoire français — contentieux soumis au
 *       filtre d'admission des pourvois (art. L. 821-2 CJA) et, le cas échéant, au
 *       délai d'appel réduit à 15 jours (OQTF sans délai de départ volontaire).</li>
 *   <li>REFUS_TITRE : refus / retrait de titre de séjour — délai d'appel de droit
 *       commun (1 mois).</li>
 *   <li>EXPULSION : arrêté d'expulsion — délai d'appel de droit commun (1 mois).</li>
 *   <li>AUTRE : autre contentieux des étrangers — délai d'appel de droit commun.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b>.
 */
public enum AppelCaaCassationTypeContentieuxEnum {
    OQTF,
    REFUS_TITRE,
    EXPULSION,
    AUTRE
}
