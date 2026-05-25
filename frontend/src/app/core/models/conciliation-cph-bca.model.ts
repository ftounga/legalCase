/**
 * SF-212-38 : modèles TypeScript de l'outil "Conciliation CPH — Bureau de
 * Conciliation et d'Orientation (BCO)" (F-DT-84-conciliation-cph-bca,
 * FRANCE uniquement — R. 1454-7 à R. 1454-12 CT ; L. 1235-1 al. 3 CT —
 * barème transactions BCA ; L. 1411-1 CT — compétence CPH ; R. 1452-1 à
 * R. 1452-4 CT — saisine et convocation BCO).
 *
 * <p>F-212 19/19 — dernier outil livré du bundle F-212.</p>
 *
 * <p>Contrat API importé de SF-212-37 (backend, endpoints POST/GET figés
 * sur `/api/v1/case-files/{caseFileId}/conciliation-cph-bca`).</p>
 */

/** Évaluation avocat de l'opportunité de la conciliation BCO. */
export type ConciliationCphBcaOpportunite =
  | 'FAVORABLE'
  | 'DEFAVORABLE'
  | 'INCERTAIN';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/conciliation-cph-bca`.
 *
 * 4 champs : ancienneté en mois, salaire mensuel brut, montant total des
 * demandes, opportunité conciliation (enum 3 valeurs).
 */
export interface ConciliationCphBcaRequest {
  ancienneteMois: number | null;
  salaireMensuelBrutEuros: number | null;
  montantDemandesEuros: number | null;
  opportuniteConciliation: ConciliationCphBcaOpportunite | null;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs
 * (pour ré-édition du formulaire UI) ET les sorties calculées (montant
 * minimum BCA, palier applicable, comparaison BCA vs Macron, checklist
 * BCO, bases juridiques, messages).
 */
export interface ConciliationCphBcaResponse extends ConciliationCphBcaRequest {
  caseFileId: string;
  /** Montant minimum d'indemnité BCA en € (= palier × salaire mensuel brut). */
  montantMinimumBcaEuros: number;
  /** Libellé du palier applicable (ex. "8 à 15 ans : 4 mois"). */
  palierBcaApplicable: string;
  /** Texte comparatif BCA vs montant des demandes (et barème Macron). */
  comparaisonBcaVsMacron: string;
  /** Liste des pièces à produire au BCO. */
  checklistBco: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
