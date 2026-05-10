/**
 * F-210 SF-210-02 : modèles TypeScript pour l'outil "Médiation familiale
 * obligatoire pré-saisine JAF" (Cciv art. 373-2-10 al. 3 + Loi 18/11/2016).
 * Backend : SF-210-01.
 */

export type MediationMotifSaisine =
  | 'AUTORITE_PARENTALE'
  | 'CONTRIBUTION_ENTRETIEN'
  | 'RESIDENCE_ENFANT'
  | 'DROIT_VISITE'
  | 'DIVORCE_CONTENTIEUX'
  | 'SUCCESSION'
  | 'AUTRE';

export type MediationException =
  | 'AUCUNE'
  | 'URGENCE'
  | 'VIOLENCE'
  | 'ACCORD_PREALABLE'
  | 'MOTIF_LEGITIME'
  | 'AUTRE';

export type MediationVerdict = 'RECEVABLE' | 'IRRECEVABLE' | 'NON_CONCERNE';

export interface MediationFamilialePreSaisineRequest {
  motifSaisine: MediationMotifSaisine;
  mediationTentee: boolean;
  dateMediation?: string | null;
  exceptionApplicable: MediationException;
  exceptionDetail?: string | null;
}

export interface MediationFamilialePreSaisineResponse {
  caseFileId: string;
  motifSaisine: MediationMotifSaisine;
  mediationTentee: boolean;
  dateMediation: string | null;
  exceptionApplicable: MediationException;
  exceptionDetail: string | null;
  verdict: MediationVerdict;
  motifInScope: boolean;
  dispenseApplicable: boolean;
  piecesAJoindre: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
