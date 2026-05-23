/**
 * SF-216-22 : modèles TypeScript de l'outil "Recel de succession"
 * (F-FA-RECEL-SUCCESSION, FRANCE uniquement — art. 778 Cciv +
 * Cass. 1ère civ., 14/11/2012, n° 11-20.582).
 *
 * Contrat API importé de SF-216-21 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/recel-succession
 *   GET  /api/v1/case-files/{caseFileId}/recel-succession
 */

/** Type de recel envisagé (art. 778 Cciv). */
export type TypeRecel =
  | 'DISSIMULATION_BIEN'
  | 'DISSIMULATION_DONATION'
  | 'DESTRUCTION_TESTAMENT'
  | 'RECEL_CREANCE'
  | 'AUTRE';

/** Nature de la preuve disponible (art. 778 + Cass. 1ère civ. 14/11/2012). */
export type PreuveRecel =
  | 'AVEUX'
  | 'DOCUMENT'
  | 'TEMOIGNAGE'
  | 'EXPERTISE'
  | 'FAISCEAU_INDICES'
  | 'AUCUNE';

/** Qualité du receleur envisagé (art. 778 Cciv). */
export type ReceleurQualite =
  | 'HERITIER'
  | 'LEGATAIRE'
  | 'DONATAIRE'
  | 'TIERS_COMPLICITE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/recel-succession`.
 *
 * 6 champs (cf. backend `RecelSuccessionRequest`).
 */
export interface RecelSuccessionRequest {
  typeRecel: TypeRecel | null;
  bienCeleValeurEur: number | null;
  preuveRecel: PreuveRecel | null;
  receleurQualite: ReceleurQualite | null;
  /** ISO date YYYY-MM-DD. */
  dateOuvertureSuccession: string | null;
  actionIntentee: boolean | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface RecelSuccessionResponse {
  caseFileId: string;
  /** "CARACTERISE" | "PROBABLE" | "INSUFFISANT" | "HORS_PERIMETRE" | "PAS_DE_RECEL". */
  qualificationRecel: string;
  /**
   * "RECEL_CARACTERISE" | "RECEL_PROBABLE_FAISCEAU" | "PREUVE_INSUFFISANTE" |
   * "QUALITE_RECELEUR_EXCLUE" | "DELAI_FORCLOS" | "PAS_DE_RECEL".
   */
  verdictRecel: string;
  sanctionCivile: string;
  preuveRequise: string;
  delaiAction: string;
  delaiForclos: boolean;
  voletPenalSignale: boolean;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
