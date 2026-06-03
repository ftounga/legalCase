package fr.ailegalcase.casefile;

/**
 * SF-218-51 : requête POST pour l'outil "Temps de trajet / déplacement
 * professionnel" (art. L.3121-4 CT ; CJUE C-266/14 « Tyco », F-DT-81). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param typeTrajet type de trajet (requis) ∈ DOMICILE_TRAVAIL_HABITUEL /
 *        DOMICILE_CLIENT_DEPASSEMENT / ITINERANT_SANS_LIEU_FIXE.
 * @param tempsTrajetQuotidienMinutes temps de trajet quotidien constaté en
 *        minutes (requis, &ge; 0).
 * @param tempsTrajetNormalMinutes temps de trajet « normal » de référence en
 *        minutes (requis, &ge; 0).
 * @param contrepartiePrevueAccord une contrepartie (repos / financière) est déjà
 *        prévue par accord ou usage (optionnel, défaut false).
 */
public record TempsTrajetDeplacementRequest(
        TypeTrajet typeTrajet,
        Integer tempsTrajetQuotidienMinutes,
        Integer tempsTrajetNormalMinutes,
        Boolean contrepartiePrevueAccord
) {}
