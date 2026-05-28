package fr.ailegalcase.casefile;

/**
 * SF-219-27 : résultat brut produit par
 * {@link InastriStatutTravailleurIndependantChecker} — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link InastriStatutTravailleurIndependantService} dans
 * {@link InastriStatutTravailleurIndependantResponse}).</p>
 *
 * <p>Outputs dérivés : verdict de qualification, scores pondérés
 * des 4 critères généraux art. 333 (indice indépendant vs salarié),
 * drapeau présomption sectorielle art. 337/2, drapeau présomption
 * générale art. 328 mobilisable, drapeau requalification recommandée
 * + indicateur DIMONA cohérente avec le statut déclaré.</p>
 */
public record InastriStatutTravailleurIndependantResult(
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

        // Outputs analytiques
        InastriStatutTravailleurIndependantVerdict verdict,
        int scoreCriteresGenerauxIndependant,
        int scoreCriteresGenerauxSalarie,
        boolean presumptionSectorielleApplicable,
        boolean presumptionSectorielleDeclenchee,
        boolean presumptionGeneraleSalariatMobilisable,
        boolean requalificationSalariatRecommandee,
        boolean dimonaCoherenteAvecStatutDeclare,
        boolean monoClientIndiceSalariat,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
