import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  Naturalisation12bisNiveauLangue,
  Naturalisation12bisTypeSejour,
} from '../../core/models/naturalisation-12bis-be.model';

/**
 * SF-215-08 — Helper partagé pour {@link Naturalisation12bisBeSectionComponent}
 * (F-IM-28-naturalisation-12bis-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dureeSejour  : `aiData.naturalisationBeDureeSejour`
 *      (entier mois borné 0–600)
 *  - typeSejour   : `aiData.naturalisationBeTypeSejour`
 *      (whitelist `LIMITE` / `ILLIMITE`)
 *  - niveauLangue : `aiData.naturalisationBeNiveauLangue`
 *      (whitelist `INFERIEUR_A2` / `A2` / `SUPERIEUR_A2`)
 *
 * Champs ASPIRATIONNELS (non extraits par l'IA — saisie avocat,
 * `PREFILL_COUNT_ALWAYS_ZERO`) :
 *  - preuveIntegration / preuveEmploi : la PROVENANCE des preuves
 *    (cours régional / attestations Onem / fiches de paie 468 jours) ne
 *    peut pas être inférée d'un dossier brut sans documents officiels
 *    déjà fournis.
 *  - menaceOrdrePublic / condamnationPenale : information rédhibitoire,
 *    saisie strictement par l'avocat après vérification extrait casier
 *    judiciaire — pas de pré-fill IA pour éviter un verdict erroné.
 *
 * Total pré-fill IA : **3 maximum**.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace
 * FR tout retourne null/0 (le composant affiche une bannière info).
 */

const MAX_DUREE_SEJOUR_MOIS = 600;

export const NATURALISATION_12BIS_TYPE_SEJOUR_SET: ReadonlySet<Naturalisation12bisTypeSejour> =
  new Set<Naturalisation12bisTypeSejour>(['LIMITE', 'ILLIMITE']);

export const NATURALISATION_12BIS_NIVEAU_LANGUE_SET: ReadonlySet<Naturalisation12bisNiveauLangue> =
  new Set<Naturalisation12bisNiveauLangue>(['INFERIEUR_A2', 'A2', 'SUPERIEUR_A2']);

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function boundedInt(v: unknown, min: number, max: number): number | null {
  if (typeof v !== 'number' || isNaN(v) || !Number.isFinite(v)) return null;
  if (!Number.isInteger(v)) return null;
  if (v < min || v > max) return null;
  return v;
}

export const Naturalisation12bisBePrefillRules = {
  NATURALISATION_12BIS_TYPE_SEJOUR_SET,
  NATURALISATION_12BIS_NIVEAU_LANGUE_SET,

  computeDureeSejour(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.naturalisationBeDureeSejour, 0, MAX_DUREE_SEJOUR_MOIS);
  },

  computeTypeSejour(input: PrefillCountInput): Naturalisation12bisTypeSejour | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.naturalisationBeTypeSejour;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!NATURALISATION_12BIS_TYPE_SEJOUR_SET.has(upper as Naturalisation12bisTypeSejour)) return null;
    return upper as Naturalisation12bisTypeSejour;
  },

  computeNiveauLangue(input: PrefillCountInput): Naturalisation12bisNiveauLangue | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.naturalisationBeNiveauLangue;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!NATURALISATION_12BIS_NIVEAU_LANGUE_SET.has(upper as Naturalisation12bisNiveauLangue)) return null;
    return upper as Naturalisation12bisNiveauLangue;
  },

  /**
   * Max **3** — les 4 checkboxes (preuveIntegration, preuveEmploi,
   * menaceOrdrePublic, condamnationPenale) sont aspirationnelles et
   * `PREFILL_COUNT_ALWAYS_ZERO`.
   */
  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDureeSejour(input) !== null) n++;
    if (this.computeTypeSejour(input) !== null) n++;
    if (this.computeNiveauLangue(input) !== null) n++;
    return n;
  },
} as const;
