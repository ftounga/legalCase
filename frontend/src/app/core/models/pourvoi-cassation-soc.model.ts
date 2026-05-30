/**
 * SF-218-06 : modèles miroirs du contrat API (backend SF-218-05) pour l'outil
 * décisionnel « Pourvoi en cassation chambre sociale »
 * (F-DT-87-pourvoi-cassation-soc). FRANCE uniquement — pourvoi en cassation
 * d'un arrêt de la chambre sociale de la Cour d'appel devant la Cour de
 * cassation (art. 901 et s. CPC ; art. 604 CPC ; art. 612 CPC ; art. 973 CPC ;
 * art. 1014 CPC — procédure de non-admission / filtre NPC).
 *
 * Analyse des cas d'ouverture (violation de la loi, défaut de base légale,
 * dénaturation…), calcul du délai de pourvoi de 2 mois (date limite + jours
 * restants + verdict délai), évaluation du risque de non-admission (filtre NPC)
 * et verdict global d'opportunité.
 *
 * Pattern miroir de {@link AppelCphResponse} (F-DT-86, SF-218-02).
 */

/** Les 7 cas d'ouverture classiques du pourvoi en cassation (enum backend). */
export type CasOuverturePourvoi =
  | 'VIOLATION_LOI'
  | 'DEFAUT_BASE_LEGALE'
  | 'DENATURATION'
  | 'MANQUE_DE_BASE'
  | 'CONTRADICTION_MOTIFS'
  | 'VICE_FORME'
  | 'PERTE_FONDEMENT_JURIDIQUE';

/**
 * Verdict délai de pourvoi (art. 612 CPC — 2 mois) :
 *  - DELAI_OUVERT : délai de 2 mois en cours, plus de 14 jours restants.
 *  - DELAI_URGENT : 1 à 14 jours restants — pourvoi à former sans délai.
 *  - DELAI_EXPIRE : délai de 2 mois expiré — pourvoi irrecevable.
 */
export type PourvoiVerdictDelai =
  | 'DELAI_OUVERT'
  | 'DELAI_URGENT'
  | 'DELAI_EXPIRE';

/**
 * Verdict global d'opportunité du pourvoi (aligné EXACTEMENT sur l'enum
 * backend {@code PourvoiCassationSocVerdict}) :
 *  - POURVOI_RECOMMANDE : moyen sérieux identifié + cas d'ouverture solide,
 *    risque de non-admission faible.
 *  - POURVOI_RISQUE : pourvoi recevable mais risque de non-admission (NPC).
 *  - POURVOI_DECONSEILLE : aucun cas d'ouverture solide, pourvoi voué à l'échec.
 *  - DELAI_EXPIRE : délai de 2 mois expiré — pourvoi irrecevable.
 */
export type PourvoiCassationSocVerdict =
  | 'POURVOI_RECOMMANDE'
  | 'POURVOI_RISQUE'
  | 'POURVOI_DECONSEILLE'
  | 'DELAI_EXPIRE';

/** Niveau de risque de non-admission (filtre NPC, art. 1014 CPC). */
export type RisqueNonAdmission = 'ELEVE' | 'MODERE' | 'FAIBLE';

/** Force probatoire d'un cas d'ouverture (étendue du contrôle de cassation). */
export type ForceProbatoireCas = 'FORTE' | 'MOYENNE' | 'FAIBLE';

/**
 * Résultat d'analyse d'un cas d'ouverture — miroir de
 * {@code CasOuvertureAnalyse} backend.
 */
export interface CasOuvertureAnalyse {
  cas: CasOuverturePourvoi;
  libelle: string;
  baseJuridique: string;
  forceProbatoire: ForceProbatoireCas;
}

export interface PourvoiCassationSocRequest {
  dateNotificationArret: string; // YYYY-MM-DD — requis (notification de l'arrêt CA)
  casOuverture: CasOuverturePourvoi[]; // ≥ 1 cas d'ouverture invoqué
  representationAvocatCassation: boolean; // avocat aux Conseils constitué
  moyenSerieuxIdentifie: boolean; // appréciation du caractère sérieux du moyen
}

export interface PourvoiCassationSocResponse {
  caseFileId: string;
  dateNotificationArret: string;
  casOuverture: CasOuverturePourvoi[];
  representationAvocatCassation: boolean;
  moyenSerieuxIdentifie: boolean;
  dateLimitePourvoi: string; // YYYY-MM-DD (notification + 2 mois)
  joursRestants: number; // jours calendaires restants (négatif si expiré)
  verdictDelai: PourvoiVerdictDelai;
  verdict: PourvoiCassationSocVerdict;
  risqueNonAdmission: RisqueNonAdmission;
  casOuvertureAnalyses: CasOuvertureAnalyse[];
  /** Item bloquant représentation avocat aux Conseils (art. 973 CPC), null si OK. */
  itemBloquantRepresentation: string | null;
  country: string;
  baseJuridique: string;
}
