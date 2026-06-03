package fr.ailegalcase.casefile;

/**
 * SF-218-39 : verdict de conformité de la prime de partage de la valeur (PPV) au
 * plafond d'exonération sociale (loi n° 2022-1158 du 16/08/2022 art. 1 + loi
 * n° 2023-1107 du 29/11/2023 sur le partage de la valeur, F-DT-52). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONFORME : le montant de la PPV est inférieur ou égal au plafond
 *       d'exonération sociale applicable (3 000 € ou 6 000 €) → intégralement
 *       exonéré de cotisations sociales, aucune part imposable au titre du
 *       dépassement.</li>
 *   <li>PLAFOND_DEPASSE : le montant dépasse le plafond applicable → la fraction
 *       excédentaire est réintégrée dans l'assiette (montant imposable).</li>
 * </ul>
 */
public enum PpvExonerationStatut {
    CONFORME,
    PLAFOND_DEPASSE
}
