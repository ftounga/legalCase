/**
 * SF-217-05 : modèles TypeScript de l'outil décisionnel "Autorité parentale
 * (Belgique)" (Vague 2 Famille BE — CC art. 374-375). BELGIQUE uniquement.
 *
 * Contrat API importé de SF-217-04 (backend, endpoints POST/GET figés).
 */

/** Verdict de qualification du régime d'autorité parentale. */
export type AutoriteParentaleBeVerdict =
  | 'AUTORITE_CONJOINTE'
  | 'AUTORITE_EXCLUSIVE_FONDEE'
  | 'AUTORITE_EXCLUSIVE_NON_FONDEE'
  | 'QUALIFICATION_INCOMPLETE';

/** Voie procédurale recommandée. */
export type VoieProceduraleApBe =
  | 'AUCUNE_AUTORITE_CONJOINTE_DROIT'
  | 'ACCORD_HOMOLOGUE_TF'
  | 'REQUETE_TRIBUNAL_FAMILLE'
  | 'ETABLISSEMENT_FILIATION_PREALABLE';

/** Mode d'hébergement principal de l'enfant. */
export type ModeHebergementBe =
  | 'HEBERGEMENT_EGALITAIRE'
  | 'HEBERGEMENT_PRINCIPAL_UN_PARENT'
  | 'HEBERGEMENT_NON_FIXE';

/** Code structuré d'un facteur retenu. */
export type FacteurApBeCode =
  | 'DESINTERET_DURABLE'
  | 'MISE_EN_DANGER'
  | 'INCAPACITE_PARENT'
  | 'ACCORD_PARENTAL'
  | 'DECISION_ANTERIEURE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/autorite-parentale-be`.
 * Tous les booléens sont obligatoires. Commentaire nullable (max 1000).
 */
export interface AutoriteParentaleBeRequest {
  filiationEtablieDeuxParents: boolean;
  accordParentalExiste: boolean;
  demandeAutoriteExclusive: boolean;
  desinteretDurableParent: boolean;
  miseEnDangerEnfant: boolean;
  incapaciteParent: boolean;
  decisionJudiciaireAnterieure: boolean;
  modeHebergementPrincipal: ModeHebergementBe;
  commentaire: string | null;
}

/** Facteur retenu (élément de `facteurs`). */
export interface FacteurApBe {
  code: FacteurApBeCode;
  libelle: string;
  fondement: string;
  favorableExclusive: boolean;
  explication: string;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs (hérité de
 * `AutoriteParentaleBeRequest`) pour permettre la ré-édition du formulaire
 * après rechargement, **plus** les champs calculés.
 */
export interface AutoriteParentaleBeResponse extends AutoriteParentaleBeRequest {
  caseFileId: string;
  verdict: AutoriteParentaleBeVerdict;
  voieProcedurale: VoieProceduraleApBe;
  facteurs: FacteurApBe[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
