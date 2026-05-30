package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-09 : requête POST pour l'analyse de recevabilité d'une action de
 * groupe en discrimination au travail (art. L. 1134-7 à L. 1134-10 Code
 * travail). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param typeOrganisation organisation exerçant l'action — qualité à agir
 *        (requis).
 * @param dateMiseEnDemeure date de la mise en demeure adressée à l'employeur
 *        (optionnel) — déclenche le délai de carence de 6 mois.
 * @param motifDiscrimination critère de discrimination invoqué (requis).
 * @param nombrePersonnesConcernees nombre de personnes placées dans une
 *        situation similaire (Integer ≥ 1, requis).
 * @param objetAction objet de l'action — défaut {@code LES_DEUX}.
 */
public record ActionGroupeDiscriminationRequest(
        ActionGroupeDiscriminationTypeOrganisation typeOrganisation,
        LocalDate dateMiseEnDemeure,
        ActionGroupeDiscriminationMotif motifDiscrimination,
        Integer nombrePersonnesConcernees,
        ActionGroupeDiscriminationObjet objetAction
) {}
