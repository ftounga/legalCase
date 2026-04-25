package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-35-01 : payload de création/upsert d'une analyse de contestation ARE.
 */
public record ContestationAreRequest(
        String typeDecisionContestee,
        String motifContestation,
        LocalDate dateNotificationDecision,
        LocalDate dateRecoursHierarchiquePropose,
        List<String> preuvesProduites,
        BigDecimal montantContesteEur,
        Boolean demandeurDejaSaisiTribunal,
        Boolean delaiContestationRespecte
) {}
