export interface ReferentialEntry {
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
