package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-06 : réponse de l'endpoint d'estimation de la contribution alimentaire
 * des enfants belge.
 *
 * <p>Ré-expose l'intégralité du snapshot d'inputs (pour pré-remplissage et
 * ré-édition du formulaire UI) ET les sorties calculées (méthode Renard).</p>
 */
public record ContributionAlimentaireEnfantsBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour ré-édition du formulaire UI) ---
        Integer nombreEnfants,
        ContributionAlimentaireEnfantsBeCalculator.TrancheAge trancheAgeEnfants,
        BigDecimal revenuMensuelParent1,
        BigDecimal revenuMensuelParent2,
        BigDecimal coutMensuelGlobalEnfants,
        Integer nuitsHebergementParent1,
        Integer nuitsHebergementParent2,
        BigDecimal allocationsFamilialesMensuelles,
        BigDecimal fraisExtraordinairesMensuels,
        Boolean parentDebiteurEstParent1,
        String commentaire,
        // --- Outputs calculés ---
        ContributionAlimentaireEnfantsBeCalculator.Verdict verdict,
        BigDecimal coutMensuelRetenu,
        BigDecimal coutNetApresAllocations,
        BigDecimal quotePartParent1Pct,
        BigDecimal quotePartParent2Pct,
        BigDecimal partContributiveParent1,
        BigDecimal partContributiveParent2,
        BigDecimal partHebergementParent1,
        BigDecimal partHebergementParent2,
        BigDecimal contributionMensuelleNette,
        ContributionAlimentaireEnfantsBeCalculator.ParentDebiteur parentDebiteur,
        BigDecimal fraisExtraordinairesQuotePartParent1,
        BigDecimal fraisExtraordinairesQuotePartParent2,
        List<String> detailCalcul,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
