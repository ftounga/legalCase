export interface PrudhomeDemandeur {
  nom: string;
  prenom: string | null;
  adresse: string | null;
  telephone: string | null;
  email: string | null;
  profession: string | null;
}

export interface PrudhomeDefendeur {
  nom: string | null;
  adresse: string | null;
  siret: string | null;
  representant: string | null;
}

export interface PrudhomeDemande {
  label: string;
  montant: number | null;
}

export interface PrudhomePiece {
  numero: number;
  nom: string;
}

export interface PrudhomeFiche {
  id: string | null;
  demandeur: PrudhomeDemandeur;
  defendeur: PrudhomeDefendeur;
  demandes: PrudhomeDemande[];
  faitsTexte: string | null;
  moyensDroitTexte: string | null;
  piecesList: PrudhomePiece[];
  updatedAt: string | null;
}
