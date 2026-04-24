/**
 * SF-DT-19-02 : modèles TypeScript pour l'outil décisionnel
 * "Calculateur heures supplémentaires" (F-DT-19).
 * Contrat importé de SF-DT-19-01 (backend).
 */

export interface HeuresSupRequest {
  tauxHoraireBrut: number;
  // FR
  heuresSupDeclarees25pct?: number;
  heuresSupDeclarees50pct?: number;
  heuresHorsContingent?: number;
  tauxMajoration25?: number;
  tauxMajoration50?: number;
  // BE
  heuresSupSemaine?: number;
  heuresDimancheJoursFeries?: number;
}

export interface HeuresSupResponse {
  caseFileId: string;
  tauxHoraireBrut: number;
  heuresSupDeclarees25pct: number | null;
  heuresSupDeclarees50pct: number | null;
  heuresHorsContingent: number | null;
  tauxMajoration25: number | null;
  tauxMajoration50: number | null;
  heuresSupSemaine: number | null;
  heuresDimancheJoursFeries: number | null;
  country: 'FRANCE' | 'BELGIQUE';
  rappelMajoration25pct: number;
  rappelMajoration50pct: number;
  rappelMajoration100pct: number;
  rappelTotal: number;
  reposCompensateurHeuresDues: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
