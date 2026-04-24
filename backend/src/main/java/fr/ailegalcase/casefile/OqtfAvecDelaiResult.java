package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-08-01 : résultat de l'analyse d'une OQTF assortie d'un délai de départ volontaire
 * (art. L.614-5 CESEDA). Calcule les délais de recours devant le TA et identifie les référés
 * disponibles. Outil single-country FR — équivalent BE (annexe 13) couvert par SF-IM-08-05.
 */
public record OqtfAvecDelaiResult(
        LocalDate dateNotificationOqtf,
        String motifOqtf,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateExpirationDdv,
        LocalDate dateExpirationDelaiRecours,
        int joursRestantsAvantExpirationDelai,
        String statutDelaiRecours,
        LocalDate dateAudiencePrevisionnelle,
        LocalDate dateDecisionTaPrevisionnelle,
        List<String> referedDisponibles,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
