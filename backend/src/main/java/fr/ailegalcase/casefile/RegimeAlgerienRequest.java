package fr.ailegalcase.casefile;

/**
 * SF-IM-17-01 : requête pour l'analyse du régime franco-algérien.
 *
 * <p>Régime parallèle au CESEDA, applicable uniquement aux ressortissants <b>algériens</b>,
 * fondé sur l'<i>accord franco-algérien du 27 décembre 1968</i> (modifié par avenants
 * 22 décembre 1985, 28 septembre 1994 et 11 juillet 2001).
 *
 * <p>Outil <b>single-country FRANCE</b>. Aucun équivalent côté Belgique.
 *
 * @param voieDemande                       code voie : {@code CRA_1_AN} (art. 5),
 *                                          {@code CRA_10_ANS_LIEN_FRANCE} (art. 6),
 *                                          {@code CRA_10_ANS_RESIDENT_ANCIEN} (art. 7bis),
 *                                          {@code CHANGEMENT_VERS_TRAVAILLEUR} (art. 7),
 *                                          {@code REGROUPEMENT_FAMILIAL_ACCORD_1968} (art. 4)
 * @param nationaliteAlgerienne             nationalité algérienne (gate métier — false → 400)
 * @param documentEtatCivilOriginal         acte d'état civil algérien original disponible
 * @param presenceReguliereFranceMois       nombre de mois de présence régulière en France
 * @param casierJudiciaireVierge            casier vierge (default true)
 * @param visaLongSejourValide              visa de long séjour valide (CRA 1 an)
 * @param conjointFrancais                  marié à un français (CRA 10 ans art. 6 al. 1)
 * @param parentEnfantFrancais              parent d'un enfant français (CRA 10 ans art. 6 al. 2)
 * @param neEnFrance                        ressortissant algérien né en France (art. 7bis)
 * @param arriveeAvant13Ans                 arrivé en France avant l'âge de 13 ans (art. 7bis)
 * @param contratTravailValide              contrat de travail valide (changement vers travailleur)
 * @param ressourcesSuffisantes             ressources stables et suffisantes (regroupement familial)
 * @param logementDecent                    logement décent et adapté (regroupement familial)
 * @param nombrePersonnesFoyer              nombre de personnes du foyer (regroupement familial)
 */
public record RegimeAlgerienRequest(
        String voieDemande,
        Boolean nationaliteAlgerienne,
        Boolean documentEtatCivilOriginal,
        Integer presenceReguliereFranceMois,
        Boolean casierJudiciaireVierge,
        Boolean visaLongSejourValide,
        Boolean conjointFrancais,
        Boolean parentEnfantFrancais,
        Boolean neEnFrance,
        Boolean arriveeAvant13Ans,
        Boolean contratTravailValide,
        Boolean ressourcesSuffisantes,
        Boolean logementDecent,
        Integer nombrePersonnesFoyer
) {}
