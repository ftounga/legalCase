/**
 * SF-212-20 : modèles TypeScript de l'outil "Mise à pied disciplinaire —
 * régularité" (F-DT-48-mise-a-pied-disciplinaire, FRANCE uniquement —
 * L. 1331-1 CT ; L. 1332-1 à L. 1332-4 CT ; Cass. soc. constante).
 *
 * Contrat API importé de SF-212-19 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/mise-a-pied-disciplinaire`).
 */

/** Nature de la mise à pied — 3 valeurs (Cass. soc. 26/10/2010). */
export type MapNatureMiseAPied =
  | 'DISCIPLINAIRE'
  | 'CONSERVATOIRE'
  | 'INCONNUE';

/** Verdict d'analyse de la régularité de la mise à pied — 4 niveaux. */
export type MapAnalyse =
  | 'REGULIERE'
  | 'IRREGULIERE_FORME'
  | 'IRREGULIERE_FOND'
  | 'CONSERVATOIRE';

/** Code structuré F-IA-03 d'un point de régularité. */
export type MapCodeCritere =
  | 'DT48_NATURE_MISE_A_PIED'
  | 'DT48_PROCEDURE_SUIVIE'
  | 'DT48_PRESCRIPTION_FAUTE'
  | 'DT48_DOUBLE_SANCTION';

/** Point de régularité exposé dans la réponse API. */
export interface MapPointRegularite {
  code: MapCodeCritere;
  libelle: string;
  fondement: string;
  conclusion: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/mise-a-pied-disciplinaire`.
 *
 * 8 champs : nature (select), procédure suivie (toggle), prescription
 * respectée (toggle), durée définie au RI (toggle), durée en jours (entier),
 * salaire suspendu (toggle), sanctions antérieures pour les mêmes faits
 * (toggle), salaire mensuel brut.
 */
export interface MiseAPiedDisciplinaireRequest {
  natureMiseAPied: MapNatureMiseAPied;
  procedureEntretienSuivie: boolean;
  prescriptionFauteVerifiee: boolean;
  dureeDefiniedansRI: boolean;
  dureeJours: number | null;
  salaireSuspendu: boolean;
  sancionsAnterieuresMemesFaits: boolean;
  salaireMensuelBrutEuros: number;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (verdict, score,
 * points de régularité F-IA-03, salaire dû pendant la période, bases
 * juridiques, messages).
 */
export interface MiseAPiedDisciplinaireResponse extends MiseAPiedDisciplinaireRequest {
  caseFileId: string;
  analyseMiseAPied: MapAnalyse;
  scoreMiseAPied: number;
  pointsRegularite: MapPointRegularite[];
  salaireDuPeriodeMiseAPiedEuros: number;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
