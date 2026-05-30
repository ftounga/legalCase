package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-01 : requête POST pour l'analyse de l'appel d'un jugement CPH devant
 * la chambre sociale de la Cour d'appel (art. 538 CPC ; R. 1461-1 CPC). Outil
 * single-country FR.
 *
 * @param dateNotificationJugement date de notification du jugement CPH (requise).
 * @param partieAppelante salarié ou employeur (requis).
 * @param modeNotification mode de notification — défaut {@code SIGNIFICATION}.
 * @param representationConstituee qualité du représentant — défaut {@code AUCUNE}.
 * @param jugementEnDernierRessort true si le jugement n'est pas susceptible
 *        d'appel (taux de compétence atteint) — défaut false.
 */
public record AppelCphRequest(
        LocalDate dateNotificationJugement,
        AppelCphPartieAppelante partieAppelante,
        AppelCphModeNotification modeNotification,
        AppelCphRepresentation representationConstituee,
        boolean jugementEnDernierRessort
) {}
