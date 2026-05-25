package fr.ailegalcase.casefile;

/**
 * SF-212-27 : données d'entrée du calculateur d'évaluation des chances
 * de reconnaissance du burn-out comme maladie professionnelle hors tableau
 * (F-DT-64-burnout-reconnaissance-mp, FRANCE — L. 461-1 al. 4 et 5 CSS).
 *
 * <p>Le seuil légal d'ouverture de la procédure CRRMP est un taux d'IPP
 * (incapacité permanente partielle) ≥ 25 % (al. 4) OU le décès du salarié
 * (al. 5). En deçà, la procédure hors tableau est irrecevable.</p>
 *
 * @param diagnosticBurnoutPose            diagnostic médical de burn-out
 *                                         posé (certificat, expertise)
 * @param tauxIPPEstime                    taux IPP estimé en %
 *                                         (doit être ≥ 25 pour CRRMP)
 * @param anneesExpositionProfessionnelle  années d'exposition au contexte
 *                                         professionnel mis en cause
 * @param surchargeChargeDocumentee        surcharge de travail documentée
 *                                         (heures sup, alertes CSE, RPS)
 * @param manquementsSecuriteDocumentes    manquements à l'obligation de
 *                                         sécurité L. 4121-1 CT documentés
 * @param harcelementConcomitant           harcèlement moral concomitant
 *                                         (L. 1152-1 CT)
 * @param arretsMaladieMultiples           arrêts maladie multiples ou
 *                                         prolongés en lien avec le travail
 * @param lienCausalDirectEtabli           lien direct entre la pathologie
 *                                         et le travail établi par une
 *                                         pièce médicale (L. 461-1 al. 4)
 */
public record BurnoutReconnaissanceMpInput(
        Boolean diagnosticBurnoutPose,
        Double tauxIPPEstime,
        Integer anneesExpositionProfessionnelle,
        Boolean surchargeChargeDocumentee,
        Boolean manquementsSecuriteDocumentes,
        Boolean harcelementConcomitant,
        Boolean arretsMaladieMultiples,
        Boolean lienCausalDirectEtabli
) {
}
