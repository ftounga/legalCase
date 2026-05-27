package fr.ailegalcase.casefile;

/**
 * SF-219-01 : résultat brut produit par
 * {@link RccBeMetiersLourdsValidator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté par
 * {@link RccBeMetiersLourdsService} dans {@link RccBeMetiersLourdsResponse}).</p>
 *
 * <p>{@code raisonIneligibilite} est {@code null} si {@code verdict=ELIGIBLE}.
 * {@code avertissement} signale notamment la nécessité de vérifier la liste
 * officielle des métiers lourds annuellement.</p>
 */
public record RccBeMetiersLourdsResult(
        Integer ageFinContrat,
        Integer anneesCarriereTotal,
        Integer anneesMetierLourdRecent10,
        Integer anneesMetierLourdRecent15,
        RccBeMetiersLourdsTypeEnum typeMetierLourd,
        Boolean licenciementEffectif,

        RccBeMetiersLourdsVerdictEnum verdict,
        RccBeMetiersLourdsRaisonIneligibilite raisonIneligibilite,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
