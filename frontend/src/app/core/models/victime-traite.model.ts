/**
 * SF-214-22 : modèles miroirs du contrat API (backend SF-214-21) pour l'outil
 * décisionnel « Victime de traite des êtres humains — protection L. 425-1 »
 * (F-IM-35). FR uniquement (régime distinct en BE).
 *
 * Évalue l'éligibilité à la carte de séjour temporaire « vie privée et
 * familiale » de l'article L. 425-1 du CESEDA pour la victime de traite des
 * êtres humains / proxénétisme ayant porté plainte ou témoigné, et alerte sur
 * le risque immédiat pour la sécurité de la victime (risqueVictimeEnDanger).
 */

export type VictimeTraiteVerdict =
  | 'ELIGIBLE_PROBABLE'
  | 'ELIGIBLE_SOUS_RESERVE_PLAINTE'
  | 'NON_ELIGIBLE'
  | 'EN_COURS_IDENTIFICATION';

export interface VictimeTraiteRequest {
  plainteDeposee: boolean;
  collaborationOCRTEH: boolean;
  datePlainte?: string | null; // ISO yyyy-MM-dd, optionnel
  titreActuel?: string | null; // optionnel
  presenceAutoriteRefugieDetectee?: boolean | null; // optionnel
}

export interface VictimeTraiteResponse {
  caseFileId: string;
  country: string;
  verdict: VictimeTraiteVerdict;
  chipsCriteresManquants: string[];
  mesuresProtection: string[];
  risqueVictimeEnDanger: boolean;
  baseJuridique: string;
}
