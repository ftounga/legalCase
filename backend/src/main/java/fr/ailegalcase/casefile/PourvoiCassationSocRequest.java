package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-05 : requête POST pour l'analyse d'un pourvoi en cassation devant la
 * chambre sociale (art. 612 CPC ; art. 604 CPC ; art. 973 CPC ; art. 1014 CPC).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateNotificationArret date de notification de l'arrêt de la Cour
 *        d'appel (requise, non future) — point de départ du délai de 2 mois.
 * @param casOuverture cas d'ouverture invoqués au soutien du pourvoi (requis,
 *        au moins un).
 * @param representationAvocatCassation true si un avocat aux Conseils est
 *        constitué (représentation obligatoire — art. 973 CPC) ; défaut false.
 * @param moyenSerieuxIdentifie true si un moyen sérieux de cassation est
 *        identifié (anti-filtre NPC — art. 1014 CPC) ; défaut false.
 */
public record PourvoiCassationSocRequest(
        LocalDate dateNotificationArret,
        List<PourvoiCassationSocCasOuverture> casOuverture,
        Boolean representationAvocatCassation,
        Boolean moyenSerieuxIdentifie
) {}
