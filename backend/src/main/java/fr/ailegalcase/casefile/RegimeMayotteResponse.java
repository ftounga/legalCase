package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-220-02 : réponse de l'analyse de portée territoriale du titre mahorais
 * (F-IM-48-regime-mayotte-fr).
 */
public record RegimeMayotteResponse(
        UUID caseFileId,
        boolean titreDelivreAMayotte,
        String typeTitre,
        boolean projetDeplacementMetropole,
        LocalDate dateDelivrance,
        String country,
        String porteeTerritoriale,
        String sousStatutDeplacement,
        List<String> obligationsSpecifiques,
        List<String> demarchesDeplacementMetropole,
        List<String> basesJuridiques,
        List<String> messages
) {}
