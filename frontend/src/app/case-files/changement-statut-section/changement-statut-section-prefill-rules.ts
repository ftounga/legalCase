import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  TitreSejourCode,
  mapTitreSejourFromIa,
} from '../../core/models/changement-statut.model';

/**
 * F-236 SF-236-02 — Helper partagé pour le pré-remplissage IA de
 * `ChangementStatutSectionComponent` (F-IM-11-changement-statut).
 *
 * 2 champs pré-remplis :
 *   1. `titreActuel` (TitreSejourCode) — `mapTitreSejourFromIa` cascade :
 *      d'abord depuis `aiData.typeTitreSejourCode`, sinon depuis
 *      `aiData.typeTitreSejour` (texte libre).
 *   2. `dureeRestanteSurTitreActuelMois` (number) — calculé depuis
 *      `aiData.dateExpirationTitre` (ISO string parsable) : floor((expiration
 *      - aujourd'hui) / 30.44 jours), plancher à 0. Posé même si la date
 *      est passée (champ=0).
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

  computePrefillCount(input: PrefillCountInput): number {
    if ((input.workspaceCountry ?? 'FRANCE') !== 'FRANCE') return 0;
    let n = 0;
    if (this.computeTitreActuel(input) !== null) n++;
    if (this.computeDureeRestanteMois(input) !== null) n++;
    return n;
  },
} as const;
