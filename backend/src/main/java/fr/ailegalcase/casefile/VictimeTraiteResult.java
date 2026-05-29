package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-214-21 : résultat de l'analyse d'éligibilité au titre victime de la traite
 * des êtres humains L. 425-1 CESEDA. Outil single-country FR.
 */
public record VictimeTraiteResult(
        boolean plainteDeposee,
        boolean collaborationOCRTEH,
        String datePlainte,
        String titreActuel,
        boolean presenceAutoriteRefugieDetectee,
        String verdict,
        List<String> chipsCriteresManquants,
        List<String> mesuresProtection,
        boolean risqueVictimeEnDanger,
        List<String> recommandations,
        String baseJuridique
) {}
