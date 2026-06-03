package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-08 : requête HTTP pour les endpoints DIP reconnaissance / exequatur
 * d'une décision familiale étrangère BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code natureDecision} est requise (validation portée par le Calculator
 * → 400 via {@code IllegalArgumentException}). {@code conformiteOrdrePublicBelge}
 * est un boolean primitif (absent → false interprété comme « non conforme » →
 * refus ; l'UI envoie toujours la valeur saisie).</p>
 */
public record DipBeReconnaissanceDecisionEtrangereRequest(
        DipBeReconnaissanceDecisionEtrangereCalculator.NatureDecision natureDecision,
        String paysOrigine,
        LocalDate dateDecision,
        Boolean decisionDefinitive,
        Boolean droitsDefenseRespectes,
        boolean conformiteOrdrePublicBelge,
        Boolean absenceFraude,
        Boolean mariageCivilPrealable
) {

    DipBeReconnaissanceDecisionEtrangereInput toInput() {
        return new DipBeReconnaissanceDecisionEtrangereInput(
                natureDecision,
                paysOrigine,
                dateDecision,
                decisionDefinitive,
                droitsDefenseRespectes,
                conformiteOrdrePublicBelge,
                absenceFraude,
                mariageCivilPrealable);
    }
}
