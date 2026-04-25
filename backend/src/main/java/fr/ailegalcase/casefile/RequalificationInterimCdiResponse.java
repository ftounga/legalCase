package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RequalificationInterimCdiResponse(
        UUID caseFileId,
        String motifInterimInvoque,
        boolean motifInterdit,
        String motifInterditType,
        List<RequalificationInterimCdiRequest.MissionInterim> successionMissions,
        boolean delaiCarenceRespecte,
        int dureeMissionsTotaleMois,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateFinDerniereMission,
        boolean memeEntrepriseUtilisatrice,
        int scoreRequalification,
        String verdictProbabiliteRequalification,
        BigDecimal indemniteRequalificationEur,
        BigDecimal indemniteFinMissionInterimEur,
        BigDecimal totalDommagesIndemniteEur,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
