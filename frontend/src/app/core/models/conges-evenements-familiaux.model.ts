/**
 * SF-218-44 : modèles miroirs du contrat API (backend SF-218-43) pour l'outil
 * décisionnel « Congés pour évènements familiaux »
 * (F-DT-76-conges-evenements-familiaux). FRANCE uniquement.
 *
 * Détermine la durée de congé applicable pour un évènement familial (art.
 * L.3142-1 à L.3142-5 CT) : durées légales minimales (L.3142-4), durée
 * conventionnelle plus favorable retenue le cas échéant (L.3142-5), maintien
 * intégral du salaire (assimilation à du temps de travail effectif).
 *
 * Distinct du congé de paternité/maternité (F-212) et du congé parental
 * d'éducation (F-DT-78) — invariant « un outil = une situation ».
 */

/**
 * Nature de l'évènement familial (aligné EXACTEMENT sur l'enum backend
 * {@code CongesEvenementsFamiliauxTypeEvenement}).
 */
export type CongesEvenementsFamiliauxTypeEvenement =
  | 'MARIAGE_PACS'
  | 'NAISSANCE'
  | 'DECES_ENFANT'
  | 'DECES_CONJOINT_PARTENAIRE'
  | 'DECES_PERE_MERE'
  | 'ANNONCE_HANDICAP_ENFANT'
  | 'DEMENAGEMENT_NON_LEGAL';

/**
 * Base de calcul de la durée retenue (aligné sur l'enum backend
 * {@code CongesEvenementsFamiliauxBase}) :
 *  - LEGALE : durée légale minimale (L.3142-4).
 *  - CONVENTIONNELLE : durée conventionnelle plus favorable (L.3142-5).
 */
export type CongesEvenementsFamiliauxBase = 'LEGALE' | 'CONVENTIONNELLE';

export interface CongesEvenementsFamiliauxRequest {
  /** Nature de l'évènement familial. */
  typeEvenement: CongesEvenementsFamiliauxTypeEvenement;
  /** La convention collective prévoit une durée plus favorable que la loi. */
  conventionPlusFavorable: boolean;
  /** Durée conventionnelle (jours), requise si conventionPlusFavorable = true. */
  dureeConventionnelleJours: number | null;
}

export interface CongesEvenementsFamiliauxResponse {
  caseFileId: string;
  typeEvenement: CongesEvenementsFamiliauxTypeEvenement;
  conventionPlusFavorable: boolean;
  dureeConventionnelleJours: number | null;
  /** Durée légale minimale (L.3142-4) pour cet évènement. */
  dureeLegaleJours: number;
  /** Durée de congé applicable (la plus favorable). */
  dureeApplicableJours: number;
  /** Base de calcul retenue. */
  base: CongesEvenementsFamiliauxBase;
  /** Maintien intégral du salaire (toujours true — temps de travail effectif). */
  maintienSalaire: boolean;
  /** Assimilation à du temps de travail effectif (pas de réduction CP). */
  assimileTempsTravailEffectif: boolean;
  /** Durée légale majorée possible (décès d'enfant : 7 jours ouvrés). */
  dureeMajoreePossible: boolean;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
