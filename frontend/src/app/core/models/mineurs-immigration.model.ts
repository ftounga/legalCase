/**
 * SF-IM-19-02 : types frontend pour l'outil décisionnel "Mineurs étrangers
 * — éligibilité" (F-IM-19). FR uniquement (CESEDA + Cciv + CASF).
 *
 * Contrat figé dans SF-IM-19-01 (backend, mergé PR #642).
 */

/** Code des 4 dispositifs supportés (alignés enum backend). */
export type DispositifMineur =
  | 'MNA_ORDONNANCE_JE'
  | 'TITRE_SEJOUR_L435_3'
  | 'DCEM'
  | 'TIR';

/** Verdict d'éligibilité (scoring backend). */
export type VerdictEligibiliteMineur = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio dispositif. */
export const DISPOSITIF_MINEUR_LABELS:
  ReadonlyArray<{ code: DispositifMineur; label: string; sub: string }> = [
  {
    code: 'MNA_ORDONNANCE_JE',
    label: 'MNA — ordonnance juge des enfants',
    sub: 'art. 375 Cciv + L.221-2-2 CASF — mineur étranger isolé',
  },
  {
    code: 'TITRE_SEJOUR_L435_3',
    label: 'Titre de séjour L.435-3',
    sub: 'CESEDA L.435-3 — enfant né en France ≥ 3 ans',
  },
  {
    code: 'DCEM',
    label: 'DCEM — Document de Circulation Étranger Mineur',
    sub: 'CESEDA R.321-3 — retour après voyage sans visa',
  },
  {
    code: 'TIR',
    label: 'TIR — Titre d\'Identité Républicain',
    sub: 'CESEDA R.321-7 — mineur apatride / réfugié',
  },
];

export interface MineursImmigrationRequest {
  dispositifVise: DispositifMineur;
  dateNaissance: string; // YYYY-MM-DD
  dateEntreeFrance?: string | null;
  parentRegulier?: boolean | null;
  isolementAvere?: boolean | null;
  motifOrdrePublic?: boolean | null;
  nationalite?: string | null;
}

export interface MineursImmigrationResponse {
  caseFileId: string;
  country: 'FRANCE';
  dispositifVise: string;
  dispositifRecommande: string;
  dateNaissance: string;
  dateEntreeFrance: string | null;
  parentRegulier: boolean;
  isolementAvere: boolean;
  motifOrdrePublic: boolean;
  nationalite: string | null;
  ageAnnees: number;
  verdictEligibilite: VerdictEligibiliteMineur;
  criteresNonRemplis: string[];
  documentsRequis: string[];
  delaiInstructionMois: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
}

/** Libellé humain depuis un code dispositif. */
export function dispositifLabel(code: DispositifMineur | string | null | undefined): string {
  if (!code) return '';
  const opt = DISPOSITIF_MINEUR_LABELS.find((o) => o.code === code);
  return opt?.label ?? String(code);
}
