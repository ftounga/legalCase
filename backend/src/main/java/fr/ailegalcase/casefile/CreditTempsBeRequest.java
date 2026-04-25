package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-DT-29-01 : requête pour l'analyse du crédit-temps belge
 * (CCT 103 + AR 29/10/1997). BE uniquement.
 */
public record CreditTempsBeRequest(
        String regime,
        String motif,
        Integer ancienneteEntrepriseMois,
        Integer tailleEntrepriseEtp,
        String dureeReductionType,
        Integer ageDemandeurAnnees,
        LocalDate dateDemande
) {}
