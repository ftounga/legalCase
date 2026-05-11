/**
 * F-243 : modèles TypeScript pour l'outil "Divorce par consentement mutuel — Belgique"
 * (CJ art. 1287-1304 + Loi du 27/04/2007 + CC art. 229 §1). BELGIQUE uniquement.
 *
 * Backend : `DivorceDcBeRequest` / `DivorceDcBeResponse` / `DivorceDcBeCalculator`
 * (SF-211-01, PR backend antérieure — endpoints `POST` / `GET`
 * `/api/v1/case-files/{id}/divorce-dc-be-analysis`).
 */

export type DivorceDcBeVerdict = 'RECEVABLE' | 'IRRECEVABLE' | 'NON_CONCERNE';

export interface DivorceDcBeRequest {
  /** ISO YYYY-MM-DD — date de signature de la convention préalable (art. 1287 CJ). */
  dateSignatureConvention: string;
  /** ISO YYYY-MM-DD — date de l'audience d'homologation devant le tribunal famille (optionnel). */
  dateAudienceHomologation: string | null;
  conventionLogement: boolean;
  conventionBiens: boolean;
  conventionGardeEnfants: boolean;
  conventionContributions: boolean;
  enfantsMineursCommuns: boolean;
  epouxConsentent: boolean;
}

export interface DivorceDcBeResponse {
  caseFileId: string;
  country: 'BELGIQUE';
  dateSignatureConvention: string;
  dateAudienceHomologation: string | null;
  /** Nombre de jours calendaires entre signature et audience (-1 si audience non fixée). */
  delaiReflexionJours: number;
  delaiReflexionRespecte: boolean;
  conventionLogement: boolean;
  conventionBiens: boolean;
  conventionGardeEnfants: boolean;
  conventionContributions: boolean;
  conventionComplete: boolean;
  enfantsMineursCommuns: boolean;
  epouxConsentent: boolean;
  verdict: DivorceDcBeVerdict;
  motifsIrrecevabilite: string[];
  formule: string;
  baseJuridique: string;
  messages: string[];
}
