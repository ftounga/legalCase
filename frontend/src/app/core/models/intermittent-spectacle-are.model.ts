/**
 * SF-218-18 : modèles miroirs du contrat API (backend SF-218-17) pour l'outil
 * décisionnel « Intermittent du spectacle : ouverture des droits ARE (annexes
 * 8/10) » (F-DT-106-intermittent-spectacle-are). FRANCE uniquement.
 *
 * Régime des intermittents du spectacle (annexes 8 — techniciens et 10 —
 * artistes du règlement d'assurance chômage Unedic) : condition d'affiliation
 * de 507 heures sur les 12 mois précédant la fin du contrat. Conversion des
 * cachets artistiques (1 cachet = 12 h) et écrêtage des heures de formation /
 * enseignement assimilables. Verdict d'ouverture des droits, déficit / excédent
 * d'heures et date indicative de réexamen annuel.
 */

/**
 * Annexe du règlement d'assurance chômage applicable (aligné EXACTEMENT sur
 * l'enum backend {@code Annexe}) :
 *  - ANNEXE_8_TECHNICIENS : ouvriers et techniciens de l'édition, de la
 *    production cinématographique / audiovisuelle et du spectacle.
 *  - ANNEXE_10_ARTISTES : artistes du spectacle (cachets convertis en heures).
 */
export type AnnexeIntermittent = 'ANNEXE_8_TECHNICIENS' | 'ANNEXE_10_ARTISTES';

/**
 * Verdict d'ouverture des droits ARE (aligné EXACTEMENT sur l'enum backend
 * {@code OuvertureDroits}) : seuil d'affiliation 507 h atteint ou non.
 */
export type OuvertureDroits = 'OUVERTS' | 'NON_OUVERTS';

/**
 * Statut consolidé de l'ouverture des droits (aligné EXACTEMENT sur l'enum
 * backend {@code StatutOuverture}) :
 *  - DROITS_OUVERTS : seuil atteint, droits ouverts.
 *  - DROITS_NON_OUVERTS : seuil non atteint.
 *  - A_VERIFIER : total dans la marge de ± 10 h du seuil → vérification finale
 *    France Travail recommandée.
 */
export type StatutOuverture = 'DROITS_OUVERTS' | 'DROITS_NON_OUVERTS' | 'A_VERIFIER';

export interface IntermittentSpectacleAreRequest {
  annexe: AnnexeIntermittent;
  dateFinContrat: string; // ISO YYYY-MM-DD — fin du dernier contrat
  heuresTravaillees12Mois: number; // >= 0 — heures déclarées sur la période de référence
  nombreCachets: number; // >= 0 — cachets artistiques annexe 10 (1 cachet = 12 h)
  heuresFormationDispensees: number; // >= 0 — heures d'enseignement assimilables (écrêtées au plafond)
}

export interface IntermittentSpectacleAreResponse {
  caseFileId: string;
  annexe: AnnexeIntermittent;
  dateFinContrat: string;
  heuresTravaillees12Mois: number;
  nombreCachets: number;
  heuresFormationDispensees: number;
  /** Heures issues de la conversion des cachets (nombreCachets × 12). */
  heuresCachets: number;
  /** Heures de formation retenues après écrêtage au plafond. */
  heuresFormationRetenues: number;
  /** Total d'heures retenues = travaillées + cachets + formation retenue. */
  heuresTotalesRetenues: number;
  /** Seuil d'affiliation (507 h sur 12 mois). */
  seuilHeures: number;
  /** Verdict d'ouverture des droits ARE. */
  ouvertureDroits: OuvertureDroits;
  /** Heures manquantes pour atteindre le seuil (= max(0, 507 − total)). */
  heuresManquantes: number;
  /** Heures excédentaires au-delà du seuil (= max(0, total − 507)). */
  heuresExcedentaires: number;
  /** Statut consolidé (DROITS_OUVERTS / DROITS_NON_OUVERTS / A_VERIFIER). */
  statut: StatutOuverture;
  /** Date indicative du prochain réexamen annuel (= dateFinContrat + 12 mois). */
  dateProchainExamen: string;
  country: string;
  baseJuridique: string;
}
