package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-37 : réponse de l'analyse d'une interdiction du territoire français
 * (ITF) prononcée par un juge pénal (C. pén. 131-30). Outil single-country FR.
 */
public record ItfJudiciaireResponse(
        UUID caseFileId,
        LocalDate dateCondamnation,
        int dureeITFAnnees,
        String infractionPrincipale,
        boolean condamnationDefinitive,
        LocalDate dateEcheanceReleve,
        List<String> voiesRecours,
        List<String> requisReleve,
        String distinctionItfVsIrtf,
        ItfJudiciaireStatut statut,
        String country,
        String baseJuridique
) {}
