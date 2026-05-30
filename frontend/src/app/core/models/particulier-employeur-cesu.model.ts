/**
 * SF-218-14 : modèles miroirs du contrat API (backend SF-218-13) pour l'outil
 * décisionnel « Particulier employeur (CESU) : préavis et indemnité de
 * licenciement » (F-DT-108-particulier-employeur-cesu). FRANCE uniquement.
 *
 * Régime conventionnel propre du salarié du particulier employeur (CESU :
 * garde d'enfants, employé de maison, assistant maternel), distinct du droit
 * commun : préavis et indemnité de licenciement / rupture selon la CCN des
 * salariés du particulier employeur (IDCC 3239, 2021) ou la CCN des assistants
 * maternels (indemnité de rupture, formule 1/80).
 *
 * Pattern miroir de {@link SaisieRemunerationResponse} (F-DT-89, SF-218-08).
 */

/**
 * Catégorie de l'employé — pilote la CCN applicable (aligné EXACTEMENT sur
 * l'enum backend {@code CategorieEmploye}).
 */
export type CategorieEmploye =
  | 'SALARIE_PARTICULIER_EMPLOYEUR'
  | 'ASSISTANT_MATERNEL';

/**
 * Cause de la rupture (aligné EXACTEMENT sur l'enum backend {@code CauseRupture}).
 * `RETRAIT_ENFANT` est réservé à l'assistant maternel.
 */
export type CauseRupture =
  | 'LICENCIEMENT_MOTIF_PERSONNEL'
  | 'RETRAIT_ENFANT'
  | 'FAUTE_GRAVE'
  | 'FORCE_MAJEURE'
  | 'DEPART_RETRAITE';

/**
 * Éligibilité à l'indemnité de licenciement / rupture (aligné EXACTEMENT sur
 * l'enum backend {@code EligibiliteIndemnite}).
 */
export type EligibiliteIndemnite = 'DUE' | 'NON_DUE';

/**
 * Méthode de calcul de l'indemnité (aligné EXACTEMENT sur l'enum backend
 * {@code MethodeCalcul}) :
 *  - CONVENTIONNEL_PE : CCN salariés du particulier employeur (1/4 mois/an les
 *    10 premières années + 1/3 au-delà, aligné R. 1234-2).
 *  - INDEMNITE_RUPTURE_ASSMAT : CCN assistants maternels (1/80 du total des
 *    salaires nets perçus depuis le 1er jour du contrat).
 */
export type MethodeCalcul = 'CONVENTIONNEL_PE' | 'INDEMNITE_RUPTURE_ASSMAT';

/**
 * Verdict d'orientation global (aligné EXACTEMENT sur l'enum backend
 * {@code ParticulierEmployeurCesuVerdict}) :
 *  - RUPTURE_REGULIERE : préavis et indemnité formalisés, rupture régulière.
 *  - RUPTURE_A_SECURISER : préavis ou indemnité à formaliser.
 *  - INDEMNITE_NON_DUE : indemnité non due (faute grave / ancienneté insuffisante).
 */
export type ParticulierEmployeurCesuVerdict =
  | 'RUPTURE_REGULIERE'
  | 'RUPTURE_A_SECURISER'
  | 'INDEMNITE_NON_DUE';

export interface ParticulierEmployeurCesuRequest {
  dateEntree: string; // ISO YYYY-MM-DD — début du contrat
  dateRupture: string; // ISO YYYY-MM-DD — notification du licenciement
  categorieEmploye: CategorieEmploye;
  causeRupture: CauseRupture;
  salaireMensuelMoyen: number; // > 0 — base IL
}

export interface ParticulierEmployeurCesuResponse {
  caseFileId: string;
  dateEntree: string;
  dateRupture: string;
  categorieEmploye: CategorieEmploye;
  causeRupture: CauseRupture;
  salaireMensuelMoyen: number;
  /** Durée du préavis convertie en jours (barème conventionnel). */
  dureePreavisJours: number;
  /** Libellé lisible du préavis (ex. « 1 semaine », « 1 mois », « 2 mois »). */
  dureePreavisLibelle: string;
  /** Éligibilité à l'indemnité de licenciement / rupture. */
  eligibiliteIndemnite: EligibiliteIndemnite;
  /** Motif si NON_DUE (faute grave, ancienneté < 8 mois…) ; null si DUE. */
  motifNonDue: string | null;
  /** Montant de l'indemnité de licenciement / rupture (€) ; 0 si NON_DUE. */
  indemniteLicenciement: number;
  /** Méthode de calcul appliquée (CCN PE / 1/80 assmat). */
  methodeCalcul: MethodeCalcul;
  /** Verdict d'orientation global. */
  verdictGlobal: ParticulierEmployeurCesuVerdict;
  country: string;
  baseJuridique: string;
}
