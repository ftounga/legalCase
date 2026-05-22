/**
 * SF-216-12 : modèles TypeScript de l'outil "Retrait d'autorité parentale"
 * (F-FA-RETRAIT-AP, FRANCE uniquement — art. 378-381 Cciv + loi n°2022-140 du
 * 7 février 2022 LMVSS + art. 343-1 al. 2 Cciv + Cass. 1ère civ., 26/10/2011).
 *
 * Contrat API importé de SF-216-11 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/retrait-autorite-parentale
 *   GET  /api/v1/case-files/{caseFileId}/retrait-autorite-parentale
 */

/** Type de retrait envisagé par l'avocat. */
export type TypeRetraitAp =
  | 'TOTAL'
  | 'PARTIEL_EXERCICE'
  | 'PARTIEL_ATTRIBUTS';

/** Motif fondant la demande de retrait (art. 378 / 378-1 Cciv + loi 2022). */
export type MotifRetraitAp =
  | 'CONDAMNATION_PENALE'
  | 'DANGER_CARACTERISE_VIOLENCES'
  | 'DESINTERET_GRAVE'
  | 'COMPORTEMENT_GRAVEMENT_COMPROMETTANT'
  | 'VIOLENCES_LMVSS_2022';

/** Verdict de recevabilité émis par le calculateur. */
export type VerdictRetraitAp =
  | 'RETRAIT_PLEIN_DROIT'
  | 'RETRAIT_CIVIL_JAF'
  | 'SUSPENSION_ACCELEREE_LMVSS_2022'
  | 'IRRECEVABLE_ENFANT_MAJEUR'
  | 'IRRECEVABLE_MOTIF_NON_CARACTERISE';

/** Voie procédurale effective. */
export type VoieProceduraleRetraitAp =
  | 'JURIDICTION_PENALE_ACCESSOIRE'
  | 'JAF_TRIBUNAL_JUDICIAIRE'
  | 'PROCUREUR_REPUBLIQUE_ASSISTANCE_EDUCATIVE'
  | 'LMVSS_2022_SUSPENSION_AUTOMATIQUE'
  | 'SANS_OBJET';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/retrait-autorite-parentale`.
 *
 * 7 champs (cf. backend `RetraitAutoriteParentaleRequest`).
 */
export interface RetraitAutoriteParentaleRequest {
  typeRetrait: TypeRetraitAp | null;
  motifRetrait: MotifRetraitAp | null;
  condamnationPenaleDetectee: boolean | null;
  dangerCaracterise: boolean | null;
  violencesConjugalesDetectees: boolean | null;
  ageEnfant: number | null;
  decisionsJudiciairesPrecedentes: boolean | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface RetraitAutoriteParentaleResponse {
  caseFileId: string;
  verdictRetrait: VerdictRetraitAp;
  voieProcedurale: VoieProceduraleRetraitAp;
  admissibiliteAdoption: boolean;
  consequencesJuridiques: string[];
  etapes: string[];
  dureeEstimeeJours: number;
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
