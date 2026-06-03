package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-220-05 : réponse de l'analyse de validité d'une mesure de déchéance de
 * nationalité (Cciv 25 / 25-1, F-IM-51-decheance-nationalite-fr). Outil
 * single-country FR.
 */
public record DecheanceNationaliteResponse(
        UUID caseFileId,
        String motif,
        Boolean binational,
        LocalDate dateAcquisitionNationalite,
        LocalDate dateFaits,
        Boolean mesurePrononcee,
        LocalDate dateDecret,
        String country,
        String validite,
        List<String> conditionsManquantes,
        List<String> voiesRecours,
        Integer delaiRecoursJours,
        List<String> basesJuridiques,
        List<String> messages
) {}
