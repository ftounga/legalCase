/**
 * SF-212-30 — Helper partagé pour l'outil "Congé maternité / paternité —
 * protection et indemnités" (F-DT-77). Module pur — runtime
 * (`prefillFromAi()`) et static (`getPrefillCount()`) appellent les MÊMES
 * fonctions (contrat F-236 / F-237 / F-246).
 *
 * 5 champs IA projetés depuis le sous-objet `congeMaternitePaterniteDetail` :
 *   typeConge                   ← aiData.congeMaternitePaterniteDetail.congeMaternitePaterniteType
 *   rangEnfant                  ← aiData.congeMaternitePaterniteDetail.congeMaterniteRangEnfant
 *   naissanceMultiple           ← aiData.congeMaternitePaterniteDetail.congeMaterniteNaissanceMultiple
 *   dateDebutConge              ← aiData.congeMaternitePaterniteDetail.congeMaterniteDateDebut
 *   salaireMensuelBrutEuros     ← aiData.congeMaternitePaterniteDetail.congeMaterniteSalaireMensuelBrut
 *
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — le régime BE INAMI /
 * mutualité loi 16/03/1971 est juridiquement distinct).
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CongeMaternitePaterniteType } from '../../core/models/conge-maternite-paternite.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface CongeMaternitePaternitePrefillInput {
  aiData?: Pick<TravailExtractedData, 'congeMaternitePaterniteDetail'> | null;
  workspaceCountry?: string;
}

/** Valeurs autorisées pour le type de congé. */
const TYPE_CONGE_VALEURS: ReadonlySet<CongeMaternitePaterniteType> = new Set(['MATERNITE', 'PATERNITE']);

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: CongeMaternitePaternitePrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Enum — `typeConge`. */
export function computeTypeConge(
  input: CongeMaternitePaternitePrefillInput,
): CongeMaternitePaterniteType | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.congeMaternitePaterniteDetail?.congeMaternitePaterniteType;
  if (typeof v !== 'string') return null;
  return TYPE_CONGE_VALEURS.has(v as CongeMaternitePaterniteType)
    ? (v as CongeMaternitePaterniteType)
    : null;
}

/** Entier — `rangEnfant`. Borne raisonnable [1, 20]. */
export function computeRangEnfant(
  input: CongeMaternitePaternitePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.congeMaternitePaterniteDetail?.congeMaterniteRangEnfant;
  if (typeof v !== 'number') return null;
  if (!Number.isInteger(v)) return null;
  if (v < 1 || v > 20) return null;
  return v;
}

/** Booléen — `naissanceMultiple`. */
export function computeNaissanceMultiple(
  input: CongeMaternitePaternitePrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.congeMaternitePaterniteDetail?.congeMaterniteNaissanceMultiple;
  if (typeof v !== 'boolean') return null;
  return v;
}

/** Date ISO YYYY-MM-DD — `dateDebutConge`. */
export function computeDateDebutConge(
  input: CongeMaternitePaternitePrefillInput,
): string | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.congeMaternitePaterniteDetail?.congeMaterniteDateDebut;
  if (typeof v !== 'string') return null;
  // Validation minimale du format ISO YYYY-MM-DD.
  if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return null;
  return v;
}

/** Nombre — `salaireMensuelBrutEuros`. Strictement positif. */
export function computeSalaireMensuelBrut(
  input: CongeMaternitePaternitePrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.congeMaternitePaterniteDetail?.congeMaterniteSalaireMensuelBrut;
  if (typeof v !== 'number') return null;
  if (!Number.isFinite(v) || v <= 0) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si
 * `workspaceCountry !== 'FRANCE'`. Couvre les 5 champs IA du sous-objet
 * `congeMaternitePaterniteDetail`.
 */
export function computePrefillCount(input: CongeMaternitePaternitePrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeTypeConge(input) !== null) count++;
  if (computeRangEnfant(input) !== null) count++;
  if (computeNaissanceMultiple(input) !== null) count++;
  if (computeDateDebutConge(input) !== null) count++;
  if (computeSalaireMensuelBrut(input) !== null) count++;
  return count;
}

export const CongeMaternitePaterniteSectionPrefillRules = {
  computeTypeConge,
  computeRangEnfant,
  computeNaissanceMultiple,
  computeDateDebutConge,
  computeSalaireMensuelBrut,
  computePrefillCount,
};
