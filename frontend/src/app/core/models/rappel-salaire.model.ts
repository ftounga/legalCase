/**
 * SF-DT-20-02 : modèles TypeScript pour l'outil décisionnel
 * "Rappel de salaire FR" (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail).
 *
 * Contrat API figé dans SF-DT-20-01 (backend, parallélisation autorisée).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/rappel-salaire
 *
 * France uniquement — la Belgique applique un régime distinct (prescription
 * 5 ans art. 2262bis CC + pécule de vacances) traitée dans une feature
 * jumelle F-DT-20-BE au backlog.
 */

/**
 * Méthode de calcul des congés payés sur le rappel de salaire
 * (art. L.3141-26 Code du travail).
 *
 * - `DIX_POURCENT` : règle légale du dixième (10 % du brut), pratique majoritaire.
 * - `MAINTIEN` : méthode du maintien (approximation 1/12 du brut, équivalent
 *   à 1 mois de CP par an de travail).
 * - `AUCUN` : pas d'ICCP appliquée sur le rappel (typiquement CDD avec ICCP
 *   déjà versée séparément à la fin du contrat).
 */
export type RappelSalaireMethodeCpSurRappel = 'DIX_POURCENT' | 'MAINTIEN' | 'AUCUN';

export interface RappelSalaireRequest {
  /** Premier mois de la période de rappel (inclus, YYYY-MM-DD). */
  periodeDebut: string;
  /** Dernier mois de la période de rappel (inclus, ≥ periodeDebut). */
  periodeFin: string;
  /** Salaire mensuel dû (> 0). */
  montantSalaireDuMensuelEur: number;
  /** Salaire mensuel effectivement perçu (≥ 0, < dû). */
  montantSalairePerVerseMensuelEur: number;
  /** Code CCN (nullable — sera normalisé côté backend). */
  conventionCollectiveCode?: string | null;
  /** Ancienneté (années) servant à appliquer la prime CCN (≥ 0). */
  ancienneteAnneesPrime: number;
  /** True si la revalorisation INSEE doit être appliquée. */
  indexInseeRevalorise: boolean;
  /** Taux saisi par l'avocat (0..100, requis si indexInseeRevalorise=true). */
  tauxRevalorisationPct?: number | null;
  /** Méthode de calcul des CP sur le rappel. */
  methodeCpSurRappel: RappelSalaireMethodeCpSurRappel;
}

export interface RappelSalaireResponse {
  caseFileId: string;
  periodeDebut: string;
  periodeFin: string;
  montantSalaireDuMensuelEur: number;
  montantSalairePerVerseMensuelEur: number;
  conventionCollectiveCode: string | null;
  ancienneteAnneesPrime: number;
  indexInseeRevalorise: boolean;
  tauxRevalorisationPct: number | null;
  methodeCpSurRappel: RappelSalaireMethodeCpSurRappel;
  /** Nombre de mois entiers entre debut et fin (inclus). */
  nbMoisPeriode: number;
  /** Différentiel mensuel (dû - versé). */
  differentielMensuelEur: number;
  /** Total brut hors revalorisation = différentiel × nbMois. */
  totalRappelBrutHorsRevalorisationEur: number;
  /** Montant de la revalorisation INSEE (0 si désactivée). */
  montantRevalorisationEur: number;
  /** Prime d'ancienneté CCN appliquée sur le brut hors revalorisation. */
  primeAncienneteEur: number;
  /** Total brut = brut hors revalo + revalo + prime ancienneté. */
  totalRappelBrutEur: number;
  /** Congés payés sur rappel (10 % / maintien / 0). */
  congesPayesSurRappelEur: number;
  /** Total final dû au salarié = total brut + CP sur rappel. */
  totalAvecCpEur: number;
  /** Citation juridique consolidée. */
  baseJuridique: string;
  /** Formule littérale du calcul (JetBrains Mono). */
  formule: string;
  /** Messages explicatifs et avertissements (incl. prescription L.3245-1). */
  messages: string[];
  country: 'FRANCE';
}

export interface MethodeCpOption {
  code: RappelSalaireMethodeCpSurRappel;
  label: string;
  article: string;
}

export const METHODES_CP_SUR_RAPPEL: MethodeCpOption[] = [
  { code: 'DIX_POURCENT', label: 'Règle du dixième (10 %)', article: 'Art. L.3141-24 Code du travail' },
  { code: 'MAINTIEN',     label: 'Méthode du maintien (1/12)', article: 'Art. L.3141-22 Code du travail' },
  { code: 'AUCUN',        label: 'Aucune (CDD avec ICCP versée séparément)', article: 'Art. L.1243-8 Code du travail' },
];
