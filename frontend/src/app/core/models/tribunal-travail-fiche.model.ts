export interface TribunalRequerant {
  nom: string;
  prenom: string | null;
  domicile: string | null;
  registreNational: string | null;
}

export interface TribunalDefendeur {
  nom: string | null;
  siegeSocial: string | null;
  numeroBce: string | null;
  representant: string | null;
}

export interface TribunalProcedureInfo {
  tribunal: string | null;
  division: string | null;
  langue: string | null;
  commissionParitaire: string | null;
}

export interface TribunalContratInfo {
  typeContrat: string | null;
  dateDebut: string | null;
  dateFin: string | null;
  motifRupture: string | null;
}

export interface TribunalDemande {
  label: string;
  montant: number | null;
}

export interface TribunalPiece {
  numero: number;
  nom: string;
}

export interface TribunalTravailFiche {
  id: string | null;
  requerant: TribunalRequerant;
  defendeur: TribunalDefendeur;
  procedureInfo: TribunalProcedureInfo;
  contratInfo: TribunalContratInfo;
  demandes: TribunalDemande[];
  exposeDesMoyens: string | null;
  piecesList: TribunalPiece[];
  updatedAt: string | null;
}
