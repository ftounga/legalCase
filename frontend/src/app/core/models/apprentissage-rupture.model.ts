/**
 * SF-218-24 : modèles miroirs du contrat API (backend SF-218-23) pour l'outil
 * décisionnel « Apprentissage : validité de la rupture du contrat »
 * (F-DT-110-apprentissage-rupture). FRANCE uniquement.
 *
 * Régime hybride du contrat d'apprentissage (art. L.6222-18 et s. CT) :
 * distingue la rupture pendant les 45 premiers jours (libre, sans motif) de la
 * rupture après ce délai (limitée à : accord écrit des parties, faute grave,
 * force majeure, inaptitude médicale, exclusion définitive du CFA), qualifie le
 * motif invoqué et signale les conséquences procédurales (saisine du conseil de
 * prud'hommes, procédure de licenciement, indemnités).
 *
 * Pattern miroir de {@link StagiaireGratificationResponse} (F-DT-109, SF-218-22).
 */

/**
 * Auteur de la rupture du contrat d'apprentissage, aligné EXACTEMENT sur l'enum
 * backend {@code AuteurRupture}.
 */
export type AuteurRupture = 'EMPLOYEUR' | 'APPRENTI';

/**
 * Motif invoqué pour la rupture, aligné EXACTEMENT sur l'enum backend
 * {@code MotifRupture} :
 *  - ACCORD_PARTIES : accord écrit des deux parties (valable après 45 j).
 *  - FAUTE_GRAVE : faute grave de l'apprenti (procédure de licenciement requise).
 *  - FORCE_MAJEURE : cas de force majeure.
 *  - INAPTITUDE : inaptitude médicale constatée par le médecin du travail.
 *  - EXCLUSION_DEFINITIVE_CFA : exclusion définitive du CFA (motif employeur).
 *  - SANS_MOTIF : rupture libre (valable uniquement dans les 45 premiers jours).
 */
export type MotifRupture =
  | 'ACCORD_PARTIES'
  | 'FAUTE_GRAVE'
  | 'FORCE_MAJEURE'
  | 'INAPTITUDE'
  | 'EXCLUSION_DEFINITIVE_CFA'
  | 'SANS_MOTIF';

/**
 * Qualification de la période de rupture par rapport au seuil des 45 premiers
 * jours de formation pratique en entreprise (aligné sur l'enum backend
 * {@code PeriodeRupture}).
 */
export type PeriodeRupture = 'DANS_45_PREMIERS_JOURS' | 'APRES_45_JOURS';

/**
 * Validité du motif de rupture (aligné EXACTEMENT sur l'enum backend
 * {@code ValiditeRupture}) :
 *  - VALIDE : rupture régulière (vert).
 *  - NON_VALIDE : rupture irrégulière, dommages-intérêts possibles (rouge).
 *  - A_SECURISER : motif recevable mais formalité manquante (orange).
 */
export type ValiditeRupture = 'VALIDE' | 'NON_VALIDE' | 'A_SECURISER';

/**
 * Verdict global de l'analyse (aligné EXACTEMENT sur l'enum backend
 * {@code VerdictGlobal}) :
 *  - RUPTURE_REGULIERE : rupture valide.
 *  - RUPTURE_IRREGULIERE : rupture non valide.
 *  - RUPTURE_A_SECURISER : rupture à sécuriser (formalité manquante).
 */
export type VerdictGlobal =
  | 'RUPTURE_REGULIERE'
  | 'RUPTURE_IRREGULIERE'
  | 'RUPTURE_A_SECURISER';

export interface ApprentissageRuptureRequest {
  /** Date de début d'exécution du contrat d'apprentissage (ISO YYYY-MM-DD, requis). */
  dateDebutContrat: string;
  /** Date de la rupture (ISO YYYY-MM-DD, requis). */
  dateRupture: string;
  /** Auteur de la rupture (requis). */
  auteurRupture: AuteurRupture;
  /** Motif invoqué (requis). */
  motifRupture: MotifRupture;
  /** Apprenti majeur (pertinent pour certaines formalités, défaut true). */
  apprentiMajeur?: boolean | null;
}

export interface ApprentissageRuptureResponse {
  caseFileId: string;
  dateDebutContrat: string;
  dateRupture: string;
  auteurRupture: AuteurRupture;
  motifRupture: MotifRupture;
  apprentiMajeur: boolean;
  /** Nombre de jours écoulés entre le début du contrat et la rupture. */
  joursDepuisDebut: number;
  /** Qualification de la période (≤ 45 jours vs après). */
  periode: PeriodeRupture;
  /** Validité du motif de rupture. */
  validite: ValiditeRupture;
  /** Explication de la validité / motif retenu. */
  motif: string;
  /** Conséquences procédurales (saisine CPH, procédure de licenciement, indemnités...). */
  consequences: string[];
  /** Verdict global de l'analyse. */
  verdictGlobal: VerdictGlobal;
  country: string;
  baseJuridique: string;
}
