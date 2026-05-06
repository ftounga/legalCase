package fr.ailegalcase.analysis;

/**
 * F-197 SF-197-01 — Réponse GET / PUT override.
 *
 * <p>Pour un dossier Travail FR : {@code typeLitigeAvocat} renseigné, {@code typeProcedureAvocat} null.
 * Pour un dossier Immigration : {@code typeProcedureAvocat} renseigné, {@code typeLitigeAvocat} null.
 * {@code raison} optionnel.</p>
 *
 * <p>Tous les champs nullable — un dossier sans override retourne
 * {@code TypeLitigeOverrideResponse(null, null, null)}.</p>
 */
public record TypeLitigeOverrideResponse(
        String typeLitigeAvocat,
        String typeProcedureAvocat,
        String raison) {

    public static TypeLitigeOverrideResponse from(CaseAnalysis analysis) {
        if (analysis == null) {
            return new TypeLitigeOverrideResponse(null, null, null);
        }
        return new TypeLitigeOverrideResponse(
                analysis.getTypeLitigeAvocatOverride(),
                analysis.getTypeProcedureAvocatOverride(),
                analysis.getTypeOverrideRaison());
    }
}
