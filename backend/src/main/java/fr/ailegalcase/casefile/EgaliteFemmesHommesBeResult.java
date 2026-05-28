package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-22 : résultat brut produit par
 * {@link EgaliteFemmesHommesBeChecker} — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link EgaliteFemmesHommesBeService} dans
 * {@link EgaliteFemmesHommesBeResponse}).</p>
 *
 * <p>Outputs dérivés : ventilation par dimension de conformité (seuil
 * 50 travailleurs, rapport déposé, contenu complet 5 sections art. 4
 * AR 17/08/2013, plan d'action requis le cas échéant).</p>
 */
public record EgaliteFemmesHommesBeResult(
        // Inputs (snapshot)
        int effectifEntreprise,
        EgaliteFemmesHommesBeStatutRapport statutRapport,
        LocalDate dateDepotRapport,
        boolean ventilationNiveauFonctionFournie,
        boolean ventilationAncienneteFournie,
        boolean ventilationQualificationFournie,
        boolean ventilationRegimeTravailFournie,
        boolean ventilationComposantsRemunerationFournie,
        boolean ecartSalarialNonJustifieConstate,
        BigDecimal pourcentageEcartConstate,
        boolean planActionEtabli,
        boolean mediateurDesigne,
        boolean plainteIefhOuInspectionEnCours,

        // Outputs analytiques
        EgaliteFemmesHommesBeVerdict verdict,
        boolean seuilAtteint,
        boolean rapportDepose,
        boolean ventilationComplete,
        boolean planActionRequis,
        boolean planActionConforme,
        String typeFormulaireRequis,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
