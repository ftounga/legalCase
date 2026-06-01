/**
 * SF-218-34 : modèles miroirs du contrat API (backend SF-218-33) pour l'outil
 * décisionnel « Délégué syndical / RSS : désignation et protection »
 * (F-DT-69-delegation-syndicale-protection). FRANCE uniquement.
 *
 * Analyseur de statut + protection : régularité de la désignation d'un délégué
 * syndical (DS) ou d'un représentant de section syndicale (RSS) (effectif,
 * représentativité de l'organisation, score personnel du candidat pour le DS,
 * art. L.2143-1 et s., L.2142-1-1, L.2143-3 CT) et risque de nullité d'un
 * licenciement de salarié protégé prononcé sans autorisation préalable de
 * l'inspecteur du travail (art. L.2411-3 CT — nullité + réintégration).
 */

/**
 * Type de mandat syndical analysé (aligné EXACTEMENT sur l'enum backend
 * {@code MandatSyndicalType}) :
 *  - DELEGUE_SYNDICAL : délégué syndical désigné par une organisation syndicale
 *    représentative (effectif ≥ 50, score personnel ≥ 10 %, L.2143-1 et s.).
 *  - RSS : représentant de section syndicale désigné par un syndicat NON
 *    représentatif (art. L.2142-1-1), sans condition de score personnel.
 */
export type MandatSyndicalType = 'DELEGUE_SYNDICAL' | 'RSS';

/**
 * Verdict de régularité de la désignation (aligné EXACTEMENT sur l'enum backend
 * {@code DelegationSyndicaleStatutDesignation}) :
 *  - REGULIERE : tous les items conformes.
 *  - IRREGULIERE : item d'effectif ou de représentativité (ou score DS
 *    renseigné < 10 %) non conforme.
 *  - A_VERIFIER : DS sans score personnel renseigné (condition des 10 % à
 *    confirmer).
 */
export type DelegationSyndicaleStatutDesignation = 'REGULIERE' | 'IRREGULIERE' | 'A_VERIFIER';

/**
 * Statut de salarié protégé (aligné EXACTEMENT sur l'enum backend
 * {@code DelegationSyndicaleStatutProtege}). DS et RSS sont, par construction,
 * des salariés protégés → toujours {@code OUI}.
 */
export type DelegationSyndicaleStatutProtege = 'OUI';

/**
 * Niveau de risque de nullité d'un licenciement (aligné EXACTEMENT sur l'enum
 * backend {@code DelegationSyndicaleRisqueNullite}) :
 *  - ELEVE : licenciement envisagé SANS autorisation de l'inspecteur du travail
 *    → nullité + réintégration (art. L.2411-3).
 *  - FAIBLE : licenciement envisagé AVEC autorisation obtenue.
 *  - SANS_OBJET : aucun licenciement envisagé.
 */
export type DelegationSyndicaleRisqueNullite = 'ELEVE' | 'FAIBLE' | 'SANS_OBJET';

/**
 * Détail d'un item de la checklist de régularité de la désignation (aligné
 * EXACTEMENT sur le DTO backend {@code DelegationSyndicaleChecklistItem}).
 */
export interface DelegationSyndicaleChecklistItem {
  /** Libellé de l'item (condition de désignation). */
  item: string;
  /** Item conforme ou non. */
  conforme: boolean;
  /** Commentaire explicatif (base légale / preuve). */
  commentaire: string;
}

export interface DelegationSyndicaleRequest {
  /** Effectif de l'entreprise (requis, > 0). Un DS suppose ≥ 50 salariés. */
  effectif: number;
  /** Type de mandat — DELEGUE_SYNDICAL ou RSS (requis). */
  typeMandat: MandatSyndicalType;
  /** L'organisation désignante est-elle représentative (≥ 10 % CSE) ? */
  syndicatRepresentatif: boolean;
  /** Score personnel du candidat aux dernières élections (0–100, DS uniquement). */
  pourcentageScorePersonnel?: number | null;
  /** Date de désignation (ISO YYYY-MM-DD, optionnelle). */
  dateDesignation?: string | null;
  /** Un licenciement est-il envisagé / engagé ? */
  licenciementEnvisage: boolean;
  /** L'autorisation préalable de l'inspecteur du travail a-t-elle été obtenue ? */
  autorisationInspecteurTravail: boolean;
}

export interface DelegationSyndicaleResponse {
  caseFileId: string;
  effectif: number;
  typeMandat: MandatSyndicalType;
  syndicatRepresentatif: boolean;
  pourcentageScorePersonnel?: number | null;
  dateDesignation?: string | null;
  /** Checklist de régularité de la désignation (item + conforme + commentaire). */
  checklist: DelegationSyndicaleChecklistItem[];
  /** Verdict de régularité de la désignation. */
  statutDesignation: DelegationSyndicaleStatutDesignation;
  /** Statut de salarié protégé (toujours OUI). */
  statutProtege: DelegationSyndicaleStatutProtege;
  licenciementEnvisage: boolean;
  autorisationInspecteurTravail: boolean;
  /** Niveau de risque de nullité du licenciement. */
  risqueNulliteLicenciement: DelegationSyndicaleRisqueNullite;
  /** Conséquences / points de vigilance identifiés. */
  consequences: string[];
  country: string;
  baseJuridique: string;
}
