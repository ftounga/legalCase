/**
 * SF-218-46 : modèles miroirs du contrat API (backend SF-218-45) pour l'outil
 * décisionnel « Congé parental d'éducation »
 * (F-DT-78-conge-parental-education). FRANCE uniquement.
 *
 * Détermine l'éligibilité au congé parental d'éducation (art. L.1225-47 à
 * L.1225-60 CT) : condition d'un an d'ancienneté minimum à la date de
 * naissance / d'arrivée de l'enfant (L.1225-47), date de fin maximale du droit
 * au 3e anniversaire de l'enfant (L.1225-48), réintégration dans le précédent
 * emploi ou un emploi similaire (L.1225-55), information PreParE (CAF).
 *
 * Distinct du congé de paternité/maternité (F-212) et du congé pour évènements
 * familiaux (F-DT-76) — invariant « un outil = une situation ».
 */

/**
 * Modalité d'exercice (aligné EXACTEMENT sur l'enum backend
 * {@code CongeParentalEducationModalite}).
 */
export type CongeParentalEducationModalite = 'TEMPS_PLEIN' | 'TEMPS_PARTIEL';

/**
 * Verdict d'éligibilité (aligné sur l'enum backend
 * {@code CongeParentalEducationStatut}).
 */
export type CongeParentalEducationStatut = 'ELIGIBLE' | 'NON_ELIGIBLE';

export interface CongeParentalEducationRequest {
  /** Ancienneté à la date de naissance / adoption, en mois (≥ 0). */
  ancienneteMois: number;
  /** Modalité d'exercice du congé. */
  modalite: CongeParentalEducationModalite;
  /** Nombre d'enfants concernés (≥ 1). */
  nombreEnfants: number;
  /** Date de naissance ou d'arrivée de l'enfant au foyer (ISO yyyy-MM-dd). */
  dateNaissanceOuAdoption: string;
}

export interface CongeParentalEducationResponse {
  caseFileId: string;
  /** Verdict d'éligibilité. */
  statut: CongeParentalEducationStatut;
  ancienneteMois: number;
  modaliteRetenue: CongeParentalEducationModalite;
  nombreEnfants: number;
  dateNaissanceOuAdoption: string;
  /** Date de fin maximale du droit (3e anniversaire), null si non éligible. */
  dateFinMax: string | null;
  /** Durée maximale du congé en mois (36 si éligible, 0 sinon). */
  dureeMaxMois: number;
  /** Réintégration garantie (L.1225-55). */
  protectionReintegration: boolean;
  /** Mention PreParE (CAF) — information, montant non calculé. */
  mentionPreparE: boolean;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
