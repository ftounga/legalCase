package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-07 : résultat brut produit par {@link
 * LicenciementBeCollectifRenaultProcedureChecker} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * LicenciementBeCollectifRenaultService} dans {@link
 * LicenciementBeCollectifRenaultResponse}).</p>
 *
 * <p>{@code dateFinDelaiAttente} n'est non nulle que si la notification à
 * l'autorité régionale a été effectuée et qu'on a pu calculer la date de
 * fin du délai d'attente (notification + 30 jours).</p>
 */
public record LicenciementBeCollectifRenaultResult(
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

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
