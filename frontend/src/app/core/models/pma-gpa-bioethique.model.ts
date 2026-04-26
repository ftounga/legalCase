/**
 * SF-FA-27-02 : types frontend pour l'outil décisionnel "PMA / GPA / bioéthique"
 * (F-FA-27). FR uniquement — loi bioéthique 2/8/2021 :
 *
 * - art. 342-9 et s. Cciv : reconnaissance conjointe anticipée PMA
 * - Cass. ass. plén. 4 arrêts 18/12/2022 : transcription état civil GPA
 * - art. 16-8-1 Cciv (depuis 1/9/2022) : accès à l'identité du donneur
 *
 * Contrat figé dans SF-FA-27-01 (backend, mergé PR #656).
 */

/** 3 dispositifs mutuellement exclusifs. */
export type DispositifBioethique =
  | 'PMA_RECONNAISSANCE_ANTICIPEE'
  | 'GPA_TRANSCRIPTION_ETAT_CIVIL'
  | 'DON_GAMETES_ACCES_ORIGINES';

/** Verdict de recevabilité (scoring niveau 5). */
export type VerdictRecevabiliteBioethique = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Risque de refus. */
export type RisqueRefusBioethique = 'FAIBLE' | 'MOYEN' | 'ELEVE';

/** Libellés humains des dispositifs (radio-button). */
export const DISPOSITIF_BIOETHIQUE_LABELS:
  ReadonlyArray<{ code: DispositifBioethique; label: string; description: string }> = [
  {
    code: 'PMA_RECONNAISSANCE_ANTICIPEE',
    label: 'PMA — Reconnaissance anticipée',
    description: 'Reconnaissance conjointe notariée AVANT la PMA (art. 342-9 et s. Cciv).',
  },
  {
    code: 'GPA_TRANSCRIPTION_ETAT_CIVIL',
    label: 'GPA — Transcription état civil',
    description: 'Transcription parent biologique + adoption simple (Cass. 18/12/2022).',
  },
  {
    code: 'DON_GAMETES_ACCES_ORIGINES',
    label: 'Don de gamètes — Accès aux origines',
    description: 'Demande à la CAPADD pour l\'enfant majeur (art. 16-8-1 Cciv, ≥ 1/9/2022).',
  },
];

/** Pays GPA légaux les plus fréquemment rencontrés (radio "paysGPALegal"). */
export const PAYS_GPA_OPTIONS: ReadonlyArray<{ code: string; label: string; legal: boolean }> = [
  { code: 'USA', label: 'États-Unis', legal: true },
  { code: 'CANADA', label: 'Canada', legal: true },
  { code: 'UK', label: 'Royaume-Uni', legal: true },
  { code: 'GREECE', label: 'Grèce', legal: true },
  { code: 'BELGIUM', label: 'Belgique (encadrement souple)', legal: true },
  { code: 'AUTRE_LEGAL', label: 'Autre pays légal', legal: true },
  { code: 'AUTRE_NON_LEGAL', label: 'Pays où la GPA n\'est pas légale', legal: false },
];

export interface PmaGpaBioethiqueRequest {
  dispositif: DispositifBioethique;
  // PMA
  consentementsConjointsNotaire?: boolean | null;
  /** ISO YYYY-MM-DD. */
  dateReconnaissanceAnterieurePMA?: string | null;
  /** ISO YYYY-MM-DD. */
  datePMA?: string | null;
  conditionsAccesPMA?: boolean | null;
  // GPA
  paysGPALegal?: boolean | null;
  parentBiologiqueAvere?: boolean | null;
  decisionEtrangereProduite?: boolean | null;
  adoptionDemande?: boolean | null;
  // DON gamètes
  /** ISO YYYY-MM-DD. */
  dateDon?: string | null;
  demandeAcces?: boolean | null;
  ageDemandeur?: number | null;
}

export interface PmaGpaBioethiqueResponse {
  caseFileId: string;
  dispositif: DispositifBioethique;
  verdictRecevabilite: VerdictRecevabiliteBioethique;
  proceduresAFollow: string[];
  risqueRefus: RisqueRefusBioethique;
  documentsRequis: string[];
  delaiInstructionMois: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
