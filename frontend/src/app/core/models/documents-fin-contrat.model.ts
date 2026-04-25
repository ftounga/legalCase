/**
 * SF-DT-32-02 : modèles TypeScript pour l'outil décisionnel "Documents de
 * fin de contrat" (art. L.1234-19 / R.1234-9 / L.1234-20 Code du travail).
 *
 * Contrat API figé dans SF-DT-32-01 (backend, PR #595 mergée).
 * Endpoint : POST/GET /api/v1/case-files/{caseFileId}/documents-fin-contrat
 *
 * France uniquement — l'équivalent BE (C4 ONEM, certificat Loi 03/07/1978
 * art. 22, attestation vacances) est une feature jumelle au backlog.
 */

/** Verdict de risque contentieux dérivé du score (FAIBLE ≥ 80, MOYEN 50-79, ELEVE < 50). */
export type VerdictRisqueContentieux = 'FAIBLE' | 'MOYEN' | 'ELEVE';

export interface DocumentsFinContratRequest {
  /** Date de fin de contrat (YYYY-MM-DD, requis). */
  dateFinContrat: string;
  /** Vrai si le certificat de travail a été remis. */
  certificatTravailRemis: boolean | null;
  /** Date de remise du certificat (requis si remis). */
  dateCertificatTravail: string | null;
  /** Vrai si l'attestation France Travail a été remise. */
  attestationFranceTravailRemise: boolean | null;
  /** Date de remise de l'attestation (requis si remise). */
  dateAttestationFranceTravail: string | null;
  /** Vrai si le reçu pour solde de tout compte a été signé. */
  souldeToutCompteSigne: boolean | null;
  /** Date de signature du STC (requis si signé). */
  dateSouldeToutCompte: string | null;
  /** Salaire mensuel brut de référence (> 0, requis). */
  salaireMensuelBrutEur: number;
}

export interface DocumentsFinContratResponse {
  caseFileId: string;
  // --- Snapshot inputs ---
  dateFinContrat: string;
  certificatTravailRemis: boolean | null;
  dateCertificatTravail: string | null;
  attestationFranceTravailRemise: boolean | null;
  dateAttestationFranceTravail: string | null;
  souldeToutCompteSigne: boolean | null;
  dateSouldeToutCompte: string | null;
  salaireMensuelBrutEur: number;
  // --- Outputs calculés ---
  /** Certificat de travail remis dans les 7 jours suivant la fin de contrat (L.1234-19 + jurisprudence). */
  certificatRemisDansDelai: boolean;
  /** Attestation France Travail remise dans les 7 jours (R.1234-9 + jurisprudence). */
  attestationRemiseDansDelai: boolean;
  /** STC signé dans les 30 jours suivant la fin de contrat (L.1234-20). */
  souldeToutCompteValide: boolean;
  /** STC encore contestable : fenêtre de 6 mois après signature (L.1234-20 al. 2). */
  souldeToutCompteContestable: boolean;
  /** Nombre de sanctions cumulables (1 par document non conforme). */
  totalSanctionsCumulables: number;
  /** Indemnité de retard certificat = (salaire / 30) × jours de retard. */
  indemniteRetardCertificatEur: number;
  /** Indemnité de retard attestation = (salaire / 30) × jours de retard. */
  indemniteRetardAttestationEur: number;
  /** Score 0-100 (Certificat 35 + Attestation 35 + STC 30). */
  scoreConformiteEmployeur: number;
  /** Verdict dérivé (FAIBLE / MOYEN / ELEVE). */
  verdictRisqueContentieux: VerdictRisqueContentieux;
  /** Référence juridique compactée. */
  baseJuridique: string;
  /** Formule littérale du calcul. */
  formule: string;
  /** Messages explicatifs (citations juridiques). */
  messages: string[];
  /** Messages spécifiques aux sanctions encourues par l'employeur. */
  messagesContentieux: string[];
  country: 'FRANCE';
}
