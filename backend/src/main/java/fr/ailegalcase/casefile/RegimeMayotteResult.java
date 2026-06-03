package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-220-02 : résultat de l'analyse de portée territoriale du titre mahorais
 * (F-IM-48-regime-mayotte-fr). Outil single-country FR.
 */
public record RegimeMayotteResult(
        boolean titreDelivreAMayotte,
        String typeTitre,
        boolean projetDeplacementMetropole,
        String porteeTerritoriale,
        String sousStatutDeplacement,
        List<String> obligationsSpecifiques,
        List<String> demarchesDeplacementMetropole,
        List<String> basesJuridiques,
        List<String> messages
) {}
