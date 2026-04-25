/**
 * SF-IM-08-08 : modèles TypeScript pour l'outil décisionnel
 * "Référés administratifs L.521-1 / L.521-2 — France" (F-IM-08).
 * FR uniquement. Contrat figé par SF-IM-08-07 (backend).
 */

export type TypeRefere = 'SUSPENSION' | 'LIBERTE' | 'LES_DEUX';

export type DecisionContestee =
  | 'OQTF'
  | 'RETRAIT_TITRE'
  | 'REFUS_TITRE'
  | 'IRTF'
  | 'AUTRE_MESURE_ADMIN';

export type PreuveUrgence =
  | 'MENACE_VIE_PRIVEE'
  | 'TRANSFERT_IMMINENT'
  | 'IMPACT_SCOLARITE_ENFANTS'
  | 'RUPTURE_SOINS_MEDICAUX'
  | 'RISQUE_VIOLENCES_PAYS_ORIGINE'
  | 'AUTRE';

export type VerdictRecommandation =
  | 'REFERE_SUSPENSION_PRIORITAIRE'
  | 'REFERE_LIBERTE_PRIORITAIRE'
  | 'LES_DEUX_CUMULES'
  | 'AUCUN_PROBABLE';

export interface ReferesAdminRequest {
  typeRefere: TypeRefere;
  decisionContestee: DecisionContestee;
  dateNotificationDecision: string; // YYYY-MM-DD
  urgenceCaracterisee: boolean;
  atteinteLiberteFondamentale: boolean;
  doutesSerieuxLegalite: boolean;
  preuvesUrgence: PreuveUrgence[];
  demandeurDejaPrived: boolean;
}

export interface ReferesAdminResponse {
  caseFileId: string;
  // Inputs reflétés (pour mode résultat → édition).
  typeRefere: TypeRefere;
  decisionContestee: DecisionContestee;
  dateNotificationDecision: string;
  urgenceCaracterisee: boolean;
  atteinteLiberteFondamentale: boolean;
  doutesSerieuxLegalite: boolean;
  preuvesUrgence: PreuveUrgence[];
  demandeurDejaPrived: boolean;
  // Outputs scoring.
  scoreSuccessProbabiliteSuspension: number; // 0..100
  scoreSuccessProbabiliteLiberte: number; // 0..100
  verdictRecommandation: VerdictRecommandation;
  delaiJugeTaJoursL521_1: number; // typiquement 30
  delaiJugeTaHeuresL521_2: number; // 48
  conditionsCumulativesL521_1Ok: boolean;
  conditionsCumulativesL521_2Ok: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface TypeRefereOption {
  code: TypeRefere;
  label: string;
}

export interface DecisionContesteeOption {
  code: DecisionContestee;
  label: string;
}

export interface PreuveUrgenceOption {
  code: PreuveUrgence;
  label: string;
}

export const TYPES_REFERE: TypeRefereOption[] = [
  { code: 'SUSPENSION', label: 'Référé suspension (L.521-1)' },
  { code: 'LIBERTE', label: 'Référé liberté (L.521-2)' },
  { code: 'LES_DEUX', label: 'Cumul L.521-1 + L.521-2' },
];

export const DECISIONS_CONTESTEES: DecisionContesteeOption[] = [
  { code: 'OQTF', label: 'OQTF' },
  { code: 'RETRAIT_TITRE', label: 'Retrait de titre de séjour' },
  { code: 'REFUS_TITRE', label: 'Refus de titre de séjour' },
  { code: 'IRTF', label: 'IRTF (interdiction de retour)' },
  { code: 'AUTRE_MESURE_ADMIN', label: 'Autre mesure administrative' },
];

export const PREUVES_URGENCE: PreuveUrgenceOption[] = [
  { code: 'MENACE_VIE_PRIVEE', label: 'Menace vie privée et familiale' },
  { code: 'TRANSFERT_IMMINENT', label: 'Transfert imminent (CRA / frontière)' },
  { code: 'IMPACT_SCOLARITE_ENFANTS', label: 'Impact scolarité enfants' },
  { code: 'RUPTURE_SOINS_MEDICAUX', label: 'Rupture de soins médicaux' },
  { code: 'RISQUE_VIOLENCES_PAYS_ORIGINE', label: 'Risque de violences au pays d\'origine' },
  { code: 'AUTRE', label: 'Autre situation d\'urgence' },
];

/** Verdict labels (pour bannière). */
export const VERDICT_LABELS: Record<VerdictRecommandation, string> = {
  REFERE_SUSPENSION_PRIORITAIRE: 'Référé suspension (L.521-1) prioritaire',
  REFERE_LIBERTE_PRIORITAIRE: 'Référé liberté (L.521-2) prioritaire',
  LES_DEUX_CUMULES: 'Cumul L.521-1 + L.521-2 recommandé',
  AUCUN_PROBABLE: 'Aucun référé probable — chances faibles',
};
