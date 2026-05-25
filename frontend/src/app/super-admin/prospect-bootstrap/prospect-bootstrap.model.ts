/**
 * F-251 SF-251-04 — Modèles TypeScript pour le bootstrap prospect.
 *
 * <p>Strictement alignés sur le contrat API figé par SF-251-03
 * ({@code POST /api/v1/super-admin/prospect-bootstrap}).</p>
 */

/** Pays cible — V1 limitée à FRANCE et BELGIQUE. */
export type ProspectCountry = 'FRANCE' | 'BELGIQUE';

/** Domaine juridique cible — V1 limitée à 3 valeurs. */
export type ProspectLegalDomain =
  | 'DROIT_DU_TRAVAIL'
  | 'DROIT_IMMOBILIER'
  | 'DROIT_DE_LA_FAMILLE';

/**
 * Payload du POST `/api/v1/super-admin/prospect-bootstrap` (SF-251-03).
 *
 * <p>Tous les champs sont obligatoires côté backend. La validation côté
 * frontend reproduit les bornes du contrat (email, longueur, password ≥ 8).</p>
 */
export interface ProspectBootstrapRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  country: ProspectCountry;
  legalDomain: ProspectLegalDomain;
  workspaceName: string;
}

/**
 * Réponse 201 du POST `/api/v1/super-admin/prospect-bootstrap` (SF-251-03).
 *
 * <p>{@code expiresAt} est une date ISO-8601 UTC indiquant la fin de la
 * période d'évaluation provisionnée. Le frontend la reformate en
 * {@code dd/MM/yyyy} pour l'affichage opérateur.</p>
 */
export interface ProspectBootstrapResponse {
  userId: string;
  workspaceId: string;
  workspaceName: string;
  expiresAt: string;
}

/**
 * Forme typée de l'erreur HTTP renvoyée par le backend (cohérent avec le
 * {@code GlobalExceptionHandler} et la convention SF-251-03 pour les 400 / 409
 * qui transmettent {@code message} + (optionnel) {@code field} ou
 * {@code userId}).
 */
export interface ProspectBootstrapErrorBody {
  message?: string;
  field?: keyof ProspectBootstrapRequest | string;
  userId?: string;
}
