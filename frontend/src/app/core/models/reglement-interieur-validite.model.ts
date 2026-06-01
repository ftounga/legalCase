/**
 * SF-218-36 : modèles miroirs du contrat API (backend SF-218-35) pour l'outil
 * décisionnel « Règlement intérieur : validité »
 * (F-DT-100-reglement-interieur-validite). FRANCE uniquement.
 *
 * Analyseur de conformité et d'opposabilité d'un règlement intérieur (art.
 * L.1311-1 à L.1322-4, L.1321-1 et s. CT) :
 *  - contenu obligatoire (hygiène/sécurité, discipline, droits de la défense,
 *    harcèlement — L.1321-1 / L.1321-2) ;
 *  - clauses interdites (atteinte aux libertés non justifiée/proportionnée,
 *    sanction pécuniaire — L.1321-3 / L.1331-2) ;
 *  - procédure de mise en place (consultation CSE, transmission inspection,
 *    dépôt greffe CPH — L.1321-4) conditionnant l'opposabilité aux salariés.
 *
 * Distinct de la validité d'une sanction disciplinaire fondée sur le RI et du
 * lanceur d'alerte (F-DT-61).
 */

/**
 * Nature d'un item de la checklist (aligné EXACTEMENT sur l'enum backend
 * {@code ReglementInterieurChecklistType}) :
 *  - OBLIGATOIRE : contenu que le RI doit obligatoirement comporter.
 *  - INTERDIT : clause prohibée — {@code conforme=true} = absence de la clause.
 *  - PROCEDURE : formalité de mise en place conditionnant l'opposabilité.
 */
export type ReglementInterieurChecklistType = 'OBLIGATOIRE' | 'INTERDIT' | 'PROCEDURE';

/**
 * Verdict global de validité (aligné EXACTEMENT sur l'enum backend
 * {@code ReglementInterieurValiditeStatut}) :
 *  - NON_REQUIS : effectif < 50 et aucun RI existant → RI facultatif (L.1311-2).
 *  - CONFORME : contenu obligatoire présent, aucune clause interdite, procédure
 *    de mise en place respectée.
 *  - NON_CONFORME : un contenu obligatoire manque ou une clause interdite est
 *    présente (L.1321-1, L.1321-2, L.1321-3, L.1331-2).
 *  - INOPPOSABLE : procédure de mise en place non respectée → RI inopposable aux
 *    salariés (L.1321-4).
 */
export type ReglementInterieurValiditeStatut =
  | 'CONFORME'
  | 'NON_CONFORME'
  | 'INOPPOSABLE'
  | 'NON_REQUIS';

/**
 * Opposabilité du règlement intérieur aux salariés (aligné EXACTEMENT sur l'enum
 * backend {@code ReglementInterieurOpposabilite}) :
 *  - OPPOSABLE : procédure de mise en place respectée → RI opposable.
 *  - INOPPOSABLE : une formalité manque → RI inopposable (L.1321-4).
 */
export type ReglementInterieurOpposabilite = 'OPPOSABLE' | 'INOPPOSABLE';

/**
 * Item de la checklist de validité (aligné EXACTEMENT sur le DTO backend
 * {@code ReglementInterieurValiditeChecklistItem}).
 */
export interface ReglementInterieurValiditeChecklistItem {
  /** Libellé de l'obligation / interdiction / formalité vérifiée. */
  item: string;
  /**
   * Item satisfait ou non. Pour un item de type {@code INTERDIT},
   * {@code conforme=true} signifie l'absence de la clause interdite.
   */
  conforme: boolean;
  /** Nature de l'item (OBLIGATOIRE / INTERDIT / PROCEDURE). */
  type: ReglementInterieurChecklistType;
  /** Fondement / point de vigilance attaché à l'item. */
  commentaire: string;
}

export interface ReglementInterieurValiditeRequest {
  /** Effectif de l'entreprise (RI obligatoire dès 50 salariés, L.1311-2). */
  effectif: number;
  /** Un règlement intérieur existe. */
  reglementExiste: boolean;
  /** Le RI comporte les mesures d'hygiène et de sécurité (L.1321-1 1°). */
  contenuHygieneSecurite: boolean;
  /** Le RI comporte les règles de discipline / échelle des sanctions (L.1321-1 2°). */
  contenuDiscipline: boolean;
  /** Le RI comporte les dispositions sur les droits de la défense (L.1321-1 3°). */
  contenuDroitsDefense: boolean;
  /** Le RI rappelle harcèlement moral / sexuel / agissements sexistes (L.1321-2). */
  contenuHarcelementAgissements: boolean;
  /** Clause interdite : atteinte aux libertés non justifiée/proportionnée (L.1321-3). */
  clauseAtteinteLibertesNonJustifiee: boolean;
  /** Clause interdite : sanction pécuniaire (L.1331-2). */
  clauseSanctionPecuniaire: boolean;
  /** Le CSE a été consulté avant la mise en place du RI (L.1321-4). */
  consultationCseRealisee: boolean;
  /** Le RI a été transmis à l'inspection du travail (L.1321-4). */
  transmissionInspectionTravail: boolean;
  /** Le RI a été déposé au greffe du conseil de prud'hommes (L.1321-4). */
  depotGreffeCph: boolean;
}

export interface ReglementInterieurValiditeResponse {
  caseFileId: string;
  effectif: number;
  reglementExiste: boolean;
  /** Checklist de validité (contenu obligatoire / clauses interdites / procédure). */
  checklist: ReglementInterieurValiditeChecklistItem[];
  /** Nombre de contenus obligatoires manquants. */
  itemsObligatoiresManquants: number;
  /** Nombre de clauses interdites présentes. */
  clausesInterditesPresentes: number;
  /** Verdict global de validité. */
  statut: ReglementInterieurValiditeStatut;
  /** Opposabilité du RI aux salariés. */
  opposabilite: ReglementInterieurOpposabilite;
  /** Conséquences / points de vigilance identifiés. */
  consequences: string[];
  country: string;
  baseJuridique: string;
}
