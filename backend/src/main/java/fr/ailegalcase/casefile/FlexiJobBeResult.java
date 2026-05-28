package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-12 : résultat brut produit par {@link FlexiJobBeChecker} —
 * fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par {@link
 * FlexiJobBeService} dans {@link FlexiJobBeResponse}).</p>
 *
 * <p>Outputs dérivés : éligibilité au régime flexi-job (booléens
 * granulaires pour restitution UI), montant excédentaire éventuel au
 * plafond annuel, montant taxable au régime normal (excédent flexi-
 * salaire si sous-payé / part au-delà du plafond).</p>
 */
public record FlexiJobBeResult(
        // Inputs (snapshot)
        LocalDate datePremiereOccupationFlexi,
        FlexiJobBeStatutTravailleur statutTravailleur,
        FlexiJobBeSecteurEmployeur secteurEmployeur,
        boolean dejaOccupeChezMemeEmployeur,
        boolean contratCadreSigne,
        boolean dimonaFlxDeclaree,
        BigDecimal flexiSalaireHoraireBrut,
        BigDecimal flexiSalaireMinimumApplicable,
        BigDecimal revenuAnnuelFlexiCumule,
        BigDecimal plafondAnnuelExonere,

        // Outputs analytiques
        FlexiJobBeVerdict verdict,
        boolean travailleurEligible,
        boolean secteurEligible,
        boolean cumulRespecte,
        boolean formalismeRespecte,
        boolean remunerationConforme,
        BigDecimal montantExcedentaireAuPlafond,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
