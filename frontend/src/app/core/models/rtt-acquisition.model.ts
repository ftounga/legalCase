/**
 * SF-218-50 : modèles miroirs du contrat API (backend SF-218-49) pour l'outil
 * décisionnel « RTT — acquisition selon accord d'aménagement »
 * (F-DT-80-rtt-acquisition). FRANCE uniquement.
 *
 * Calcule le nombre théorique de jours de réduction du temps de travail (JRTT)
 * acquis dans le cadre d'un accord d'aménagement du temps de travail sur l'année
 * (art. L.3121-41 à L.3121-44 CT) : les heures effectuées entre 35 h et
 * l'horaire collectif sont compensées par des JRTT, SANS majoration. À défaut
 * d'accord, l'outil renvoie au régime des heures supplémentaires (F-DT-19).
 *
 * DISTINCT des heures supplémentaires (F-DT-19) et de la monétisation de RTT
 * (F-DT-51) — invariant « un outil = une situation ».
 */

/**
 * Verdict (aligné sur l'enum backend {@code RttAcquisitionStatut}).
 *  - CALCULE : accord d'aménagement présent, nombre de JRTT calculé.
 *  - RENVOI_HEURES_SUP : pas d'accord, renvoi au régime des heures sup (F-DT-19).
 */
export type RttAcquisitionStatut = 'CALCULE' | 'RENVOI_HEURES_SUP';

export interface RttAcquisitionRequest {
  /** Horaire hebdomadaire fixé par l'accord d'aménagement (> 35 et ≤ 48 ; ex. 37, 39). */
  horaireHebdomadaireCollectif: number;
  /** Un accord d'aménagement du temps de travail sur l'année existe. */
  accordCollectifPresent: boolean;
  /** Nombre de semaines effectivement travaillées dans l'année (optionnel, défaut 47). */
  semainesTravailleesAn?: number | null;
}

export interface RttAcquisitionResponse {
  caseFileId: string;
  /** Verdict (CALCULE / RENVOI_HEURES_SUP). */
  statut: RttAcquisitionStatut;
  horaireHebdomadaireCollectif: number;
  accordCollectifPresent: boolean;
  semainesTravailleesAn: number;
  /** Nombre théorique de JRTT acquis sur l'année (sans majoration), null si renvoi heures sup. */
  nombreJrttTheorique: number | null;
  /** Description de la base de calcul (horaire, semaines, sans majoration). */
  base: string;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
