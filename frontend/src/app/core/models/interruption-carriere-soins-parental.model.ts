/**
 * SF-219-32b : modele frontend de l'outil F-219 — Interruption de
 * carriere pour conge parental BE (Loi de redressement du 22/01/1985
 * art. 99 a 107quater + AR du 29/10/1997 + CCT n 64 du 29/04/1997 +
 * AR du 12/12/2001 (cinquieme temps) + AR du 12/08/1991 (allocations
 * ONEM) + Loi du 28/12/2011 (allongement 4 mois) + AR du 05/05/2019
 * (dixieme temps hors scope V1)).
 *
 * <p><b>BE uniquement</b> — le regime francais du conge parental
 * d'education (Code du travail FR art. L. 1225-47 a L. 1225-60,
 * duree 1 an renouvelable jusqu'au 3e anniversaire de l'enfant,
 * pas de base AT distincte, allocations CAF — pas ONEM) est
 * juridiquement distinct. Aucune transposition mecanique. Le gate
 * {@code workspaceCountry} est porte cote composant ; le backend
 * renvoie 404 pour FR par coherence.</p>
 *
 * <p>Contrat fige dans SF-219-32 backend
 * ({@code InterruptionCarriereSoinsParentalRequest} +
 * {@code InterruptionCarriereSoinsParentalResponse}).</p>
 *
 * <p><b>Distinct de F-DT-29 {@code credit-temps-be}</b> qui couvre
 * le credit-temps CCT 103 (regime universel sans motif specifique).
 * Cet outil couvre <b>uniquement</b> le conge parental regi par la
 * Loi 22/01/1985 et la CCT 64 — droit individuel par enfant et par
 * parent, conditions d'age enfant 12 ans / 21 ans handicap, formes
 * specifiques 4 mois temps plein / 8 mois mi-temps / 20 mois
 * cinquieme temps.</p>
 */

/**
 * Forme d'interruption de carriere pour conge parental BE — AR du
 * 29/10/1997 art. 3 (modifie par AR 12/12/2001 introduisant le
 * cinquieme temps).
 *
 * <ul>
 *   <li>{@code TEMPS_PLEIN} — suspension complete des prestations
 *       pendant 4 mois.</li>
 *   <li>{@code MI_TEMPS} — reduction des prestations a mi-temps
 *       pendant 8 mois (equivalent 4 mois ETP).</li>
 *   <li>{@code CINQUIEME_TEMPS} — reduction d'un cinquieme des
 *       prestations pendant 20 mois (equivalent 4 mois ETP, AR
 *       12/12/2001).</li>
 * </ul>
 *
 * <p>Note : la forme {@code DIXIEME_TEMPS} (40 mois — AR 05/05/2019)
 * n'est ouverte que sur autorisation employeur, hors scope V1.</p>
 */
export type InterruptionCarriereSoinsParentalForme =
  | 'TEMPS_PLEIN'
  | 'MI_TEMPS'
  | 'CINQUIEME_TEMPS';

/**
 * Mode de notification de la demande de conge parental BE — AR du
 * 29/10/1997 art. 6.
 *
 * <ul>
 *   <li>{@code LETTRE_RECOMMANDEE} — voie privilegiee (date certaine
 *       de depot = date de presentation).</li>
 *   <li>{@code REMISE_EN_MAIN} — admise sous reserve d'un accuse de
 *       reception date et signe par l'employeur (jurisprudence Cass.
 *       BE 26/05/2008).</li>
 *   <li>{@code AUTRE} — mode non conforme art. 6 AR 29/10/1997 ;
 *       expose a un verdict INELIGIBLE_FORMALISME.</li>
 * </ul>
 */
export type InterruptionCarriereSoinsParentalModeNotification =
  | 'LETTRE_RECOMMANDEE'
  | 'REMISE_EN_MAIN'
  | 'AUTRE';

/**
 * Verdict 8 etats de l'outil.
 *
 * <ul>
 *   <li>{@code ELIGIBLE_COMPLET} — toutes conditions remplies
 *       (anciennete &gt;= 12 mois, enfant eligible, formalisme respecte,
 *       solde disponible).</li>
 *   <li>{@code ELIGIBLE_AVEC_RESERVES} — eligibilite confirmee mais
 *       points de vigilance (delai notification limite, solde tendu).</li>
 *   <li>{@code INELIGIBLE_ANCIENNETE} — moins de 12 mois auprès de
 *       l'employeur (art. 5 AR 29/10/1997).</li>
 *   <li>{@code INELIGIBLE_AGE_ENFANT} — enfant &gt;= 12 ans (sans
 *       handicap) ou &gt;= 21 ans (handicap &gt;= 66 pour cent) au
 *       debut (art. 4 AR 29/10/1997).</li>
 *   <li>{@code INELIGIBLE_SOLDE_INSUFFISANT} — demande depasse le
 *       solde individuel 4 mois ETP par enfant et par parent
 *       (art. 2 AR 29/10/1997).</li>
 *   <li>{@code INELIGIBLE_FORMALISME} — notification non conforme :
 *       hors delai 2 a 3 mois ou mode autre que recommandee /
 *       remise en main (art. 6 AR 29/10/1997).</li>
 *   <li>{@code DIFFERE_EMPLOYEUR} — l'employeur a notifie un differe
 *       motive maximum 6 mois (art. 7 AR 29/10/1997).</li>
 *   <li>{@code A_QUALIFIER} — inputs insuffisants ou contradictoires.</li>
 * </ul>
 */
export type InterruptionCarriereSoinsParentalVerdict =
  | 'ELIGIBLE_COMPLET'
  | 'ELIGIBLE_AVEC_RESERVES'
  | 'INELIGIBLE_ANCIENNETE'
  | 'INELIGIBLE_AGE_ENFANT'
  | 'INELIGIBLE_SOLDE_INSUFFISANT'
  | 'INELIGIBLE_FORMALISME'
  | 'DIFFERE_EMPLOYEUR'
  | 'A_QUALIFIER';

/**
 * Body POST
 * /api/v1/case-files/:caseFileId/decision-tools/interruption-carriere-soins-parental.
 */
export interface InterruptionCarriereSoinsParentalRequest {
  /** Forme demandee (AR 29/10/1997 art. 3). */
  forme: InterruptionCarriereSoinsParentalForme;
  /** Anciennete chez l'employeur en mois (&gt;= 12 — art. 5 AR
   *  29/10/1997). */
  ancienneteMois: number;
  /** Age de l'enfant en mois au debut (&lt; 144 sans handicap, &lt; 252
   *  avec handicap &gt;= 66 pour cent — art. 4 AR 29/10/1997). */
  ageEnfantMois: number;
  /** Drapeau enfant handicap &gt;= 66 pour cent (porte plafond a
   *  21 ans — art. 4 al. 2 AR 29/10/1997). */
  enfantHandicap: boolean;
  /** Solde individuel restant du droit 4 mois ETP exprime en mois
   *  ETP (art. 2 AR 29/10/1997). */
  soldeRestantMoisEtp: number;
  /** Mode de notification employeur (art. 6 AR 29/10/1997). */
  modeNotification: InterruptionCarriereSoinsParentalModeNotification;
  /** Drapeau accord employeur sur la forme demandee. */
  employeurAccepte: boolean;
  /** Drapeau employeur a notifie un differe motive max 6 mois
   *  (art. 7 AR 29/10/1997). */
  employeurAdiffere: boolean;
  /** Drapeau cumul allocation ONEM forfaitaire mensuelle demandee
   *  (AR 12/08/1991 art. 4). */
  cumulAllocationOnemDemande: boolean;
  /** Date souhaitee du debut (ISO yyyy-MM-dd) — sert au calcul de
   *  la date de fin et de la protection art. 101 Loi 22/01/1985. */
  dateDebutInterruption: string | null;
  /** Date de notification a l'employeur (ISO yyyy-MM-dd) — sert au
   *  controle delai 2 a 3 mois (art. 6 AR 29/10/1997). */
  dateNotification: string | null;
  /** Remuneration mensuelle brute (EUR) — sert au calcul de
   *  l'indemnite de protection 6 mois art. 101 Loi 22/01/1985. */
  remunerationMensuelleBrute: number | null;
}

/**
 * Snapshot complet renvoye par POST / GET — inputs (echo) + outputs
 * analytiques + metadonnees legales.
 */
export interface InterruptionCarriereSoinsParentalResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  forme: InterruptionCarriereSoinsParentalForme;
  ancienneteMois: number;
  ageEnfantMois: number;
  enfantHandicap: boolean;
  soldeRestantMoisEtp: number;
  modeNotification: InterruptionCarriereSoinsParentalModeNotification;
  employeurAccepte: boolean;
  employeurAdiffere: boolean;
  cumulAllocationOnemDemande: boolean;
  dateDebutInterruption: string | null;
  dateNotification: string | null;
  remunerationMensuelleBrute: number | null;

  // -- Outputs analytiques --
  /** Verdict 8 etats. */
  verdict: InterruptionCarriereSoinsParentalVerdict;
  /** Anciennete &gt;= 12 mois remplie. */
  ancienneteOk: boolean;
  /** Age enfant dans les bornes legales (12 ans / 21 ans handicap). */
  ageEnfantOk: boolean;
  /** Formalisme respecte (mode et delai notification). */
  formalismeOk: boolean;
  /** Duree de l'interruption en mois (4 / 8 / 20 selon forme). */
  dureeInterruptionMois: number;
  /** Solde restant apres imputation de la demande (en mois ETP). */
  soldeRestantApresImputationMoisEtp: number;
  /** Allocation ONEM forfaitaire mensuelle (EUR) — null si non
   *  demandee ou non eligible. */
  allocationOnemMensuelle: number | null;
  /** Allocation ONEM totale sur la duree (EUR) — null si non
   *  applicable. */
  allocationOnemTotale: number | null;
  /** Date de fin de l'interruption (ISO yyyy-MM-dd) — null si dates
   *  non fournies. */
  dateFinInterruption: string | null;
  /** Fin de la protection contre le licenciement art. 101 Loi
   *  22/01/1985 (ISO yyyy-MM-dd). */
  finProtectionLicenciement: string | null;
  /** Drapeau protection contre le licenciement actuellement active. */
  protectionLicenciementActive: boolean;
  /** Indemnite de protection en cas de licenciement (EUR) — 6 mois
   *  de remuneration art. 101 Loi 22/01/1985 ; null si remuneration
   *  non fournie. */
  indemniteProtectionEnCasLicenciement: number | null;

  // -- Synthese + metadonnees legales --
  /** Code raison du verdict. */
  raison: string | null;
  /** Synthese humaine. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (etapes procedurales, protection, voies
   *  externes). */
  avertissement: string;
}
