/**
 * SF-206-04 : modèles TypeScript de l'outil "Congés payés acquis pendant
 * arrêt maladie" (F-DT-75 — FRANCE uniquement).
 *
 * Contrat API importé de SF-206-03 (backend, endpoints POST/GET figés).
 * Fondement : loi 22/04/2024 (art. 37), L.3141-5 / L.3141-5-1 CT, transposition
 * Cass. soc. 13/09/2023 n°22-17.340.
 */

/** Type d'arrêt à l'origine de l'acquisition des congés payés. */
export type CongesPayesArretMaladieTypeArret =
  | 'MALADIE_NON_PROFESSIONNELLE'
  | 'ACCIDENT_TRAVAIL_MALADIE_PRO';

/** Verdict 4 niveaux du chiffrage de rappel. */
export type CongesPayesArretMaladieVerdict =
  | 'RAPPEL_SIGNIFICATIF'
  | 'RAPPEL_LIMITE'
  | 'PAS_DE_RAPPEL'
  | 'ACTION_FORCLOSE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/conges-payes-arret-maladie`.
 *
 * `dateRuptureContrat` est requis si `salarieEncoreEnPoste = false` (validation
 * backend 400). `joursCpDejaAccordes` accepte 0 par défaut.
 */
export interface CongesPayesArretMaladieRequest {
  typeArret: CongesPayesArretMaladieTypeArret;
  nombreMoisArret: number;
  salarieEncoreEnPoste: boolean;
  dateRuptureContrat: string | null;
  joursCpDejaAccordes: number;
  salaireBrutMensuel: number;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose le snapshot complet des inputs (ré-édition possible après recharge)
 * **plus** les champs calculés (verdict, jours acquis, jours rappel,
 * valorisation indicative, date limite d'action, action encore ouverte, bases
 * juridiques, messages).
 */
export interface CongesPayesArretMaladieResponse extends CongesPayesArretMaladieRequest {
  caseFileId: string;
  verdict: CongesPayesArretMaladieVerdict;
  joursCpAcquis: number;
  joursCpRappel: number;
  valorisationIndicativeEur: number;
  /** Échéance d'action — exposée pour suivi en onglet Suivi (SF-206-00b). */
  dateLimiteAction: string;
  actionEncoreOuverte: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
