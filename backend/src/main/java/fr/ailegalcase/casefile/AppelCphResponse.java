package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-01 : réponse de l'analyse de l'appel d'un jugement CPH devant la
 * chambre sociale de la Cour d'appel (art. 538 CPC ; R. 1461-1 CPC). Outil
 * single-country FR.
 */
public record AppelCphResponse(
        UUID caseFileId,
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
        String country,
        String baseJuridique
) {}
