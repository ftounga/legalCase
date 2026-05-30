package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-19 : réponse de l'analyse de qualification du cadre dirigeant
 * (art. L.3111-2 CT — 3 critères cumulatifs, F-DT-107). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record CadreDirigeantStatutResponse(
        UUID caseFileId,
        boolean independanceEmploiDuTemps,
        boolean autonomieDecision,
        boolean remunerationParmiPlusElevees,
        boolean participationDirectionEntreprise,
        BigDecimal niveauRemunerationConstate,
        List<CadreDirigeantCritere> criteres,
        int criteresRemplis,
        CadreDirigeantQualification qualification,
        CadreDirigeantExclusionDureeTravail exclusionDureeTravail,
        CadreDirigeantRisqueRappelHeuresSupp risqueRappelHeuresSupp,
        String country,
        String baseJuridique
) {}
