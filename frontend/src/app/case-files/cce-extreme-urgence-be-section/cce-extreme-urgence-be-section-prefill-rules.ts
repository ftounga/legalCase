import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CceExtremeUrgenceTypeActe } from '../../core/models/cce-extreme-urgence-be.model';

/**
 * SF-215-16 — Helper partagé pour {@link CceExtremeUrgenceBeSectionComponent}
 * (F-IM-32-cce-extreme-urgence-5j-be) — BE mono-pays.
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (droit des étrangers belge),
 * PAS la Centrale des Crédits.
 *
 * Champs pré-fill RÉELS (depuis {@link ImmigrationExtractedData}, miroir backend) :
 *  - dateActeExecutoire : `aiData.recoursExtremeUrgenceDateActe` (ISO yyyy-MM-dd)
 *  - typeActe           : `aiData.recoursExtremeUrgenceTypeActe`
 *      (whitelist OQT_EXECUTE / TRANSFERT_DUBLIN / REFUS_ACCES_TERRITOIRE /
 *       EXPULSION_IMMEDIATE / AUTRE)
 *
 * Total : 2 champs pré-remplissables. Les 2 champs `recoursForme` (checkbox) et
 * `dateRecours` (date conditionnelle) sont ASPIRATIONNELS — non extraits par l'IA,
 * saisie avocat — et ne comptent JAMAIS dans le prefill count.
 *
 * Gate BELGIQUE : `workspaceCountry === 'BELGIQUE'` (strict). Sur workspace FR
 * tout retourne null (le composant affiche une bannière info dans ce cas).
 */

export const CCE_EXTREME_URGENCE_TYPE_ACTE_SET: ReadonlySet<CceExtremeUrgenceTypeActe> =
  new Set<CceExtremeUrgenceTypeActe>([
    'OQT_EXECUTE',
    'TRANSFERT_DUBLIN',
    'REFUS_ACCES_TERRITOIRE',
    'EXPULSION_IMMEDIATE',
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

export const CceExtremeUrgenceBePrefillRules = {
  CCE_EXTREME_URGENCE_TYPE_ACTE_SET,

  computeDateActeExecutoire(input: PrefillCountInput): string | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    return normalizeIsoDate(ai.recoursExtremeUrgenceDateActe);
  },

  computeTypeActe(input: PrefillCountInput): CceExtremeUrgenceTypeActe | null {
    if (!isBelgique(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const v = ai.recoursExtremeUrgenceTypeActe;
    if (typeof v !== 'string') return null;
    const upper = v.trim().toUpperCase();
    if (!CCE_EXTREME_URGENCE_TYPE_ACTE_SET.has(upper as CceExtremeUrgenceTypeActe)) return null;
    return upper as CceExtremeUrgenceTypeActe;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isBelgique(input)) return 0;
    let n = 0;
    if (this.computeDateActeExecutoire(input) !== null) n++;
    if (this.computeTypeActe(input) !== null) n++;
    return n;
  },
} as const;
