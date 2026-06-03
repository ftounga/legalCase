/**
 * SF-221-03 — modèles miroirs du contrat API (backend) pour l'outil décisionnel
 * « Résident longue durée UE (BE) » (F-IM-55-residence-longue-duree-ue-be).
 *
 * BELGIQUE uniquement — analyseur d'éligibilité au statut de RÉSIDENT LONGUE DURÉE UE
 * (art. 15bis Loi 15/12/1980 — directive 2003/109/CE) : 5 ans (= 60 mois) de séjour
 * légal ininterrompu + ressources stables/suffisantes + assurance maladie + condition
 * d'intégration, sans absences hors UE excessives — à vérifier par avocat.
 *
 * DISTINCT de F-IM-54 (carte B, séjour ILLIMITÉ NATIONAL sans mobilité UE). Le statut
 * résident longue durée UE ajoute des conditions matérielles propres ET confère la
 * mobilité intra-UE.
 */

/**
 * Verdict de l'analyse :
 *  - ELIGIBLE                           : ≥ 5 ans + continuité + ressources + assurance + intégration (vert)
 *  - DUREE_INSUFFISANTE                 : moins de 60 mois — moisRestants > 0 (orange)
 *  - CONDITIONS_MATERIELLES_NON_REUNIES : ressources / assurance / intégration manquante (orange)
 *  - CONTINUITE_ROMPUE                  : séjour non ininterrompu / absences hors UE excessives (rouge)
 *  - A_EXAMINER                         : données partielles (bleu info)
 */
export type ResidenceLongueDureeUeVerdict =
  | 'ELIGIBLE'
  | 'DUREE_INSUFFISANTE'
  | 'CONDITIONS_MATERIELLES_NON_REUNIES'
  | 'CONTINUITE_ROMPUE'
  | 'A_EXAMINER';

export interface ResidenceLongueDureeUeBeRequest {
  /** Date de début du séjour légal ininterrompu (ISO yyyy-MM-dd, non future). */
  dateDebutSejourLegal: string;
  sejourLegalIninterrompu: boolean;
  ressourcesStablesSuffisantes: boolean;
  assuranceMaladie: boolean;
  conditionIntegrationRemplie: boolean;
  absencesHorsUeExcessives: boolean;
}

export interface ResidenceLongueDureeUeBeResponse {
  caseFileId: string;
  dateDebutSejourLegal: string;
  sejourLegalIninterrompu: boolean;
  ressourcesStablesSuffisantes: boolean;
  assuranceMaladie: boolean;
  conditionIntegrationRemplie: boolean;
  absencesHorsUeExcessives: boolean;
  verdict: ResidenceLongueDureeUeVerdict;
  /** Mois de séjour légal écoulés depuis dateDebutSejourLegal. */
  dureeSejourMois: number;
  /** Mois restants avant le seuil de 60 mois (0 si déjà atteint). */
  moisRestants: number;
  /** Conditions matérielles non remplies (ressources / assurance / intégration). */
  conditionsManquantes: string[];
  basesJuridiques: string[];
  messages: string[];
}

/** Seuil de durée — 5 ans = 60 mois de séjour légal ininterrompu. */
export const RLUE_SEUIL_DUREE_MOIS = 60;
