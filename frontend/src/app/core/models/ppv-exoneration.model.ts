/**
 * SF-218-40 : modèles miroirs du contrat API (backend SF-218-39) pour l'outil
 * décisionnel « PPV — exonération (prime de partage de la valeur) »
 * (F-DT-52-ppv-exoneration). FRANCE uniquement.
 *
 * Calculateur d'exonération de la prime de partage de la valeur (loi
 * n° 2022-1158 du 16/08/2022 art. 1 + loi n° 2023-1107 du 29/11/2023 sur le
 * partage de la valeur) : conformité au plafond d'exonération sociale (3 000 € /
 * 6 000 €), part exonérée vs part imposable, exonération fiscale IR
 * conditionnelle (effectif < 50 + rémunération < 3 SMIC, jusqu'au 31/12/2026).
 *
 * Distinct de F-DT-53 intéressement / participation.
 */

/**
 * Verdict de conformité (aligné EXACTEMENT sur l'enum backend
 * {@code PpvExonerationStatut}) :
 *  - CONFORME : montant ≤ plafond d'exonération sociale → intégralement exonéré,
 *    aucune part imposable au titre du dépassement.
 *  - PLAFOND_DEPASSE : montant > plafond → fraction excédentaire réintégrée.
 */
export type PpvExonerationStatut = 'CONFORME' | 'PLAFOND_DEPASSE';

export interface PpvExonerationRequest {
  /** Montant de la PPV versée sur l'année civile (> 0). */
  montantPrime: number;
  /** Présence d'un accord d'intéressement (porte le plafond à 6 000 €). */
  accordInteressementPresent: boolean;
  /** Rémunération annuelle brute du bénéficiaire (> 0) — base du test « < 3 SMIC ». */
  remunerationAnnuelleBrute: number;
  /** Entreprise de moins de 50 salariés. */
  effectifMoins50: boolean;
  /** Prime (ou partie) affectée à un plan d'épargne salariale (défaut false). */
  versementPlanEpargne?: boolean | null;
}

export interface PpvExonerationResponse {
  caseFileId: string;
  montantPrime: number;
  accordInteressementPresent: boolean;
  remunerationAnnuelleBrute: number;
  effectifMoins50: boolean;
  versementPlanEpargne: boolean;
  /** Plafond d'exonération sociale retenu (3 000 € ou 6 000 €). */
  plafondSocialApplique: number;
  /** Fraction exonérée de cotisations sociales (≤ plafond). */
  montantExonere: number;
  /** Fraction excédentaire réintégrée (0 si conforme). */
  montantImposable: number;
  /** Exonération d'impôt sur le revenu de la part exonérée socialement. */
  exonerationFiscaleIr: boolean;
  /** Verdict de conformité au plafond. */
  statut: PpvExonerationStatut;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
