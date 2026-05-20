/**
 * F-243 — Helper partagé `DivorceDcBeSectionPrefillRules` (module pur sans imports Angular).
 *
 * <p>Pré-fill IA pour l'outil "Divorce par consentement mutuel — Belgique"
 * (F-FA-11 BE, CJ art. 1287-1304).</p>
 *
 * SF-246-27 : 2 champs pré-fillables (BELGIQUE UNIQUEMENT) :
 * <ul>
 *   <li><code>dateSignatureConvention</code> — depuis <code>dateAcceptationPV</code>
 *       (date de signature de la convention préalable, format ISO `YYYY-MM-DD`).</li>
 *   <li><code>dateAudienceHomologation</code> — depuis
 *       <code>dateAudienceHomologationDcBe</code> (SF-246-27 — source réelle,
 *       BELGIQUE UNIQUEMENT — art. 1288bis C.jud.BE).</li>
 * </ul>
 *
 * <p>Les autres champs (conventions logement / biens / garde / contributions,
 * enfants mineurs, consentement des époux) ne sont pas pré-fillables de
 * manière fiable depuis l'IA aujourd'hui — l'avocat les saisit manuellement.</p>
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

export const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

export interface DivorceDcBePrefillInput {
  aiData?: FamilleExtractedData | null;
  procedureChecks?: unknown[] | null;
  aiQuestions?: unknown[] | null;
  piecesManquantes?: unknown[] | null;
  triggerEvents?: unknown[] | null;
  workspaceCountry?: string;
}

export function computeDateSignatureConvention(input: DivorceDcBePrefillInput): string | null {
  const v = input.aiData?.dateAcceptationPV;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

/**
 * SF-246-27 : BELGIQUE UNIQUEMENT — date de l'audience d'homologation de la
 * convention DC (art. 1288bis C.jud.BE), ISO YYYY-MM-DD.
 */
export function computeDateAudienceHomologation(input: DivorceDcBePrefillInput): string | null {
  const v = input.aiData?.dateAudienceHomologationDcBe;
  return typeof v === 'string' && ISO_DATE_REGEX.test(v) ? v : null;
}

export function computePrefillCount(input: DivorceDcBePrefillInput): number {
  let n = 0;
  if (computeDateSignatureConvention(input) !== null) n++;
  if (computeDateAudienceHomologation(input) !== null) n++;
  return n;
}

export const DivorceDcBeSectionPrefillRules = {
  ISO_DATE_REGEX,
  computeDateSignatureConvention,
  computeDateAudienceHomologation,
  computePrefillCount,
};
