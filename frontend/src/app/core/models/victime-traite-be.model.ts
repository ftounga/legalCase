/**
 * SF-221-06 — modèles miroirs du contrat API (backend) pour l'outil décisionnel
 * « Titre de séjour victime de la traite des êtres humains (BE) »
 * (F-IM-58-victime-traite-be).
 *
 * BELGIQUE uniquement — évalue l'éligibilité au titre de séjour spécifique victime de la
 * traite (coopération judiciaire + rupture avec le réseau + accompagnement par un centre
 * spécialisé agréé PAG-ASA / Sürya / Payoke ; art. 61/2 et s. Loi 15/12/1980 ; circulaire
 * du 26/09/2008) et situe l'étape de la procédure.
 *
 * Régime BE PROPRE (3 phases : délai de réflexion → titre temporaire → titre lié à la
 * procédure pénale), DISTINCT du pendant FR F-IM-35 (L. 425-1 CESEDA).
 */

/**
 * Phase de la procédure (whitelist alignée backend `VictimeTraiteBePhase`).
 */
export type VictimeTraiteBePhase =
  | 'REFLEXION_45J'
  | 'DECLARATION_FAITE'
  | 'PROCEDURE_PENALE_EN_COURS'
  | 'AUCUNE';

/**
 * Verdict de l'analyse :
 *  - DELAI_REFLEXION                : délai de réflexion (~45 j) avant déclaration (bleu info)
 *  - ELIGIBLE_TITRE_TEMPORAIRE      : rupture + accompagnement + déclaration faite/pénale (vert)
 *  - ELIGIBLE_SOUS_PROCEDURE_PENALE : coopération + procédure pénale en cours (vert)
 *  - CONDITIONS_NON_REUNIES         : pas de rupture OU pas d'accompagnement (orange)
 *  - A_ORIENTER_CENTRE              : aucune démarche → orienter vers un centre (bleu info)
 */
export type VictimeTraiteBeVerdict =
  | 'DELAI_REFLEXION'
  | 'ELIGIBLE_TITRE_TEMPORAIRE'
  | 'ELIGIBLE_SOUS_PROCEDURE_PENALE'
  | 'CONDITIONS_NON_REUNIES'
  | 'A_ORIENTER_CENTRE';

export interface VictimeTraiteBeRequest {
  phaseProcedure: VictimeTraiteBePhase;
  ruptureAvecReseau: boolean;
  cooperationJudiciaire: boolean;
  accompagnementCentreSpecialise: boolean;
  /** Date de début de l'accompagnement (ISO yyyy-MM-dd, non future) ou null. */
  dateDebutAccompagnement: string | null;
}

export interface VictimeTraiteBeResponse {
  caseFileId: string;
  phaseProcedure: VictimeTraiteBePhase;
  ruptureAvecReseau: boolean;
  cooperationJudiciaire: boolean;
  accompagnementCentreSpecialise: boolean;
  dateDebutAccompagnement: string | null;
  verdict: VictimeTraiteBeVerdict;
  /** Libellé de l'étape de la procédure. */
  etapeProcedure: string;
  basesJuridiques: string[];
  messages: string[];
}

/** Centres spécialisés agréés vers lesquels orienter la victime (référentiel BE). */
export const VICTIME_TRAITE_BE_CENTRES =
  'PAG-ASA (Bruxelles), Sürya (Wallonie), Payoke (Flandre)';
