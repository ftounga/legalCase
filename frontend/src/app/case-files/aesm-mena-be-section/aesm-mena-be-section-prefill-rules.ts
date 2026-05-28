import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-215-12 — Helper partagé pour
 * {@link AesmMenaBeSectionComponent} (F-IM-30-aesm-mena-be) — BE mono-pays.
 *
 * Champs pré-fill RÉELS (depuis `ImmigrationExtractedData`, miroir backend) :
 *  - ageActuel         : `aiData.menaAge` (entier borné 0–17)
 *  - dateArriveeBelgique : `aiData.menaDateArrivee` (ISO yyyy-MM-dd, regex stricte)
 *  - dureeScolaire     : `aiData.menaDureeScolaire` (entier borné 0–120 mois)
 *
 * Champs ASPIRATIONNELS (non extraits par l'IA — saisie avocat,
 * `PREFILL_COUNT_ALWAYS_ZERO`) :
 *  - tuteurDesigne, integrationScolaire : statuts vérifiables uniquement
 *    via documents officiels DGDE / établissement scolaire — pas
 *    d'inférence LLM fiable.
 *  - projetVieElabore, perspectiveAutonomie : rapport tutelle / contrat
 *    apprentissage requis — saisie avocat après collecte docs.
 *  - menaceOrdrePublic : information rédhibitoire, saisie strictement par
 *    l'avocat après vérification extrait casier judiciaire.
 *
 * Total pré-fill IA : **3 maximum**.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace
 * FR tout retourne null/0 (le composant affiche une bannière info).
 */

const MAX_AGE_MENA = 17;
const MAX_DUREE_SCOLAIRE_MOIS = 120;

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

export const AesmMenaBePrefillRules = {
  computeAgeActuel(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.menaAge, 0, MAX_AGE_MENA);
  },

  computeDateArriveeBelgique(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.menaDateArrivee;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    if (!ISO_DATE_REGEX.test(trimmed)) return null;
    return trimmed;
  },

  computeDureeScolaire(input: PrefillCountInput): number | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return boundedInt(ai.menaDureeScolaire, 0, MAX_DUREE_SCOLAIRE_MOIS);
  },

  /**
   * Max **3** — les 5 champs (tuteurDesigne, integrationScolaire,
   * projetVieElabore, perspectiveAutonomie, menaceOrdrePublic) sont
   * aspirationnels et `PREFILL_COUNT_ALWAYS_ZERO`.
   */
  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeAgeActuel(input) !== null) n++;
    if (this.computeDateArriveeBelgique(input) !== null) n++;
    if (this.computeDureeScolaire(input) !== null) n++;
    return n;
  },
} as const;
