/**
 * SF-212-16 — Helper partagé pour l'outil "Télétravail — conformité et
 * litige" (F-DT-82). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-15 backend) extrait un sous-objet `teletravail_detail`
 * (7 champs) projeté à plat sur `TravailExtractedData`. FRANCE uniquement
 * (`workspaceCountry === 'FRANCE'` — l'ANI 2020 et le régime L. 1222-9 sont
 * strictement français ; en BE, régime distinct CCT n°85 / Loi 05/03/2017).
 *
 *   cadreTeletravail                       ← aiData.teletravailCadre
 *   doubleVolontariatRespectee             ← aiData.teletravailDoubleVolontariat
 *   indemniteOccupationVersee              ← aiData.teletravailIndemniteVersee
 *   montantIndemniteJournalierEuros        ← aiData.teletravailMontantIndemniteJournalier
 *   accidentDomicileDetecte                ← aiData.teletravailAccidentDomicile
 *   retourBureauImposeUnilateralement      ← aiData.teletravailRetourBureauImpose
 *   refusTeletravailCauseIncrimination     ← aiData.teletravailRefusCauseIncrimination
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { TeletravailCadre } from '../../core/models/teletravail-accord.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface TeletravailAccordPrefillInput {
  aiData?: Pick<
    TravailExtractedData,
    | 'teletravailCadre'
    | 'teletravailDoubleVolontariat'
    | 'teletravailIndemniteVersee'
    | 'teletravailMontantIndemniteJournalier'
    | 'teletravailAccidentDomicile'
    | 'teletravailRetourBureauImpose'
    | 'teletravailRefusCauseIncrimination'
  > | null;
  workspaceCountry?: string;
}

/** Valeurs autorisées du cadre juridique (whitelist enum backend). */
const CADRE_VALUES: ReadonlySet<TeletravailCadre> = new Set<TeletravailCadre>([
  'ACCORD_COLLECTIF',
  'CHARTE_UNILATERALE',
  'ACCORD_INDIVIDUEL',
  'AUCUN',
]);

/** Borne supérieure raisonnable du montant journalier (€/jour). */
const MONTANT_JOURNALIER_MAX_EUROS = 50;

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: TeletravailAccordPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Chaîne — `cadreTeletravail`. Whitelistée sur l'enum backend. */
export function computeCadre(
  input: TeletravailAccordPrefillInput,
): TeletravailCadre | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.teletravailCadre;
  if (typeof v !== 'string') return null;
  return CADRE_VALUES.has(v as TeletravailCadre) ? (v as TeletravailCadre) : null;
}

/** Booléen direct — `doubleVolontariatRespectee`. */
export function computeDoubleVolontariat(
  input: TeletravailAccordPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.teletravailDoubleVolontariat;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `indemniteOccupationVersee`. */
export function computeIndemniteVersee(
  input: TeletravailAccordPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.teletravailIndemniteVersee;
  return typeof v === 'boolean' ? v : null;
}

/** Décimal — `montantIndemniteJournalierEuros`. Borné [0, 50]. */
export function computeMontantJournalier(
  input: TeletravailAccordPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.teletravailMontantIndemniteJournalier;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > MONTANT_JOURNALIER_MAX_EUROS) return null;
  return v;
}

/** Booléen direct — `accidentDomicileDetecte`. */
export function computeAccidentDomicile(
  input: TeletravailAccordPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.teletravailAccidentDomicile;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `retourBureauImposeUnilateralement`. */
export function computeRetourBureauImpose(
  input: TeletravailAccordPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.teletravailRetourBureauImpose;
  return typeof v === 'boolean' ? v : null;
}

/** Booléen direct — `refusTeletravailCauseIncrimination`. */
export function computeRefusIncrimine(
  input: TeletravailAccordPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.teletravailRefusCauseIncrimination;
  return typeof v === 'boolean' ? v : null;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 7 champs IA.
 */
export function computePrefillCount(input: TeletravailAccordPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeCadre(input) !== null) count++;
  if (computeDoubleVolontariat(input) !== null) count++;
  if (computeIndemniteVersee(input) !== null) count++;
  if (computeMontantJournalier(input) !== null) count++;
  if (computeAccidentDomicile(input) !== null) count++;
  if (computeRetourBureauImpose(input) !== null) count++;
  if (computeRefusIncrimine(input) !== null) count++;
  return count;
}

export const TeletravailAccordSectionPrefillRules = {
  computeCadre,
  computeDoubleVolontariat,
  computeIndemniteVersee,
  computeMontantJournalier,
  computeAccidentDomicile,
  computeRetourBureauImpose,
  computeRefusIncrimine,
  computePrefillCount,
};
