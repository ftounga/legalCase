/**
 * SF-DT-12-02 : modèle TypeScript pour l'outil décisionnel
 * "Discrimination — dommages-intérêts" (F-DT-12). FR + BE.
 * Contrat importé de SF-DT-12-01 (backend) — cf.
 * `backend/src/main/java/fr/ailegalcase/casefile/DiscriminationRequest.java`
 * et `DiscriminationResponse.java`.
 */

/** 8 motifs FR regroupés depuis les 25 motifs prohibés L.1132-1. */
export type MotifDiscriminationFr =
  | 'ORIGINE_ETHNIQUE'
  | 'SEXE_GROSSESSE'
  | 'ORIENTATION_SEXUELLE'
  | 'AGE'
  | 'ETAT_SANTE_HANDICAP'
  | 'OPINIONS_POLITIQUES'
  | 'ORIGINE_SOCIALE'
  | 'SITUATION_FAMILIALE';

/** 5 motifs BE (loi 10/5/2007 + loi 30/7/1981 racisme + loi genre). */
export type MotifDiscriminationBe =
  | 'DISCRIMINATION_RACIALE_BE'
  | 'DISCRIMINATION_GENRE_BE'
  | 'DISCRIMINATION_HANDICAP_BE'
  | 'DISCRIMINATION_AGE_BE'
  | 'DISCRIMINATION_ORIENTATION_BE';

export type MotifDiscrimination = MotifDiscriminationFr | MotifDiscriminationBe;

/** 6 contextes d'acte communs FR/BE. */
export type ContexteActe =
  | 'LICENCIEMENT'
  | 'SANCTION'
  | 'EMBAUCHE_REFUSEE'
  | 'PROMOTION_REFUSEE'
  | 'DIFFERENCE_SALARIALE'
  | 'HARCELEMENT_LIE_DISCRIMINATION';

export interface DiscriminationRequest {
  salaireMensuelReference: number;
  motifDiscrimination: MotifDiscrimination;
  contexteActe: ContexteActe;
}

export interface DiscriminationResponse {
  caseFileId: string;
  salaireMensuelReference: number;
  motifDiscrimination: MotifDiscrimination;
  contexteActe: ContexteActe;
  country: 'FRANCE' | 'BELGIQUE';
  fourchetteMin: number;
  fourchetteMediane: number;
  fourchetteMax: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface MotifDiscriminationOption {
  code: MotifDiscrimination;
  label: string;
}

export interface ContexteActeOption {
  code: ContexteActe;
  label: string;
}

export const MOTIFS_DISCRIMINATION_FR: MotifDiscriminationOption[] = [
  { code: 'ORIGINE_ETHNIQUE', label: 'Origine / ethnie / nationalité' },
  { code: 'SEXE_GROSSESSE', label: 'Sexe / grossesse / maternité / paternité' },
  { code: 'ORIENTATION_SEXUELLE', label: 'Orientation sexuelle / identité de genre' },
  { code: 'AGE', label: 'Âge' },
  { code: 'ETAT_SANTE_HANDICAP', label: 'État de santé / handicap / apparence physique' },
  { code: 'OPINIONS_POLITIQUES', label: 'Opinions politiques / convictions / activités syndicales' },
  { code: 'ORIGINE_SOCIALE', label: 'Origine sociale / patronyme / lieu de résidence' },
  { code: 'SITUATION_FAMILIALE', label: 'Situation familiale / charges de famille' },
];

export const MOTIFS_DISCRIMINATION_BE: MotifDiscriminationOption[] = [
  { code: 'DISCRIMINATION_RACIALE_BE', label: 'Racisme / origine / race (BE)' },
  { code: 'DISCRIMINATION_GENRE_BE', label: 'Genre / grossesse / changement de sexe (BE)' },
  { code: 'DISCRIMINATION_HANDICAP_BE', label: 'Handicap / état de santé (BE)' },
  { code: 'DISCRIMINATION_AGE_BE', label: 'Âge (BE)' },
  { code: 'DISCRIMINATION_ORIENTATION_BE', label: 'Orientation sexuelle (BE)' },
];

export const CONTEXTES_ACTE: ContexteActeOption[] = [
  { code: 'LICENCIEMENT', label: 'Licenciement discriminatoire' },
  { code: 'SANCTION', label: 'Sanction disciplinaire discriminatoire' },
  { code: 'EMBAUCHE_REFUSEE', label: 'Refus d\'embauche discriminatoire' },
  { code: 'PROMOTION_REFUSEE', label: 'Refus de promotion / stagnation' },
  { code: 'DIFFERENCE_SALARIALE', label: 'Écart salarial injustifié' },
  { code: 'HARCELEMENT_LIE_DISCRIMINATION', label: 'Harcèlement motivé par discrimination' },
];
