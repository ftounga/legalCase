/**
 * SF-219-29b : modele frontend de l'outil F-219 — Rente AT/MP vs
 * capitalisation BE (Loi du 10/04/1971 sur les accidents du travail
 * art. 24 ; Lois coordonnees du 03/06/1970 art. 35 pour les maladies
 * professionnelles — renvoi au regime AT ; AR du 21/12/1971 portant
 * execution de la Loi 10/04/1971 — bareme de capitalisation et
 * coefficients d'age ; AR du 10/12/1987 fixant les regles de
 * conversion partielle de la rente en capital ; AR du 24/02/2005
 * actualisant le bareme).
 *
 * <p><b>BE uniquement</b> — le regime francais des rentes AT/MP CPAM
 * (art. L. 434-1 et s. CSS, seuil 10 pour cent / 50 pour cent au lieu
 * de 19 pour cent BE, bareme indicatif art. R. 434-2 differemment
 * construit, voies de recours TASS / Pole social TJ) est juridiquement
 * distinct. Aucune transposition mecanique. Le gate
 * {@code workspaceCountry} est porte cote composant ; le backend
 * renvoie 404 pour FR par coherence.</p>
 *
 * <p>Contrat fige dans SF-219-29 backend
 * ({@code AtMpRenteCapitalBeRequest} +
 * {@code AtMpRenteCapitalBeResponse}).</p>
 */

/**
 * Origine de l'incapacite permanente partielle.
 *
 * <ul>
 *   <li>{@code ACCIDENT_TRAVAIL} — AT au sens art. 7 Loi 10/04/1971
 *       (evenement soudain, anormal et exterieur, dans le cours et
 *       par le fait de l'execution du contrat de travail) ou accident
 *       sur le chemin du travail art. 8.</li>
 *   <li>{@code MALADIE_PROFESSIONNELLE} — MP reconnue par Fedris au
 *       sens des Lois coordonnees 03/06/1970 — renvoi au regime AT
 *       pour le mode de versement art. 35.</li>
 * </ul>
 */
export type AtMpRenteCapitalBeOrigine =
  | 'ACCIDENT_TRAVAIL'
  | 'MALADIE_PROFESSIONNELLE';

/**
 * Statut de reconnaissance par Fedris / assureur-loi.
 *
 * <ul>
 *   <li>{@code RECONNU} — decision de reconnaissance acquise, taux
 *       IPP arrete par l'expertise medicale Fedris ou
 *       assureur-loi.</li>
 *   <li>{@code CONTESTE} — reconnaissance refusee ou taux conteste —
 *       voie de recours tribunal du travail art. 580 1 C. jud.</li>
 *   <li>{@code EN_COURS} — dossier en instruction Fedris /
 *       assureur-loi — calcul indicatif, ne pas verser.</li>
 * </ul>
 */
export type AtMpRenteCapitalBeStatutReconnaissance =
  | 'RECONNU'
  | 'CONTESTE'
  | 'EN_COURS';

/**
 * Verdict 5 etats de l'outil.
 *
 * <ul>
 *   <li>{@code CAPITAL_FORFAITAIRE_LT_19} — taux IPP &lt; 19 pour cent :
 *       capital unique (capitalisation d'office art. 24 al. 2 Loi
 *       10/04/1971 ; art. 4 AR 10/12/1987).</li>
 *   <li>{@code RENTE_ANNUELLE_GE_19} — taux IPP &gt;= 19 pour cent :
 *       rente annuelle viagere (art. 24 al. 1 Loi 10/04/1971).</li>
 *   <li>{@code INELIGIBLE_NON_RECONNU} — AT ou MP non reconnu —
 *       ni rente ni capital.</li>
 *   <li>{@code IPP_NON_DETERMINE} — taux IPP pas encore arrete —
 *       calcul premature.</li>
 *   <li>{@code A_QUALIFIER} — inputs ambigus.</li>
 * </ul>
 */
export type AtMpRenteCapitalBeVerdict =
  | 'CAPITAL_FORFAITAIRE_LT_19'
  | 'RENTE_ANNUELLE_GE_19'
  | 'INELIGIBLE_NON_RECONNU'
  | 'IPP_NON_DETERMINE'
  | 'A_QUALIFIER';

/**
 * Body POST
 * /api/v1/case-files/:caseFileId/decision-tools/at-mp-rente-capital-be.
 */
export interface AtMpRenteCapitalBeRequest {
  /** Origine de l'incapacite (AT ou MP). */
  origine: AtMpRenteCapitalBeOrigine;
  /** Statut de reconnaissance Fedris / assureur-loi. */
  statutReconnaissance: AtMpRenteCapitalBeStatutReconnaissance;
  /**
   * Date d'arret de l'incapacite temporaire et de fixation du taux IPP
   * par l'expertise medicale Fedris / assureur-loi (ISO yyyy-MM-dd).
   * Optionnelle si statut EN_COURS.
   */
  dateConsolidation: string | null;
  /**
   * Taux d'incapacite permanente partielle en pourcentage (0-100).
   * Si null, statut doit etre EN_COURS ou CONTESTE.
   */
  tauxIpp: number | null;
  /**
   * Remuneration de base annuelle (EUR) — art. 34 Loi 10/04/1971,
   * plafonnee art. 39 (plafond ONSS).
   */
  remunerationBaseAnnuelle: number;
  /** Date de naissance de la victime (ISO yyyy-MM-dd). */
  dateNaissance: string;
  /**
   * Drapeau de demande de conversion partielle (1/3 max) en capital
   * art. 45ter Loi 10/04/1971 + AR 10/12/1987.
   */
  demandeConversionPartielle: boolean;
  /**
   * Date envisagee de la demande de conversion (ISO yyyy-MM-dd).
   * Si null, le calculateur utilise la date du jour Europe/Brussels.
   */
  dateDemandeConversion: string | null;
  /**
   * Override de l'age a la consolidation (mode test / scenario), 0-120.
   * Si null, calcule depuis dateConsolidation et dateNaissance.
   */
  ageConsolidationOverride: number | null;
}

/**
 * Snapshot complet renvoye par POST / GET — inputs (echo) + outputs
 * (verdict, capital, rente, coefficient) + metadonnees legales.
 */
export interface AtMpRenteCapitalBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  origine: AtMpRenteCapitalBeOrigine;
  statutReconnaissance: AtMpRenteCapitalBeStatutReconnaissance;
  dateConsolidation: string | null;
  tauxIpp: number | null;
  remunerationBaseAnnuelle: number;
  dateNaissance: string;
  demandeConversionPartielle: boolean;
  dateDemandeConversion: string | null;
  ageConsolidationOverride: number | null;

  // -- Outputs calcules --
  /** Verdict 5 etats. */
  verdict: AtMpRenteCapitalBeVerdict;
  /** Capitalisation d'office (IPP &lt; 19 pour cent). */
  capitalisationDoffice: boolean;
  /** Conversion partielle possible (IPP &gt;= 19, &gt;= 3 ans consolidation). */
  conversionPartiellePossible: boolean;
  /** Age effectif de la victime a la consolidation. */
  ageConsolidationCalcule: number | null;
  /** Coefficient d'age applique du bareme AR 24/02/2005. */
  coefficientAge: number | null;
  /** Rente annuelle viagere (EUR) — si IPP &gt;= 19. */
  renteAnnuelle: number | null;
  /** Capital de capitalisation (EUR) — si IPP &lt; 19. */
  capitalCapitalisation: number | null;
  /** Capital issu de la conversion partielle 1/3 (EUR), si applicable. */
  capitalConversionPartielle: number | null;
  /** Rente residuelle apres conversion partielle (EUR), si applicable. */
  renteResiduelleApresConversion: number | null;

  // -- Verdict + metadonnees legales --
  /** Code raison du verdict. */
  raison: string | null;
  /** Synthese humaine. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (etapes procedurales, indexation, bareme). */
  avertissement: string;
}
