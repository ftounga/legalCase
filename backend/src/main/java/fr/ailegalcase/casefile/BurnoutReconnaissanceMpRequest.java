package fr.ailegalcase.casefile;

/**
 * SF-212-27 : requête HTTP pour l'endpoint d'évaluation des chances de
 * reconnaissance du burn-out comme maladie professionnelle hors tableau
 * (F-DT-64-burnout-reconnaissance-mp, FRANCE — L. 461-1 al. 4 et 5 CSS).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record BurnoutReconnaissanceMpRequest(
        Boolean diagnosticBurnoutPose,
        Double tauxIPPEstime,
        Integer anneesExpositionProfessionnelle,
        Boolean surchargeChargeDocumentee,
        Boolean manquementsSecuriteDocumentes,
        Boolean harcelementConcomitant,
        Boolean arretsMaladieMultiples,
        Boolean lienCausalDirectEtabli
) {

    BurnoutReconnaissanceMpInput toInput() {
        return new BurnoutReconnaissanceMpInput(
                diagnosticBurnoutPose,
                tauxIPPEstime,
                anneesExpositionProfessionnelle,
                surchargeChargeDocumentee,
                manquementsSecuriteDocumentes,
                harcelementConcomitant,
                arretsMaladieMultiples,
                lienCausalDirectEtabli
        );
    }
}
