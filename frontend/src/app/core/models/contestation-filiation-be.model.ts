/**
 * F-217 SF-217-19 : modèles TypeScript de l'outil décisionnel
 * "Contestation de filiation BE" (`contestation-filiation-be`).
 *
 * BELGIQUE uniquement. Contrat API importé de SF-217-18 (backend, endpoints
 * POST/GET figés — CC art. 318 nouveau).
 */

/** Verdict d'analyse de recevabilité de l'action en contestation. */
export type ContestationFiliationBeVerdict =
  | 'ACTION_RECEVABLE'
  | 'ACTION_RECEVABLE_DELAI_CRITIQUE'
  | 'IRRECEVABLE_DELAI_DEPASSE'
  | 'IRRECEVABLE_QUALITE_A_AGIR'
  | 'IRRECEVABLE_POSSESSION_ETAT'
  | 'QUALIFICATION_INCOMPLETE';

/** Nature de la filiation contestée. */
export type NatureActionFiliationBe =
  | 'PATERNITE_PRESUMEE_MARI'
  | 'PATERNITE_RECONNUE_VOLONTAIRE'
  | 'MATERNITE'; // hors scope V1 — accepté côté model, rejeté par backend (400)

/** Qualité du demandeur à l'action. */
export type QualiteDemandeurFiliationBe =
  | 'ENFANT_MAJEUR'
  | 'ENFANT_MINEUR_REPRESENTE'
  | 'MERE'
  | 'PERE_PRESUME_OU_RECONNAISSANT'
  | 'TIERS_PRETENDANT_PERE_BIOLOGIQUE'
  | 'MINISTERE_PUBLIC';

/** Voie procédurale recommandée. */
export type VoieProceduraleFiliationBe =
  | 'REQUETE_TRIBUNAL_FAMILLE'
  | 'AUCUNE_ACTION_RECEVABLE';

/** Statut du délai d'action à la date du calcul. */
export type DelaiStatutBe = 'OK' | 'CRITIQUE' | 'DEPASSE';

/** Code structuré d'un motif d'irrecevabilité. */
export type MotifIrrecevabiliteFiliationBeCode =
  | 'DELAI_FORCLUSION'
  | 'QUALITE_NON_RECONNUE'
  | 'POSSESSION_ETAT_CONFORME_5_ANS'
  | 'MATERNITE_HORS_SCOPE_V1';

/** Motif d'irrecevabilité — exposé dans la réponse. */
export interface MotifIrrecevabiliteFiliationBe {
  code: MotifIrrecevabiliteFiliationBeCode;
  libelle: string;
  fondement: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/contestation-filiation-be`.
 * Toutes les dates au format ISO `YYYY-MM-DD`.
 */
export interface ContestationFiliationBeRequest {
  natureActionFiliation: NatureActionFiliationBe;
  qualiteDemandeur: QualiteDemandeurFiliationBe;
  dateNaissanceEnfant: string;
  dateConnaissanceFaitContestation: string;
  possessionEtatConforme: boolean;
  dureePossessionEtatAnnees: number;
  expertiseAdnDisponible: boolean;
  demandeExpertiseAdnEnvisagee: boolean;
  commentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot des inputs (hérité de
 * `ContestationFiliationBeRequest`) pour permettre la ré-édition du
 * formulaire après rechargement, **plus** les champs calculés (verdict,
 * délai, motifs d'irrecevabilité, voie procédurale, actions concrètes,
 * bases juridiques).
 */
export interface ContestationFiliationBeResponse
  extends ContestationFiliationBeRequest {
  caseFileId: string;
  verdict: ContestationFiliationBeVerdict;
  dateLimiteAction: string | null;
  joursRestants: number | null;
  delaiStatut: DelaiStatutBe | null;
  voieProcedurale: VoieProceduraleFiliationBe;
  motifsIrrecevabilite: MotifIrrecevabiliteFiliationBe[];
  actionsConcretes: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
