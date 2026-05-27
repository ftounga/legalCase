package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-06 : résultat brut produit par {@link
 * LicenciementBeFermetureEntrepriseCalculator} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * LicenciementBeFermetureEntrepriseService} dans {@link
 * LicenciementBeFermetureEntrepriseResponse}).</p>
 *
 * <p>{@code montantTotalCreancesFfe} est la somme {salaires + pécule +
 * indemnité rupture} effectivement reprise par le FFE — non nulle
 * uniquement si {@link
 * LicenciementBeFermetureEntrepriseVerdict#ELIGIBLE_FFE_REPRISE_CREANCES}
 * ou {@link
 * LicenciementBeFermetureEntrepriseVerdict#ELIGIBLE_FFE_COMPLET}.</p>
 */
public record LicenciementBeFermetureEntrepriseResult(
        // Inputs (snapshot)
        LocalDate dateNaissance,
        LocalDate dateDebutContrat,
        LocalDate dateFermeture,
        BigDecimal remunerationMensuelleBrute,
        LicenciementBeFermetureEntrepriseTypeFermeture typeFermeture,
        LicenciementBeFermetureEntrepriseStatutEmployeur statutEmployeur,
        Integer effectifEtp,
        BigDecimal salairesImpayes,
        BigDecimal peculeVacancesImpaye,
        BigDecimal indemniteRuptureImpayee,

        // Outputs
        LicenciementBeFermetureEntrepriseVerdict verdict,
        boolean eligible,

        // Indicateurs calculés
        int ageADateFermeture,
        int anneesAnciennete,
        BigDecimal indemniteFermeture,
        BigDecimal montantForfaitaireParAnnee,
        BigDecimal supplementAgeMensuel,
        BigDecimal montantTotalCreancesFfe,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
