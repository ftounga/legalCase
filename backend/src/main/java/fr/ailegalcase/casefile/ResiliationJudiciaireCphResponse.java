package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-206-07 : réponse de l'endpoint d'analyse d'une demande de résiliation
 * judiciaire du contrat de travail aux torts de l'employeur (FRANCE).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (verdict, score, manquements retenus, date d'effet
 * probable, bases juridiques).</p>
 */
public record ResiliationJudiciaireCphResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        Boolean defautPaiementSalaire,
        BigDecimal montantImpayesEur,
        Boolean harcelement,
        Boolean manquementSecurite,
        Boolean modificationUnilateraleContrat,
        Boolean declassement,
        Boolean discrimination,
        Boolean heuresSupNonPayees,
        Boolean nonRespectDureesRepos,
        Boolean manquementsPersistantsAuJourDemande,
        Boolean salarieToujoursEnPoste,
        Boolean licenciementIntervenuEnCoursInstance,
        String manquementsCommentaire,
        // --- Outputs calculés ---
        ResiliationJudiciaireCphCalculator.Verdict verdict,
        int scoreSolidite,
        List<ResiliationJudiciaireCphCalculator.ManquementRetenu> manquementsRetenus,
        ResiliationJudiciaireCphCalculator.DateEffetProbable dateEffetProbable,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
