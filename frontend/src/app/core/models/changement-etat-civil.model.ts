/**
 * SF-FA-26-02 : modèles TypeScript pour l'outil décisionnel
 * "Changement d'état civil" (F-FA-26, FRANCE uniquement, art. 60 / 61-1 et s. /
 * 61-5 et s. Cciv ; loi 2016-1547, loi 2022-301).
 *
 * Contrat importé du backend SF-FA-26-01 (parallèle).
 */

export type TypeChangement =
  | 'NOM'
  | 'PRENOM'
  | 'SEXE'
  | 'NOM_ET_PRENOM';

export type MotifInvoque =
  | 'INTERET_LEGITIME'
  | 'MARIAGE'
  | 'RECTIFICATION_ERREUR'
  | 'IDENTIFICATION_GENRE'
  | 'AUTRE';

export type PreuveEtatCivil =
  | 'JUSTIFICATIF_USAGE_30ANS'
  | 'LIVRET_FAMILLE'
  | 'CERTIFICAT_NAISSANCE'
  | 'ACTES_CIVILS'
  | 'TEMOIGNAGES'
  | 'EXPERTISE_MEDICALE'
  | 'AUTRE';

export type CompetenceProcedure = 'MAIRIE' | 'JUGE' | 'TRIBUNAL_JUDICIAIRE';

export type VerdictEtatCivil = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface ChangementEtatCivilRequest {
  typeChangement: TypeChangement;
  motifInvoque: MotifInvoque;
  preuvesProduites: PreuveEtatCivil[];
  majeurDemandeur: boolean;
  consentementParental: boolean;
  datesDocsConcordants: boolean;
  dejaChangeAuparavant: boolean;
  /** ISO YYYY-MM-DD */
  dateNaissanceDemandeur: string;
  departementDeclaration: string;
}

export interface ChangementEtatCivilResponse {
  caseFileId: string;
  // Inputs persistés (echoed)
  typeChangement: TypeChangement;
  motifInvoque: MotifInvoque;
  preuvesProduites: PreuveEtatCivil[];
  majeurDemandeur: boolean;
  consentementParental: boolean;
  datesDocsConcordants: boolean;
  dejaChangeAuparavant: boolean;
  dateNaissanceDemandeur: string;
  departementDeclaration: string;
  // Sortie décisionnelle
  competenceProcedure: CompetenceProcedure;
  delaiInstructionMoisPrevisionnel: number;
  scoreAcceptabilite: number;
  verdictAcceptabilite: VerdictEtatCivil;
  documentsRequisManquants: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface TypeChangementOption {
  code: TypeChangement;
  label: string;
}

export const TYPES_CHANGEMENT_FR: TypeChangementOption[] = [
  { code: 'NOM', label: 'Changement de nom' },
  { code: 'PRENOM', label: 'Changement de prénom' },
  { code: 'SEXE', label: 'Changement de sexe à l\'état civil' },
  { code: 'NOM_ET_PRENOM', label: 'Changement de nom et de prénom' },
];

export interface MotifInvoqueOption {
  code: MotifInvoque;
  label: string;
}

export const MOTIFS_INVOQUES_FR: MotifInvoqueOption[] = [
  { code: 'INTERET_LEGITIME', label: 'Intérêt légitime' },
  { code: 'MARIAGE', label: 'Suite à mariage / divorce' },
  { code: 'RECTIFICATION_ERREUR', label: 'Rectification d\'erreur état civil' },
  { code: 'IDENTIFICATION_GENRE', label: 'Identification de genre (art. 61-5 Cciv)' },
  { code: 'AUTRE', label: 'Autre motif' },
];

export interface PreuveEtatCivilOption {
  code: PreuveEtatCivil;
  label: string;
}

export const PREUVES_ETAT_CIVIL_FR: PreuveEtatCivilOption[] = [
  { code: 'JUSTIFICATIF_USAGE_30ANS', label: 'Justificatif d\'usage prolongé (30 ans)' },
  { code: 'LIVRET_FAMILLE', label: 'Livret de famille' },
  { code: 'CERTIFICAT_NAISSANCE', label: 'Certificat / acte de naissance' },
  { code: 'ACTES_CIVILS', label: 'Actes civils (mariage, jugement)' },
  { code: 'TEMOIGNAGES', label: 'Témoignages écrits' },
  { code: 'EXPERTISE_MEDICALE', label: 'Expertise médicale (art. 61-5)' },
  { code: 'AUTRE', label: 'Autre justificatif' },
];

export function typeChangementLabel(code: TypeChangement): string {
  return TYPES_CHANGEMENT_FR.find((t) => t.code === code)?.label ?? code;
}

export function motifInvoqueLabel(code: MotifInvoque): string {
  return MOTIFS_INVOQUES_FR.find((m) => m.code === code)?.label ?? code;
}

export function preuveEtatCivilLabel(code: PreuveEtatCivil): string {
  return PREUVES_ETAT_CIVIL_FR.find((p) => p.code === code)?.label ?? code;
}

export function competenceProcedureLabel(c: CompetenceProcedure): string {
  switch (c) {
    case 'MAIRIE': return 'Mairie';
    case 'JUGE': return 'Juge des contentieux de la protection';
    case 'TRIBUNAL_JUDICIAIRE': return 'Tribunal judiciaire';
  }
}

export function verdictEtatCivilLabel(v: VerdictEtatCivil): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}
