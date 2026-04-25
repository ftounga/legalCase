/**
 * SF-DT-28-02 : types frontend pour l'outil décisionnel "Avantages
 * conventionnels belges" (F-DT-28). Calcule pécule de vacances simple
 * + double pécule (loi 28/06/1971 + AR), prime de fin d'année (CCT/CP),
 * éco-chèques (CCT 98), chèques-repas, et le total annuel.
 *
 * BE uniquement. Pas d'équivalent direct FR — voir mini-spec SF-DT-28-02.
 *
 * Contrat figé dans SF-DT-28-01 (backend).
 */

/**
 * Commissions paritaires belges les plus fréquentes proposées dans le
 * mat-select. Liste non exhaustive — la dynamisation totale via
 * ConventionReferentialService est hors scope SF-DT-28-02 (cf. mini-spec
 * "Hors scope"). Les codes envoyés au backend sont des chaînes ; les
 * libellés humains sont définis dans `COMMISSION_PARITAIRE_LABELS`.
 */
export type CommissionParitaireBe =
  | 'CP_200'   // Commission paritaire auxiliaire pour employés (par défaut, +800k employés)
  | 'CP_124'   // Construction
  | 'CP_111'   // Métal
  | 'CP_226'   // Commerce international, transport et logistique
  | 'CP_337';  // Secteur non-marchand (santé, éducation, social)

/**
 * Libellés humains pour le mat-select.
 */
export const COMMISSION_PARITAIRE_LABELS: ReadonlyArray<{ code: CommissionParitaireBe; label: string }> = [
  { code: 'CP_200', label: 'CP 200 — Employés (auxiliaire)' },
  { code: 'CP_124', label: 'CP 124 — Construction' },
  { code: 'CP_111', label: 'CP 111 — Métal' },
  { code: 'CP_226', label: 'CP 226 — Commerce international, transport et logistique' },
  { code: 'CP_337', label: 'CP 337 — Secteur non-marchand' },
];

export interface AvantagesConventionnelsBeRequest {
  /** Salaire mensuel brut de référence (€), > 0. */
  salaireMensuelBrutEur: number;
  /** Jours travaillés sur l'année précédente, ≥ 0 et ≤ 365. */
  joursTravaillesAnneePrecedente: number;
  /** Ancienneté en mois (≥ 0). */
  anciennetteMois: number;
  /** Code de la commission paritaire (CP_xxx). */
  commissionParitaire: string;
  /** Année de calcul (YYYY, plage attendue 2020–2030). */
  annee: number;
  /** Vrai si l'employeur a déjà versé le 2e (double) pécule de vacances. */
  doublePeculeVacancesPercu: boolean;
  /** Vrai si la CCT/CP prévoit la prime de fin d'année. */
  primeFinAnneePrevueCcCp: boolean;
  /** Vrai si la CCT/CP prévoit les éco-chèques (CCT 98). */
  ecoChequesPrevuCcCp: boolean;
  /** Vrai si l'utilisation des éco-chèques a lieu dans l'année. */
  ecoChequesUtilisationDansAn: boolean;
  /** Vrai si la convention prévoit les chèques-repas. */
  chequesRepasPrevu: boolean;
  /** Jours prestés effectifs sur l'année (≥ 0), base chèques-repas. */
  joursPrestesEffectifs: number;
}

export interface AvantagesConventionnelsBeResponse {
  caseFileId: string;
  // Inputs réaffichés
  salaireMensuelBrutEur: number;
  joursTravaillesAnneePrecedente: number;
  anciennetteMois: number;
  commissionParitaire: string;
  annee: number;
  doublePeculeVacancesPercu: boolean;
  primeFinAnneePrevueCcCp: boolean;
  ecoChequesPrevuCcCp: boolean;
  ecoChequesUtilisationDansAn: boolean;
  chequesRepasPrevu: boolean;
  joursPrestesEffectifs: number;
  // Outputs
  /** Pécule de vacances simple annuel. */
  peculeVacancesSimpleEur: number;
  /** Double pécule de vacances annuel. */
  doublePeculeVacancesEur: number;
  /** Prime de fin d'année (13e mois) annuelle. */
  primeFinAnneeEur: number;
  /** Valeur annuelle des éco-chèques. */
  ecoChequesValeurAnnuelleEur: number;
  /** Valeur annuelle des chèques-repas. */
  chequesRepasValeurAnnuelleEur: number;
  /** Somme des 5 avantages annuels. */
  totalAvantagesAnnuelsEur: number;
  /** Base juridique en texte JetBrains Mono italique. */
  baseJuridique: string;
  /** Formule en texte JetBrains Mono. */
  formule: string;
  /** Messages explicatifs (rendus via LegalCitationsPipe). */
  messages: string[];
  /** Toujours 'BELGIQUE' pour cet outil BE-only. */
  country: 'BELGIQUE';
}
