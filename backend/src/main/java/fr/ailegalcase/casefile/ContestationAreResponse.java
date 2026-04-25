package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-35-01 : projection complète (snapshot) renvoyée au client.
 */
public record ContestationAreResponse(
        UUID caseFileId,
        String typeDecisionContestee,
        String motifContestation,
        LocalDate dateNotificationDecision,
        LocalDate dateRecoursHierarchiquePropose,
        List<String> preuvesProduites,
        BigDecimal montantContesteEur,
        boolean demandeurDejaSaisiTribunal,
        boolean delaiContestationRespecte,
        boolean delaiRecoursHierarchiqueJoursOk,
        boolean delaiRecoursContentieuxTaJoursOk,
        int scoreSuccessProbable,
        String verdictRecommandation,
        int delaiInstructionMoisPrevisionnel,
        boolean expertiseTraitementSalaireRecommandee,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
