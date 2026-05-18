package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-08 : réponse de l'endpoint d'analyse de la pension alimentaire entre
 * ex-époux belge.
 *
 * <p>Ré-expose l'intégralité du snapshot d'inputs (pour pré-remplissage et
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, durée,
 * montant, motifs d'exclusion).</p>
 */
public record ContributionConjointBeResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour ré-édition du formulaire UI) ---
        ContributionConjointBeCalculator.TypeDivorce typeDivorce,
        Boolean renonciationPensionConvention,
        Boolean creancierEnEtatDeBesoin,
        Boolean fauteGraveCreancier,
        Integer dureeMariageAnnees,
        BigDecimal revenuMensuelCreancier,
        BigDecimal revenuMensuelDebiteur,
        Boolean degradationEconomiqueLieeAuMariage,
        String commentaire,
        // --- Outputs calculés ---
        ContributionConjointBeCalculator.Verdict verdict,
        int dureeMaximaleMois,
        BigDecimal montantMensuelIndicatif,
        BigDecimal plafondTiersRevenusDebiteur,
        List<ContributionConjointBeCalculator.MotifExclusion> motifsExclusion,
        List<String> detailCalcul,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
