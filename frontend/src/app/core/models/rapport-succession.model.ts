/**
 * SF-FA-24-14 : modèles TypeScript pour l'outil décisionnel "Rapport à
 * succession" (F-FA-24, FR — art. 843-863 + 919 Cciv).
 *
 * Contrat figé dans SF-FA-24-13 (backend, mergé PR #679).
 */

/** Qualité de l'héritier vis-à-vis de l'obligation de rapport (art. 843). */
export type QualiteHeritierRapport =
  | 'DESCENDANT'
  | 'CONJOINT_SURVIVANT';

/** Mode de rapport recommandé (art. 858 / 860). */
export type ModeRapport =
  | 'RAPPORT_EN_NATURE'
  | 'RAPPORT_EN_VALEUR'
  | 'RAPPORT_EN_MOINS_PRENANT'
  | 'NON_APPLICABLE';

/** Verdict d'obligation de rapport. */
export type VerdictObligationRapport =
  | 'RAPPORTABLE'
  | 'EXEMPT'
  | 'DISPENSÉ'
  | 'NON_OBLIGÉ';

/** Libellés humains des qualités de l'héritier. */
export const QUALITE_HERITIER_RAPPORT_LABELS:
  Readonly<Record<QualiteHeritierRapport, string>> = {
  DESCENDANT: 'Héritier descendant (enfant / petit-enfant)',
  CONJOINT_SURVIVANT: 'Conjoint survivant',
};

/** Libellés humains du mode de rapport. */
export const MODE_RAPPORT_LABELS:
  Readonly<Record<ModeRapport, string>> = {
  RAPPORT_EN_NATURE: 'Rapport en nature (restitution du bien — art. 858)',
  RAPPORT_EN_VALEUR: 'Rapport en valeur (mode par défaut — art. 860)',
  RAPPORT_EN_MOINS_PRENANT: 'Rapport en moins prenant (art. 858 dégradé)',
  NON_APPLICABLE: 'Pas de rapport applicable',
};

/** Libellés humains du verdict (4 valeurs). */
export const VERDICT_OBLIGATION_RAPPORT_LABELS:
  Readonly<Record<VerdictObligationRapport, string>> = {
  RAPPORTABLE: 'Donation rapportable à la succession',
  EXEMPT: 'Donation exempte de rapport (art. 851/852)',
  DISPENSÉ: 'Donation dispensée de rapport (art. 919)',
  NON_OBLIGÉ: 'Héritier non obligé au rapport (art. 843 a contrario)',
};

export interface RapportSuccessionRequest {
  donationsRecuesEur: number;
  /** Format ISO `YYYY-MM-DD`. */
  dateDonation: string;
  valeurAuJourPartage: number;
  donationDispenseDeRapport: boolean;
  naturePresumeeNonRapportable: boolean;
  qualiteHeritier: QualiteHeritierRapport;
}

export interface RapportSuccessionResponse {
  caseFileId: string;
  donationsRecuesEur: number;
  dateDonation: string;
  valeurAuJourPartage: number;
  donationDispenseDeRapport: boolean;
  naturePresumeeNonRapportable: boolean;
  qualiteHeritier: QualiteHeritierRapport;
  verdictObligation: VerdictObligationRapport;
  modeRapportRecommande: ModeRapport;
  montantRapportable: number;
  delaiPrescriptionAns: number;
  scoreEligibilite: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
