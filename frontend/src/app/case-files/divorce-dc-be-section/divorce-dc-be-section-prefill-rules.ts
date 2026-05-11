/**
 * F-243 — Helper partagé `DivorceDcBeSectionPrefillRules` (module pur sans imports Angular).
 *
 * <p>Pré-fill IA pour l'outil "Divorce par consentement mutuel — Belgique"
 * (F-FA-11 BE, CJ art. 1287-1304). Le seul signal IA actuellement
 * exploité est `aiData.dateAcceptationPV` (renommée dans le prompt F-241
 * en `date_accord_initial_divorce` mais conservée sous ce nom côté
 * `FamilleExtractedData` frontend pour rétro-compat) — la date de
 * signature de la convention préalable, format ISO `YYYY-MM-DD`.</p>
 *
 * <p>Champs pré-remplis :
 * <ul>
 *   <li><code>dateSignatureConvention</code> — quand <code>dateAcceptationPV</code>
 *       est une chaîne ISO `YYYY-MM-DD` valide.</li>
 * </ul></p>
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

export function computePrefillCount(input: DivorceDcBePrefillInput): number {
  let n = 0;
  if (computeDateSignatureConvention(input) !== null) n++;
  return n;
}

export const DivorceDcBeSectionPrefillRules = {
  ISO_DATE_REGEX,
  computeDateSignatureConvention,
  computePrefillCount,
};
