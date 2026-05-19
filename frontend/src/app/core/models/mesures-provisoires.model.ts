/**
 * SF-FA-12-02 : modèles TypeScript pour l'outil décisionnel
 * "Mesures provisoires" (F-FA-12, art. 254-256 Cciv).
 * Contrat figé par SF-FA-12-01 (backend mergé PR #573).
 *
 * Outil single-country FR — BE = F-FA-11 (procédure désunion irrémédiable).
 */

export type LogementProprietaire =
  | 'EN_INDIVISION'
  | 'PROPRIETE_DEMANDEUR'
  | 'PROPRIETE_DEFENDEUR'
  | 'LOCATION_COMMUNE';

export type SouhaitResidenceEnfants =
  | 'ALTERNEE'
  | 'EXCLUSIVE_DEMANDEUR'
  | 'EXCLUSIVE_DEFENDEUR';

export type AttributionLogementRecommande =
  | 'DEMANDEUR'
  | 'DEFENDEUR'
  | 'INDIVISION_MAINTENUE';

export type ResidenceEnfantsRecommande =
  | 'ALTERNEE'
  | 'EXCLUSIVE_DEMANDEUR'
  | 'EXCLUSIVE_DEFENDEUR'
  | 'SANS_OBJET';

export type VerdictAcceptabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface EnfantMineurInfo {
  prenom: string;
  age: number;
}

export interface MesuresProvisoiresRequest {
  /** Date de l'audience JAF (ISO YYYY-MM-DD). */
  dateAudienceAOMP: string;
  /** Revenus mensuels nets de l'époux demandeur (≥ 0). */
  revenusEpouxDemandeurEur: number;
  /** Revenus mensuels nets de l'époux défendeur (≥ 0). */
  revenusEpouxDefendeurEur: number;
  /** Description libre du logement commun (adresse, type, surface). */
  logementCommunDescription?: string | null;
  /** Régime de propriété du logement commun. */
  logementProprietaire: LogementProprietaire;
  /** Liste des enfants mineurs (peut être vide). */
  enfantsMineurs: EnfantMineurInfo[];
  /** Souhait de résidence des enfants. */
  souhaitResidenceEnfants: SouhaitResidenceEnfants;
  /** Violences alléguées (priorité protection). */
  violencesAlleguees: boolean;
  /** Patrimoine commun significatif (oriente mesure conservatoire). */
  patrimoineCommunIsignificatif: boolean;
  /** Demande explicite de mesure conservatoire patrimoniale. */
  demandeMesureConservatoire: boolean;
}

export interface MesuresProvisoiresResponse {
  caseFileId: string;
  dateAudienceAOMP: string;
  revenusEpouxDemandeurEur: number;
  revenusEpouxDefendeurEur: number;
  logementCommunDescription: string | null;
  logementProprietaire: LogementProprietaire;
  enfantsMineurs: EnfantMineurInfo[];
  souhaitResidenceEnfants: SouhaitResidenceEnfants;
  violencesAlleguees: boolean;
  patrimoineCommunIsignificatif: boolean;
  demandeMesureConservatoire: boolean;
  country: 'FRANCE';
  /** Différentiel de revenus signé (demandeur - défendeur). */
  differentielRevenus: number;
  /** Pension alimentaire ad interim proposée (méthode simplifiée |diff|/2). */
  pensionAlimentairePropose: number;
  attributionLogementRecommande: AttributionLogementRecommande;
  residenceEnfantsRecommande: ResidenceEnfantsRecommande;
  /** Contribution aux charges du mariage (max revenus / 2). */
  contributionCharges: number;
  mesureConservatoireRecommande: boolean;
  /** Score de cohésion 0-100. */
  scoreCohesionMesures: number;
  verdictAcceptabilite: VerdictAcceptabilite;
  formule: string;
  baseJuridique: string;
  messages: string[];
}

/**
 * SF-FA-12-02 : interface locale pour `aiData` du composant. Les champs
 * Famille extraits par le pipeline IA évoluent (alignement avec
 * `divorce-alteration` / `divorce-accepte`). Tout champ inconnu est ignoré
 * silencieusement. Sera remplacée par `FamilleExtractedData` partagée quand
 * le pipeline IA Famille sera étendu.
 *
 * SF-246-08 : ajout `patrimoineCommunEur` (montant € réel, sous-objet backend
 * `vie_commune_detection`) et `revenusAnnuelsEpoux` (revenus annuels débiteur
 * — utilisé en priorité si présent, sinon fallback rétro-compat `revenusAnnuelsEpoux1Eur`).
 */
export interface MesuresProvisoiresAiData {
  /** Date de l'audience AOMP détectée par l'IA (ISO YYYY-MM-DD). */
  dateAudienceAOMP?: string | null;
  /** Revenus mensuels époux demandeur détectés par l'IA. */
  revenusEpouxDemandeurEur?: number | null;
  /** Revenus mensuels époux défendeur détectés par l'IA. */
  revenusEpouxDefendeurEur?: number | null;
  /**
   * Alias rétro-compatible : si le pipeline IA fournit `revenusAnnuelsEpoux1Eur`
   * (modèle FamilleAiData mutualisé), on accepte aussi (annuel / 12 puis arrondi).
   */
  revenusAnnuelsEpoux1Eur?: number | null;
  revenusAnnuelsEpoux2Eur?: number | null;
  /**
   * SF-246-08 : revenus annuels du débiteur/demandeur (€) — champ réel backend.
   * Priorité sur `revenusAnnuelsEpoux1Eur` quand les deux sont présents.
   */
  revenusAnnuelsEpoux?: number | null;
  /** Violences alléguées détectées par l'IA. */
  violencesAlleguees?: boolean | null;
  /** Patrimoine commun significatif détecté par l'IA. */
  patrimoineCommunSignificatif?: boolean | null;
  /**
   * SF-246-08 : montant du patrimoine commun (€) — invariant §5.2 (> 0 ou null).
   * Quand présent, `computePatrimoineCommun` le dérive en `true` (présence suffit).
   * Distinct du boolean `patrimoineCommunSignificatif` (rétro-compat).
   */
  patrimoineCommunEur?: number | null;
}

export const LOGEMENT_PROPRIETAIRE_OPTIONS: Array<{ value: LogementProprietaire; label: string }> = [
  { value: 'EN_INDIVISION', label: 'En indivision' },
  { value: 'PROPRIETE_DEMANDEUR', label: 'Propriété de l\'époux demandeur' },
  { value: 'PROPRIETE_DEFENDEUR', label: 'Propriété de l\'époux défendeur' },
  { value: 'LOCATION_COMMUNE', label: 'Location commune' },
];

export const SOUHAIT_RESIDENCE_OPTIONS: Array<{ value: SouhaitResidenceEnfants; label: string }> = [
  { value: 'ALTERNEE', label: 'Résidence alternée' },
  { value: 'EXCLUSIVE_DEMANDEUR', label: 'Résidence exclusive chez le demandeur' },
  { value: 'EXCLUSIVE_DEFENDEUR', label: 'Résidence exclusive chez le défendeur' },
];

export function verdictAcceptabiliteLabel(v: VerdictAcceptabilite): string {
  switch (v) {
    case 'ELEVEE': return 'Acceptabilité élevée';
    case 'MOYENNE': return 'Acceptabilité moyenne';
    case 'FAIBLE': return 'Acceptabilité faible';
  }
}

export function attributionLogementLabel(a: AttributionLogementRecommande): string {
  switch (a) {
    case 'DEMANDEUR': return 'Logement attribué au demandeur';
    case 'DEFENDEUR': return 'Logement attribué au défendeur';
    case 'INDIVISION_MAINTENUE': return 'Indivision maintenue (départage à venir)';
  }
}

export function residenceEnfantsLabel(r: ResidenceEnfantsRecommande): string {
  switch (r) {
    case 'ALTERNEE': return 'Résidence alternée';
    case 'EXCLUSIVE_DEMANDEUR': return 'Résidence exclusive chez le demandeur';
    case 'EXCLUSIVE_DEFENDEUR': return 'Résidence exclusive chez le défendeur';
    case 'SANS_OBJET': return 'Sans objet (aucun enfant mineur)';
  }
}
