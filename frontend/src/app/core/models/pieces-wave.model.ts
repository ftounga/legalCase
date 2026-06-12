// F-283 / SF-283-02 — modèle de la « vague de pièces » (ajout incrémental lisible).

export interface PendingPiece {
  documentId: string;
  filename: string;
  createdAt: string; // ISO instant
}

export interface PiecesWave {
  analyzedAt: string | null; // ISO instant ; null si jamais analysé
  pendingCount: number;
  pendingPieces: PendingPiece[];
}
