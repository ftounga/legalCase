package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-08-07 : résultat de l'analyse référés administratifs combinés FR (L.521-1 + L.521-2 CJA).
 */
public record ReferesAdminResult(
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
        List<String> messages
) {}
