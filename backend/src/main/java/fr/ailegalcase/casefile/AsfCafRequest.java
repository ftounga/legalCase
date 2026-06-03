package fr.ailegalcase.casefile;

/**
 * SF-222-01 : body POST /api/v1/case-files/{id}/asf-caf-analysis.
 *
 * <p>Outil décisionnel ASF (allocation de soutien familial — art. L. 523-1 CSS),
 * Famille FRANCE uniquement. Évalue le droit à l'ASF versée par la CAF lorsque
 * l'autre parent ne paie pas (ou paie insuffisamment) la pension alimentaire,
 * et estime le montant mensuel.</p>
 *
 * <p>Anti-doublon : cet outil <b>ne calcule pas</b> le recouvrement forcé de la
 * pension (qui relève de {@code F-FA-ARIPA-RECOUVREMENT}). En cas d'ASF
 * récupérable, il renvoie vers l'outil ARIPA.</p>
 */
public record AsfCafRequest(
        Boolean parentIsole,
        Boolean pensionFixee,
        Integer montantPensionMensuel,
        PensionPayeeEnum pensionPayee,
        Integer nbEnfantsACharge,
        Boolean autreParentDecedeOuInconnu
) {}
