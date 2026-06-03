/**
 * SF-218-38 : modèles miroirs du contrat API (backend SF-218-37) pour l'outil
 * décisionnel « RTT — monétisation (rachat de jours de RTT) »
 * (F-DT-51-rtt-monetisation). FRANCE uniquement.
 *
 * Calculateur d'indemnité de monétisation de jours de RTT (loi n° 2022-1157 du
 * 16/08/2022 art. 5, dispositif prolongé jusqu'au 31/12/2026) : sur demande du
 * salarié et avec accord de l'employeur, les jours/demi-journées de RTT acquis
 * entre le 01/01/2022 et le 31/12/2026 peuvent être renoncés contre rémunération
 * majorée (taux 10–25 %, régime social et fiscal aligné sur les heures
 * supplémentaires).
 *
 * Distinct de F-DT-19 heures supplémentaires et de F-DT-80 acquisition de JRTT.
 */

/**
 * Verdict d'éligibilité (aligné EXACTEMENT sur l'enum backend
 * {@code RttMonetisationStatut}) :
 *  - ELIGIBLE : jours acquis dans la fenêtre 01/01/2022 → 31/12/2026 → montant
 *    brut majoré calculé.
 *  - NON_ELIGIBLE : jours hors de la fenêtre → pas de monétisation.
 */
export type RttMonetisationStatut = 'ELIGIBLE' | 'NON_ELIGIBLE';

export interface RttMonetisationRequest {
  /** Nombre de jours de RTT auxquels le salarié renonce (> 0). */
  nombreJoursRttRenonces: number;
  /** Salaire journalier brut de référence (> 0). */
  salaireJournalierBrut: number;
  /** Taux de majoration applicable (optionnel, défaut 25, borné 10–25). */
  tauxMajorationConventionnel?: number | null;
  /** Jours acquis dans la fenêtre du dispositif (01/01/2022 → 31/12/2026). */
  joursAcquisDansFenetre: boolean;
}

export interface RttMonetisationResponse {
  caseFileId: string;
  nombreJoursRttRenonces: number;
  salaireJournalierBrut: number;
  /** Taux de majoration effectivement appliqué (borné 10–25). */
  tauxApplique: number;
  joursAcquisDansFenetre: boolean;
  /** Montant brut majoré de la monétisation (null si NON_ELIGIBLE). */
  montantBrut: number | null;
  /** Régime social et fiscal applicable (ALIGNE_HEURES_SUPPLEMENTAIRES). */
  regimeSocialFiscal: string;
  /** Verdict d'éligibilité. */
  statut: RttMonetisationStatut;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
