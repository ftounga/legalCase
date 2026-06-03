/**
 * SF-221-02 — modèles miroirs du contrat API (backend) pour l'outil décisionnel
 * « Carte B séjour illimité (ressortissant tiers BE) » (F-IM-54-carte-b-sejour-illimite-be).
 *
 * BELGIQUE uniquement — analyseur d'éligibilité au passage carte A → carte B
 * (séjour ILLIMITÉ) après 5 ans (= 60 mois) de séjour régulier ininterrompu,
 * Loi 15/12/1980 art. 14 — à vérifier par avocat.
 *
 * DISTINCT de F-IM-53 (prorogation carte A, maintien temporaire du même motif) et
 * de F-IM-55 (résident longue durée UE, directive 2003/109/CE — mobilité intra-UE).
 * La carte B ne confère PAS la mobilité intra-UE : séjour illimité national.
 */

/**
 * Verdict de l'analyse :
 *  - ELIGIBLE             : ≥ 5 ans + continuité + motif stable + pas d'ordre public (vert)
 *  - DUREE_INSUFFISANTE   : moins de 60 mois — moisRestants > 0 (orange)
 *  - CONTINUITE_ROMPUE    : séjour non ininterrompu / absences excessives (rouge)
 *  - RISQUE_ORDRE_PUBLIC  : risque d'ordre public signalé (rouge)
 *  - A_EXAMINER           : motif instable / données partielles (bleu info)
 */
export type CarteBSejourIllimiteVerdict =
  | 'ELIGIBLE'
  | 'DUREE_INSUFFISANTE'
  | 'CONTINUITE_ROMPUE'
  | 'RISQUE_ORDRE_PUBLIC'
  | 'A_EXAMINER';

export interface CarteBSejourIllimiteBeRequest {
  /** Date de début du séjour régulier ininterrompu (ISO yyyy-MM-dd, non future). */
  dateDebutSejourRegulier: string;
  sejourIninterrompu: boolean;
  absencesSuperieuresLimites: boolean;
  motifSejourStable: boolean;
  ordrePublicRisque: boolean;
}

export interface CarteBSejourIllimiteBeResponse {
  caseFileId: string;
  dateDebutSejourRegulier: string;
  sejourIninterrompu: boolean;
  absencesSuperieuresLimites: boolean;
  motifSejourStable: boolean;
  ordrePublicRisque: boolean;
  verdict: CarteBSejourIllimiteVerdict;
  /** Mois de séjour régulier écoulés depuis dateDebutSejourRegulier. */
  dureeSejourMois: number;
  /** Mois restants avant le seuil de 60 mois (0 si déjà atteint). */
  moisRestants: number;
  basesJuridiques: string[];
  messages: string[];
}

/** Seuil de durée — 5 ans = 60 mois de séjour régulier ininterrompu. */
export const CARTE_B_SEUIL_DUREE_MOIS = 60;
