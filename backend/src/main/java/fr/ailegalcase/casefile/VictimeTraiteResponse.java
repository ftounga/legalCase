package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-214-21 : réponse de l'analyse d'éligibilité au titre victime de la traite
 * des êtres humains L. 425-1 CESEDA.
 */
public record VictimeTraiteResponse(
        UUID caseFileId,
        boolean plainteDeposee,
        boolean collaborationOCRTEH,
        String datePlainte,
        String titreActuel,
        boolean presenceAutoriteRefugieDetectee,
        String country,
        String verdict,
        List<String> chipsCriteresManquants,
        List<String> mesuresProtection,
        boolean risqueVictimeEnDanger,
        List<String> recommandations,
        String baseJuridique
) {}
