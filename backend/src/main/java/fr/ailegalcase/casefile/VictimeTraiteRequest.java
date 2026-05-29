package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-21 : requête POST pour l'analyse d'éligibilité au titre victime de la
 * traite des êtres humains L. 425-1 CESEDA. Outil single-country FR.
 */
public record VictimeTraiteRequest(
        Boolean plainteDeposee,
        Boolean collaborationOCRTEH,
        LocalDate datePlainte,
        String titreActuel,
        Boolean presenceAutoriteRefugieDetectee
) {}
