package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-FA-11-01 : payload de création/mise à jour pour
 * l'analyse de divorce pour désunion irrémédiable BE
 * (art. 229 §3 CC belge, Loi 27/04/2007).
 */
public record DivorceDesunionIrremediableBeRequest(
        LocalDate dateSeparation,
        Boolean separationConsentue,
        Boolean preuvesSeparation,
        Boolean preuvesDocumentaires,
        Boolean tentativesReconciliation,
        LocalDate dateAssignation
) {}
