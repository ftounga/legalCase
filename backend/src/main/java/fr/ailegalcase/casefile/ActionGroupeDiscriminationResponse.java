package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-09 : réponse de l'analyse de recevabilité d'une action de groupe en
 * discrimination au travail (art. L. 1134-7 à L. 1134-10 Code travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record ActionGroupeDiscriminationResponse(
        UUID caseFileId,
        ActionGroupeDiscriminationTypeOrganisation typeOrganisation,
        ActionGroupeDiscriminationMotif motifDiscrimination,
        int nombrePersonnesConcernees,
        ActionGroupeDiscriminationObjet objetAction,
        LocalDate dateMiseEnDemeure,
        boolean qualiteAAgir,
        boolean pluraliteEtablie,
        LocalDate dateRecevabiliteSaisine,
        boolean delaiCarenceRespecte,
        ActionGroupeDiscriminationVerdict verdict,
        List<ActionGroupeDiscriminationChecklistItem> checklist,
        String country,
        String baseJuridique
) {}
