/**
 * SF-212-02 : modèles TypeScript de l'outil "Licenciement pour faute grave /
 * faute lourde" (F-DT-36 — FRANCE uniquement).
 *
 * Contrat API importé de SF-212-01 (backend, endpoints POST/GET figés sur
 * `/api/v1/case-files/{caseFileId}/licenciement-faute-grave-lourde`).
 *
 * Fondement : L.1234-1 CT (faute grave : impossibilité de maintien pendant le
 * préavis), L.1234-5 CT (privation indemnité préavis), L.1234-9 CT (privation
 * indemnité légale ; faute lourde = intention de nuire), L.1332-4 CT
 * (prescription disciplinaire 2 mois), L.1232-2 CT (convocation à entretien
 * préalable), L.1232-6 CT (lettre motivée), Cass. soc. 18/06/2013 n°11-14.393
 * (CP maintenus en faute lourde).
 */

/** Qualification retenue de la faute disciplinaire — 3 niveaux. */
export type LicenciementFauteGraveLourdQualification =
  | 'FAUTE_SIMPLE'
  | 'FAUTE_GRAVE'
  | 'FAUTE_LOURDE';

/** Code structuré d'un facteur de qualification détecté — exposé dans la réponse API. */
export type LicenciementFauteGraveLourdCodeFacteur =
  | 'DT36_QUALIFICATION_FAUTE'
  | 'DT36_PRESCRIPTION_FAUTE'
  | 'DT36_INTENTION_NUIRE'
  | 'DT36_PROCEDURE_DISCIPLINAIRE';

/** Facteur de qualification — structure exposée dans la réponse API. */
export interface LicenciementFauteGraveLourdFacteur {
  code: LicenciementFauteGraveLourdCodeFacteur;
  libelle: string;
  fondement: string;
  poids: number;
  explication: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/licenciement-faute-grave-lourde`.
 *
 * 11 champs : faits reprochés (texte), dates des faits, date de saisine
 * employeur, flag prescription, ancienneté, salaire, qualification employeur,
 * intention de nuire (booléen + preuve), procédure (convocation + motivation).
 */
export interface LicenciementFauteGraveLourdRequest {
  faitsReproches: string;
  datesFaits: string[];
  dateSaisineEmployeur: string | null;
  prescriptionFauteVerifiee: boolean | null;
  ancienneteMois: number;
  salaireMensuelBrutEuros: number;
  qualificationEmployeur: LicenciementFauteGraveLourdQualification;
  intentionNuireAlleeguee: boolean | null;
  preuveIntentionNuire: string | null;
  attenteConvocation: boolean | null;
  motivationLettreAdequate: boolean | null;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (qualification
 * retenue, score, facteurs, indemnités dues, alerte prescription, bases
 * juridiques, messages).
 */
export interface LicenciementFauteGraveLourdResponse extends LicenciementFauteGraveLourdRequest {
  caseFileId: string;
  qualificationRetenue: LicenciementFauteGraveLourdQualification;
  scoreQualification: number;
  facteursQualification: LicenciementFauteGraveLourdFacteur[];
  indemnitePreavisEuros: number;
  indemniteLegaleEuros: number;
  indemnitesCongesPayesEuros: number;
  totalIndemnitesDuesEuros: number;
  alertePrescriptionFaute: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
