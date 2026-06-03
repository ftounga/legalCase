/**
 * SF-222-03 : modèles TypeScript de l'outil "Habilitation familiale"
 * (F-FA-HABILITATION-FAMILIALE, FRANCE uniquement — art. 494-1 et s. Cciv).
 *
 * Contrat API figé côté backend SF-222-03 (POST/GET) :
 *   POST /api/v1/case-files/{caseFileId}/habilitation-familiale-analysis
 *   GET  /api/v1/case-files/{caseFileId}/habilitation-familiale-analysis
 *
 * L'outil CONSEILLE l'avocat sur les conditions de l'habilitation familiale ;
 * le PRONONCE relève du juge des contentieux de la protection.
 *
 * Anti-doublon F-FA-25 (sélecteur de régime de protection) : cet outil cadre les
 * conditions PROPRES de l'habilitation familiale, distinct du re-choix du régime
 * (sauvegarde / curatelle / tutelle / mandat).
 */

/** Verdict 3 niveaux de l'analyse des conditions de l'habilitation familiale. */
export type VerdictHabilitationFamiliale =
  | 'ELIGIBLE_HABILITATION_GENERALE'
  | 'ELIGIBLE_HABILITATION_SPECIALE'
  | 'ORIENTER_VERS_MESURE_JUDICIAIRE';

/** Lien familial éligible (art. 494-1 Cciv). */
export type LienFamilialHabilitation =
  | 'ASCENDANT'
  | 'DESCENDANT'
  | 'FRERE_SOEUR'
  | 'CONJOINT_PARTENAIRE'
  | 'AUTRE';

/** Étendue de l'habilitation (art. 494-1 / 494-6 Cciv). */
export type EtendueHabilitation = 'PONCTUELLE' | 'GENERALE';

/** Modalité (art. 494-1 Cciv). */
export type ModaliteHabilitation = 'ASSISTANCE' | 'REPRESENTATION';

/** Requête POST `/api/v1/case-files/{caseFileId}/habilitation-familiale-analysis`. */
export interface HabilitationFamilialeRequest {
  alterationFacultesMedicalementConstatee: boolean | null;
  lienFamilialEligible: LienFamilialHabilitation | null;
  consensusFamilial: boolean | null;
  besoinActesPatrimoniaux: boolean | null;
  besoinActesPersonnels: boolean | null;
  protectionPonctuelleOuGenerale: EtendueHabilitation | null;
}

/** Réponse POST / GET — résultat persisté pour le dossier. */
export interface HabilitationFamilialeResponse {
  caseFileId: string;
  verdict: VerdictHabilitationFamiliale;
  modalite: ModaliteHabilitation | null;
  actesCouverts: string[];
  conditionsManquantes: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
}
