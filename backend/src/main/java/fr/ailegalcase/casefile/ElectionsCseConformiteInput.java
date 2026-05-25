package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-31 : données d'entrée du calculateur de conformité de la procédure
 * d'élections du comité social et économique — CSE
 * (F-DT-65-elections-cse-conformite, FRANCE — L. 2314-1 à L. 2314-37 CT ;
 * R. 2314-1+ CT ; ordonnance n°2017-1386 du 22/09/2017 instituant le CSE ;
 * loi n°2018-217 du 29/03/2018 ratifiant les ordonnances Macron).
 *
 * <p>Vérifie 4 axes de conformité : protocole d'accord préélectoral (PAP),
 * délai d'invitation des organisations syndicales, conformité des collèges
 * électoraux, et bases de contestation des résultats. Le délai de
 * contestation est de <b>15 jours</b> à compter de l'élection (L. 2314-32 CT)
 * — délai très court, qualifié P1 procédural dans la mini-spec.</p>
 *
 * @param effectifEntreprise           effectif total de l'entreprise — seuil
 *                                     d'obligation CSE à 11 salariés
 *                                     (L. 2311-2 CT) ; en deçà, l'outil
 *                                     produit un message « CSE non obligatoire »
 * @param papNegocieAvecOS             protocole d'accord préélectoral négocié
 *                                     avec les organisations syndicales
 *                                     représentatives (PAP — L. 2314-6 CT) ;
 *                                     absent → IRREGULARITE_MAJEURE
 * @param delaiInvitationOSRespectee   invitation des OS à négocier le PAP
 *                                     reçue au moins 2 mois avant l'expiration
 *                                     des mandats en cours (L. 2314-4 CT)
 * @param collegesConformes            au moins 2 collèges électoraux constitués
 *                                     (ouvriers/employés + agents de maîtrise/
 *                                     cadres) — L. 2314-11 CT
 * @param resultatsContestes           résultats contestés par un électeur ou
 *                                     un syndicat — déclenche l'analyse des
 *                                     bases de contestation
 * @param dateElection                 date effective de l'élection (ISO) ;
 *                                     utilisée pour calculer la date limite
 *                                     de contestation et l'alerte délai court
 * @param motifContestation            motif libre de contestation invoqué
 *                                     (irrégularité PAP, collèges, vote
 *                                     électronique non encadré, etc.) ;
 *                                     informatif — repris dans la réponse
 */
public record ElectionsCseConformiteInput(
        Integer effectifEntreprise,
        Boolean papNegocieAvecOS,
        Boolean delaiInvitationOSRespectee,
        Boolean collegesConformes,
        Boolean resultatsContestes,
        LocalDate dateElection,
        String motifContestation
) {
}
