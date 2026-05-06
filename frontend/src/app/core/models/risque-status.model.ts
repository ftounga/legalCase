/**
 * F-195 SF-195-02 — Statut décidé par l'avocat sur un risque listé par la
 * synthèse IA dans le bloc "Risques". Trichotomie cohérente avec F-176 et
 * F-194 (pièces) :
 *
 * <ul>
 *   <li><b>A_CREUSER</b> : risque à approfondir (statut implicite par défaut
 *       tant que l'avocat n'a pas tranché — visuel navy/or).</li>
 *   <li><b>VALIDE</b>    : risque confirmé par l'avocat. Au prochain run de
 *       Synthèse enrichie, l'IA reçoit la consigne d'approfondir et certains
 *       outils décisionnels reçoivent un pré-flag (ex. risque "harcèlement"
 *       validé → flag F-DT-12).</li>
 *   <li><b>ECARTE</b>    : risque écarté par l'avocat (raison libre
 *       optionnelle). Cohérent F-176 statut DISCARDED gris discret. Au
 *       prochain run, l'IA reçoit la consigne de NE PAS re-proposer.</li>
 * </ul>
 *
 * <p>Cohérence F-176 stricte : le PUT statut est un acte pur côté backend (pas
 * de side-effect, pas de recompute). La matérialisation risque → outil et le
 * recompute du `score_risque_avocat` ne se font qu'au prochain run de Synthèse
 * enrichie via l'event SSE {@code ENRICHED_ANALYSIS DONE}.</p>
 */
export type RisqueStatutValue = 'A_CREUSER' | 'VALIDE' | 'ECARTE';

/**
 * Body envoyé au PUT /api/v1/case-files/{id}/risques/status.
 * <p>{@code risqueLibelleOriginal} = libellé exact tel qu'affiché par la
 * synthèse (issu de l'IA). Sert de clé d'upsert côté backend.</p>
 */
export interface RisqueStatusPayload {
  risqueLibelleOriginal: string;
  statut: RisqueStatutValue;
  raisonEcarte?: string | null;
}

/**
 * Réponse 200 du PUT, miroir du DTO backend `RisqueStatusResponse`.
 * Contient l'entrée upsertée (pour rafraîchir le signal local côté UI).
 */
export interface RisqueStatus {
  risqueLibelleOriginal: string;
  statut: RisqueStatutValue;
  raisonEcarte?: string | null;
  updatedAt?: string | null;
}
