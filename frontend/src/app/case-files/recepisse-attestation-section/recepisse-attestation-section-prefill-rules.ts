import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { TypeDocumentSejour } from '../../core/models/recepisse-attestation.model';

/**
 * SF-214-16 — Helper partagé pour {@link RecepisseAttestationSectionComponent}
 * (F-IM-32-recepisse-attestation-fr) — FR mono-pays.
 *
 * Champs pre-fill (depuis {@link ImmigrationExtractedData}) :
 *  - dateExpiration : `aiData.dateExpirationTitre` (ISO yyyy-MM-dd).
 *  - typeDocument : `aiData.recepisseOuAttestationType`
 *    (RECEPISSE | ATTESTATION_PROLONGATION | INCONNU).
 *
 * Champs NON pré-remplis :
 *  - dateDelivrance : pas de champ IA dédié, saisi par l'avocat depuis le document.
 *
 * Total : 2 champs pre-remplissables (sur 3 saisissables).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const VALID_TYPES: ReadonlyArray<TypeDocumentSejour> = [
  'RECEPISSE',
  'ATTESTATION_PROLONGATION',
  'INCONNU',
];

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const RecepisseAttestationPrefillRules = {
  ISO_DATE_RE,
  VALID_TYPES,

  computeDateExpiration(input: PrefillCountInput): string | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const date = (ai as { dateExpirationTitre?: unknown }).dateExpirationTitre;
    if (typeof date !== 'string' || !ISO_DATE_RE.test(date)) return null;
    return date;
  },

  computeTypeDocument(input: PrefillCountInput): TypeDocumentSejour | null {
    if (!isFrance(input)) return null;
    const ai = input.aiData;
    if (!ai) return null;
    const type = (ai as { recepisseOuAttestationType?: unknown }).recepisseOuAttestationType;
    if (typeof type !== 'string') return null;
    return VALID_TYPES.includes(type as TypeDocumentSejour)
      ? (type as TypeDocumentSejour)
      : null;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateExpiration(input) !== null) n++;
    if (this.computeTypeDocument(input) !== null) n++;
    return n;
  },
} as const;
