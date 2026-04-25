/**
 * SF-FA-23-02 : types frontend pour l'outil décisionnel "Ordonnance sur requête"
 * (mesures urgentes familiales — art. 493 + 497 CPC FR / art. 1025 et s. CJ BE).
 *
 * Contrat figé dans SF-FA-23-01 (backend, mergé PR #637).
 *
 * Outil applicable FRANCE et BELGIQUE simultanément — pas de gate pays.
 * La base juridique est adaptée par le backend selon `country`.
 */

/** Motif de la requête (5 valeurs alignées backend `MotifRequete`). */
export type MotifRequete =
  | 'DETOURNEMENT_PATRIMOINE'
  | 'ENLEVEMENT_INTERNATIONAL'
  | 'ORGANISATION_INSOLVABILITE'
  | 'PREUVE_ABANDON'
  | 'ACCES_PIECES';

/** Verdict de probabilité d'obtention. */
export type VerdictAccordeProbabilite = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Critères de recevabilité art. 493 CPC. */
export type CritereRecevabilite =
  | 'URGENCE_JUSTIFIEE'
  | 'DEROGATION_CONTRADICTOIRE_JUSTIFIEE'
  | 'PIECE_JUSTIFICATIVE_FOURNIE';

/** Libellés humains pour mat-radio motif requête. */
export const MOTIF_REQUETE_LABELS:
  ReadonlyArray<{ code: MotifRequete; label: string; description: string }> = [
  {
    code: 'DETOURNEMENT_PATRIMOINE',
    label: 'Détournement de patrimoine',
    description: 'Suspicion de détournement avant divorce — accès au dossier bancaire du conjoint',
  },
  {
    code: 'ENLEVEMENT_INTERNATIONAL',
    label: 'Risque d\'enlèvement international',
    description: 'IST 373-2-6 Cciv — interdiction de sortie du territoire pour l\'enfant',
  },
  {
    code: 'ORGANISATION_INSOLVABILITE',
    label: 'Organisation d\'insolvabilité',
    description: 'Saisies conservatoires sur biens du conjoint',
  },
  {
    code: 'PREUVE_ABANDON',
    label: 'Preuve d\'abandon / cohabitation',
    description: 'Constat d\'huissier au domicile commun',
  },
  {
    code: 'ACCES_PIECES',
    label: 'Production forcée de pièces',
    description: 'Accès aux relevés bancaires, factures, comptabilité',
  },
];

/** Libellés humains pour les codes critère de recevabilité. */
export const CRITERE_RECEVABILITE_LABELS: Record<CritereRecevabilite, string> = {
  URGENCE_JUSTIFIEE: 'Urgence justifiée par pièces',
  DEROGATION_CONTRADICTOIRE_JUSTIFIEE: 'Dérogation au contradictoire justifiée',
  PIECE_JUSTIFICATIVE_FOURNIE: 'Pièce justificative au dossier',
};

export interface OrdonnanceRequeteRequest {
  motifRequete: MotifRequete;
  urgenceJustifiee: boolean;
  derogationContradictoireJustifiee: boolean;
  pieceJustificativeFournie: boolean;
  /** Optionnel — défaut backend false. Force le délai IST (1-3 j) si motif IST. */
  presenceEnfants?: boolean;
  /** Optionnel — texte libre, max 2000 caractères. */
  commentaireUrgence?: string;
}

export interface OrdonnanceRequeteResponse {
  caseFileId: string;
  motifRequete: MotifRequete;
  scoreEligibilite: number;
  verdictAccordeProbabilite: VerdictAccordeProbabilite;
  criteresRemplis: CritereRecevabilite[];
  criteresManquants: CritereRecevabilite[];
  /** Délai typique min en jours (1 si IST, 5 sinon). */
  delaiTypiqueJoursMin: number;
  /** Délai typique max en jours (3 si IST, 15 sinon). */
  delaiTypiqueJoursMax: number;
  /** Délai du référé-rétractation art. 497 CPC (FR) / opposition (BE) — 15 jours. */
  recoursAdverseDelaiJours: number;
  /** Adaptée par le backend selon country (art. 493 CPC FR ou art. 1025 CJ BE). */
  baseJuridique: string;
  formule: string;
  messages: string[];
  /** 'FRANCE' ou 'BELGIQUE'. */
  country: string;
}
