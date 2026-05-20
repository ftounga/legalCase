/**
 * F-236 SF-236-02 — Helper partagé pour l'outil "Discrimination — dommages-intérêts".
 * SF-246-21 : branchement de 2 champs depuis `sante_discrimination_detection`
 * (discriminationMotif — mapping vers MotifDiscrimination FR, discriminationContexte
 * — mapping vers ContexteActe).
 * Module pur — runtime et static appellent les mêmes fonctions.
 *
 * Logique miroir : `salaireMensuelReference ← aiData.salaireBrutMensuel > 0`.
 *
 * Codes backend (whitelist prompt) → codes frontend (enums TypeScript).
 */

import { ContexteActe, MotifDiscriminationFr } from '../../core/models/discrimination.model';

/**
 * SF-246-21 — mapping codes IA backend → MotifDiscriminationFr (frontend).
 * Les codes BE ne sont pas produits par ce prompt (FR seul).
 * Codes backend : ORIGINE, SEXE, HANDICAP, AGE, RELIGION,
 *                 ORIENTATION_SEXUELLE, GROSSESSE, ACTIVITES_SYNDICALES, AUTRE.
 */
const AI_MOTIF_TO_FRONTEND: Readonly<Record<string, MotifDiscriminationFr>> = {
  ORIGINE:              'ORIGINE_ETHNIQUE',
  SEXE:                 'SEXE_GROSSESSE',
  GROSSESSE:            'SEXE_GROSSESSE',
  HANDICAP:             'ETAT_SANTE_HANDICAP',
  AGE:                  'AGE',
  RELIGION:             'OPINIONS_POLITIQUES',
  ORIENTATION_SEXUELLE: 'ORIENTATION_SEXUELLE',
  ACTIVITES_SYNDICALES: 'OPINIONS_POLITIQUES',
  AUTRE:                'OPINIONS_POLITIQUES', // fallback neutre
};

/**
 * SF-246-21 — mapping codes IA backend → ContexteActe (frontend).
 * Codes backend : REFUS_EMBAUCHE, LICENCIEMENT, MUTATION,
 *                 SANCTION_DISCIPLINAIRE, PROMOTION_REFUSEE,
 *                 REMUNERATION_INFERIEURE, HARCELEMENT, AUTRE.
 */
const AI_CONTEXTE_TO_FRONTEND: Readonly<Record<string, ContexteActe>> = {
  REFUS_EMBAUCHE:        'EMBAUCHE_REFUSEE',
  LICENCIEMENT:          'LICENCIEMENT',
  MUTATION:              'PROMOTION_REFUSEE',
  SANCTION_DISCIPLINAIRE: 'SANCTION',
  PROMOTION_REFUSEE:     'PROMOTION_REFUSEE',
  REMUNERATION_INFERIEURE: 'DIFFERENCE_SALARIALE',
  HARCELEMENT:           'HARCELEMENT_LIE_DISCRIMINATION',
  AUTRE:                 'LICENCIEMENT', // fallback neutre
};

export interface DiscriminationPrefillInput {
  aiData?: {
    salaireBrutMensuel?: number | null;
    // SF-246-21 — sante_discrimination_detection
    discriminationMotif?: string | null;
    discriminationContexte?: string | null;
  } | null;
}

export function computeSalaireMensuelReference(input: DiscriminationPrefillInput): number | null {
  const v = input.aiData?.salaireBrutMensuel;
  return typeof v === 'number' && v > 0 ? v : null;
}

/**
 * SF-246-21 : motif de discrimination — code backend mappé vers MotifDiscriminationFr.
 * Retourne null si le code n'est pas reconnu.
 */
export function computeDiscriminationMotif(input: DiscriminationPrefillInput): MotifDiscriminationFr | null {
  const v = input.aiData?.discriminationMotif;
  if (typeof v !== 'string' || v.trim().length === 0) return null;
  const code = v.trim().toUpperCase();
  return AI_MOTIF_TO_FRONTEND[code] ?? null;
}

/**
 * SF-246-21 : contexte de discrimination — code backend mappé vers ContexteActe.
 * Retourne null si le code n'est pas reconnu.
 */
export function computeDiscriminationContexte(input: DiscriminationPrefillInput): ContexteActe | null {
  const v = input.aiData?.discriminationContexte;
  if (typeof v !== 'string' || v.trim().length === 0) return null;
  const code = v.trim().toUpperCase();
  return AI_CONTEXTE_TO_FRONTEND[code] ?? null;
}

export function computePrefillCount(input: DiscriminationPrefillInput): number {
  let count = 0;
  if (computeSalaireMensuelReference(input) !== null) count++;
  if (computeDiscriminationMotif(input) !== null) count++;
  if (computeDiscriminationContexte(input) !== null) count++;
  return count;
}

export const DiscriminationSectionPrefillRules = {
  computeSalaireMensuelReference,
  computeDiscriminationMotif,
  computeDiscriminationContexte,
  computePrefillCount,
};
