package fr.ailegalcase.casefile;

/**
 * SF-218-29 : verdict global de conformité de la négociation annuelle obligatoire
 * (NAO, art. L.2242-1 à L.2242-8 CT, F-DT-66). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>NON_APPLICABLE : aucun délégué syndical n'est présent → l'obligation de
 *       négociation annuelle n'est pas déclenchée (art. L.2242-1).</li>
 *   <li>CONFORME : tous les items obligatoires de la checklist sont satisfaits et
 *       l'échéance de négociation n'est pas dépassée.</li>
 *   <li>NON_CONFORME : au moins un item obligatoire non satisfait, ou échéance de
 *       négociation dépassée (délit d'entrave, pénalité égalité F/H encourus).</li>
 * </ul>
 */
public enum NaoNegociationAnnuelleStatut {
    CONFORME,
    NON_CONFORME,
    NON_APPLICABLE
}
