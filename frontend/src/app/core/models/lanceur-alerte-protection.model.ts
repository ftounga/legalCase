/**
 * SF-212-26 : modèles TypeScript de l'outil "Protection du lanceur
 * d'alerte" (F-DT-61, FRANCE uniquement — L. 1132-3-3 CT ; loi Sapin II
 * n° 2016-1691 du 09/12/2016 ; loi Waserman n° 2022-401 du 21/03/2022).
 *
 * Contrat API importé de SF-212-25 (backend, endpoints POST/GET figés
 * sur `/api/v1/case-files/{caseFileId}/lanceur-alerte-protection`).
 */

/** Verdict d'évaluation de la protection — 3 niveaux. */
export type LanceurAlerteAnalyse =
  | 'PROTECTION_FORTE'
  | 'PROTECTION_PARTIELLE'
  | 'HORS_CHAMP';

/** Nature du signalement. */
export type LanceurAlerteNatureSignalement =
  | 'CRIME_DELIT'
  | 'VIOLATION_DROIT_UE'
  | 'MENACE_INTERET_GENERAL'
  | 'AUTRE';

/** Procédure de signalement utilisée. */
export type LanceurAlerteProcedure =
  | 'INTERNE'
  | 'EXTERNE'
  | 'DIVULGATION_PUBLIQUE';

/** Nature de la mesure de représailles détectée. */
export type LanceurAlerteNatureMesure =
  | 'LICENCIEMENT'
  | 'SANCTION'
  | 'MESURE_DISCRIMINATOIRE'
  | 'AUTRE'
  | 'AUCUNE';

/** Code structuré d'un point d'analyse F-IA-03. */
export type LanceurAlerteCodeCritere =
  | 'DT61_NATURE_SIGNALEMENT'
  | 'DT61_CONTREPARTIE'
  | 'DT61_PROCEDURE'
  | 'DT61_MESURE_REPRESAILLE';

/** Point d'analyse F-IA-03 exposé dans la réponse API. */
export interface LanceurAlertePointAnalyse {
  code: LanceurAlerteCodeCritere;
  libelle: string;
  fondement: string;
  conclusion: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/lanceur-alerte-protection`.
 *
 * 6 champs : nature signalement, contrepartie financière, procédure,
 * référent interne saisi (null si inapplicable), mesure de représaille,
 * nature de la mesure.
 */
export interface LanceurAlerteProtectionRequest {
  natureSignalement: LanceurAlerteNatureSignalement;
  contrepartieFinanciere: boolean | null;
  procedureSignalement: LanceurAlerteProcedure;
  referentInterneSaisi: boolean | null;
  mesureRepresailleDetectee: boolean | null;
  natureMesureRepresaille: LanceurAlerteNatureMesure;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs
 * (pour ré-édition du formulaire UI) ET les sorties calculées (verdict,
 * score, points d'analyse F-IA-03, nullité mesure, montant minimum DI
 * Waserman, bases juridiques, messages).
 */
export interface LanceurAlerteProtectionResponse extends LanceurAlerteProtectionRequest {
  caseFileId: string;
  analyseLanceurAlerte: LanceurAlerteAnalyse;
  scoreProtection: number;
  pointsAnalyse: LanceurAlertePointAnalyse[];
  nulliteMesureRepresaille: boolean;
  /** 10 000 € si PROTECTION_FORTE / PROTECTION_PARTIELLE, 0 sinon (loi Waserman 2022). */
  montantMinDommagesInterets: number;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
