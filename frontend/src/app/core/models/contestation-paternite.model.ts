/**
 * SF-FA-18-04 : modèles TypeScript pour l'outil décisionnel
 * "Contestation de paternité" (F-FA-18 — FR uniquement, art. 332-335 + 311-1
 * + 321 + 372 Cciv).
 *
 * Contrat figé dans SF-FA-18-03 (backend, mergé PR #660).
 */

/** Qualité à agir du contestant (art. 332-335 Cciv). */
export type QualiteAagir =
  | 'PERE_DECLARE'
  | 'PERE_BIOLOGIQUE_PRESUME'
  | 'MERE'
  | 'ENFANT_MAJEUR';

/** Verdict de recevabilité (scoring niveau 5). */
export type VerdictRecevabiliteContestation = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio qualité à agir. */
export const QUALITE_AAGIR_LABELS:
  ReadonlyArray<{ code: QualiteAagir; label: string; sub: string }> = [
  {
    code: 'PERE_DECLARE',
    label: 'Père légalement déclaré (art. 332-333)',
    sub: 'Le père reconnu conteste sa propre paternité — délai 5 ans',
  },
  {
    code: 'PERE_BIOLOGIQUE_PRESUME',
    label: 'Père biologique présumé (art. 333)',
    sub: 'Le père biologique conteste la filiation déclarée d\'un autre — délai 5 ans',
  },
  {
    code: 'MERE',
    label: 'Mère (art. 333)',
    sub: 'La mère conteste la paternité déclarée — délai 5 ans',
  },
  {
    code: 'ENFANT_MAJEUR',
    label: 'Enfant majeur (art. 333 + 321)',
    sub: 'L\'enfant à sa majorité — délai 10 ans',
  },
];

export interface ContestationPaterniteRequest {
  qualiteAagir: QualiteAagir;
  /** ISO YYYY-MM-DD — obligatoire. */
  dateEtablissementFiliation: string;
  /** ISO YYYY-MM-DD — obligatoire. */
  dateConnaissanceVerite: string;
  /** ISO YYYY-MM-DD — requis seulement si qualiteAagir === ENFANT_MAJEUR. */
  dateMajoriteEnfant?: string | null;
  possessionEtatConforme5Ans: boolean;
  expertiseAdnDemandee: boolean;
  motifsSerieux: boolean;
}

export interface ContestationPaterniteResponse {
  caseFileId: string;
  qualiteAagir: QualiteAagir;
  verdictRecevabilite: VerdictRecevabiliteContestation;
  scoreRecevabilite: number;
  delaiPrescriptionAns: number;
  delaiPrescriptionRestantMois: number;
  expertiseAdnRecommandee: boolean;
  risquesRefus: string[];
  documentsRequis: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
