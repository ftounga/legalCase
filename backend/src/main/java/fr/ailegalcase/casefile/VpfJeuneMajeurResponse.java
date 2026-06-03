package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-220-03 : réponse de l'analyse d'éligibilité VPF jeune majeur L.423-22
 * (F-IM-49-vpf-jeune-majeur-l42322-fr). Outil single-country FR.
 */
public record VpfJeuneMajeurResponse(
        UUID caseFileId,
        int age,
        boolean entreMineur,
        LocalDate dateEntreeFrance,
        Integer ageEntreeAse,
        boolean priseEnChargeAse,
        LocalDate dateDebutPriseEnCharge,
        Integer ancienneteMoisPriseEnCharge,
        boolean scolariseOuFormation,
        boolean caractereReelEtSerieuxFormation,
        String country,
        String eligibilite,
        int ancienneteRequiseMois,
        List<String> criteresManquants,
        List<String> basesJuridiques,
        List<String> messages
) {}
