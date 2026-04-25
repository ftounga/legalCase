/**
 * SF-FA-19-06 : modèles TypeScript pour l'outil décisionnel
 * "Désaccords parentaux art. 373-2-10" (F-FA-19, FRANCE uniquement).
 *
 * Contrat importé du backend SF-FA-19-05.
 */

export type DomaineDesaccord =
  | 'SCOLARITE'
  | 'SANTE'
  | 'RELIGION'
  | 'LOISIRS_SPORTS'
  | 'CHOIX_EDUCATIFS'
  | 'DEMENAGEMENT'
  | 'AUTRE';

export type IntensiteDesaccord = 'MAJEUR' | 'MOYEN' | 'MINEUR';

export type TentativeMediation =
  | 'MEDIATION_FAMILIALE'
  | 'MEDIATION_JUDICIAIRE'
  | 'DISCUSSIONS_DIRECTES'
  | 'THERAPIE_FAMILIALE'
  | 'AUCUNE';

export type DesaccordsParentauxVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface DesaccordsParentauxRequest {
  domaineDesaccord: DomaineDesaccord;
  intensiteDesaccord: IntensiteDesaccord;
  tentativesMediation: TentativeMediation[];
  ageEnfantsConcernes: number[];
  interetSuperieurInvoque: boolean;
  expertiseDejaRealisee: boolean;
  urgence: boolean;
  /** ISO date YYYY-MM-DD. */
  dateRequete: string;
}

export interface DesaccordsParentauxResponse {
  caseFileId: string;
  domaineDesaccord: DomaineDesaccord;
  intensiteDesaccord: IntensiteDesaccord;
  tentativesMediation: TentativeMediation[];
  ageEnfantsConcernes: number[];
  interetSuperieurInvoque: boolean;
  expertiseDejaRealisee: boolean;
  urgence: boolean;
  dateRequete: string;
  scoreEligibiliteJaf: number;
  verdictProbabiliteAcceptation: DesaccordsParentauxVerdict;
  mediationPrealableRecommandee: boolean;
  auditionEnfantsRecommandee: boolean;
  expertisePsyRecommandee: boolean;
  delaiTraitementJoursPrevisionnel: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface DomaineDesaccordOption {
  code: DomaineDesaccord;
  label: string;
}

export const DOMAINES_DESACCORD_FR: DomaineDesaccordOption[] = [
  { code: 'SCOLARITE', label: 'Scolarité (établissement, orientation, redoublement)' },
  { code: 'SANTE', label: 'Santé (vaccins, opérations, soins psy)' },
  { code: 'RELIGION', label: 'Religion / éducation religieuse' },
  { code: 'LOISIRS_SPORTS', label: 'Loisirs / sports / activités extra-scolaires' },
  { code: 'CHOIX_EDUCATIFS', label: 'Choix éducatifs (langues, valeurs, méthodes)' },
  { code: 'DEMENAGEMENT', label: 'Déménagement (changement de résidence)' },
  { code: 'AUTRE', label: 'Autre (à préciser dans les notes du dossier)' },
];

export interface IntensiteDesaccordOption {
  code: IntensiteDesaccord;
  label: string;
}

export const INTENSITES_DESACCORD_FR: IntensiteDesaccordOption[] = [
  { code: 'MAJEUR', label: 'Majeur (impact direct sur l\'intérêt de l\'enfant)' },
  { code: 'MOYEN', label: 'Moyen (point de friction récurrent)' },
  { code: 'MINEUR', label: 'Mineur (divergence ponctuelle)' },
];

export interface TentativeMediationOption {
  code: TentativeMediation;
  label: string;
}

export const TENTATIVES_MEDIATION_FR: TentativeMediationOption[] = [
  { code: 'MEDIATION_FAMILIALE', label: 'Médiation familiale (CAF / médiateur agréé)' },
  { code: 'MEDIATION_JUDICIAIRE', label: 'Médiation judiciaire (ordonnée par JAF)' },
  { code: 'DISCUSSIONS_DIRECTES', label: 'Discussions directes entre parents' },
  { code: 'THERAPIE_FAMILIALE', label: 'Thérapie familiale' },
  { code: 'AUCUNE', label: 'Aucune tentative préalable' },
];

export function domaineDesaccordLabel(code: DomaineDesaccord): string {
  return DOMAINES_DESACCORD_FR.find((d) => d.code === code)?.label ?? code;
}

export function intensiteDesaccordLabel(code: IntensiteDesaccord): string {
  return INTENSITES_DESACCORD_FR.find((i) => i.code === code)?.label ?? code;
}

export function tentativeMediationLabel(code: TentativeMediation): string {
  return TENTATIVES_MEDIATION_FR.find((t) => t.code === code)?.label ?? code;
}

export function verdictDesaccordsParentauxLabel(v: DesaccordsParentauxVerdict): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}
