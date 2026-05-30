/**
 * SF-218-16 : modèles miroirs du contrat API (backend SF-218-15) pour l'outil
 * décisionnel « Statut du journaliste professionnel : clauses spécifiques et
 * indemnité de congédiement » (F-DT-105-journaliste-statut). FRANCE uniquement.
 *
 * Régime du journaliste professionnel (art. L.7111-1 et s. CT) lors d'une
 * rupture : qualification de la rupture éligible à la clause de cession
 * (cession du titre) ou à la clause de conscience (changement notable de
 * l'orientation du journal), indemnité de congédiement spécifique
 * (1 mois/année, art. L.7112-3) et passage par la commission arbitrale
 * paritaire (art. L.7112-4) lorsque l'ancienneté dépasse 15 ans.
 */

/**
 * Type de rupture invoqué (aligné EXACTEMENT sur l'enum backend
 * {@code TypeRupture}).
 */
export type TypeRuptureJournaliste =
  | 'LICENCIEMENT'
  | 'CLAUSE_CESSION'
  | 'CLAUSE_CONSCIENCE'
  | 'DEMISSION'
  | 'FAUTE_GRAVE';

/**
 * Qualification du statut de journaliste professionnel (aligné EXACTEMENT sur
 * l'enum backend {@code StatutJournaliste}). Présumé CONFIRME si détention de
 * la carte d'identité professionnelle (CCIJP).
 */
export type StatutJournaliste = 'CONFIRME' | 'A_QUALIFIER';

/**
 * Validité de la clause invoquée (aligné EXACTEMENT sur l'enum backend
 * {@code ClauseValide}) :
 *  - VALIDE : fait générateur constaté (cession / changement d'orientation).
 *  - NON_VALIDE : clause invoquée mais fait générateur non constaté.
 *  - SANS_OBJET : rupture hors clause de cession / conscience.
 */
export type ClauseValide = 'VALIDE' | 'NON_VALIDE' | 'SANS_OBJET';

/**
 * Verdict d'orientation global (aligné EXACTEMENT sur l'enum backend
 * {@code JournalisteStatutVerdict}) :
 *  - RUPTURE_ASSIMILEE_LICENCIEMENT : clause valide, rupture assimilée à un
 *    licenciement ouvrant droit à indemnité.
 *  - INDEMNITE_DUE : indemnité de congédiement due.
 *  - COMMISSION_ARBITRALE : montant relevant de la compétence exclusive de la
 *    commission arbitrale paritaire.
 *  - INDEMNITE_NON_DUE : indemnité non due de droit (faute grave / démission).
 */
export type JournalisteStatutVerdict =
  | 'RUPTURE_ASSIMILEE_LICENCIEMENT'
  | 'INDEMNITE_DUE'
  | 'COMMISSION_ARBITRALE'
  | 'INDEMNITE_NON_DUE';

export interface JournalisteStatutRequest {
  dateEntree: string; // ISO YYYY-MM-DD — début du contrat
  dateRupture: string; // ISO YYYY-MM-DD — notification de la rupture
  typeRupture: TypeRuptureJournaliste;
  salaireMensuelMoyen: number; // > 0 — base de l'indemnité de congédiement
  carteIdentiteProfessionnelle: boolean; // présomption de qualité de journaliste
  cessionTitreConstatee: boolean; // fait générateur clause de cession
  changementOrientationConstate: boolean; // fait générateur clause de conscience
}

export interface JournalisteStatutResponse {
  caseFileId: string;
  dateEntree: string;
  dateRupture: string;
  typeRupture: TypeRuptureJournaliste;
  salaireMensuelMoyen: number;
  carteIdentiteProfessionnelle: boolean;
  cessionTitreConstatee: boolean;
  changementOrientationConstate: boolean;
  /** Qualification du statut de journaliste professionnel. */
  statutJournaliste: StatutJournaliste;
  /** Validité de la clause invoquée. */
  clauseValide: ClauseValide;
  /** Motif explicitant la validité / non-validité de la clause ; null si SANS_OBJET. */
  motif: string | null;
  /** Indemnité de congédiement (€, 1 mois/année, art. L.7112-3) ; 0 si non due. */
  indemniteCongediement: number;
  /** Ancienneté en années (fraction d'année comptée comme année pleine). */
  ancienneteAnnees: number;
  /** true si passage obligatoire par la commission arbitrale (ancienneté > 15 ans / faute grave). */
  commissionArbitraleRequise: boolean;
  /** Verdict d'orientation global. */
  verdictGlobal: JournalisteStatutVerdict;
  country: string;
  baseJuridique: string;
}
