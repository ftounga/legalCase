package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SF-IM-08-03 : résultat de l'analyse d'une OQTF sans délai de départ volontaire
 * (art. L.731-1 CESEDA). Procédure d'extrême urgence — recours 48h devant juge unique TA
 * (L.614-8), audience 96h (R.776-26), décision 6 semaines (R.776-27).
 * Outil single-country FR — équivalent BE (procédure annexe 13 distincte) couvert par SF-IM-08-05.
 */
public record OqtfSansDelaiResult(
        LocalDateTime dateHeureNotificationOqtf,
        String motifSansDelai,
        boolean placementCra,
        boolean recoursForme,
        LocalDateTime dateHeureRecours,
        LocalDateTime dateHeureExpirationDelaiRecours,
        long heuresRestantes,
        String statutDelaiRecours,
        LocalDateTime dateHeureAudiencePrevisionnelle,
        LocalDate dateDecisionPrevisionnelle,
        List<String> refereDisponibles,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
