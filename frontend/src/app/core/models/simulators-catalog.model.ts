/**
 * F-163 / SF-163-01 — Modèle de réponse de l'endpoint
 * `GET /api/v1/simulators` qui liste les outils décisionnels
 * disponibles pour le workspace courant (domaine + pays).
 *
 * Contrat figé avec la SF backend `feat/SF-163-01-page-simulateurs-backend`.
 *
 * - `legalDomain` : domaine juridique du workspace courant (peut être null
 *   si non renseigné — la page affichera alors une bannière info + CTA).
 * - `country` : pays du workspace courant (FRANCE / BELGIQUE / null).
 * - `toolIds` : liste dédupliquée des `tool_id` issus de
 *   `decision_tool_visibility_rules.findForDomainAndCountry()`.
 *
 * Codes possibles `legalDomain` : `DROIT_DU_TRAVAIL`, `DROIT_FAMILLE`,
 * `DROIT_IMMIGRATION` (ou null).
 */
export interface SimulatorsCatalogResponse {
  legalDomain: string | null;
  country: string | null;
  toolIds: string[];
}
