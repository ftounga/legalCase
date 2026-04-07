export interface DivorceEtapeStatus { code: string; label: string; ordre: number; description: string; delai: string; obligatoire: boolean; statut: string; }
export interface DivorcePieceStatus { code: string; label: string; description: string; obligatoire: boolean; statut: string; }
export interface DivorceChecklistRequest { country: string; etapeStatuts: Record<string, string>; pieceStatuts: Record<string, string>; }
export interface DivorceChecklistResponse {
  caseFileId: string; country: string;
  etapes: DivorceEtapeStatus[]; pieces: DivorcePieceStatus[];
  etapesCompletees: number; etapesTotal: number;
  piecesPresentes: number; piecesTotal: number;
  baseJuridique: string;
}
