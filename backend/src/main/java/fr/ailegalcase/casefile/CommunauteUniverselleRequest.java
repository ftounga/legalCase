package fr.ailegalcase.casefile;

/**
 * SF-FA-16-01 : requête d'analyse du régime conventionnel de
 * communauté universelle (FR — art. 1526 Cciv + 1527 al. 2 Cciv).
 *
 * <p>L'outil supporte 2 dispositifs distincts :</p>
 * <ul>
 *   <li><strong>VALIDITE_CONVENTION</strong> — vérification de la validité du
 *       contrat de mariage instituant la communauté universelle.</li>
 *   <li><strong>LIQUIDATION_DECES</strong> — liquidation suite décès d'un
 *       époux (avec ou sans clause d'attribution intégrale).</li>
 * </ul>
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record CommunauteUniverselleRequest(
        CommunauteUniverselleCalculator.DispositifAnalyse dispositifAnalyse,
        Boolean contratNotarie,
        Boolean inscriptionEtatCivil,
        Boolean consentementLibreDesEpoux,
        Boolean respectReserveHereditaire,
        Boolean clauseAttributionIntegrale,
        Boolean enfantsNonCommuns,
        Double valeurCommunauteEur
) {}
