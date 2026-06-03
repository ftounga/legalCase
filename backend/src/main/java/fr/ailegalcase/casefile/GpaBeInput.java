package fr.ailegalcase.casefile;

/**
 * SF-223-04 : input du moteur décisionnel BE de cadrage de la situation
 * contentieuse de l'établissement de la filiation après une gestation pour
 * autrui (GPA).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code gpaRealiseeEnBelgiqueOuEtranger} et {@code lienGenetiqueParentIntentionnel}
 * sont requis (validation portée par le Calculator → 400). Les autres champs
 * sont des booléens nullables (informatifs / pré-remplissables).</p>
 */
public record GpaBeInput(
        GpaBeCalculator.LieuGpa gpaRealiseeEnBelgiqueOuEtranger,
        GpaBeCalculator.LienGenetique lienGenetiqueParentIntentionnel,
        Boolean acteNaissanceEtrangerEtabli,
        Boolean merePorteuseDesignee,
        Boolean consentementMerePorteuse,
        Boolean coupleIntentionnelMarieOuCohabitant,
        String commentaire
) {}
