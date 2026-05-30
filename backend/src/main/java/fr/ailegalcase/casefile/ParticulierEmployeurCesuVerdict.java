package fr.ailegalcase.casefile;

/**
 * SF-218-13 : verdict global de l'analyse de la rupture du contrat d'un salarié
 * du particulier employeur (CESU / assistant maternel). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>RUPTURE_REGULIERE : préavis et indemnité de licenciement déterminés
 *       et dus selon la CCN applicable, sans point d'attention particulier.</li>
 *   <li>RUPTURE_A_SECURISER : la rupture donne lieu à un préavis et/ou à une
 *       indemnité de licenciement à formaliser (notification, calcul, paiement)
 *       — vigilance procédurale recommandée.</li>
 *   <li>INDEMNITE_NON_DUE : aucune indemnité de licenciement n'est due (faute
 *       grave, ou ancienneté inférieure au seuil conventionnel).</li>
 * </ul>
 */
public enum ParticulierEmployeurCesuVerdict {
    RUPTURE_REGULIERE,
    RUPTURE_A_SECURISER,
    INDEMNITE_NON_DUE
}
