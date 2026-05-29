import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CceAnnulationTypeDecision } from '../../core/models/cce-annulation-be.model';

/**
 * SF-215-14 — Helper partagé pour {@link CceAnnulationBeSectionComponent}
 * (F-IM-31-cce-annulation-30j-be) — BE mono-pays.
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (droit des étrangers belge),
 * PAS la Centrale des Crédits.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateNotificationDecision : `aiData.recoursCceDateNotification` (ISO yyyy-MM-dd)
 *  - typeDecision             : `aiData.recoursCceTypeDecision`
 *      (whitelist REFUS_TITRE / REFUS_REGROUPEMENT / REFUS_9BIS / REFUS_9TER /
 *       OQT_ANNEXE13 / DECISION_CGRA / AUTRE)
 *
 * Total : 2 champs pré-remplissables. Les 2 champs `recoursForme` (checkbox) et
 * `dateRecours` (date conditionnelle) sont ASPIRATIONNELS — non extraits par l'IA,
 * saisie avocat — et ne comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null (le composant affiche une bannière info dans ce cas).
 */

export const CCE_ANNULATION_TYPE_DECISION_SET: ReadonlySet<CceAnnulationTypeDecision> =
  new Set<CceAnnulationTypeDecision>([
    'REFUS_TITRE',
    'REFUS_REGROUPEMENT',
    'REFUS_9BIS',
    'REFUS_9TER',
    'OQT_ANNEXE13',
    'DECISION_CGRA',
    'AUTRE',
  ]);

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isBelgique(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'BELGIQUE';
}

function normalizeIsoDate(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const trimmed = v.trim();
  if (!ISO_DATE_RE.test(trimmed)) return null;
  // Validité calendaire stricte (ex. rejette 2026-02-30).
  const d = new Date(`${trimmed}T00:00:00Z`);
  if (isNaN(d.getTime())) return null;
  const iso = d.toISOString().slice(0, 10);
  return iso === trimmed ? trimmed : null;
}

export const CceAnnulationBePrefillRules = {
  CCE_ANNULATION_TYPE_DECISION_SET,

  computeDateNotificationDecision(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.recoursCceDateNotification);
  },

  computeTypeDecision(input: PrefillCountInput): CceAnnulationTypeDecision | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.recoursCceTypeDecision;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!CCE_ANNULATION_TYPE_DECISION_SET.has(upper as CceAnnulationTypeDecision)) return null;
    return upper as CceAnnulationTypeDecision;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateNotificationDecision(input) !== null) n++;
    if (this.computeTypeDecision(input) !== null) n++;
    return n;
  },
} as const;
