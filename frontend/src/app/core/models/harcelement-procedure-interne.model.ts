/**
 * SF-218-28 : modèles miroirs du contrat API (backend SF-218-27) pour l'outil
 * décisionnel « Harcèlement : procédure interne de traitement d'un signalement »
 * (F-DT-59-harcelement-procedure-interne). FRANCE uniquement.
 *
 * Outil de conformité EMPLOYEUR de la procédure interne de traitement d'un
 * signalement de harcèlement (art. L.1153-5-1, L.2314-1, L.1152-4 CT) :
 * désignation d'un référent harcèlement sexuel au CSE (≥ 11 salariés) et d'un
 * référent employeur (≥ 250 salariés), information / affichage des salariés,
 * déclenchement d'une enquête interne contradictoire dans un délai raisonnable,
 * mesures conservatoires. Distinct de F-DT-11 (nullité du licenciement
 * consécutif au harcèlement).
 */

/**
 * Verdict de conformité de la procédure interne (aligné EXACTEMENT sur l'enum
 * backend {@code StatutConformite}) :
 *  - CONFORME : tous les items obligatoires conformes.
 *  - NON_CONFORME : au moins un item obligatoire non conforme (hors enquête).
 *  - CARENCE_GRAVE : signalement reçu et enquête non déclenchée / non
 *    contradictoire / délai NON → manquement à l'obligation de sécurité
 *    (L.4121-1).
 */
export type StatutConformiteHarcelement = 'CONFORME' | 'NON_CONFORME' | 'CARENCE_GRAVE';

/**
 * Caractère raisonnable du délai de réaction de l'employeur (aligné EXACTEMENT
 * sur l'enum backend {@code DelaiRaisonnable}) :
 *  - OUI : délai ≤ 15 jours.
 *  - LIMITE : délai 16–60 jours.
 *  - NON : délai > 60 jours, ou enquête non ouverte alors qu'un signalement a
 *    été reçu.
 */
export type DelaiRaisonnableHarcelement = 'OUI' | 'LIMITE' | 'NON';

/**
 * Niveau de risque de responsabilité de l'employeur (aligné EXACTEMENT sur
 * l'enum backend {@code RisqueResponsabiliteEmployeur}) :
 *  - FAIBLE : CONFORME.
 *  - MODERE : NON_CONFORME.
 *  - ELEVE : CARENCE_GRAVE.
 */
export type RisqueResponsabiliteEmployeur = 'FAIBLE' | 'MODERE' | 'ELEVE';

/**
 * Détail d'un item de la checklist de conformité (aligné EXACTEMENT sur le DTO
 * backend {@code ChecklistItemConformite}).
 */
export interface ChecklistItemConformite {
  /** Libellé de l'item (obligation légale). */
  item: string;
  /** Item conforme ou non. */
  conforme: boolean;
  /** Item obligatoire (vs. recommandé) dans le contexte du dossier. */
  obligatoire: boolean;
  /** Commentaire explicatif (base légale / preuve). */
  commentaire: string;
}

export interface HarcelementProcedureInterneRequest {
  /** Effectif de l'entreprise (≥ 11 → CSE obligatoire avec référent harcèlement). */
  effectif: number;
  /** Référent harcèlement sexuel désigné parmi les membres du CSE (L.2314-1). */
  referentCseDesigne: boolean;
  /** Référent employeur désigné (entreprises ≥ 250, L.1153-5-1). */
  referentEmployeurDesigne: boolean;
  /** Information / affichage des salariés réalisé (L.1152-4, L.1153-5). */
  informationAffichageRealisee: boolean;
  /** Un signalement a été reçu. */
  signalementRecu: boolean;
  /** Date de réception du signalement (ISO YYYY-MM-DD, optionnel). */
  dateSignalement?: string | null;
  /** Date d'ouverture de l'enquête interne (ISO YYYY-MM-DD, optionnel). */
  dateOuvertureEnquete?: string | null;
  /** Enquête menée de façon contradictoire et impartiale. */
  enqueteContradictoire: boolean;
  /** Mesures conservatoires prises durant l'enquête (éloignement, suspension). */
  mesuresConservatoiresPrises: boolean;
}

export interface HarcelementProcedureInterneResponse {
  caseFileId: string;
  effectif: number;
  referentCseDesigne: boolean;
  referentEmployeurDesigne: boolean;
  informationAffichageRealisee: boolean;
  signalementRecu: boolean;
  dateSignalement?: string | null;
  dateOuvertureEnquete?: string | null;
  enqueteContradictoire: boolean;
  mesuresConservatoiresPrises: boolean;
  /** Checklist de conformité (item + conforme + obligatoire + commentaire). */
  checklist: ChecklistItemConformite[];
  /** Nombre d'items obligatoires non conformes. */
  itemsObligatoiresManquants: number;
  /** Verdict de conformité. */
  statut: StatutConformiteHarcelement;
  /** Délai de réaction en jours (si dateSignalement + dateOuvertureEnquete). */
  delaiReactionJours?: number | null;
  /** Caractère raisonnable du délai. */
  delaiRaisonnable: DelaiRaisonnableHarcelement;
  /** Niveau de risque de responsabilité de l'employeur. */
  risqueResponsabiliteEmployeur: RisqueResponsabiliteEmployeur;
  country: string;
  baseJuridique: string;
}
