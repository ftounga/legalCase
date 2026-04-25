/**
 * SF-DT-25-02 : modèle frontend de l'outil F-DT-25 — Indemnité compensatrice
 * de préavis (FR uniquement, art. L.1234-1 et s. Code du travail).
 * Contrat figé dans SF-DT-25-01 backend.
 */

export type FonctionPreavis =
  | 'OUVRIER'
  | 'EMPLOYE'
  | 'AGENT_MAITRISE'
  | 'CADRE';

export type SourceDureePreavis = 'LEGALE' | 'CCN' | 'USAGE';

export interface IndemnitePreavisRequest {
  ancienneteAnnees: number;
  ancienneteMois: number;
  salaireMensuelBrutEur: number;
  conventionCollectiveCode?: string | null;
  fonction: FonctionPreavis;
  exemptionEmployeur: boolean;
  /** Date de rupture au format ISO YYYY-MM-DD. */
  dateRupture: string;
}

export interface IndemnitePreavisResponse {
  caseFileId: string;
  ancienneteAnnees: number;
  ancienneteMois: number;
  salaireMensuelBrutEur: number;
  conventionCollectiveCode?: string | null;
  fonction: FonctionPreavis;
  exemptionEmployeur: boolean;
  dateRupture: string;
  dureePreavisMois: number;
  sourceDuree: SourceDureePreavis;
  montantIndemniteEur: number;
  exemptionRetenue: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface FonctionOption {
  code: FonctionPreavis;
  label: string;
}

export const FONCTIONS_PREAVIS: FonctionOption[] = [
  { code: 'OUVRIER', label: 'Ouvrier' },
  { code: 'EMPLOYE', label: 'Employé' },
  { code: 'AGENT_MAITRISE', label: 'Agent de maîtrise' },
  { code: 'CADRE', label: 'Cadre' },
];

export interface SourceDureeOption {
  code: SourceDureePreavis;
  label: string;
}

export const SOURCES_DUREE: SourceDureeOption[] = [
  { code: 'LEGALE', label: 'Légale (L.1234-1)' },
  { code: 'CCN', label: 'Convention collective' },
  { code: 'USAGE', label: 'Usage' },
];
