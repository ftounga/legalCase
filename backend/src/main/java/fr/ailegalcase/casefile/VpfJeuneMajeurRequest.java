package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-220-03 : requête POST pour l'outil décisionnel VPF jeune majeur L.423-22
 * (F-IM-49-vpf-jeune-majeur-l42322-fr). Outil single-country FR.
 */
public record VpfJeuneMajeurRequest(
        Integer age,
        Boolean entreMineur,
        LocalDate dateEntreeFrance,
        Integer ageEntreeAse,
        Boolean priseEnChargeAse,
        LocalDate dateDebutPriseEnCharge,
        Integer ancienneteMoisPriseEnCharge,
        Boolean scolariseOuFormation,
        Boolean caractereReelEtSerieuxFormation,
        Boolean avisStructureFavorable,
        Boolean absenceLienFamillePays
) {}
