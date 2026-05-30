/**
 * SF-218-20 : modèles miroirs du contrat API (backend SF-218-19) pour l'outil
 * décisionnel « Cadre dirigeant : qualification (3 critères cumulatifs) »
 * (F-DT-107-cadre-dirigeant-statut). FRANCE uniquement.
 *
 * Qualification de cadre dirigeant (art. L.3111-2 CT) : vérification des trois
 * critères cumulatifs dégagés par la jurisprudence (grande indépendance dans
 * l'organisation de l'emploi du temps ; habilitation à prendre des décisions de
 * façon largement autonome ; rémunération parmi les plus élevées de
 * l'entreprise), complétée par l'indice de participation effective à la
 * direction de l'entreprise (Cass. soc. post-2012). Détermine l'exclusion (ou
 * non) des règles de durée du travail et le risque de rappel d'heures
 * supplémentaires en cas de requalification.
 */

/**
 * Verdict de qualification (aligné EXACTEMENT sur l'enum backend
 * {@code Qualification}) :
 *  - CADRE_DIRIGEANT : 3 critères remplis + participation effective à la
 *    direction → exclusion de la durée du travail confirmée.
 *  - CADRE_DIRIGEANT_FRAGILE : 3 critères remplis MAIS sans participation
 *    effective à la direction → risque de requalification.
 *  - NON_CADRE_DIRIGEANT : moins de 3 critères → soumis aux règles de durée du
 *    travail (rappel d'heures supplémentaires possible).
 */
export type Qualification =
  | 'CADRE_DIRIGEANT'
  | 'CADRE_DIRIGEANT_FRAGILE'
  | 'NON_CADRE_DIRIGEANT';

/**
 * Conséquence sur les règles de durée du travail (aligné EXACTEMENT sur l'enum
 * backend {@code ExclusionDureeTravail}).
 */
export type ExclusionDureeTravail = 'EXCLU' | 'NON_EXCLU';

/**
 * Niveau de risque de rappel d'heures supplémentaires en cas de contestation /
 * requalification (aligné EXACTEMENT sur l'enum backend
 * {@code RisqueRappelHeuresSupp}) :
 *  - FAIBLE : CADRE_DIRIGEANT.
 *  - MODERE : CADRE_DIRIGEANT_FRAGILE.
 *  - ELEVE : NON_CADRE_DIRIGEANT.
 */
export type RisqueRappelHeuresSupp = 'FAIBLE' | 'MODERE' | 'ELEVE';

/**
 * Détail d'un des 3 critères cumulatifs évalués (aligné EXACTEMENT sur le DTO
 * backend {@code CritereCadreDirigeant}).
 */
export interface CritereCadreDirigeant {
  /** Libellé du critère (art. L.3111-2 CT). */
  critere: string;
  /** Critère rempli ou non. */
  rempli: boolean;
  /** Commentaire explicatif (jurisprudence / preuve). */
  commentaire: string;
}

export interface CadreDirigeantStatutRequest {
  /** Critère 1 : grande indépendance dans l'organisation de l'emploi du temps. */
  independanceEmploiDuTemps: boolean;
  /** Critère 2 : habilité à prendre des décisions de façon largement autonome. */
  autonomieDecision: boolean;
  /** Critère 3 : rémunération parmi les plus élevées de l'entreprise. */
  remunerationParmiPlusElevees: boolean;
  /** Indice complémentaire : participation effective à la direction (Cass. soc.). */
  participationDirectionEntreprise: boolean;
  /** Rémunération mensuelle constatée (élément de preuve, optionnel). */
  niveauRemunerationConstate?: number | null;
}

export interface CadreDirigeantStatutResponse {
  caseFileId: string;
  independanceEmploiDuTemps: boolean;
  autonomieDecision: boolean;
  remunerationParmiPlusElevees: boolean;
  participationDirectionEntreprise: boolean;
  niveauRemunerationConstate?: number | null;
  /** Détail des 3 critères cumulatifs (libellé + rempli + commentaire). */
  criteres: CritereCadreDirigeant[];
  /** Nombre de critères remplis (0..3). */
  criteresRemplis: number;
  /** Verdict de qualification. */
  qualification: Qualification;
  /** Conséquence sur les règles de durée du travail. */
  exclusionDureeTravail: ExclusionDureeTravail;
  /** Niveau de risque de rappel d'heures supplémentaires. */
  risqueRappelHeuresSupp: RisqueRappelHeuresSupp;
  country: string;
  baseJuridique: string;
}
