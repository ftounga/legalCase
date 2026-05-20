package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-206-05 : réponse de l'endpoint d'analyse d'une prise d'acte de la
 * rupture aux torts de l'employeur (FRANCE).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (verdict, score, griefs retenus, effet probable, bases
 * juridiques).</p>
 */
public record PriseActeRuptureResponse(
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
        Boolean griefsActuelsEtPersistants,
        Boolean griefRendImpossiblePoursuite,
        String griefsCommentaire,
        // --- Outputs calculés ---
        PriseActeRuptureCalculator.Verdict verdict,
        int scoreSolidite,
        List<PriseActeRuptureCalculator.GriefRetenu> griefsRetenus,
        PriseActeRuptureCalculator.EffetProbable effetProbable,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
