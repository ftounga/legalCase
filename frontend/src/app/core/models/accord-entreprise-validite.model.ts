/**
 * SF-218-32 : modèles miroirs du contrat API (backend SF-218-31) pour l'outil
 * décisionnel « Accord d'entreprise : validité (conditions de majorité) »
 * (F-DT-67-accord-entreprise-validite). FRANCE uniquement.
 *
 * Analyseur de validité d'un accord d'entreprise au regard des conditions de
 * majorité (art. L.2232-12 CT) et, selon l'opération, des conditions de révision
 * (parties habilitées, art. L.2261-7) ou de dénonciation (préavis de 3 mois +
 * survie de l'accord pendant 12 mois, art. L.2261-9 à L.2261-11). Distinct de
 * F-DT-66 (NAO : conformité de la négociation annuelle obligatoire).
 */

/**
 * Type d'opération portant sur l'accord (aligné EXACTEMENT sur l'enum backend
 * {@code AccordTypeOperation}) :
 *  - CONCLUSION : conclusion d'un accord (validité appréciée aux seules
 *    conditions de majorité, L.2232-12).
 *  - REVISION : avenant de révision (ajoute la vérification des parties
 *    habilitées, L.2261-7 et s.).
 *  - DENONCIATION : dénonciation d'un accord (préavis 3 mois + survie 12 mois,
 *    L.2261-9 à L.2261-11).
 */
export type AccordTypeOperation = 'CONCLUSION' | 'REVISION' | 'DENONCIATION';

/**
 * Qualification de la condition de majorité (aligné EXACTEMENT sur l'enum backend
 * {@code AccordConditionMajorite}) :
 *  - MAJORITE_50 : signataires > 50 % des suffrages exprimés au 1er tour →
 *    accord valide sans référendum.
 *  - REFERENDUM_30 : signataires ∈ [30 % ; 50 %[ et accord approuvé par référendum
 *    → accord valide sous réserve de la régularité du référendum.
 *  - INSUFFISANTE : signataires < 30 %, ou ∈ [30 % ; 50 %[ sans référendum approuvé.
 */
export type AccordConditionMajorite = 'MAJORITE_50' | 'REFERENDUM_30' | 'INSUFFISANTE';

/**
 * Verdict global de validité (aligné EXACTEMENT sur l'enum backend
 * {@code AccordEntrepriseValiditeStatut}) :
 *  - VALIDE : conditions de majorité remplies (> 50 %) + item d'opération satisfait.
 *  - VALIDE_SOUS_RESERVE : majorité atteinte par référendum (≥ 30 % + référendum
 *    approuvé) — validité subordonnée à la régularité du référendum.
 *  - NON_VALIDE : conditions de majorité non remplies, ou item d'opération non
 *    satisfait (parties non habilitées en révision, préavis non respecté en
 *    dénonciation).
 */
export type AccordEntrepriseValiditeStatut = 'VALIDE' | 'VALIDE_SOUS_RESERVE' | 'NON_VALIDE';

/**
 * Détail d'un item de la checklist de validité (aligné EXACTEMENT sur le DTO
 * backend {@code AccordValiditeChecklistItem}).
 */
export interface AccordValiditeChecklistItem {
  /** Libellé de la condition vérifiée (majorité, référendum, parties habilitées, préavis). */
  item: string;
  /** Condition satisfaite ou non. */
  conforme: boolean;
  /** Fondement / point de vigilance attaché à l'item. */
  commentaire: string;
}

export interface AccordEntrepriseValiditeRequest {
  /** % des suffrages exprimés au 1er tour recueilli par les signataires (∈ [0 ; 100]). */
  pourcentageSuffragesSignataires: number;
  /** Un référendum de validation a été organisé. */
  referendumOrganise: boolean;
  /** Le référendum a approuvé l'accord à la majorité des suffrages exprimés. */
  referendumApprouve: boolean;
  /** Type d'opération (conclusion / révision / dénonciation). */
  typeOperation: AccordTypeOperation;
  /** Avenant signé par les parties habilitées à engager la révision (L.2261-7) ; requis si REVISION. */
  signePartiesHabilitees: boolean;
  /** Préavis de dénonciation de 3 mois respecté (pertinent en dénonciation). */
  preavisDenonciationRespecte: boolean;
  /** Date de la dénonciation (ISO YYYY-MM-DD, point de départ du calcul de fin de survie). */
  dateDenonciation?: string | null;
}

export interface AccordEntrepriseValiditeResponse {
  caseFileId: string;
  pourcentageSuffragesSignataires: number;
  typeOperation: AccordTypeOperation;
  referendumOrganise: boolean;
  referendumApprouve: boolean;
  /** Qualification de la condition de majorité. */
  conditionMajorite: AccordConditionMajorite;
  /** Date de la dénonciation (ISO YYYY-MM-DD, null hors dénonciation). */
  dateDenonciation?: string | null;
  /** Date de fin de survie de l'accord (dénonciation + 3 mois préavis + 12 mois survie ; null sinon). */
  dateFinSurvie?: string | null;
  /** Checklist de validité (item + conforme + commentaire). */
  checklist: AccordValiditeChecklistItem[];
  /** Nombre d'items non conformes. */
  itemsNonConformes: number;
  /** Verdict global de validité. */
  statut: AccordEntrepriseValiditeStatut;
  /** Conséquences / points de vigilance identifiés. */
  consequences: string[];
  country: string;
  baseJuridique: string;
}
