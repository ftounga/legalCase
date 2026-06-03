package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-220-03 : résultat de l'analyse d'éligibilité VPF jeune majeur L.423-22
 * (F-IM-49-vpf-jeune-majeur-l42322-fr). Outil single-country FR.
 */
public record VpfJeuneMajeurResult(
        int age,
        boolean entreMineur,
        Integer ageEntreeAse,
        boolean priseEnChargeAse,
        Integer ancienneteMoisPriseEnCharge,
        boolean scolariseOuFormation,
        boolean caractereReelEtSerieuxFormation,
        String eligibilite,
        int ancienneteRequiseMois,
        List<String> criteresManquants,
        List<String> basesJuridiques,
        List<String> messages
) {}
