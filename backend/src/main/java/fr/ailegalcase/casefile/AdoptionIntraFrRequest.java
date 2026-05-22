package fr.ailegalcase.casefile;

/**
 * SF-216-15 : body POST /api/v1/case-files/{id}/adoption-intra-fr.
 *
 * <p>Outil single-country FRANCE — art. 343-1 al. 2 Cciv + art. 345-1 Cciv +
 * art. 360-362 Cciv + Loi n°2022-219 du 21/2/2022.</p>
 *
 * <p>Le service rejette les valeurs négatives, l'absence de
 * {@code formeAdoptionDemandee} ou {@code mariageOuPacsAdoptantParent}, et
 * l'incohérence entre ce drapeau et la forme d'adoption demandée.</p>
 */
public record AdoptionIntraFrRequest(
        FormeAdoptionEnum formeAdoptionDemandee,
        Integer ageAdoptant,
        Integer ageAdopte,
        Boolean mariageOuPacsAdoptantParent,
        Boolean consentementEnfant,
        Boolean consentementAutreParentBiologique,
        Boolean decheanceAutreParent,
        Boolean autreParentDecede
) {}
