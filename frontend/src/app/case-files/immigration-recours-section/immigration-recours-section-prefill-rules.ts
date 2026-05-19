import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `ImmigrationRecoursSectionComponent` (F-IM-06-recours).
 *
 * SF-246-16 : extension de 2 à 7 champs.
 *   `recoursType`       ← aiData.typeRecoursCode
 *   `dateNotification`  ← aiData.dateNotificationDecisionContestee
 *   `nom`               ← aiData.nomRequerant
 *   `prenom`            ← aiData.prenomRequerant
 *   `nationalite`       ← aiData.nationalite
 *   `dateDecision`      ← aiData.dateDecisionContestee
 *   `reference`         ← aiData.referenceDecision
 */

export interface ImmigrationRecoursPrefillInput {
  aiData?: Partial<ImmigrationExtractedData> | null;
}

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function nonEmpty(v: unknown): string | null {
  return typeof v === 'string' && v.length > 0 ? v : null;
}

export const VALID_RECOURS_CODES = new Set([
  'RECOURS_GRACIEUX_PREFET', 'RECOURS_CONTENTIEUX_TA', 'RECOURS_CNDA',
  'RECOURS_CGRA', 'RECOURS_CCE', 'RECOURS_CE_BELGIQUE',
]);

export function computeRecoursType(input: ImmigrationRecoursPrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  const code = typeof ai.typeRecoursCode === 'string'
    ? ai.typeRecoursCode.toUpperCase()
    : null;
  if (!code || !VALID_RECOURS_CODES.has(code)) return null;
  return code;
}

export function computeDateNotification(input: ImmigrationRecoursPrefillInput): string | null {
  const ai = input.aiData;
  if (!ai) return null;
  const date = ai.dateNotificationDecisionContestee;
  if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
  return date;
}

/** SF-246-16 */
export function computeNomRequerant(input: ImmigrationRecoursPrefillInput): string | null {
  return nonEmpty(input.aiData?.nomRequerant);
}

/** SF-246-16 */
export function computePrenomRequerant(input: ImmigrationRecoursPrefillInput): string | null {
  return nonEmpty(input.aiData?.prenomRequerant);
}

/** SF-246-16 : nationalite déjà présente backend F-235, absente du DTO frontend avant cette SF. */
export function computeNationalite(input: ImmigrationRecoursPrefillInput): string | null {
  return nonEmpty(input.aiData?.nationalite);
}

/** SF-246-16 : date de la décision contestée (format ISO attendu). */
export function computeDateDecision(input: ImmigrationRecoursPrefillInput): string | null {
  const v = input.aiData?.dateDecisionContestee;
  if (typeof v !== 'string' || !ISO_DATE_RE.test(v)) return null;
  return v;
}

/** SF-246-16 : référence/numéro de la décision contestée. */
export function computeReferenceDecision(input: ImmigrationRecoursPrefillInput): string | null {
  return nonEmpty(input.aiData?.referenceDecision);
}

export function computePrefillCount(input: ImmigrationRecoursPrefillInput): number {
  let n = 0;
  if (computeRecoursType(input) !== null) n++;
  if (computeDateNotification(input) !== null) n++;
  if (computeNomRequerant(input) !== null) n++;
  if (computePrenomRequerant(input) !== null) n++;
  if (computeNationalite(input) !== null) n++;
  if (computeDateDecision(input) !== null) n++;
  if (computeReferenceDecision(input) !== null) n++;
  return n;
}

export const ImmigrationRecoursPrefillRules = {
  ISO_DATE_RE,
  VALID_RECOURS_CODES,
  computeRecoursType,
  computeDateNotification,
  computeNomRequerant,
  computePrenomRequerant,
  computeNationalite,
  computeDateDecision,
  computeReferenceDecision,
  computePrefillCount,
} as const;
