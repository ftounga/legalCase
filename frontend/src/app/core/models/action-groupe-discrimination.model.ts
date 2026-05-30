/**
 * SF-218-10 : modèles miroirs du contrat API (backend SF-218-09) pour l'outil
 * décisionnel « Action de groupe en discrimination » (F-DT-90-action-groupe-discrimination).
 * FRANCE uniquement — analyse de la recevabilité d'une action de groupe en
 * discrimination au travail : qualité de l'organisation habilitée (L. 1134-7),
 * mise en demeure préalable + délai de carence de 6 mois (L. 1134-9), pluralité
 * de situations similaires, et checklist procédurale.
 *
 * Pattern miroir de {@code SaisieRemunerationResponse} (F-DT-89, SF-218-08).
 */

/**
 * Type d'organisation exerçant l'action de groupe (L. 1134-7) :
 *  - SYNDICAT_REPRESENTATIF : syndicat représentatif (qualité à agir OK).
 *  - ASSOCIATION_AGREEE_5ANS : association régulièrement déclarée depuis ≥ 5 ans
 *    (qualité à agir OK).
 *  - AUTRE : autre entité — non habilitée → IRRECEVABLE_QUALITE.
 */
export type TypeOrganisationActionGroupe =
  | 'SYNDICAT_REPRESENTATIF'
  | 'ASSOCIATION_AGREEE_5ANS'
  | 'AUTRE';

/** Critères de discrimination prohibés (L. 1132-1). */
export type MotifDiscrimination =
  | 'ORIGINE'
  | 'SEXE'
  | 'AGE'
  | 'HANDICAP'
  | 'ETAT_SANTE'
  | 'GROSSESSE'
  | 'ACTIVITE_SYNDICALE'
  | 'RELIGION'
  | 'ORIENTATION_SEXUELLE'
  | 'AUTRE';

/** Objet de l'action de groupe (L. 1134-8). */
export type ObjetActionGroupe =
  | 'CESSATION_MANQUEMENT'
  | 'REPARATION_PREJUDICES'
  | 'LES_DEUX';

/**
 * Verdict de recevabilité (aligné EXACTEMENT sur l'enum backend) :
 *  - RECEVABLE : qualité à agir + pluralité + délai de carence respecté.
 *  - PREMATURE : mise en demeure faite mais délai de 6 mois non écoulé.
 *  - IRRECEVABLE_QUALITE : organisation non habilitée à exercer l'action.
 *  - INFO_MANQUANTE : mise en demeure absente — recevabilité non vérifiable.
 */
export type ActionGroupeDiscriminationVerdict =
  | 'RECEVABLE'
  | 'PREMATURE'
  | 'IRRECEVABLE_QUALITE'
  | 'INFO_MANQUANTE';

/**
 * Item de la checklist procédurale (miroir de {@code ChecklistItem} backend).
 * Chaque item porte son libellé, son caractère obligatoire / bloquant et sa
 * base juridique.
 */
export interface ActionGroupeChecklistItem {
  /** Libellé de la condition procédurale. */
  libelle: string;
  /** True si la condition est obligatoire pour la recevabilité. */
  obligatoire: boolean;
  /** True si la condition n'est pas remplie et bloque la recevabilité. */
  bloquant: boolean;
  /** Base juridique de la condition (article du Code du travail). */
  baseJuridique: string;
}

export interface ActionGroupeDiscriminationRequest {
  typeOrganisation: TypeOrganisationActionGroupe;
  /** Date de la mise en demeure de l'employeur (ISO YYYY-MM-DD) ; optionnelle. */
  dateMiseEnDemeure: string | null;
  motifDiscrimination: MotifDiscrimination;
  /** Pluralité de candidats/salariés placés dans une situation similaire (≥ 1). */
  nombrePersonnesConcernees: number;
  objetAction: ObjetActionGroupe;
}

export interface ActionGroupeDiscriminationResponse {
  caseFileId: string;
  typeOrganisation: TypeOrganisationActionGroupe;
  dateMiseEnDemeure: string | null;
  motifDiscrimination: MotifDiscrimination;
  nombrePersonnesConcernees: number;
  objetAction: ObjetActionGroupe;
  verdict: ActionGroupeDiscriminationVerdict;
  /** Qualité à agir de l'organisation (L. 1134-7). */
  qualiteAAgir: boolean;
  /** Pluralité de situations établie (nombrePersonnesConcernees ≥ 2). */
  pluraliteEtablie: boolean;
  /** Date à partir de laquelle la saisine est recevable (mise en demeure + 6 mois). */
  dateRecevabiliteSaisine: string | null;
  /** True si le délai de carence de 6 mois est écoulé (aujourd'hui ≥ recevabilité). */
  delaiCarenceRespecte: boolean;
  /** Checklist procédurale (L. 1134-7 à L. 1134-10). */
  checklist: ActionGroupeChecklistItem[];
  country: string;
  baseJuridique: string;
}
