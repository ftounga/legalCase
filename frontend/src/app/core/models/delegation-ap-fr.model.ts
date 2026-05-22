/**
 * SF-216-10 : modèles TypeScript de l'outil "Délégation d'autorité parentale"
 * (F-FA-XX-delegation-ap, FRANCE uniquement — art. 376-1 Cciv + art. 371-1
 * Cciv + art. L. 228-1 CASF + Cass. 1ère civ., 22/11/2005).
 *
 * Contrat API importé de SF-216-09 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/delegation-ap-fr
 *   GET  /api/v1/case-files/{caseFileId}/delegation-ap-fr
 */

/** Voie de délégation envisagée par l'avocat. */
export type TypeDelegationAp =
  | 'VOLONTAIRE_CONJOINTE'
  | 'JUDICIAIRE_TIERS'
  | 'JUDICIAIRE_DESINTERET';

/** Lien entre l'enfant et le tiers candidat à recevoir la délégation. */
export type TiersLienFamilial =
  | 'GRANDS_PARENTS'
  | 'ONCLE_TANTE'
  | 'FAMILLE_ELARGIE'
  | 'ASSOCIATION_HABILITEE'
  | 'AUTRE';

/** Verdict de recevabilité émis par le calculateur. */
export type VerdictRecevabiliteDelegationAp =
  | 'RECEVABLE_VOLONTAIRE'
  | 'RECEVABLE_JUDICIAIRE_DESINTERET'
  | 'RECEVABLE_JUDICIAIRE_TIERS'
  | 'IRRECEVABLE_ENFANT_MAJEUR'
  | 'IRRECEVABLE_VOIE_INADEQUATE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/delegation-ap-fr`.
 *
 * 8 champs (cf. backend `DelegationApFrRequest`).
 */
export interface DelegationApFrRequest {
  typeDelegation: TypeDelegationAp | null;
  tiersLienFamilial: TiersLienFamilial | null;
  accordParents: boolean | null;
  motivationDelegation: string | null;
  ageEnfant: number | null;
  dangerCaracterise: boolean | null;
  decisionsJudiciairesPrecedentes: boolean | null;
  desinteretParentPlusUnAn: boolean | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface DelegationApFrResponse {
  caseFileId: string;
  verdictRecevabilite: VerdictRecevabiliteDelegationAp;
  voieProcedurale: TypeDelegationAp;
  etapes: string[];
  dureeEstimeeJours: number;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
