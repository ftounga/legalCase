package fr.ailegalcase.casefile;

/**
 * SF-218-07 : requête POST pour l'analyse de la saisie sur rémunération
 * (quotité saisissable — art. R. 3252-2 et s. Code du travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * @param remunerationNetteMensuelle rémunération nette mensuelle servant
 *        d'assiette (€, strictement positif, requis).
 * @param nombrePersonnesACharge nombre de personnes à charge du salarié saisi
 *        (≥ 0, défaut 0) — majore les seuils de tranche (art. R. 3252-3 CT).
 * @param creanceTotale montant total de la créance à recouvrer (€, strictement
 *        positif, requis).
 * @param creanceAlimentaire true si la créance est alimentaire (paiement direct
 *        prioritaire — loi du 2 janvier 1973) ; défaut false.
 */
public record SaisieRemunerationRequest(
        Double remunerationNetteMensuelle,
        Integer nombrePersonnesACharge,
        Double creanceTotale,
        Boolean creanceAlimentaire
) {}
