/**
 * SF-218-42 : modèles miroirs du contrat API (backend SF-218-41) pour l'outil
 * décisionnel « Épargne salariale — conformité (intéressement / participation /
 * partage de la valeur) » (F-DT-53-epargne-salariale-conformite). FRANCE
 * uniquement.
 *
 * Analyseur de conformité aux obligations d'épargne salariale (art. L.3311-1 et
 * s., L.3321-1 et s., L.3322-2 CT + loi n° 2023-1107 du 29/11/2023 sur le partage
 * de la valeur) : participation obligatoire au-delà de 50 salariés, obligation de
 * dispositif de partage de la valeur pour les entreprises de 11 à 49 salariés
 * rentables (exercices ouverts à compter de 2025). Produit une checklist de
 * conformité et un verdict.
 *
 * Distinct de F-DT-52 PPV exonération (qui porte sur l'exonération d'une prime
 * versée — invariant « un outil = une situation »).
 */

/**
 * Verdict de conformité (aligné EXACTEMENT sur l'enum backend
 * {@code EpargneSalarialeConformiteStatut}) :
 *  - CONFORME : toutes les obligations applicables sont remplies.
 *  - OBLIGATION_NON_REMPLIE : au moins une obligation applicable n'est pas
 *    satisfaite.
 *  - NON_REQUIS : aucune obligation applicable.
 */
export type EpargneSalarialeConformiteStatut =
  | 'CONFORME'
  | 'OBLIGATION_NON_REMPLIE'
  | 'NON_REQUIS';

export interface EpargneSalarialeConformiteItem {
  /** Libellé de l'obligation / du point de contrôle. */
  item: string;
  /** true si l'obligation est remplie (ou non applicable). */
  conforme: boolean;
  /** 'OBLIGATION' (obligation légale applicable) ou 'INFORMATION' (facultatif). */
  type: string;
  /** Précision / fondement. */
  commentaire: string;
}

export interface EpargneSalarialeConformiteRequest {
  /** Effectif de l'entreprise (> 0). */
  effectif: number;
  /** Présence d'un accord de participation. */
  accordParticipationPresent: boolean;
  /** Présence d'un accord d'intéressement. */
  accordInteressementPresent: boolean;
  /** Bénéfice net fiscal ≥ 1 % du CA sur les 3 derniers exercices. */
  beneficeNetFiscalPositif3Ans: boolean;
  /** Entreprise de 11 à 49 salariés. */
  entreprise11a49: boolean;
}

export interface EpargneSalarialeConformiteResponse {
  caseFileId: string;
  effectif: number;
  accordParticipationPresent: boolean;
  accordInteressementPresent: boolean;
  beneficeNetFiscalPositif3Ans: boolean;
  entreprise11a49: boolean;
  /** Checklist de conformité. */
  checklist: EpargneSalarialeConformiteItem[];
  /** Nombre d'obligations légales applicables. */
  obligationsApplicables: number;
  /** Nombre d'obligations applicables non remplies. */
  obligationsNonRemplies: number;
  /** Verdict de conformité. */
  statut: EpargneSalarialeConformiteStatut;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
