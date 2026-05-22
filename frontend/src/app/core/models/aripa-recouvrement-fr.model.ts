/**
 * SF-216-08 : modèles TypeScript de l'outil "ARIPA recouvrement pension
 * alimentaire impayée" (F-FA-ARIPA-RECOUVREMENT, FRANCE uniquement —
 * art. L. 581-1 à L. 581-14 CSS + art. L. 582-1 CSS + art. L. 523-1 à
 * L. 523-6 CSS + Ordonnance n°2002-1358 + Convention La Haye 2007 +
 * Règlement UE n°4/2009).
 *
 * Contrat API importé de SF-216-07 (backend, endpoints POST/GET figés) :
 *   POST /api/v1/case-files/{caseFileId}/aripa-recouvrement-fr
 *   GET  /api/v1/case-files/{caseFileId}/aripa-recouvrement-fr
 */

/** Voie de recouvrement recommandée par le calculateur. */
export type VoieRecouvrementAripa =
  | 'TITRE_REQUIS'
  | 'SDR_ARIPA'
  | 'SAISIE_SUR_SALAIRE'
  | 'SATD'
  | 'CONVENTION_INTERNATIONALE';

/** Situation professionnelle du créancier ou du débiteur. */
export type SituationAripa =
  | 'SALARIE'
  | 'CHOMEUR'
  | 'INDEPENDANT'
  | 'SANS_ACTIVITE'
  | 'INCONNU';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/aripa-recouvrement-fr`.
 *
 * 8 champs (cf. backend `AripaRecouvrementFrRequest`).
 */
export interface AripaRecouvrementFrRequest {
  montantPensionMensuelleEur: number | null;
  nombreMoisImpayes: number | null;
  situationCreancier: SituationAripa | null;
  situationDebiteur: SituationAripa | null;
  titreExecutoire: boolean | null;
  debiteurEnFrance: boolean | null;
  debiteurEmployeurPublic: boolean | null;
  nombreEnfantsACharge: number | null;
}

/**
 * Réponse POST / GET — résultat persisté pour le dossier.
 */
export interface AripaRecouvrementFrResponse {
  caseFileId: string;
  voieRecommandee: VoieRecouvrementAripa;
  montantArrieres: number;
  montantAsfEligibleMensuelEur: number;
  delaiEstimeJours: number;
  etapes: string[];
  baseLegale: string;
  messages: string[];
  alertes: string[];
  country: string;
}
