package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-35-01 : résultat brut produit par {@link ContestationAreCalculator}.
 *
 * <p>Snapshot canonique (inputs normalisés + outputs calculés) sérialisé en JSON dans
 * la table {@code contestation_are_analyses.snapshot_data}. Représente la dernière
 * analyse en date pour un dossier.
 */
public record ContestationAreResult(
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
        List<String> messages
) {}
