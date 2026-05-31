package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-29 : requête POST pour l'analyse de conformité de la négociation annuelle
 * obligatoire (NAO, art. L.2242-1 à L.2242-8, L.2242-11, L.2242-15, L.2242-17 CT,
 * F-DT-66). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param effectif effectif de l'entreprise (requis, &gt; 0). La NAO est obligatoire
 *        dès lors qu'un délégué syndical est désigné (en pratique ≥ 50 salariés).
 * @param delegueSyndicalPresent true si au moins un délégué syndical est désigné
 *        (déclencheur de l'obligation, art. L.2242-1 ; requis).
 * @param blocRemunerationNegocie true si le bloc « rémunération, temps de travail
 *        et partage de la valeur ajoutée » a été engagé (art. L.2242-15 ; requis).
 * @param blocEgaliteQvtNegocie true si le bloc « égalité professionnelle F/H et
 *        qualité de vie au travail » a été engagé (art. L.2242-17 ; requis).
 * @param accordMethodePeriodicite true si un accord de méthode porte la périodicité
 *        au-delà de l'année (max 4 ans, art. L.2242-11 ; défaut false).
 * @param dateDerniereNegociation date de la dernière négociation engagée
 *        (optionnelle, point de départ du calcul d'échéance).
 * @param periodiciteMois périodicité retenue en mois (défaut 12 ; 13–48 uniquement
 *        avec accord de méthode).
 * @param pvDesaccordEtabli true si un PV de désaccord a été établi en cas d'échec
 *        (défaut false).
 * @param negociationAboutie true si la négociation a abouti à un accord (défaut
 *        false ; dispense alors du PV de désaccord).
 */
public record NaoNegociationAnnuelleRequest(
        Integer effectif,
        Boolean delegueSyndicalPresent,
        Boolean blocRemunerationNegocie,
        Boolean blocEgaliteQvtNegocie,
        Boolean accordMethodePeriodicite,
        LocalDate dateDerniereNegociation,
        Integer periodiciteMois,
        Boolean pvDesaccordEtabli,
        Boolean negociationAboutie
) {}
