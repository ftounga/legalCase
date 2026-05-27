import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { NaturalisationConjointBelgeBeNiveauLangue } from '../../core/models/naturalisation-conjoint-belge-be.model';

/**
 * SF-215-10 — Helper partagé pour
 * {@link NaturalisationConjointBelgeBeSectionComponent}
 * (F-IM-29-naturalisation-conjoint-belge-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis `ImmigrationExtractedData`, miroir backend) :
 *  - dateMarriage         : `aiData.naturalisationBeArt16DateMarriage`
 *      (ISO yyyy-MM-dd, regex stricte)
 *  - dureeCohabitationMois : `aiData.naturalisationBeArt16DureeCohabitation`
 *      (entier mois borné 0–600)
 *  - niveauLangue         : `aiData.naturalisationBeArt16NiveauLangue`
 *      (whitelist `INFERIEUR_A2` / `A2` / `SUPERIEUR_A2`)
 *
 * Champs ASPIRATIONNELS (non extraits par l'IA — saisie avocat,
 * `PREFILL_COUNT_ALWAYS_ZERO`) :
 *  - cohabitationLegale, preuveIntegration : la PROVENANCE de la
 *    cohabitation et des preuves d'intégration (cours régional / attestation
 *    commune) ne peut pas être inférée sans documents officiels déjà fournis.
 *  - menaceOrdrePublic / condamnationPenale : information rédhibitoire,
 *    saisie strictement par l'avocat après vérification extrait casier
 *    judiciaire — pas de pré-fill IA pour éviter un verdict erroné.
 *
 * Total pré-fill IA : **3 maximum**.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace
 * FR tout retourne null/0 (le composant affiche une bannière info).
 */

const MAX_DUREE_COHABITATION_MOIS = 600;

export const NATURALISATION_CONJOINT_BELGE_NIVEAU_LANGUE_SET:
  ReadonlySet<NaturalisationConjointBelgeBeNiveauLangue> =
    new Set<NaturalisationConjointBelgeBeNiveauLangue>(['INFERIEUR_A2', 'A2', 'SUPERIEUR_A2']);

/** ISO date stricte yyyy-MM-dd (whitelist regex, pas de parse Date()). */
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function boundedInt(v: unknown, min: number, max: number): number | null {
  if (typeof v !== 'number' || isNaN(v) || !Number.isFinite(v)) return null;
  if (!Number.isInteger(v)) return null;
  if (v < min || v > max) return null;
  return v;
}

export const NaturalisationConjointBelgeBePrefillRules = {
  NATURALISATION_CONJOINT_BELGE_NIVEAU_LANGUE_SET,

  computeDateMarriage(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.naturalisationBeArt16DateMarriage;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_REGEX.test(trimmed)) return null;
    return trimmed;
  },

  computeDureeCohabitation(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.naturalisationBeArt16DureeCohabitation, 0, MAX_DUREE_COHABITATION_MOIS);
  },

  computeNiveauLangue(input: PrefillCountInput): NaturalisationConjointBelgeBeNiveauLangue | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.naturalisationBeArt16NiveauLangue;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!NATURALISATION_CONJOINT_BELGE_NIVEAU_LANGUE_SET.has(
        upper as NaturalisationConjointBelgeBeNiveauLangue)) return null;
    return upper as NaturalisationConjointBelgeBeNiveauLangue;
  },

  /**
   * Max **3** — les 4 champs (cohabitationLegale, preuveIntegration,
   * menaceOrdrePublic, condamnationPenale) sont aspirationnels et
   * `PREFILL_COUNT_ALWAYS_ZERO`.
   */
  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateMarriage(input) !== null) n++;
    if (this.computeDureeCohabitation(input) !== null) n++;
    if (this.computeNiveauLangue(input) !== null) n++;
    return n;
  },
} as const;
