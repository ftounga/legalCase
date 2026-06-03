package fr.ailegalcase.casefile;

/**
 * SF-223-04 : requête HTTP pour les endpoints de la situation contentieuse
 * post-GPA BE.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans le
 * body. {@code gpaRealiseeEnBelgiqueOuEtranger} et
 * {@code lienGenetiqueParentIntentionnel} sont requis (validation portée par le
 * Calculator → 400 via {@code IllegalArgumentException}).</p>
 */
public record GpaBeRequest(
        GpaBeCalculator.LieuGpa gpaRealiseeEnBelgiqueOuEtranger,
        GpaBeCalculator.LienGenetique lienGenetiqueParentIntentionnel,
        Boolean acteNaissanceEtrangerEtabli,
        Boolean merePorteuseDesignee,
        Boolean consentementMerePorteuse,
        Boolean coupleIntentionnelMarieOuCohabitant,
        String commentaire
) {

    GpaBeInput toInput() {
        return new GpaBeInput(
                gpaRealiseeEnBelgiqueOuEtranger,
                lienGenetiqueParentIntentionnel,
                acteNaissanceEtrangerEtabli,
                merePorteuseDesignee,
                consentementMerePorteuse,
                coupleIntentionnelMarieOuCohabitant,
                commentaire);
    }
}
