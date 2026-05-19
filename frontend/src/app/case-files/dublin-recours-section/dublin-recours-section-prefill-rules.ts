import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MotifTransfertDublin } from '../../core/models/dublin-recours.model';

/**
 * SF-208-06 / SF-246-17 — Helper partage pour {@link DublinRecoursSectionComponent}
 * (F-IM-22-dublin-recours-fr) — FR mono-pays.
 *
 * Champs pre-fill :
 *  - dateNotificationDecisionTransfert : `aiData.dateNotificationDecisionContestee`
 *    (source generique des decisions immigration contestees) ou
 *    `aiData.dateNotificationOqtf` (fallback secondaire). ISO non future requis.
 *  - etatMembreResponsable : `aiData.dublinEtatMembreResponsable` (texte libre, SF-246-17).
 *  - motifTransfert : `aiData.dublinMotifTransfert` (code enum normalise, SF-246-17).
 *
 * Total : 3 champs pre-remplissables (sur 5 saisissables — recoursForme + dateRecours
 * restent saisis par l'avocat car ce sont des actes a venir).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export const MOTIFS_TRANSFERT_DUBLIN_SET: ReadonlySet<MotifTransfertDublin> =
  new Set<MotifTransfertDublin>([
    'DEMANDE_ASILE_AUTRE_ETAT',
    'VISA_DELIVRE_AUTRE_ETAT',
    'ENTREE_IRREGULIERE_AUTRE_ETAT',
    'MEMBRE_FAMILLE_AUTRE_ETAT',
    'AUTRE',
  ]);

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const DublinRecoursPrefillRules = {
  ISO_DATE_RE,
  MOTIFS_TRANSFERT_DUBLIN_SET,

  computeDateNotificationDecisionTransfert(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const candidate =
      (typeof ai.dateNotificationDecisionContestee === 'string'
        ? ai.dateNotificationDecisionContestee
        : null) ??
      (typeof ai.dateNotificationOqtf === 'string' ? ai.dateNotificationOqtf : null);
    if (!candidate || !ISO_DATE_RE.test(candidate)) return null;
    if (candidate > todayIso()) return null;
    return candidate;
  },

  /** SF-246-17 : État membre UE responsable — texte libre non vide (≤ 200 car.). */
  computeEtatMembreResponsable(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dublinEtatMembreResponsable;
    if (typeof v !== 'string') return null;
    const trimmed = v.trim();
    return trimmed.length > 0 ? trimmed : null;
  },

  /** SF-246-17 : motif de transfert Dublin — code enum normalisé (whitelist 5 codes). */
  computeMotifTransfert(input: PrefillCountInput): MotifTransfertDublin | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.dublinMotifTransfert;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase() as MotifTransfertDublin;
    return MOTIFS_TRANSFERT_DUBLIN_SET.has(upper) ? upper : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationDecisionTransfert(input) !== null) n++;
    // SF-246-17 : 2 nouveaux champs
    if (this.computeEtatMembreResponsable(input) !== null) n++;
    if (this.computeMotifTransfert(input) !== null) n++;
    return n;
  },
} as const;
