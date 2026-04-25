/**
 * SF-IM-11-02 : types frontend pour l'outil décisionnel "Changement de statut"
 * (F-IM-11). FR uniquement (régime CESEDA — art. L.421+ + R.5221).
 *
 * Contrat figé dans SF-IM-11-01 (backend, mergé PR #635).
 */

/**
 * Code des titres de séjour CESEDA admis par l'outil. 7 valeurs alignées
 * sur le switch interne du calculateur backend.
 */
export type TitreSejourCode =
  | 'ETUDIANT'
  | 'VISITEUR'
  | 'VPF'
  | 'SALARIE'
  | 'PASSEPORT_TALENT_SALARIE_QUALIFIE'
  | 'PASSEPORT_TALENT_INNOVANT'
  | 'APS';

/** Verdict de viabilité de la transition (scoring backend). */
export type VerdictTransition = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio titre de séjour. */
export const TITRE_SEJOUR_LABELS:
  ReadonlyArray<{ code: TitreSejourCode; label: string }> = [
  { code: 'ETUDIANT', label: 'Étudiant (L.422-1)' },
  { code: 'VISITEUR', label: 'Visiteur (L.426-20)' },
  { code: 'VPF', label: 'Vie privée et familiale (L.423-1)' },
  { code: 'SALARIE', label: 'Salarié (L.421-1)' },
  { code: 'PASSEPORT_TALENT_SALARIE_QUALIFIE', label: 'Passeport talent — salarié qualifié (L.421-9)' },
  { code: 'PASSEPORT_TALENT_INNOVANT', label: 'Passeport talent — projet innovant (L.421-19)' },
  { code: 'APS', label: 'APS — recherche d\'emploi (L.422-10)' },
];

export interface ChangementStatutRequest {
  titreActuel: TitreSejourCode;
  titreEnvisage: TitreSejourCode;
  dureeRestanteSurTitreActuelMois: number;
  documentJustificatifFourni: boolean;
  /** Requis uniquement si `titreEnvisage = SALARIE`. */
  remunerationContratEur?: number | null;
  casierJudiciaireVierge: boolean;
}

export interface ChangementStatutResponse {
  caseFileId: string;
  country: 'FRANCE';
  titreActuel: string;
  titreEnvisage: string;
  nouveauTitreEnvisage: string;
  dureeRestanteMois: number;
  documentJustificatifFourni: boolean;
  remunerationContratEur: number | null;
  casierJudiciaireVierge: boolean;
  verdictTransition: VerdictTransition;
  documentsRequis: string[];
  risqueRefus: string[];
  /** 2-4 mois selon la transition (préfecture). */
  delaiInstructionMois: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
}

/**
 * Mappe une chaîne IA libre (ex. `aiData.typeTitreSejourCode`) vers l'enum
 * frontend. Normalisation insensible à la casse, accents et tirets/underscores.
 * Retourne `null` si non reconnu (no-op gracieux pour le pré-fill).
 *
 * Mapping pragmatique :
 * - VLS_TS_ETUDIANT, ETUDIANT → ETUDIANT
 * - VLS_TS_VISITEUR, VISITEUR → VISITEUR
 * - CST_VPF, VPF, VIE_PRIVEE_FAMILIALE → VPF
 * - VLS_TS_SALARIE, SALARIE, PERMIS_UNIQUE → SALARIE
 * - PASSEPORT_TALENT_SALARIE_QUALIFIE, TALENT_SALARIE → PASSEPORT_TALENT_SALARIE_QUALIFIE
 * - PASSEPORT_TALENT_INNOVANT, TALENT_INNOVANT → PASSEPORT_TALENT_INNOVANT
 * - APS, RECHERCHE_EMPLOI → APS
 */
export function mapTitreSejourFromIa(value: string | null | undefined): TitreSejourCode | null {
  if (!value) return null;
  const normalized = value.toString().trim().toUpperCase()
    .replace(/[\s\-]+/g, '_')
    .replace(/[ÉÈÊË]/g, 'E')
    .replace(/[ÀÂÄ]/g, 'A')
    .replace(/[ÎÏ]/g, 'I')
    .replace(/[ÔÖ]/g, 'O')
    .replace(/[ÙÛÜ]/g, 'U');

  if (normalized === 'ETUDIANT' || normalized === 'VLS_TS_ETUDIANT'
      || normalized === 'CARTE_ETUDIANT' || normalized === 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE') {
    return 'ETUDIANT';
  }
  if (normalized === 'VISITEUR' || normalized === 'VLS_TS_VISITEUR') {
    return 'VISITEUR';
  }
  if (normalized === 'VPF' || normalized === 'CST_VPF' || normalized === 'CST_VPF_CONJOINT_FR'
      || normalized === 'VIE_PRIVEE_FAMILIALE' || normalized === 'CARTE_VPF') {
    return 'VPF';
  }
  if (normalized === 'SALARIE' || normalized === 'VLS_TS_SALARIE' || normalized === 'PERMIS_UNIQUE'
      || normalized === 'CARTE_SALARIE') {
    return 'SALARIE';
  }
  if (normalized === 'PASSEPORT_TALENT_SALARIE_QUALIFIE' || normalized === 'TALENT_SALARIE'
      || normalized === 'TALENT_SALARIE_QUALIFIE') {
    return 'PASSEPORT_TALENT_SALARIE_QUALIFIE';
  }
  if (normalized === 'PASSEPORT_TALENT_INNOVANT' || normalized === 'TALENT_INNOVANT'
      || normalized === 'TALENT_PROJET_INNOVANT') {
    return 'PASSEPORT_TALENT_INNOVANT';
  }
  if (normalized === 'APS' || normalized === 'RECHERCHE_EMPLOI') {
    return 'APS';
  }
  return null;
}

/** Libellé humain depuis un code titre. */
export function titreLabel(code: TitreSejourCode | string | null | undefined): string {
  if (!code) return '';
  const opt = TITRE_SEJOUR_LABELS.find((o) => o.code === code);
  return opt?.label ?? String(code);
}
