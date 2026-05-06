/**
 * F-197 SF-197-02 — Override single-value du type de litige (Travail FR) ou
 * du type de procédure (Immigration) décidé manuellement par l'avocat.
 *
 * <p>Un avocat peut surcharger ponctuellement le type détecté par l'IA quand
 * (1) l'IA s'est trompée, (2) l'avocat veut tester un autre angle stratégique
 * (ex. PRISE_ACTE_RUPTURE plutôt que LICENCIEMENT_SANS_CAUSE_REELLE).</p>
 *
 * <p>Cohérence F-176 stricte : le PUT est un acte pur côté backend (pas de
 * recompute, pas de side-effect). Aucun refresh côté frontend après PUT —
 * la propagation outils décisionnels se fait au prochain run de Synthèse
 * enrichie via l'event SSE {@code ENRICHED_ANALYSIS DONE}.</p>
 */

/** Enum Travail FR — 7 valeurs fixées par {@code EnrichedAnalysisService}. */
export type TypeLitigeTravailFrCode =
  | 'LICENCIEMENT_SANS_CAUSE_REELLE'
  | 'LICENCIEMENT_ECONOMIQUE'
  | 'PRISE_ACTE_RUPTURE'
  | 'HARCELEMENT_MORAL'
  | 'DISCRIMINATION'
  | 'HEURES_SUPPLEMENTAIRES'
  | 'RAPPEL_SALAIRE';

/**
 * Enum Immigration — valeurs alignées sur {@code LegalDomainPromptBuilder}
 * (champ {@code type_procedure_detectee}).
 */
export type TypeProcedureImmigrationCode =
  | 'RENOUVELLEMENT_TITRE_SEJOUR'
  | 'DEMANDE_ASILE_OFPRA'
  | 'RECOURS_CNDA'
  | 'REGROUPEMENT_FAMILIAL'
  | 'NATURALISATION_DECRET'
  | 'CHANGEMENT_STATUT'
  | 'AES_SALARIE'
  | 'REGULARISATION_EXCEPTIONNELLE'
  | 'OQTF_AVEC_DELAI'
  | 'OQTF_SANS_DELAI';

/** Domaine cible pour le dialog override (résolu via {@code legalDomain} du dossier). */
export type TypeLitigeOverrideDomain = 'TRAVAIL_FR' | 'IMMIGRATION';

/**
 * Body envoyé au PUT /api/v1/case-files/{id}/type-litige-override.
 * <p>Le champ {@code type} contient un code Travail FR OU un code Immigration
 * (le backend choisit la colonne cible selon {@code legalDomain} du dossier).</p>
 */
export interface TypeLitigeOverridePayload {
  /** Code enum (Travail FR ou Immigration), exclusif. */
  type: TypeLitigeTravailFrCode | TypeProcedureImmigrationCode;
  /** Raison libre optionnelle (max 500 chars côté backend). */
  raison?: string | null;
}

/**
 * Réponse 200 du GET et du PUT — au plus un des deux champs typés est rempli
 * selon le domaine du dossier. Si aucun override n'a été posé, les 3 champs
 * sont {@code null}.
 */
export interface TypeLitigeOverrideResponse {
  typeLitigeAvocat?: TypeLitigeTravailFrCode | null;
  typeProcedureAvocat?: TypeProcedureImmigrationCode | null;
  raison?: string | null;
}

/** Libellés FR avocat-friendly pour le MatSelect Travail FR. */
export const TYPE_LITIGE_TRAVAIL_FR_LABELS: Readonly<Record<TypeLitigeTravailFrCode, string>> = {
  LICENCIEMENT_SANS_CAUSE_REELLE: 'Licenciement sans cause réelle et sérieuse',
  LICENCIEMENT_ECONOMIQUE: 'Licenciement économique',
  PRISE_ACTE_RUPTURE: 'Prise d\'acte de rupture',
  HARCELEMENT_MORAL: 'Harcèlement moral',
  DISCRIMINATION: 'Discrimination',
  HEURES_SUPPLEMENTAIRES: 'Heures supplémentaires',
  RAPPEL_SALAIRE: 'Rappel de salaire',
};

/** Libellés FR avocat-friendly pour le MatSelect Immigration. */
export const TYPE_PROCEDURE_IMMIGRATION_LABELS: Readonly<Record<TypeProcedureImmigrationCode, string>> = {
  RENOUVELLEMENT_TITRE_SEJOUR: 'Renouvellement de titre de séjour',
  DEMANDE_ASILE_OFPRA: 'Demande d\'asile (OFPRA)',
  RECOURS_CNDA: 'Recours CNDA',
  REGROUPEMENT_FAMILIAL: 'Regroupement familial',
  NATURALISATION_DECRET: 'Naturalisation par décret',
  CHANGEMENT_STATUT: 'Changement de statut',
  AES_SALARIE: 'Admission exceptionnelle au séjour (salarié)',
  REGULARISATION_EXCEPTIONNELLE: 'Régularisation exceptionnelle (L.435-1)',
  OQTF_AVEC_DELAI: 'OQTF avec délai',
  OQTF_SANS_DELAI: 'OQTF sans délai',
};

/**
 * Résout le domaine cible du dialog override depuis le {@code legalDomain}
 * du dossier. {@code null} si le domaine ne supporte pas l'override (V1 : famille
 * exclu, traité dans une SF V2 ultérieure).
 */
export function resolveOverrideDomain(legalDomain: string | null | undefined): TypeLitigeOverrideDomain | null {
  if (!legalDomain) return null;
  if (legalDomain === 'DROIT_DU_TRAVAIL') return 'TRAVAIL_FR';
  if (legalDomain === 'DROIT_IMMIGRATION') return 'IMMIGRATION';
  return null;
}
