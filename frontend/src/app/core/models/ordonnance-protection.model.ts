/**
 * SF-FA-14-02 : modèle frontend pour l'outil décisionnel F-FA-14
 * "Ordonnance de protection" (FR uniquement, art. 515-9 à 515-13 Cciv +
 * Loi 30/07/2020 BAR).
 *
 * Contrat importé du backend SF-FA-14-01 (PR #568).
 */

export type ViolenceCode =
  | 'PHYSIQUES'
  | 'PSYCHOLOGIQUES'
  | 'SEXUELLES'
  | 'ECONOMIQUES'
  | 'MENACES_MORT';

export type PreuveCode =
  | 'CONSTAT_HUISSIER'
  | 'MAIN_COURANTE'
  | 'CERTIFICAT_MEDICAL'
  | 'TEMOIGNAGES'
  | 'PHOTOS'
  | 'PLAINTE_DEPOSEE'
  | 'JUGEMENT_CORRECTIONNEL'
  | 'AUTRE';

export type MesureCode =
  | 'EVICTION_CONJOINT'
  | 'INTERDICTION_APPROCHER'
  | 'TGD'
  | 'BAR'
  | 'INTERDICTION_PARAITRE'
  | 'OBLIGATION_SOIN'
  | 'RESIDENCE_ENFANTS';

export type VerdictProbabiliteOctroi = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface OrdonnanceProtectionRequest {
  dateRequete?: string | null;
  violencesAlleguees: ViolenceCode[];
  preuvesViolences: PreuveCode[];
  dangerImmediat: boolean;
  presenceEnfants: boolean;
  ageEnfants?: number[] | null;
  logementCommun: boolean;
  victimeFinanciairementDependante: boolean;
  demandeurDejaProtege: boolean;
  demandeMesures: MesureCode[];
}

export interface OrdonnanceProtectionResponse {
  caseFileId: string;
  dateRequete: string | null;
  violencesAlleguees: ViolenceCode[];
  preuvesViolences: PreuveCode[];
  dangerImmediat: boolean;
  presenceEnfants: boolean;
  ageEnfants: number[] | null;
  logementCommun: boolean;
  victimeFinanciairementDependante: boolean;
  demandeurDejaProtege: boolean;
  demandeMesures: MesureCode[];
  scoreVraisemblance: number;
  verdictProbabiliteOctroi: VerdictProbabiliteOctroi;
  mesuresRecommandees: MesureCode[];
  delaiTraitementJoursPrevisionnel: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface ViolenceOption {
  code: ViolenceCode;
  label: string;
}

export interface PreuveOption {
  code: PreuveCode;
  label: string;
}

export interface MesureOption {
  code: MesureCode;
  label: string;
}

export const VIOLENCES_FR: ViolenceOption[] = [
  { code: 'PHYSIQUES', label: 'Physiques' },
  { code: 'PSYCHOLOGIQUES', label: 'Psychologiques' },
  { code: 'SEXUELLES', label: 'Sexuelles' },
  { code: 'ECONOMIQUES', label: 'Économiques' },
  { code: 'MENACES_MORT', label: 'Menaces de mort' },
];

export const PREUVES_FR: PreuveOption[] = [
  { code: 'CONSTAT_HUISSIER', label: 'Constat d\'huissier' },
  { code: 'MAIN_COURANTE', label: 'Main courante' },
  { code: 'CERTIFICAT_MEDICAL', label: 'Certificat médical' },
  { code: 'TEMOIGNAGES', label: 'Témoignages' },
  { code: 'PHOTOS', label: 'Photos' },
  { code: 'PLAINTE_DEPOSEE', label: 'Plainte déposée' },
  { code: 'JUGEMENT_CORRECTIONNEL', label: 'Jugement correctionnel' },
  { code: 'AUTRE', label: 'Autre' },
];

export const MESURES_FR: MesureOption[] = [
  { code: 'EVICTION_CONJOINT', label: 'Éviction du conjoint violent' },
  { code: 'INTERDICTION_APPROCHER', label: 'Interdiction d\'approcher' },
  { code: 'TGD', label: 'TGD (Téléphone Grave Danger)' },
  { code: 'BAR', label: 'BAR (Bracelet Anti-Rapprochement)' },
  { code: 'INTERDICTION_PARAITRE', label: 'Interdiction de paraître certains lieux' },
  { code: 'OBLIGATION_SOIN', label: 'Obligation de soin' },
  { code: 'RESIDENCE_ENFANTS', label: 'Fixation de la résidence des enfants' },
];

export function violenceLabel(code: ViolenceCode): string {
  return VIOLENCES_FR.find((v) => v.code === code)?.label ?? code;
}

export function preuveLabel(code: PreuveCode): string {
  return PREUVES_FR.find((p) => p.code === code)?.label ?? code;
}

export function mesureLabel(code: MesureCode): string {
  return MESURES_FR.find((m) => m.code === code)?.label ?? code;
}

export function verdictProbabiliteOctroiLabel(v: VerdictProbabiliteOctroi): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité d\'octroi élevée';
    case 'MOYENNE': return 'Probabilité d\'octroi moyenne';
    case 'FAIBLE': return 'Probabilité d\'octroi faible';
  }
}
