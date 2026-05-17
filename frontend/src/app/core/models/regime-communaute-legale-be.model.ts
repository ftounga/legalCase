/**
 * F-217 SF-217-03 : modèles TypeScript de l'outil décisionnel
 * "Régime de communauté légale (Belgique)" (`regime-mat-be-communaute-legale`).
 * BELGIQUE uniquement.
 *
 * Contrat API importé de SF-217-01 (backend, endpoints POST/GET figés —
 * Livre 3 du Code civil belge, loi du 22/07/2018).
 */

/** Verdict global de composition du patrimoine du couple. */
export type RegimeCommunauteLegaleBeVerdict =
  | 'COMMUNAUTE_LEGALE_APPLICABLE'
  | 'REGIME_CONVENTIONNEL_DETECTE'
  | 'QUALIFICATION_INCOMPLETE';

/** Catégorie d'un bien soumis à qualification. */
export type BienCategorieBe =
  | 'IMMOBILIER'
  | 'MOBILIER'
  | 'FINANCIER'
  | 'PROFESSIONNEL'
  | 'AUTRE';

/** Qualification d'un bien (résultat calculé). */
export type QualificationBienBe =
  | 'COMMUN'
  | 'PROPRE'
  | 'MIXTE_RECOMPENSE'
  | 'INDETERMINE';

/** Mode de gestion applicable à un bien (résultat calculé). */
export type ModeGestionBe =
  | 'GESTION_CONCURRENTE'
  | 'GESTION_EXCLUSIVE'
  | 'COGESTION';

/** Qualification d'une dette (résultat calculé). */
export type QualificationDetteBe =
  | 'DETTE_COMMUNE'
  | 'DETTE_PROPRE'
  | 'DETTE_PROPRE_AVEC_RECOURS';

/** Bien saisi par l'avocat — critères de qualification (input). */
export interface BienBeInput {
  libelle: string;
  categorie: BienCategorieBe;
  acquisAvantMariage: boolean;
  acquisParDonationOuSuccession: boolean;
  financeParFondsPropres: boolean;
  affectationProfessionnelle: boolean;
  outilDeTravailPersonnel: boolean;
  commentaire: string | null;
}

/** Dette saisie par l'avocat — critères de qualification (input). */
export interface DetteBeInput {
  libelle: string;
  anterieureAuMariage: boolean;
  contracteeDansLInteretDuMenage: boolean;
  contracteeParUnSeulEpoux: boolean;
  commentaire: string | null;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/regime-mat-be-communaute-legale`.
 * `dateMariage` au format ISO `YYYY-MM-DD`, non future.
 */
export interface RegimeCommunauteLegaleBeRequest {
  dateMariage: string;
  contratMariageSigne: boolean;
  biens: BienBeInput[];
  dettes: DetteBeInput[];
}

/** Bien qualifié par le calculateur (élément de `biensQualifies`). */
export interface BienQualifieBe {
  libelle: string;
  qualification: QualificationBienBe;
  modeGestion: ModeGestionBe;
  fondement: string;
  explication: string;
}

/** Dette qualifiée par le calculateur (élément de `dettesQualifiees`). */
export interface DetteQualifieeBe {
  libelle: string;
  qualification: QualificationDetteBe;
  fondement: string;
  explication: string;
}

/** Synthèse des comptes de composition du patrimoine. */
export interface SyntheseCompositionBe {
  nbBiensCommuns: number;
  nbBiensPropres: number;
  nbBiensMixtesAvecRecompense: number;
  nbDettesCommunes: number;
  nbDettesPropres: number;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot d'inputs (hérité de
 * `RegimeCommunauteLegaleBeRequest`) pour permettre la ré-édition du formulaire
 * après rechargement, **plus** les champs calculés (verdict, biens/dettes
 * qualifiés, synthèse, bases juridiques).
 */
export interface RegimeCommunauteLegaleBeResponse
  extends RegimeCommunauteLegaleBeRequest {
  caseFileId: string;
  verdict: RegimeCommunauteLegaleBeVerdict;
  biensQualifies: BienQualifieBe[];
  dettesQualifiees: DetteQualifieeBe[];
  syntheseComposition: SyntheseCompositionBe;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
