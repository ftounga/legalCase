package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-11-01 : réponse complète de l'endpoint
 * <code>/api/v1/case-files/{id}/desunion-irremediable-be</code>.
 * Contrat figé pour SF-FA-11-02 frontend.
 */
public record DivorceDesunionIrremediableBeResponse(
        UUID caseFileId,
        LocalDate dateSeparation,
        boolean separationConsentue,
        boolean preuvesSeparation,
        boolean preuvesDocumentaires,
        boolean tentativesReconciliation,
        LocalDate dateAssignation,
        int dureeSeparationMois,
        int seuilSeparationMois,
        boolean delaiObjectifOk,
        boolean conditionsReunies,
        int scoreGlobal,
        String verdictProbabilite,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
