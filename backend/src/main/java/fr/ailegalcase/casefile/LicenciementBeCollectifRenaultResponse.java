package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-07 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-collectif-renault}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir {@link
 * LicenciementBeFermetureEntrepriseResponse} SF-219-06).</p>
 */
public record LicenciementBeCollectifRenaultResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate dateProjet,
        LicenciementBeCollectifRenaultTailleEntreprise tailleEntreprise,
        Integer effectifMoyen,
        Integer nombreLicenciementsEnvisages,
        LicenciementBeCollectifRenaultPhase phaseAtteinte,
        Boolean informationEcriteCePrealable,
        Boolean documentsLegauxCommuniques,
        Boolean consultationEffectiveTenue,
        Boolean reponsesMotiveesEmployeur,
        Boolean notificationAutoriteRegionaleFaite,
        LocalDate dateNotificationAutorite,
        LocalDate datePremierPreavisNotifie,

        // Outputs
        LicenciementBeCollectifRenaultVerdict verdict,
        boolean conforme,
        boolean seuilDeclenchementAtteint,
        int seuilLegalApplicable,
        LocalDate dateFinDelaiAttente,
        boolean delaiAttenteRespecte,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
