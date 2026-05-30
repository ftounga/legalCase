package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-01 : résultat interne business de l'analyse de l'appel d'un jugement
 * du Conseil de prud'hommes (CPH) devant la chambre sociale de la Cour d'appel.
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b>. Calcule le délai d'appel d'un mois
 * (art. 538 CPC ; R. 1461-1 CPC) et la checklist des formalités de l'appel
 * social (déclaration RPVA, chefs de jugement critiqués art. 901 CPC,
 * représentation obligatoire R. 1461-2 CPC, procédure orale art. 946 CPC,
 * constitution de l'intimé).
 *
 * @param dateNotificationJugement date de notification du jugement CPH.
 * @param partieAppelante salarié ou employeur.
 * @param modeNotification mode de notification (signification / LRAR).
 * @param representationConstituee qualité du représentant constitué.
 * @param jugementEnDernierRessort true si le jugement n'est pas susceptible
 *        d'appel (taux de compétence atteint, R. 1462-1 CPC).
 * @param dateLimiteAppel date limite pour interjeter appel (notification + 1 mois).
 * @param joursRestants jours calendaires restants jusqu'à la date limite
 *        (négatif si le délai est expiré ; non significatif si voie fermée).
 * @param verdict verdict de recevabilité.
 * @param checklist checklist des formalités de l'appel social.
 * @param renvoiPourvoiCassation renvoi vers le pourvoi en cassation (F-DT-87)
 *        lorsque la voie d'appel est fermée ; null sinon.
 * @param baseJuridique fondements juridiques applicables.
 */
public record AppelCphResult(
        LocalDate dateNotificationJugement,
        AppelCphPartieAppelante partieAppelante,
        AppelCphModeNotification modeNotification,
        AppelCphRepresentation representationConstituee,
        boolean jugementEnDernierRessort,
        LocalDate dateLimiteAppel,
        long joursRestants,
        AppelCphVerdict verdict,
        List<AppelCphChecklistItem> checklist,
        String renvoiPourvoiCassation,
        String baseJuridique
) {}
