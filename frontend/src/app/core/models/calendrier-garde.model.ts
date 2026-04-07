export interface CalendrierGardeRequest { gardeCode: string; parentANom: string; parentBNom: string; }
export interface CalendrierGardeResponse {
  caseFileId: string; gardeCode: string; gardeLabel: string; country: string;
  parentANom: string; parentBNom: string; repartitionType: string;
  semaineTypeParentA: string[]; semaineTypeParentB: string[];
  vacancesRegle: string; joursParAnParentA: number; joursParAnParentB: number;
  baseJuridique: string; commentaire: string;
}
