import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * F-236 SF-236-03 — Helper partagé pour le pré-remplissage IA de
 * `DivorceChecklistSectionComponent` (F-FA-07-checklist-divorce).
 *
 * Référence canonique du contrat (cf. `docs/features/F-236/contract-prefill-rules.md`).
 *
 * <p>Le seul signal IA exploitable au pré-fill est `aiData.dateAcceptationPV`
 * (date de signature du PV/convention) — quand renseignée et valide
 * (format ISO `YYYY-MM-DD`), le runtime pré-coche l'étape "signature/rédaction
 * de la convention" sur les deux pays (FR et BE). Le `SIGNATURE_STEP_CODES`
 * couvre les 2 codes — la checklist du dossier ne contient en pratique que
 * les étapes du pays courant, mais le compteur reflète **l'expérience UX
 * réelle pour l'avocat** (2 cases pré-cochées si les 2 pays étaient mélangés
 * — corrigé en SF-236-03 — divergence remontée par audit SF-236-01).</p>
 *
 * <p>Champs pré-remplis :
 * <ul>
 *   <li>Étape `FR_SIGNATURE_CONVENTION` — quand `dateAcceptationPV` ISO valide.</li>
 *   <li>Étape `BE_REDACTION_CONVENTION` — quand `dateAcceptationPV` ISO valide.</li>
 * </ul></p>
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-155-19 : codes étape "signature de la convention" (FR + BE).
 * Si l'IA a fourni `dateAcceptationPV`, ces 2 étapes sont marquées FAIT.
 */
export const SIGNATURE_STEP_CODES = ['FR_SIGNATURE_CONVENTION', 'BE_REDACTION_CONVENTION'] as const;

export const DivorceChecklistPrefillRules = {
  ISO_DATE_RE,
  SIGNATURE_STEP_CODES,

  /**
   * Renvoie la date ISO si valide ; `null` sinon. C'est la fonction "valeur"
   * unique du helper — un seul input IA, deux étapes pré-cochées.
   */
  computeDateAcceptationPV(input: PrefillCountInput): string | null {
    const ai = input.aiData;
    if (!ai) return null;
    if (typeof ai.dateAcceptationPV !== 'string') return null;
    if (!ISO_DATE_RE.test(ai.dateAcceptationPV)) return null;
    return ai.dateAcceptationPV;
  },

  /**
   * Maître : compte les étapes pré-cochées par l'IA. Quand `dateAcceptationPV`
   * est valide, 2 étapes sont pré-cochées (FR_SIGNATURE_CONVENTION +
   * BE_REDACTION_CONVENTION) — chiffre qui reflète l'expérience UX réelle
   * de l'avocat (parité avec `prefillFromAi()` runtime — SF-236-03).
   */
  computePrefillCount(input: PrefillCountInput): number {
    if (this.computeDateAcceptationPV(input) === null) return 0;
    return SIGNATURE_STEP_CODES.length;
  },
} as const;
