package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-08 : input du moteur décisionnel BE qualifiant la reconnaissance /
 * exequatur d'une décision familiale étrangère (CDIP art. 22-27 ; art. 21
 * Const. / CC art. 161 pour le mariage religieux non-civil).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code natureDecision} est requise (validation portée par le Calculator →
 * 400). {@code paysOrigine} est un code pays ISO 3166-1 alpha-2 nullable ;
 * {@code dateDecision} est facultative. Les booleans de fond
 * ({@code decisionDefinitive}, {@code droitsDefenseRespectes},
 * {@code absenceFraude}) sont nullables (manquants → QUALIFICATION_INCOMPLETE
 * pour le jugement). {@code conformiteOrdrePublicBelge} est un boolean primitif
 * (par défaut conforme). {@code mariageCivilPrealable} est propre au cas du
 * mariage religieux.</p>
 */
public record DipBeReconnaissanceDecisionEtrangereInput(
        DipBeReconnaissanceDecisionEtrangereCalculator.NatureDecision natureDecision,
        String paysOrigine,
        LocalDate dateDecision,
        Boolean decisionDefinitive,
        Boolean droitsDefenseRespectes,
        boolean conformiteOrdrePublicBelge,
        Boolean absenceFraude,
        Boolean mariageCivilPrealable
) {}
