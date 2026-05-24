/**
 * SF-212-24 — Helper partagé pour l'outil "Égalité salariale femmes/hommes"
 * (F-DT-56). Module pur — runtime (`prefillFromAi()`) et static
 * (`getPrefillCount()`) appellent les MÊMES fonctions (contrat F-236 / F-237 /
 * F-246).
 *
 * Le pipeline IA (SF-212-23 backend) extrait un sous-objet
 * `egalite_salariale_detail` (4 champs) regroupé en un record imbriqué
 * `egaliteSalarialeDetail` (sous limite JVM 255 slots TravailExtractedData).
 * FRANCE uniquement (`workspaceCountry === 'FRANCE'` — L. 1144-1 charge de la
 * preuve aménagée et index L. 1142-8 sont propres au droit français).
 *
 *   sexeSalarie                       ← aiData.egaliteSalarialeDetail.sexeSalarie
 *   salaireMensuelBrutSalarieEuros    ← aiData.egaliteSalarialeDetail.salaireBrut
 *   ancienneteMois                    ← aiData.egaliteSalarialeDetail.anciennete
 *   ecartPourcentage                  ← aiData.egaliteSalarialeDetail.ecartPourcentage
 */

import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { EgsSexeSalarie } from '../../core/models/egalite-salariale-fh.model';

/** Sous-ensemble du record `TravailExtractedData` consommé par le pré-fill. */
export interface EgaliteSalarialeFhPrefillInput {
  aiData?: Pick<TravailExtractedData, 'egaliteSalarialeDetail'> | null;
  workspaceCountry?: string;
}

/** Borne supérieure raisonnable de l'ancienneté en mois (50 ans). */
const ANCIENNETE_MAX_MOIS = 600;

/** Borne supérieure raisonnable du salaire brut mensuel en euros. */
const SALAIRE_MAX_EUROS = 1_000_000;

/** Valeurs autorisées pour le sexe du salarié. */
const SEXE_VALEURS: ReadonlySet<EgsSexeSalarie> = new Set(['FEMME', 'HOMME']);

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: EgaliteSalarialeFhPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

/** Enum — `sexeSalarie`. */
export function computeSexeSalarie(
  input: EgaliteSalarialeFhPrefillInput,
): EgsSexeSalarie | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.egaliteSalarialeDetail?.sexeSalarie;
  if (typeof v !== 'string') return null;
  return SEXE_VALEURS.has(v as EgsSexeSalarie) ? (v as EgsSexeSalarie) : null;
}

/** Nombre décimal — `salaireMensuelBrutSalarieEuros`. Borné [0, 1_000_000]. */
export function computeSalaireBrut(
  input: EgaliteSalarialeFhPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.egaliteSalarialeDetail?.salaireBrut;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > SALAIRE_MAX_EUROS) return null;
  return v;
}

/** Entier — `ancienneteMois`. Borné [0, 600] et integer fini. */
export function computeAncienneteMois(
  input: EgaliteSalarialeFhPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.egaliteSalarialeDetail?.anciennete;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > ANCIENNETE_MAX_MOIS) return null;
  return Math.trunc(v);
}

/** Nombre décimal — `ecartPourcentage`. Borné [0, 100]. */
export function computeEcartPourcentage(
  input: EgaliteSalarialeFhPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.egaliteSalarialeDetail?.ecartPourcentage;
  if (typeof v !== 'number' || !Number.isFinite(v)) return null;
  if (v < 0 || v > 100) return null;
  return v;
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 * Couvre les 4 champs IA du sous-objet `egaliteSalarialeDetail`.
 */
export function computePrefillCount(input: EgaliteSalarialeFhPrefillInput): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeSexeSalarie(input) !== null) count++;
  if (computeSalaireBrut(input) !== null) count++;
  if (computeAncienneteMois(input) !== null) count++;
  if (computeEcartPourcentage(input) !== null) count++;
  return count;
}

export const EgaliteSalarialeFhSectionPrefillRules = {
  computeSexeSalarie,
  computeSalaireBrut,
  computeAncienneteMois,
  computeEcartPourcentage,
  computePrefillCount,
};
