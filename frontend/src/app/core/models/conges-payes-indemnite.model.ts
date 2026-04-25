/**
 * SF-DT-26-02 : modèles TypeScript pour l'outil décisionnel
 * "Indemnité compensatrice de congés payés" (art. L.3141-26 / L.3141-22 /
 * L.3141-24 Code du travail).
 *
 * Contrat API figé dans SF-DT-26-01 (backend, parallélisation autorisée).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/conges-payes-indemnite
 *
 * France uniquement — l'équivalent BE (pécule de vacances ONSS) suit un
 * régime distinct traité dans une feature séparée.
 */

/**
 * Méthode forcée par l'avocat. `null` = Auto = le backend choisit la
 * méthode la plus favorable au salarié.
 */
export type MethodeCpForcee = 'DIX_POURCENT' | 'MAINTIEN';

export interface CongesPayesIndemniteRequest {
  /** Total des rémunérations brutes de la période de référence (> 0). */
  totalRemunerationPeriodeEur: number;
  /** Jours de congés acquis sur la période (ouvrables, ≥ 0). */
  joursAcquisAnnee: number;
  /** Jours de congés effectivement pris (ouvrables, ≥ 0, ≤ joursAcquisAnnee). */
  joursPris: number;
  /** Salaire mensuel brut de référence pour la méthode du maintien (> 0). */
  salaireMensuelBrutEur: number;
  /** Date de rupture du contrat (YYYY-MM-DD). */
  dateRupture: string;
  /**
   * Surcharge de la méthode retenue. `null` ou absent = backend choisit le
   * plus favorable au salarié (règle L.3141-26 + jurisprudence Cass. soc.).
   */
  methodeForcee?: MethodeCpForcee | null;
}

export interface CongesPayesIndemniteResponse {
  caseFileId: string;
  totalRemunerationPeriodeEur: number;
  joursAcquisAnnee: number;
  joursPris: number;
  salaireMensuelBrutEur: number;
  dateRupture: string;
  methodeForcee: MethodeCpForcee | null;
  /** Jours de congés dus au salarié (joursAcquisAnnee - joursPris, ≥ 0). */
  joursDus: number;
  /** Indemnité calculée par la méthode du dixième (10 % × totalRemuneration × joursDus / joursAcquisAnnee). */
  montantMethodeDixPourcentEur: number;
  /** Indemnité calculée par la méthode du maintien (joursDus / 25 × salaireMensuelBrut, en mois théoriques). */
  montantMethodeMaintienEur: number;
  /** Méthode finalement retenue (la plus favorable, sauf surcharge methodeForcee). */
  methodeRetenue: MethodeCpForcee;
  /** Montant final dû au salarié (max des 2 méthodes ou méthode forcée). */
  montantIndemniteEur: number;
  /** Référence juridique (art. L.3141-26 / L.3141-22 / L.3141-24). */
  baseJuridique: string;
  /** Formule littérale du calcul retenu pour affichage. */
  formule: string;
  /** Messages explicatifs et avertissements (citations juridiques). */
  messages: string[];
  country: 'FRANCE';
}
