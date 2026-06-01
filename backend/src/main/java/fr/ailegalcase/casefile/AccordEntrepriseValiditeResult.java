package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-218-31 : résultat interne business de l'analyse de validité d'un accord
 * d'entreprise au regard des conditions de majorité (art. L.2232-12 CT ;
 * L.2261-7 et s. CT, F-DT-67). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param pourcentageSuffragesSignataires pourcentage des suffrages exprimés au
 *        1er tour recueilli par les signataires.
 * @param typeOperation type d'opération (conclusion / révision / dénonciation).
 * @param referendumOrganise true si un référendum a été organisé.
 * @param referendumApprouve true si le référendum a approuvé l'accord.
 * @param conditionMajorite qualification de la condition de majorité.
 * @param dateDenonciation date de la dénonciation (null hors dénonciation).
 * @param dateFinSurvie date de fin de survie de l'accord (dénonciation : date de
 *        dénonciation + 3 mois de préavis + 12 mois de survie ; null sinon).
 * @param checklist items de validité (majorité, référendum, parties habilitées,
 *        préavis).
 * @param itemsNonConformes nombre d'items non conformes.
 * @param statut verdict global de validité.
 * @param consequences conséquences / points de vigilance identifiés.
 * @param baseJuridique fondements juridiques applicables.
 */
public record AccordEntrepriseValiditeResult(
        BigDecimal pourcentageSuffragesSignataires,
        AccordTypeOperation typeOperation,
        boolean referendumOrganise,
        boolean referendumApprouve,
        AccordConditionMajorite conditionMajorite,
        LocalDate dateDenonciation,
        LocalDate dateFinSurvie,
        List<AccordValiditeChecklistItem> checklist,
        int itemsNonConformes,
        AccordEntrepriseValiditeStatut statut,
        List<String> consequences,
        String baseJuridique
) {}
