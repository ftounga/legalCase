package fr.ailegalcase.casefile;

import java.util.UUID;

/**
 * SF-219-27 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/inastri-statut-travailleur-independant}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link TravailNoirBeDimonaResponse} SF-219-26).</p>
 */
public record InastriStatutTravailleurIndependantResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        InastriStatutTravailleurIndependantVolonte volonteParties,
        InastriStatutTravailleurIndependantLiberte liberteOrganisationTemps,
        InastriStatutTravailleurIndependantLiberte liberteOrganisationTravail,
        InastriStatutTravailleurIndependantLiberte controleHierarchique,
        InastriStatutTravailleurIndependantSecteur secteur,
        int nbCriteresSectorielsSalarie,
        InastriStatutTravailleurIndependantStatutDeclare statutDeclare,
        boolean dimonaPresente,
        boolean autreClientPresent,

        // Outputs
        InastriStatutTravailleurIndependantVerdict verdict,
        int scoreCriteresGenerauxIndependant,
        int scoreCriteresGenerauxSalarie,
        boolean presumptionSectorielleApplicable,
        boolean presumptionSectorielleDeclenchee,
        boolean presumptionGeneraleSalariatMobilisable,
        boolean requalificationSalariatRecommandee,
        boolean dimonaCoherenteAvecStatutDeclare,
        boolean monoClientIndiceSalariat,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
