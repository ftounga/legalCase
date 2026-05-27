package fr.ailegalcase.casefile;

import java.util.UUID;

/**
 * SF-219-01 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-metiers-lourds}.
 *
 * <p>Snapshot complet (inputs + outputs : verdict + raison + synthèse) —
 * pattern uniforme avec les autres outils décisionnels BE F-213.</p>
 */
public record RccBeMetiersLourdsResponse(
        UUID caseFileId,

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
