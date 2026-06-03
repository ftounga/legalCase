/**
 * SF-223-01 : modèles TypeScript de l'outil décisionnel "Régime de la
 * cohabitation légale en Belgique" (`cohabitation-legale-be`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-01 (endpoints
 * POST/GET — loi du 23/11/1998 ; CC art. 1475-1479 — à vérifier par avocat
 * belge, renumérotation CC post-réformes 2017-2019). ≠ PACS français
 * (F-FA-12) et ≠ cohabitation de fait (P4 F-224).
 */

/** Vue analysée (outil multi-vues unique). */
export type VueCohabitationLegaleBe = 'FORMATION' | 'EFFETS' | 'DISSOLUTION';

/** Verdict de l'analyse. */
export type CohabitationLegaleBeVerdict =
  | 'FORMATION_VALIDE'
  | 'FORMATION_IMPOSSIBLE'
  | 'EFFETS_QUALIFIES'
  | 'DISSOLUTION_QUALIFIEE';

/** Mode de dissolution envisagé (CC art. 1476). */
export type ModeDissolutionCohabitationLegaleBe =
  | 'DECLARATION_COMMUNE'
  | 'DECLARATION_UNILATERALE'
  | 'MARIAGE'
  | 'DECES';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/cohabitation-legale-be-analysis`.
 *
 * `modeDissolutionEnvisage` est requis uniquement si `vue = DISSOLUTION`
 * (validation côté backend → 400). `logementFamilialEnJeu` nullable (pertinent
 * surtout pour la vue EFFETS).
 */
export interface CohabitationLegaleBeRequest {
  vue: VueCohabitationLegaleBe;
  deuxPersonnesNonMariees: boolean | null;
  capaciteJuridique: boolean | null;
  pasDejaLieParMariageOuAutreCohabitation: boolean | null;
  domicileCommun: boolean | null;
  logementFamilialEnJeu: boolean | null;
  modeDissolutionEnvisage: ModeDissolutionCohabitationLegaleBe | null;
  commentaire: string | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, conditions, actes à produire,
 * bases juridiques, messages).
 */
export interface CohabitationLegaleBeResponse extends CohabitationLegaleBeRequest {
  caseFileId: string;
  verdict: CohabitationLegaleBeVerdict;
  conditions: string[];
  actesAProduire: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
