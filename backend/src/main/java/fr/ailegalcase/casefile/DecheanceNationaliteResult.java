package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-220-05 : résultat de l'analyse de validité d'une mesure de déchéance de
 * nationalité (Cciv 25 / 25-1, F-IM-51-decheance-nationalite-fr). Outil
 * single-country FR.
 */
public record DecheanceNationaliteResult(
        String motif,
        Boolean binational,
        Boolean mesurePrononcee,
        String validite,
        List<String> conditionsManquantes,
        List<String> voiesRecours,
        Integer delaiRecoursJours,
        List<String> basesJuridiques,
        List<String> messages
) {}
