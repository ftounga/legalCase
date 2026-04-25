package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReferesAdminResponse(
        UUID caseFileId,
        String typeRefere,
        String decisionContestee,
        LocalDate dateNotificationDecision,
        boolean urgenceCaracterisee,
        boolean atteinteLiberteFondamentale,
        boolean doutesSerieuxLegalite,
        List<String> preuvesUrgence,
        boolean demandeurDejaPrived,
        int scoreSuccessProbabiliteSuspension,
        int scoreSuccessProbabiliteLiberte,
        String verdictRecommandation,
        int delaiJugeTaJoursL521_1,
        int delaiJugeTaHeuresL521_2,
        boolean conditionsCumulativesL521_1Ok,
        boolean conditionsCumulativesL521_2Ok,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
