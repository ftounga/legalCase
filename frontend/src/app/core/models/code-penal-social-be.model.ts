/**
 * SF-219-24b : modèle frontend de l'outil F-219 — Code pénal social BE
 * (Loi du 06/06/2010 introduisant le Code pénal social, M.B. 01/07/2010,
 * e.e.v. 01/07/2011).
 *
 * <p><b>BE uniquement</b> — le régime français de répression du travail
 * illégal (C. trav. art. L. 8221-1 et s. + C. pén. art. 121-2 personne
 * morale × 5) constitue un dispositif juridiquement distinct dans ses
 * quanta, sa procédure (juges du parquet, pas auditorat) et son
 * architecture (pas de « règle Una Via » strictement comparable). Aucun
 * mapping mécanique. Le gate {@code workspaceCountry} est porté côté
 * composant ; le backend renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-24 backend
 * ({@code CodePenalSocialBeRequest} +
 * {@code CodePenalSocialBeResponse}).</p>
 */

/**
 * Type d'infraction sociale qualifiée — 13 cas + AUTRE_QUALIFICATION
 * (Code pénal social Livre 2). Le type détermine le niveau de sanction
 * par défaut, sauf renseignement explicite via {@code niveauPropose} ou
 * indication via {@code elementMoralIntentionnel}.
 */
export type CodePenalSocialBeTypeInfraction =
  | 'TRAVAIL_NON_DECLARE'
  | 'ABSENCE_DIMONA'
  | 'ABSENCE_DMFA'
  | 'NON_PAIEMENT_REMUNERATION'
  | 'DEPASSEMENT_TEMPS_TRAVAIL'
  | 'INFRACTION_BIEN_ETRE'
  | 'ACCIDENT_GRAVE_NON_DECLARE'
  | 'DISCRIMINATION_EMPLOI'
  | 'ENTRAVE_INSPECTION_SOCIALE'
  | 'FAUX_SOCIAL'
  | 'ABSENCE_REGISTRE_PERSONNEL'
  | 'INFRACTION_INTERIM'
  | 'INFRACTION_DETACHEMENT'
  | 'AUTRE_QUALIFICATION';

/**
 * Verdict du Code pénal social — barème uniforme à 4 niveaux croissants
 * de gravité (art. 101-103 C. pén. soc.) + cas {@code A_QUALIFIER}
 * réservé aux qualifications incertaines.
 *
 * <ul>
 *   <li>{@code SANCTION_NIVEAU_1} — amende administrative 10 € à 100 €
 *       (non indexée, art. 101 § 1). Pas de peine pénale ; pas
 *       d'inscription au casier judiciaire.</li>
 *   <li>{@code SANCTION_NIVEAU_2} — amende administrative 50 € à 500 €
 *       OU amende pénale 50 € à 500 € (art. 102 — choix auditorat
 *       art. 73-78).</li>
 *   <li>{@code SANCTION_NIVEAU_3} — amende administrative 100 € à
 *       1 000 € OU amende pénale 100 € à 1 000 € (art. 102). Pas
 *       d'emprisonnement.</li>
 *   <li>{@code SANCTION_NIVEAU_4} — amende administrative 300 € à
 *       3 000 € OU amende pénale 600 € à 6 000 € ET / OU
 *       emprisonnement 6 mois à 3 ans (art. 103). Multiplication par
 *       le nombre de travailleurs concernés (art. 103 § 2), plafond
 *       100 × le maximum unitaire pour une personne morale (art. 105).</li>
 *   <li>{@code A_QUALIFIER} — type {@code AUTRE_QUALIFICATION} sans
 *       niveau renseigné, qualification à établir par l'avocat.</li>
 * </ul>
 */
export type CodePenalSocialBeVerdict =
  | 'SANCTION_NIVEAU_1'
  | 'SANCTION_NIVEAU_2'
  | 'SANCTION_NIVEAU_3'
  | 'SANCTION_NIVEAU_4'
  | 'A_QUALIFIER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/code-penal-social-be}.
 *
 * <p>Tous les champs marqués {@code @NotNull} backend sont requis ; la
 * date des faits et le niveau proposé sont optionnels.</p>
 */
export interface CodePenalSocialBeRequest {
  /** Qualification de l'infraction (13 types ou AUTRE_QUALIFICATION). */
  typeInfraction: CodePenalSocialBeTypeInfraction;
  /**
   * Niveau 1 à 4 explicitement saisi par l'avocat (facultatif) — prime
   * sur la table par défaut. Requis avec {@code AUTRE_QUALIFICATION}
   * pour obtenir un verdict autre que {@code A_QUALIFIER}.
   */
  niveauPropose: number | null;
  /**
   * Date de commission des faits (facultatif) — conditionne les
   * coefficients d'indexation des amendes pénales (information
   * indicative, l'outil ne chiffre pas le montant indexé).
   */
  dateFaits: string | null;
  /**
   * Nombre de travailleurs concernés — multiplicateur d'amende
   * (art. 103 § 2 C. pén. soc.), plafonné à 100 × maximum unitaire
   * pour une personne morale (art. 105).
   */
  nombreTravailleursConcernes: number;
  /** Employeur personne morale (multiplication × 5 art. 105). */
  personneMorale: boolean;
  /** Récidive ≤ 1 an depuis condamnation antérieure (art. 110). */
  recidiveDansLAn: boolean;
  /**
   * Prévenu préposé / mandataire et non l'employeur en personne
   * (art. 109 C. pén. soc.). Information sur la qualité de l'auteur,
   * n'affecte pas le niveau d'amende.
   */
  prevenuPreposeOuMandataire: boolean;
  /**
   * Élément moral intentionnel (dol) — pertinent pour les infractions
   * à double niveau selon dol (ABSENCE_DMFA : niveau 4 si frauduleux,
   * niveau 2 sinon).
   */
  elementMoralIntentionnel: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs
 * calculés (niveau + bornes d'amendes + emprisonnement + majorations)
 * + verdict + métadonnées légales.
 */
export interface CodePenalSocialBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  typeInfraction: CodePenalSocialBeTypeInfraction;
  niveauPropose: number | null;
  dateFaits: string | null;
  nombreTravailleursConcernes: number;
  personneMorale: boolean;
  recidiveDansLAn: boolean;
  prevenuPreposeOuMandataire: boolean;
  elementMoralIntentionnel: boolean;

  // -- Outputs calculés --
  /** Verdict 4 niveaux ou A_QUALIFIER. */
  verdict: CodePenalSocialBeVerdict;
  /** Niveau de sanction effectif (1 à 4, 0 si A_QUALIFIER). */
  niveauSanction: number;
  /** Borne min amende administrative (EUR, valeur de base non indexée). */
  amendeAdminMin: number;
  /** Borne max amende administrative (EUR, valeur de base non indexée). */
  amendeAdminMax: number;
  /** Borne min amende pénale (EUR, valeur de base non indexée). */
  amendePenaleMin: number;
  /** Borne max amende pénale (EUR, valeur de base non indexée). */
  amendePenaleMax: number;
  /** Emprisonnement applicable (niveau 4 uniquement). */
  emprisonnementApplicable: boolean;
  /** Emprisonnement min en mois (0 si non applicable). */
  emprisonnementMinMois: number;
  /** Emprisonnement max en mois (0 si non applicable). */
  emprisonnementMaxMois: number;
  /** Multiplication par nombre de travailleurs applicable (art. 103 § 2). */
  multiplicationParTravailleur: boolean;
  /** Majoration × 5 personne morale appliquée (art. 105). */
  majorationPersonneMorale: boolean;
  /** Majoration × 2 plafond récidive ≤ 1 an appliquée (art. 110). */
  majorationRecidive: boolean;

  // -- Verdict + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (indexation, auditorat, choix voie). */
  avertissement: string;
}
