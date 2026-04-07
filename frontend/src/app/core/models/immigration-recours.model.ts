export interface RecoursRequerantData {
  nom: string;
  prenom: string;
  nationalite: string;
  adresse: string;
}

export interface RecoursDecisionContesteeData {
  autorite: string;
  date: string;
  reference: string | null;
}

export interface RecoursDocumentData {
  enTete: string;
  objetDemande: string;
  visaTextes: string;
  exposeFaits: string;
  moyensDroit: string;
  conclusions: string;
  piecesJointes: string[];
}

export interface RecoursRequest {
  recoursType: string;
  dateNotification: string;
  requerant: RecoursRequerantData;
  decisionContestee: RecoursDecisionContesteeData;
  exposeFaits: string | null;
}

export interface RecoursResponse {
  caseFileId: string;
  recoursType: string;
  recoursLabel: string;
  dateNotification: string;
  dateLimite: string;
  dateLimiteDepassee: boolean;
  avertissement: string | null;
  requerant: RecoursRequerantData;
  decisionContestee: RecoursDecisionContesteeData;
  exposeFaits: string | null;
  document: RecoursDocumentData;
}
