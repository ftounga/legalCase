/**
 * SF-214-01 : modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel "Étranger malade L.425-9 CESEDA" (F-IM-25).
 * FR uniquement (CESEDA L.425-9, Ord. 2020-1733).
 */

export type AvisOFII = 'FAVORABLE' | 'DEFAVORABLE' | 'EN_COURS';

export type VerdictEtrangerMalade =
  | 'ELIGIBLE_PROBABLE'
  | 'ELIGIBLE_SOUS_RESERVE'
  | 'NON_ELIGIBLE'
  | 'EN_ATTENTE_AVIS_OFII';

export interface EtrangerMaladeRequest {
  dateDepotDossierOFII?: string | null;      // YYYY-MM-DD
  pathologiePrincipale: string;
  paysOrigine: string;
  traitementDisponiblePaysOrigine: boolean;
  avisOFIIRendu?: boolean | null;
  avisOFII?: AvisOFII | null;
  dateAvisOFII?: string | null;              // YYYY-MM-DD
}

export interface EtrangerMaladeResponse {
  caseFileId: string;
  pathologiePrincipale: string;
  paysOrigine: string;
  traitementDisponiblePaysOrigine: boolean;
  avisOFII: AvisOFII | null;
  dateAvisOFII: string | null;
  country: string;
  verdict: VerdictEtrangerMalade;
  delaiRecoursTA: string | null;             // YYYY-MM-DD
  motifRecours: string | null;
  chipsCriteresNonRemplis: string[];
  baseJuridique: string;
}
