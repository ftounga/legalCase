package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-218-25 : réponse de l'analyse du licenciement pour fin de chantier du CDI
 * de chantier / d'opération (art. L.1223-8 et s. ; L.1236-8 CT, F-DT-37). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public record CdiChantierResponse(
        UUID caseFileId,
        LocalDate dateEntree,
        LocalDate dateRupture,
        CdiChantierFondementRecours fondementRecours,
        CdiChantierSecteur secteur,
        boolean chantierAcheve,
        BigDecimal salaireMensuelMoyen,
        boolean reclassementAutreChantierPropose,
        int ancienneteAnnees,
        boolean recoursValide,
        String motifRecours,
        CdiChantierMotifLicenciement motifLicenciement,
        BigDecimal indemniteLicenciement,
        boolean procedureRequise,
        CdiChantierVerdict verdictGlobal,
        List<String> consequences,
        String motif,
        String country,
        String baseJuridique
) {}
