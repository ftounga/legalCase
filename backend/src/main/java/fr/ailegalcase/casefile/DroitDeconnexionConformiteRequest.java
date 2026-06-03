package fr.ailegalcase.casefile;

/**
 * SF-218-53 : requête POST pour l'analyse de conformité à l'obligation relative
 * au droit à la déconnexion (art. L.2242-17 7° CT, F-DT-83). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise (requis, &gt; 0).
 * @param delegueSyndicalPresent true si au moins un délégué syndical est désigné
 *        (requis).
 * @param accordOuChartePresent true si un accord ou une charte sur le droit à la
 *        déconnexion existe (requis).
 * @param plagesDeconnexionDefinies true si des plages / modalités de déconnexion
 *        sont définies (requis).
 * @param actionsSensibilisation true si des actions de formation / sensibilisation
 *        sont prévues (requis).
 * @param avisCseRecueilliPourCharte true si l'avis du CSE a été recueilli avant
 *        l'élaboration de la charte le cas échéant (requis).
 */
public record DroitDeconnexionConformiteRequest(
        Integer effectif,
        Boolean delegueSyndicalPresent,
        Boolean accordOuChartePresent,
        Boolean plagesDeconnexionDefinies,
        Boolean actionsSensibilisation,
        Boolean avisCseRecueilliPourCharte
) {}
