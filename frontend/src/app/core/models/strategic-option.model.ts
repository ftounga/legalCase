export type StrategicOptionStatus = 'TO_STUDY' | 'RETAINED' | 'DISCARDED';

export interface StrategicOption {
  id: string;
  texte: string;
  baseJuridique: string | null;
  horizonTemporel: string | null;
  conditions: string[];
  source: string | null;
  statut: StrategicOptionStatus;
  raisonDiscard: string | null;
  ordre: number;
  createdAt: string;
  updatedAt: string;
}
