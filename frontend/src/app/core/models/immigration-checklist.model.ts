export type ImmigrationStatut = 'PRESENT' | 'ABSENT' | 'INCONNU';

export interface ImmigrationPieceItem {
  label: string;
  statut: ImmigrationStatut;
}

export interface ImmigrationChecklist {
  caseFileId: string;
  titreType: string;
  country: string;
  pieces: ImmigrationPieceItem[];
}
