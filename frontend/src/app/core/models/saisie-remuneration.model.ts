/**
 * SF-218-08 : modèles miroirs du contrat API (backend SF-218-07) pour l'outil
 * décisionnel « Saisie sur rémunération (quotité saisissable) »
 * (F-DT-89-saisie-arret-remuneration). FRANCE uniquement — calcul de la quotité
 * mensuelle saisissable d'une rémunération selon le barème annuel par tranches
 * (R. 3252-2 Code travail), avec correction pour personnes à charge (R. 3252-3)
 * et fraction absolument insaisissable (montant forfaitaire RSA, L. 3252-3).
 *
 * Accompagne l'avocat côté créancier (recouvrer une créance sur le salaire d'un
 * débiteur après jugement) ou côté salarié saisi : quotité saisissable mensuelle,
 * montant laissé au salarié, nombre de mois de recouvrement et détail par tranche.
 *
 * Pattern miroir de {@link ExecutionJugementCphResponse} (F-DT-88, SF-218-04).
 */

/**
 * Verdict d'orientation de la saisie (aligné EXACTEMENT sur l'enum backend
 * {@code SaisieRemunerationVerdict}) :
 *  - SAISISSABLE : quotité saisissable strictement positive — saisie possible.
 *  - INSAISISSABLE : rémunération ≤ fraction absolument insaisissable (RSA) —
 *    aucune quotité saisissable.
 *  - ALIMENTAIRE_PAIEMENT_DIRECT : créance alimentaire — procédure de paiement
 *    direct (loi de 1973) hors barème classique, seule la fraction insaisissable
 *    RSA est mise à part.
 */
export type SaisieRemunerationVerdict =
  | 'SAISISSABLE'
  | 'INSAISISSABLE'
  | 'ALIMENTAIRE_PAIEMENT_DIRECT';

/**
 * Détail d'une tranche du barème (miroir de {@code TrancheSaisie} backend).
 * Chaque tranche applique sa quotité (1/20, 1/10, 1/5, 1/4, 1/3, totalité) à la
 * part de rémunération comprise dans ses bornes mensuelles (majorées des
 * personnes à charge).
 */
export interface TrancheSaisie {
  /** Rang de la tranche (1..N). */
  rang: number;
  /** Borne basse mensuelle de la tranche (€). */
  borneInferieure: number;
  /** Borne haute mensuelle de la tranche (€) ; null pour la fraction « au-delà ». */
  borneSuperieure: number | null;
  /** Libellé de la quotité appliquée (ex. « 1/20 », « 1/3 », « totalité »). */
  quotite: string;
  /** Part de rémunération comprise dans cette tranche (€). */
  montantTranche: number;
  /** Montant saisissable issu de cette tranche (montantTranche × quotité). */
  montantSaisissable: number;
}

export interface SaisieRemunerationRequest {
  remunerationNetteMensuelle: number; // > 0 — assiette nette mensuelle
  nombrePersonnesACharge: number; // ≥ 0
  creanceTotale: number; // > 0 — montant de la créance à recouvrer
  creanceAlimentaire: boolean; // créance alimentaire → paiement direct
}

export interface SaisieRemunerationResponse {
  caseFileId: string;
  remunerationNetteMensuelle: number;
  nombrePersonnesACharge: number;
  creanceTotale: number;
  creanceAlimentaire: boolean;
  verdict: SaisieRemunerationVerdict;
  /** Quotité saisissable mensuelle (€). */
  quotiteSaisissableMensuelle: number;
  /** Montant laissé au salarié (rémunération − quotité, jamais < fraction insaisissable). */
  montantLaisseAuSalarie: number;
  /** Fraction absolument insaisissable (montant forfaitaire RSA, L. 3252-3). */
  fractionInsaisissable: number;
  /** Nombre de mois de recouvrement (ceil(creanceTotale / quotité)) ; null si quotité nulle. */
  nombreMoisRecouvrement: number | null;
  /** Détail du calcul par tranche du barème. */
  tranches: TrancheSaisie[];
  /** Année de référence du barème (mention « à actualiser annuellement »). */
  anneeBareme: number;
  country: string;
  baseJuridique: string;
}
