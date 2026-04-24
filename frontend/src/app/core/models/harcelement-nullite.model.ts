export type MotifNulliteFr =
  | 'HARCELEMENT_MORAL'
  | 'HARCELEMENT_SEXUEL'
  | 'DISCRIMINATION'
  | 'GROSSESSE'
  | 'SALARIE_PROTEGE'
  | 'LIBERTE_FONDAMENTALE'
  | 'ACTION_JUSTICE'
  | 'ALERTE_ETHIQUE';

export type MotifNulliteBe =
  | 'HARCELEMENT_MORAL_BE'
  | 'HARCELEMENT_SEXUEL_BE'
  | 'VIOLENCE_AU_TRAVAIL_BE'
  | 'DISCRIMINATION_BE';

export type MotifNullite = MotifNulliteFr | MotifNulliteBe;

export interface HarcelementNulliteRequest {
  salaireMensuelReference: number;
  motifNullite: MotifNullite;
}

export interface HarcelementNulliteResponse {
  caseFileId: string;
  salaireMensuelReference: number;
  motifNullite: MotifNullite;
  country: 'FRANCE' | 'BELGIQUE';
  indemniteMinimumNullite: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

export interface MotifOption {
  code: MotifNullite;
  label: string;
}

export const MOTIFS_FR: MotifOption[] = [
  { code: 'HARCELEMENT_MORAL', label: 'Harcèlement moral' },
  { code: 'HARCELEMENT_SEXUEL', label: 'Harcèlement sexuel' },
  { code: 'DISCRIMINATION', label: 'Discrimination' },
  { code: 'GROSSESSE', label: 'Grossesse / maternité' },
  { code: 'SALARIE_PROTEGE', label: 'Salarié protégé' },
  { code: 'LIBERTE_FONDAMENTALE', label: 'Atteinte à une liberté fondamentale' },
  { code: 'ACTION_JUSTICE', label: 'Action en justice du salarié' },
  { code: 'ALERTE_ETHIQUE', label: 'Lancement d\'alerte éthique' },
];

export const MOTIFS_BE: MotifOption[] = [
  { code: 'HARCELEMENT_MORAL_BE', label: 'Harcèlement moral (BE)' },
  { code: 'HARCELEMENT_SEXUEL_BE', label: 'Harcèlement sexuel (BE)' },
  { code: 'VIOLENCE_AU_TRAVAIL_BE', label: 'Violence au travail (BE)' },
  { code: 'DISCRIMINATION_BE', label: 'Discrimination (BE)' },
];
