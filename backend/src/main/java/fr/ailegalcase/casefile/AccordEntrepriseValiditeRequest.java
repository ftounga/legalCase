package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-31 : requête POST pour l'analyse de validité d'un accord d'entreprise
 * au regard des conditions de majorité (art. L.2232-12 CT ; L.2261-7 et s. CT,
 * F-DT-67). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param pourcentageSuffragesSignataires pourcentage des suffrages exprimés au
 *        1er tour des dernières élections recueilli par les syndicats signataires
 *        (requis, ∈ [0, 100]).
 * @param referendumOrganise true si un référendum de validation a été organisé
 *        (défaut false).
 * @param referendumApprouve true si le référendum a approuvé l'accord à la
 *        majorité des suffrages exprimés (défaut false).
 * @param typeOperation type d'opération : CONCLUSION / REVISION / DENONCIATION
 *        (requis).
 * @param signePartiesHabilitees true si l'avenant est signé par les parties
 *        habilitées à engager la révision (art. L.2261-7) ; requis si
 *        {@code typeOperation = REVISION}.
 * @param preavisDenonciationRespecte true si le préavis de dénonciation de 3 mois
 *        est respecté (défaut true ; pertinent en dénonciation).
 * @param dateDenonciation date de la dénonciation (optionnelle ; point de départ
 *        du calcul de la fin de survie en dénonciation).
 */
public record AccordEntrepriseValiditeRequest(
        BigDecimal pourcentageSuffragesSignataires,
        Boolean referendumOrganise,
        Boolean referendumApprouve,
        AccordTypeOperation typeOperation,
        Boolean signePartiesHabilitees,
        Boolean preavisDenonciationRespecte,
        LocalDate dateDenonciation
) {}
