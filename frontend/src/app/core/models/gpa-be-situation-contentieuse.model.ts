/**
 * SF-223-04 : modèles TypeScript de l'outil décisionnel "Situation
 * contentieuse post-GPA — Belgique" (`gpa-be-situation-contentieuse`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-04 (endpoints
 * POST/GET). Vide juridique GPA en Belgique (ni autorisée ni pénalement
 * interdite — à vérifier par avocat belge) : la convention de GPA n'est pas
 * opposable (mater semper certa), la filiation s'établit par les voies de droit
 * commun. Outil de cadrage contentieux, DISTINCT de `adoption-be`. BE-only pur.
 */

/** Lieu où la GPA a été réalisée. */
export type LieuGpa = 'BELGIQUE' | 'ETRANGER';

/** Lien génétique entre le ou les parents intentionnels et l'enfant. */
export type LienGenetique =
  | 'PERE_INTENTIONNEL'
  | 'MERE_INTENTIONNELLE'
  | 'AUCUN'
  | 'LES_DEUX';

/** Verdict de l'analyse (4 niveaux — arbre filiation post-GPA). */
export type GpaBeVerdict =
  | 'FILIATION_PAR_RECONNAISSANCE'
  | 'FILIATION_PAR_ADOPTION_POST_NAISSANCE'
  | 'RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE'
  | 'QUALIFICATION_INCOMPLETE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/gpa-be-situation-contentieuse-analysis`.
 *
 * `gpaRealiseeEnBelgiqueOuEtranger` et `lienGenetiqueParentIntentionnel` sont
 * requis (validation côté backend → 400). Les booléens sont nullables
 * (informatifs / pré-remplissables).
 */
export interface GpaBeRequest {
  gpaRealiseeEnBelgiqueOuEtranger: LieuGpa;
  lienGenetiqueParentIntentionnel: LienGenetique;
  acteNaissanceEtrangerEtabli: boolean | null;
  merePorteuseDesignee: boolean | null;
  consentementMerePorteuse: boolean | null;
  coupleIntentionnelMarieOuCohabitant: boolean | null;
  commentaire: string | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, chemin contentieux, risques,
 * bases juridiques, messages).
 */
export interface GpaBeResponse extends GpaBeRequest {
  caseFileId: string;
  verdict: GpaBeVerdict;
  cheminContentieux: string[];
  risques: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
