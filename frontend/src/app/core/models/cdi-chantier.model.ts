/**
 * SF-218-26 : modèles miroirs du contrat API (backend SF-218-25) pour l'outil
 * décisionnel « Licenciement CDI de chantier / d'opération »
 * (F-DT-37-licenciement-cdi-chantier). FRANCE uniquement.
 *
 * Régime du contrat de chantier ou d'opération (art. L.1223-8 et L.1223-9 CT) :
 * vérifie la validité du recours (accord de branche étendu OU usage constant —
 * BTP / ingénierie), qualifie le motif (fin de chantier = cause réelle et
 * sérieuse, motif spécifique L.1236-8 CT) et calcule l'indemnité de licenciement
 * (art. R.1234-2 CT, sauf disposition conventionnelle plus favorable).
 *
 * Pattern miroir de {@link ApprentissageRuptureResponse} (F-DT-110, SF-218-24).
 */

/**
 * Fondement légal du recours au CDI de chantier, aligné EXACTEMENT sur l'enum
 * backend {@code FondementRecours} :
 *  - ACCORD_BRANCHE_ETENDU : accord de branche étendu autorisant le recours.
 *  - USAGE_CONSTANT_SECTEUR : usage constant dans le secteur (BTP / ingénierie).
 *  - AUCUN : ni accord ni usage → risque de requalification en CDI de droit commun.
 */
export type FondementRecours =
  | 'ACCORD_BRANCHE_ETENDU'
  | 'USAGE_CONSTANT_SECTEUR'
  | 'AUCUN';

/**
 * Secteur d'activité, aligné EXACTEMENT sur l'enum backend {@code SecteurChantier}.
 */
export type SecteurChantier = 'BTP' | 'INGENIERIE' | 'AUTRE';

/**
 * Qualification du motif de licenciement (aligné EXACTEMENT sur l'enum backend
 * {@code MotifLicenciement}) :
 *  - FIN_CHANTIER_CRS : fin de chantier = cause réelle et sérieuse (vert).
 *  - MOTIF_NON_FONDE : motif non caractérisé (chantier non achevé / recours
 *    invalide) (rouge).
 */
export type MotifLicenciement = 'FIN_CHANTIER_CRS' | 'MOTIF_NON_FONDE';

/**
 * Verdict global de l'analyse (aligné EXACTEMENT sur l'enum backend
 * {@code VerdictGlobal}) :
 *  - LICENCIEMENT_FONDE : licenciement fondé (vert).
 *  - LICENCIEMENT_A_SECURISER : motif fragile / reclassement non tracé (orange).
 *  - RECOURS_INVALIDE : recours invalide → requalification probable (rouge).
 */
export type CdiChantierVerdictGlobal =
  | 'LICENCIEMENT_FONDE'
  | 'LICENCIEMENT_A_SECURISER'
  | 'RECOURS_INVALIDE';

export interface CdiChantierRequest {
  /** Date d'entrée / début du contrat de chantier (ISO YYYY-MM-DD, requis). */
  dateEntree: string;
  /** Date de notification du licenciement (ISO YYYY-MM-DD, requis). */
  dateRupture: string;
  /** Fondement légal du recours au CDI de chantier (requis). */
  fondementRecours: FondementRecours;
  /** Secteur d'activité (requis). */
  secteur: SecteurChantier;
  /** Le chantier / l'opération est achevé (requis). */
  chantierAcheve: boolean;
  /** Salaire mensuel moyen, base de l'indemnité de licenciement (requis). */
  salaireMensuelMoyen: number;
  /** Proposition de poursuite sur un autre chantier (défaut false). */
  reclassementAutreChantierPropose?: boolean | null;
}

export interface CdiChantierResponse {
  caseFileId: string;
  dateEntree: string;
  dateRupture: string;
  fondementRecours: FondementRecours;
  secteur: SecteurChantier;
  chantierAcheve: boolean;
  salaireMensuelMoyen: number;
  reclassementAutreChantierPropose: boolean;
  /** Le recours au CDI de chantier est valide (accord de branche / usage). */
  recoursValide: boolean;
  /** Explication de la validité du recours (note requalification le cas échéant). */
  motifRecours: string;
  /** Qualification du motif de licenciement. */
  motifLicenciement: MotifLicenciement;
  /** Montant de l'indemnité de licenciement (art. R.1234-2 CT). */
  indemniteLicenciement: number;
  /** Procédure de licenciement de droit commun requise. */
  procedureRequise: boolean;
  /** Verdict global de l'analyse. */
  verdictGlobal: CdiChantierVerdictGlobal;
  country: string;
  baseJuridique: string;
}
