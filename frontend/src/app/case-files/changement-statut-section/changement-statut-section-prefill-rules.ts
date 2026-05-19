import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  TitreSejourCode,
  mapTitreSejourFromIa,
} from '../../core/models/changement-statut.model';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `ChangementStatutSectionComponent` (F-IM-11-changement-statut).
 *
 * 4 champs pré-remplis (SF-246-19 : +2) :
 *   1. `titreActuel` (TitreSejourCode) — `mapTitreSejourFromIa` cascade :
 *      d'abord depuis `aiData.typeTitreSejourCode`, sinon depuis
 *      `aiData.typeTitreSejour` (texte libre).
 *   2. `dureeRestanteSurTitreActuelMois` (number) — calculé depuis
 *      `aiData.dateExpirationTitre` (ISO string parsable) : floor((expiration
 *      - aujourd'hui) / 30.44 jours), plancher à 0. Posé même si la date
 *      est passée (champ=0).
 *   3. `titreEnvisage` (TitreSejourCode) — depuis `aiData.changementTitreEnvisage`
 *      via `mapTitreSejourFromIa`. (SF-246-19)
 *   4. `remunerationContratEur` (number) — depuis `aiData.changementRemunerationEur`
 *      (entier > 0, ≤ 500 000). (SF-246-19)
 *
 * Single-country FR — Le composant gate `isFrance()` côté runtime. Le
 * helper applique le même garde-fou via `workspaceCountry`.
 */

const MS_PER_AVG_MONTH = 1000 * 60 * 60 * 24 * 30.44;

export const ChangementStatutPrefillRules = {
  /** Titre actuel depuis IA — cascade code → texte. */
  computeTitreActuel(input: PrefillCountInput): TitreSejourCode | null {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const fromCode = mapTitreSejourFromIa(ai.typeTitreSejourCode ?? null);
    return fromCode ?? mapTitreSejourFromIa(ai.typeTitreSejour ?? null);
  },

  /**
   * Durée restante en mois — posée si `dateExpirationTitre` est une string
   * non vide parsable en date valide. Retourne `Math.max(0, ...)`.
   */
  computeDureeRestanteMois(input: PrefillCountInput, now: Date = new Date()): number | null {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const dateStr = ai.dateExpirationTitre;
    if (typeof dateStr !== 'string' || dateStr.trim() === '') return null;
    const expiration = new Date(dateStr);
    if (Number.isNaN(expiration.getTime())) return null;
    const diffMs = expiration.getTime() - now.getTime();
    return Math.max(0, Math.floor(diffMs / MS_PER_AVG_MONTH));
  },

  /**
   * SF-246-19 : titre de séjour envisagé depuis `aiData.changementTitreEnvisage`.
   */
  computeTitreEnvisage(input: PrefillCountInput): TitreSejourCode | null {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    return mapTitreSejourFromIa(ai.changementTitreEnvisage ?? null);
  },

  /**
   * SF-246-19 : rémunération brute annuelle depuis `aiData.changementRemunerationEur`
   * (entier > 0, ≤ 500 000).
   */
  computeRemuneration(input: PrefillCountInput): number | null {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.changementRemunerationEur;
    if (typeof v !== 'number' || !Number.isInteger(v) || v <= 0 || v > 500_000) return null;
    return v;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return 0;
    let n = 0;
    if (this.computeTitreActuel(input) !== null) n++;
    if (this.computeDureeRestanteMois(input) !== null) n++;
    if (this.computeTitreEnvisage(input) !== null) n++;
    if (this.computeRemuneration(input) !== null) n++;
    return n;
  },
} as const;
