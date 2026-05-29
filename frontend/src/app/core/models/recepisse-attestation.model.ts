/**
 * SF-214-16 : modèles miroirs du contrat API (backend SF-214-15) pour l'outil
 * décisionnel « Récépissé vs attestation » (F-IM-32). FR uniquement.
 *
 * Distingue les droits attachés à un récépissé de demande de titre de ceux
 * attachés à une attestation de prolongation d'instruction, et alerte sur le
 * risque employeur (sanctions L. 8253-1) quand le document est une attestation
 * de prolongation sans mention d'autorisation de travail.
 */

export type TypeDocumentSejour =
  | 'RECEPISSE'
  | 'ATTESTATION_PROLONGATION'
  | 'INCONNU';

export interface RecepisseAttestationRequest {
  typeDocument: TypeDocumentSejour;
  dateDelivrance: string; // ISO yyyy-MM-dd
  dateExpiration: string; // ISO yyyy-MM-dd
  mentionAutorisationTravail?: boolean | null; // optionnel
}

export interface RecepisseAttestationResponse {
  caseFileId: string;
  typeDocument: TypeDocumentSejour;
  dateDelivrance: string;
  dateExpiration: string;
  country: string;
  droitSejour: boolean;
  droitTravail: boolean;
  dureeValiditeJours: number;
  risqueEmployeur: boolean;
  recommandations: string[];
  baseJuridique: string;
}
