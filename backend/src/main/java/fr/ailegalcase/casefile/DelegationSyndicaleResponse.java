package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-33 : réponse de l'analyse du statut et de la protection d'un délégué
 * syndical / RSS (art. L.2143-1 et s., L.2142-1-1, L.2143-3, L.2411-3 CT,
 * F-DT-69). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public record DelegationSyndicaleResponse(
        UUID caseFileId,
        int effectif,
        MandatSyndicalType typeMandat,
        boolean syndicatRepresentatif,
        BigDecimal pourcentageScorePersonnel,
        LocalDate dateDesignation,
        List<DelegationSyndicaleChecklistItem> checklist,
        DelegationSyndicaleStatutDesignation statutDesignation,
        DelegationSyndicaleStatutProtege statutProtege,
        boolean licenciementEnvisage,
        boolean autorisationInspecteurTravail,
        DelegationSyndicaleRisqueNullite risqueNulliteLicenciement,
        List<String> consequences,
        String country,
        String baseJuridique
) {}
