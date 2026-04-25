package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RequalificationCddCdiResponse(
        UUID caseFileId,
        String motifCddInvoque,
        boolean motifInterdit,
        String motifInterditType,
        List<RequalificationCddCdiRequest.CddSuccessionEntry> successionCdd,
        boolean delaiCarenceRespecte,
        int dureeContratMois,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateFinDernierContrat,
        int scoreRequalification,
        String verdictProbabiliteRequalification,
        BigDecimal indemniteRequalificationEur,
        BigDecimal indemnitePrecariteEur,
        BigDecimal totalDommagesIndemniteEur,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
