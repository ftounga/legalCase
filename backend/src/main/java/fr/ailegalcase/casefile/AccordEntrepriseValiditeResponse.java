package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-31 : réponse de l'analyse de validité d'un accord d'entreprise au regard
 * des conditions de majorité (art. L.2232-12 CT ; L.2261-7 et s. CT, F-DT-67).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record AccordEntrepriseValiditeResponse(
        UUID caseFileId,
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
        String country,
        String baseJuridique
) {}
