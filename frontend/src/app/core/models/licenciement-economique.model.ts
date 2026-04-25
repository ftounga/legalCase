/**
 * SF-DT-13-02 : modèle frontend pour l'outil décisionnel F-DT-13
 * "Licenciement économique" (FR uniquement, art. L.1233-3/4/5/45 Code du travail).
 * Contrat importé du backend SF-DT-13-01 (PR #585).
 */

export type MotifEconomique =
  | 'DIFFICULTES_ECONOMIQUES'
  | 'MUTATIONS_TECHNOLOGIQUES'
  | 'REORGANISATION_COMPETITIVITE'
  | 'CESSATION_ACTIVITE'
  | 'AUTRE';

export type PreuveMotif =
  | 'BAISSE_CHIFFRE_AFFAIRES'
  | 'PERTES_EXPLOITATION'
  | 'BAISSE_COMMANDES'
  | 'BAISSE_TRESORERIE'
  | 'BAISSE_ENE'
  | 'MUTATION_TECHNOLOGIQUE_PROUVEE'
  | 'RAPPORT_EXPERT'
  | 'AUTRE';

export type CritereOrdre =
  | 'AGE'
  | 'ANCIENNETE'
  | 'CHARGES_FAMILLE'
  | 'QUALITES_PROFESSIONNELLES'
  | 'SITUATION_HANDICAP';

export type QualitesProf = 'EXCELLENT' | 'BON' | 'MOYEN' | 'INSUFFISANT';

export type TentativeReclassement =
  | 'FORMATION_INTERNE'
  | 'MUTATION_GROUPE'
  | 'OFFRE_POSTE_GROUPE'
  | 'OFFRE_POSTE_EXTERIEUR'
  | 'AUCUNE';

export type VerdictRisqueRequalification = 'FAIBLE' | 'MOYENNE' | 'ELEVEE';

export interface LicenciementEconomiqueRequest {
  motifEconomiqueInvoque: MotifEconomique;
  preuvesMotif: PreuveMotif[];
  criteresOrdreAppliques: CritereOrdre[];
  salarieAge: number | null;
  salarieAncienneteMois: number | null;
  salarieChargesFamille: number | null;
  salarieQualitesProf: QualitesProf | null;
  tentativesReclassement: TentativeReclassement[];
  prioriteReembauchePropose: boolean;
  congeReclassementPropose: boolean;
  dateNotification: string; // YYYY-MM-DD
}

export interface LicenciementEconomiqueResponse {
  caseFileId: string;
  motifEconomiqueInvoque: MotifEconomique;
  preuvesMotif: PreuveMotif[];
  criteresOrdreAppliques: CritereOrdre[];
  salarieAge: number;
  salarieAncienneteMois: number;
  salarieChargesFamille: number;
  salarieQualitesProf: QualitesProf | null;
  tentativesReclassement: TentativeReclassement[];
  prioriteReembauchePropose: boolean;
  congeReclassementPropose: boolean;
  dateNotification: string;
  scoreCausalite: number;
  scoreCriteresOrdre: number;
  scoreReclassement: number;
  scoreGlobal: number;
  verdictRisqueRequalification: VerdictRisqueRequalification;
  criteresOrdreManquants: CritereOrdre[];
  criteresOrdreObligatoiresOk: boolean;
  obligationReclassementRespectee: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface MotifEconomiqueOption {
  code: MotifEconomique;
  label: string;
}

export interface PreuveMotifOption {
  code: PreuveMotif;
  label: string;
}

export interface CritereOrdreOption {
  code: CritereOrdre;
  label: string;
  obligatoire: boolean;
}

export interface QualitesProfOption {
  code: QualitesProf;
  label: string;
}

export interface TentativeReclassementOption {
  code: TentativeReclassement;
  label: string;
}

export const MOTIFS_ECONOMIQUES: MotifEconomiqueOption[] = [
  { code: 'DIFFICULTES_ECONOMIQUES', label: 'Difficultés économiques' },
  { code: 'MUTATIONS_TECHNOLOGIQUES', label: 'Mutations technologiques' },
  { code: 'REORGANISATION_COMPETITIVITE', label: 'Réorganisation pour sauvegarder la compétitivité' },
  { code: 'CESSATION_ACTIVITE', label: "Cessation d'activité" },
  { code: 'AUTRE', label: 'Autre motif' },
];

export const PREUVES_MOTIF: PreuveMotifOption[] = [
  { code: 'BAISSE_CHIFFRE_AFFAIRES', label: "Baisse du chiffre d'affaires" },
  { code: 'PERTES_EXPLOITATION', label: "Pertes d'exploitation" },
  { code: 'BAISSE_COMMANDES', label: 'Baisse des commandes' },
  { code: 'BAISSE_TRESORERIE', label: 'Baisse de trésorerie' },
  { code: 'BAISSE_ENE', label: "Baisse de l'EBE / excédent brut d'exploitation" },
  { code: 'MUTATION_TECHNOLOGIQUE_PROUVEE', label: 'Mutation technologique prouvée' },
  { code: 'RAPPORT_EXPERT', label: "Rapport d'expert" },
  { code: 'AUTRE', label: 'Autre preuve' },
];

export const CRITERES_ORDRE: CritereOrdreOption[] = [
  { code: 'AGE', label: 'Âge', obligatoire: true },
  { code: 'ANCIENNETE', label: 'Ancienneté', obligatoire: true },
  { code: 'CHARGES_FAMILLE', label: 'Charges de famille', obligatoire: true },
  { code: 'QUALITES_PROFESSIONNELLES', label: 'Qualités professionnelles', obligatoire: true },
  { code: 'SITUATION_HANDICAP', label: 'Situation de handicap', obligatoire: false },
];

export const QUALITES_PROF: QualitesProfOption[] = [
  { code: 'EXCELLENT', label: 'Excellent' },
  { code: 'BON', label: 'Bon' },
  { code: 'MOYEN', label: 'Moyen' },
  { code: 'INSUFFISANT', label: 'Insuffisant' },
];

export const TENTATIVES_RECLASSEMENT: TentativeReclassementOption[] = [
  { code: 'FORMATION_INTERNE', label: 'Formation interne' },
  { code: 'MUTATION_GROUPE', label: 'Mutation au sein du groupe' },
  { code: 'OFFRE_POSTE_GROUPE', label: "Offre d'un poste dans le groupe" },
  { code: 'OFFRE_POSTE_EXTERIEUR', label: "Offre d'un poste extérieur (bassin d'emploi)" },
  { code: 'AUCUNE', label: 'Aucune tentative' },
];

export function motifEconomiqueLabel(code: MotifEconomique): string {
  return MOTIFS_ECONOMIQUES.find((m) => m.code === code)?.label ?? code;
}

export function preuveMotifLabel(code: PreuveMotif): string {
  return PREUVES_MOTIF.find((p) => p.code === code)?.label ?? code;
}

export function critereOrdreLabel(code: CritereOrdre): string {
  return CRITERES_ORDRE.find((c) => c.code === code)?.label ?? code;
}

export function tentativeReclassementLabel(code: TentativeReclassement): string {
  return TENTATIVES_RECLASSEMENT.find((t) => t.code === code)?.label ?? code;
}

export function verdictRisqueLabel(v: VerdictRisqueRequalification): string {
  switch (v) {
    case 'FAIBLE':
      return 'Risque faible de requalification';
    case 'MOYENNE':
      return 'Risque moyen de requalification';
    case 'ELEVEE':
      return 'Risque élevé de requalification';
  }
}

/**
 * SF-DT-13-02 : mapping IA `motifLicenciement` (chaîne libre) → enum UI
 * `MotifEconomique`. Les valeurs IA non mappables sont ignorées (no-op
 * gracieux). Recouvre les détections les plus fréquentes pour l'analyse
 * `TravailExtractedData.motifLicenciement` du pipeline IA.
 */
export const AI_MOTIF_TO_MOTIF_ECONOMIQUE: Readonly<Record<string, MotifEconomique>> = {
  LICENCIEMENT_ECONOMIQUE: 'DIFFICULTES_ECONOMIQUES',
  ECONOMIQUE: 'DIFFICULTES_ECONOMIQUES',
  DIFFICULTES_ECONOMIQUES: 'DIFFICULTES_ECONOMIQUES',
  MUTATIONS_TECHNOLOGIQUES: 'MUTATIONS_TECHNOLOGIQUES',
  REORGANISATION: 'REORGANISATION_COMPETITIVITE',
  REORGANISATION_COMPETITIVITE: 'REORGANISATION_COMPETITIVITE',
  CESSATION_ACTIVITE: 'CESSATION_ACTIVITE',
  CESSATION: 'CESSATION_ACTIVITE',
};
