/**
 * SF-216-08 — Helper partagé pour l'outil "ARIPA recouvrement pension
 * alimentaire impayée" (F-FA-ARIPA-RECOUVREMENT). Module pur — runtime
 * (`prefillFromAi()`) et static (`getPrefillCount()`) appellent les MÊMES
 * fonctions (contrat F-236 / F-237).
 *
 * Le pipeline IA SF-216-07 backend extrait :
 *  - `aripa_recouvrement_detection.envisage`                      (flag CONTEXTUAL, non remplissable)
 *  - `aripa_recouvrement_detection.montant_pension_mensuelle_due_eur` (NOUVEAU SF-216-07)
 *  - `aripa_recouvrement_detection.titre_executoire_detecte`       (NOUVEAU SF-216-07)
 *  - `filiation_detection_v2.nombre_enfants_detecte`              (déjà branché F-246)
 *
 * Le record Java `FamilleExtractedData` projette ces 3 champs à plat dans le
 * `familleExtractedData` du synthesis frontend. Noms Jackson :
 *   `aripaRecouvrementEnvisage`, `montantPensionMensuelleDueEur`,
 *   `titreExecutoireDetecte`, `nombreEnfantsDetecte`.
 *
 * FRANCE UNIQUEMENT — outil single-country (art. L. 581 CSS).
 *
 *   montantPensionMensuelleEur  ← aiData.montantPensionMensuelleDueEur
 *   titreExecutoire             ← aiData.titreExecutoireDetecte
 *   nombreEnfantsACharge        ← aiData.nombreEnfantsDetecte (sous-objet filiation v2)
 */

import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

/** Sous-ensemble du record `FamilleExtractedData` consommé par le pré-fill. */
export interface AripaRecouvrementFrPrefillInput {
  aiData?: Pick<
    FamilleExtractedData,
    | 'montantPensionMensuelleDueEur'
    | 'titreExecutoireDetecte'
    | 'nombreEnfantsDetecte'
  > | null;
  workspaceCountry?: string;
}

/** True si le pré-fill est applicable (outil FR-only). */
function isFrance(input: AripaRecouvrementFrPrefillInput): boolean {
  return input.workspaceCountry === 'FRANCE';
}

function nonNegativeIntOrNull(v: number | null | undefined): number | null {
  if (typeof v !== 'number' || !Number.isFinite(v) || v < 0) return null;
  return Math.trunc(v);
}

/** Montant de la pension mensuelle fixée par titre exécutoire (€/mois, ≥ 0). */
export function computeMontantPensionMensuelle(
  input: AripaRecouvrementFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.montantPensionMensuelleDueEur);
}

/** Présence d'un titre exécutoire détecté dans le dossier. */
export function computeTitreExecutoire(
  input: AripaRecouvrementFrPrefillInput,
): boolean | null {
  if (!isFrance(input)) return null;
  const v = input.aiData?.titreExecutoireDetecte;
  return typeof v === 'boolean' ? v : null;
}

/** Nombre d'enfants à charge (déjà branché F-246, sous-objet filiation v2). */
export function computeNombreEnfantsACharge(
  input: AripaRecouvrementFrPrefillInput,
): number | null {
  if (!isFrance(input)) return null;
  return nonNegativeIntOrNull(input.aiData?.nombreEnfantsDetecte);
}

/**
 * Nombre exact de champs pré-remplissables — parité stricte avec
 * `prefillFromAi()` du composant. Retourne 0 si `workspaceCountry !== 'FRANCE'`.
 *
 * Comptés ici : montant pension, titre exécutoire, nombre enfants (3 champs).
 */
export function computePrefillCount(
  input: AripaRecouvrementFrPrefillInput,
): number {
  if (!isFrance(input)) return 0;
  let count = 0;
  if (computeMontantPensionMensuelle(input) !== null) count++;
  if (computeTitreExecutoire(input) !== null) count++;
  if (computeNombreEnfantsACharge(input) !== null) count++;
  return count;
}

export const AripaRecouvrementFrPrefillRules = {
  computeMontantPensionMensuelle,
  computeTitreExecutoire,
  computeNombreEnfantsACharge,
  computePrefillCount,
};
