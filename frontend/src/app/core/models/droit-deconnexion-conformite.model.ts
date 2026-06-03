/**
 * SF-218-54 : modèles miroirs du contrat API (backend SF-218-53) pour l'outil
 * décisionnel « Droit à la déconnexion — conformité »
 * (F-DT-83-droit-deconnexion-conformite). FRANCE uniquement.
 *
 * Analyseur de conformité à l'obligation relative au droit à la déconnexion
 * (art. L.2242-17 7° CT) : pour les entreprises d'au moins 50 salariés dotées
 * d'au moins un délégué syndical, le droit à la déconnexion doit être négocié
 * dans la NAO QVCT ; à défaut d'accord, l'employeur élabore une charte, après
 * avis du CSE, prévoyant des actions de formation et de sensibilisation. Produit
 * une checklist de conformité et un verdict.
 *
 * Distinct de la NAO dans son ensemble (F-DT-66) et de la désignation du délégué
 * syndical (F-DT-69) — invariant « un outil = une situation ».
 */

/**
 * Verdict de conformité (aligné EXACTEMENT sur l'enum backend
 * {@code DroitDeconnexionConformiteStatut}) :
 *  - CONFORME : l'obligation s'applique et tous les items de la checklist sont
 *    remplis.
 *  - NON_CONFORME : l'obligation s'applique mais au moins un item n'est pas
 *    satisfait.
 *  - NON_REQUIS : l'obligation de négocier n'est pas déclenchée (effectif < 50
 *    ou absence de délégué syndical).
 */
export type DroitDeconnexionConformiteStatut =
  | 'CONFORME'
  | 'NON_CONFORME'
  | 'NON_REQUIS';

export interface DroitDeconnexionConformiteItem {
  /** Libellé de l'obligation / du point de contrôle. */
  item: string;
  /** true si l'obligation est remplie (ou non applicable). */
  conforme: boolean;
  /** 'OBLIGATION', 'PROCEDURE' ou 'INFORMATION'. */
  type: string;
  /** Précision / fondement. */
  commentaire: string;
}

export interface DroitDeconnexionConformiteRequest {
  /** Effectif de l'entreprise (> 0). */
  effectif: number;
  /** Présence d'au moins un délégué syndical. */
  delegueSyndicalPresent: boolean;
  /** Présence d'un accord ou d'une charte sur le droit à la déconnexion. */
  accordOuChartePresent: boolean;
  /** Plages / modalités de déconnexion définies. */
  plagesDeconnexionDefinies: boolean;
  /** Actions de formation / sensibilisation prévues. */
  actionsSensibilisation: boolean;
  /** Avis du CSE recueilli avant l'élaboration de la charte le cas échéant. */
  avisCseRecueilliPourCharte: boolean;
}

export interface DroitDeconnexionConformiteResponse {
  caseFileId: string;
  effectif: number;
  delegueSyndicalPresent: boolean;
  accordOuChartePresent: boolean;
  plagesDeconnexionDefinies: boolean;
  actionsSensibilisation: boolean;
  avisCseRecueilliPourCharte: boolean;
  /** true si l'obligation de négocier est déclenchée. */
  obligationDeNegocier: boolean;
  /** Checklist de conformité. */
  checklist: DroitDeconnexionConformiteItem[];
  /** Nombre d'items applicables non conformes. */
  itemsManquants: number;
  /** Verdict de conformité. */
  statut: DroitDeconnexionConformiteStatut;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
