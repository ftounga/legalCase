export interface ReferentialEntry {
  id?: string;
  key: string;
  label: string;
  country?: string | null;
  valueJson: string;
  isSystem: boolean;
  sourceRef?: string | null;
  /** SF-140-02 : description métier persistée en DB (peut être null si pas encore peuplée). */
  description?: string | null;
}

export interface ReferentialResponse {
  domain: string;
  sections: Record<string, ReferentialEntry[]>;
}

export interface ReferentialUpdateResponse {
  saved: boolean;
  entry?: ReferentialEntry;
  warning?: string | null;
}

export interface ReferentialAlert {
  id: string;
  entryId: string;
  entryKey: string;
  entryLabel: string;
  proposedValueJson: string;
  aiMessage?: string | null;
  detectedAt: string;
}
