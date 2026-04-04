export interface ReferentialEntry {
  id?: string;
  key: string;
  label: string;
  country?: string | null;
  valueJson: string;
  isSystem: boolean;
  sourceRef?: string | null;
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
