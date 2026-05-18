package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 — Clé d'une cellule de la matrice de génération F-98.
 *
 * <p>Une cellule = une combinaison procédurale unique
 * {@code (domaine, pays, juridiction, stade, position)}. Le registre
 * {@link ConclusionPromptRegistry} indexe les {@link ConclusionPromptProvider}
 * par cette clé.</p>
 *
 * @param domain       domaine juridique du dossier (cf. {@code ProcedureStageCatalog})
 * @param country      pays du workspace (cf. {@code ProcedureStageCatalog})
 * @param jurisdiction code de la juridiction
 * @param stage        code du stade procédural
 * @param position     code de la position juridique
 */
public record CombinationKey(String domain, String country, String jurisdiction,
                             String stage, String position) {
}
