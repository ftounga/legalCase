/**
 * SF-218-48 : modèles miroirs du contrat API (backend SF-218-47) pour l'outil
 * décisionnel « Congé de proche aidant » (F-DT-79-conge-proche-aidant). FRANCE
 * uniquement.
 *
 * Détermine l'éligibilité au congé de proche aidant (art. L.3142-16 à L.3142-27
 * CT, loi n° 2020-220 du 06/03/2020) : la personne aidée doit résider en
 * France/EEE (L.3142-16), durée maximale de 12 mois sur l'ensemble de la
 * carrière (L.3142-19), estimation indicative de l'allocation journalière du
 * proche aidant (AJPA) versée par la CAF, plafonnée à 66 jours indemnisés.
 *
 * Distinct du congé parental d'éducation (F-DT-78) et du congé pour évènements
 * familiaux (F-DT-76) — invariant « un outil = une situation ».
 */

/**
 * Lien avec la personne aidée (aligné EXACTEMENT sur l'enum backend
 * {@code CongeProcheAidantLien}).
 */
export type CongeProcheAidantLien =
  | 'CONJOINT'
  | 'ASCENDANT'
  | 'DESCENDANT'
  | 'COLLATERAL'
  | 'SANS_LIEN_RESIDENCE_COMMUNE';

/**
 * Verdict d'éligibilité (aligné sur l'enum backend
 * {@code CongeProcheAidantStatut}).
 */
export type CongeProcheAidantStatut = 'ELIGIBLE' | 'NON_ELIGIBLE';

export interface CongeProcheAidantRequest {
  /** Lien avec la personne aidée. */
  lienPersonneAidee: CongeProcheAidantLien;
  /** La personne aidée réside en France/EEE de façon stable et régulière. */
  personneAideeResideFrance: boolean;
  /** Durée de congé souhaitée, en mois (> 0). */
  dureeSouhaiteeMois: number;
  /** Le salarié demande l'AJPA auprès de la CAF. */
  ajpaDemandee: boolean;
}

export interface CongeProcheAidantResponse {
  caseFileId: string;
  /** Verdict d'éligibilité. */
  statut: CongeProcheAidantStatut;
  lienPersonneAidee: CongeProcheAidantLien;
  personneAideeResideFrance: boolean;
  dureeSouhaiteeMois: number;
  /** Durée maximale légale (12 mois sur la carrière, L.3142-19). */
  dureeMaxMois: number;
  /** Durée retenue = min(souhaitée, 12), null si non éligible. */
  dureeRetenueMois: number | null;
  ajpaDemandee: boolean;
  /** Montant journalier de l'AJPA (≈ 64,54 € en 2026, à vérifier), null si non demandée. */
  ajpaJournaliere: number | null;
  /** Estimation indicative de l'AJPA totale (plafond 66 jours), null si non demandée / non éligible. */
  estimationAjpa: number | null;
  /** Protection de l'emploi / réintégration (L.3142-20 et s.). */
  protectionEmploi: boolean;
  /** Congé non imputable sur les congés payés. */
  nonImputableCongesPayes: boolean;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
