/**
 * SF-219-28b : modele frontend de l'outil F-219 — Reconnaissance d'une
 * maladie professionnelle Fedris BE (Lois coordonnees du 03/06/1970 +
 * AR du 28/03/1969 liste fermee modifie 06/12/2018 + AR du 16/12/1985
 * systeme ouvert + Loi du 11/01/2018 reformant Fedris).
 *
 * <p><b>BE uniquement</b> — le regime francais (CPAM, tableaux art.
 * L. 461-2 CSS, CRRMP art. L. 461-1, prescription 2 ans art. L. 461-5
 * CSS) est juridiquement distinct (CPAM et non Fedris, tableaux FR
 * differents des annexes AR 28/03/1969 BE). Aucune transposition
 * mecanique. Le gate {@code workspaceCountry} est porte cote composant ;
 * le backend renvoie 404 pour FR par coherence.</p>
 *
 * <p>Contrat fige dans SF-219-28 backend
 * ({@code MpFedrisReconnaissanceRequest} +
 * {@code MpFedrisReconnaissanceResponse}).</p>
 */

/**
 * Qualification de la maladie au regard de la liste fermee AR 28/03/1969.
 *
 * <ul>
 *   <li>{@code LISTE_FERMEE} — maladie inscrite dans la liste
 *       (sections 1.x a 6.x) — declenche la presomption art. 32.</li>
 *   <li>{@code HORS_LISTE} — maladie hors liste — systeme ouvert art.
 *       30bis, preuve causalite directe et determinante requise.</li>
 * </ul>
 */
export type MpFedrisReconnaissanceTypeMaladie =
  | 'LISTE_FERMEE'
  | 'HORS_LISTE';

/**
 * Caracterisation de l'exposition professionnelle au risque.
 *
 * <ul>
 *   <li>{@code AVEREE} — documentee, conforme aux conditions de la
 *       section concernee.</li>
 *   <li>{@code INSUFFISANTE} — partielle, hors champ — la presomption
 *       ne joue pas.</li>
 *   <li>{@code INDETERMINEE} — a caracteriser.</li>
 * </ul>
 */
export type MpFedrisReconnaissanceExposition =
  | 'AVEREE'
  | 'INSUFFISANTE'
  | 'INDETERMINEE';

/**
 * Caracterisation du lien de causalite (decisive en systeme ouvert).
 *
 * <ul>
 *   <li>{@code DIRECTE_DETERMINANTE} — cause directe et essentielle.</li>
 *   <li>{@code INSUFFISANTE} — lien causal possible mais non demontre
 *       comme direct et determinant.</li>
 *   <li>{@code INDETERMINEE} — a etablir par expertise.</li>
 *   <li>{@code NON_APPLICABLE} — non applicable (liste fermee
 *       presomption art. 32).</li>
 * </ul>
 */
export type MpFedrisReconnaissanceCausalite =
  | 'DIRECTE_DETERMINANTE'
  | 'INSUFFISANTE'
  | 'INDETERMINEE'
  | 'NON_APPLICABLE';

/**
 * Verdict 6 etats de l'outil.
 *
 * <ul>
 *   <li>{@code MALADIE_LISTE_FERMEE_PRESOMPTION} — liste + exposition
 *       averee, presomption art. 32 joue (favorable).</li>
 *   <li>{@code MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE} — liste
 *       mais exposition non demontree (presomption bloquee).</li>
 *   <li>{@code SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE} — hors
 *       liste, causalite etablie (art. 30bis, favorable).</li>
 *   <li>{@code SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE} — hors liste,
 *       causalite insuffisante (refus probable Fedris).</li>
 *   <li>{@code DECLARATION_PRESCRITE} — action eteinte (3 ans art.
 *       31).</li>
 *   <li>{@code A_QUALIFIER} — inputs ambigus.</li>
 * </ul>
 */
export type MpFedrisReconnaissanceVerdict =
  | 'MALADIE_LISTE_FERMEE_PRESOMPTION'
  | 'MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE'
  | 'SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE'
  | 'SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE'
  | 'DECLARATION_PRESCRITE'
  | 'A_QUALIFIER';

/**
 * Body POST
 * /api/v1/case-files/:caseFileId/decision-tools/mp-fedris-reconnaissance.
 */
export interface MpFedrisReconnaissanceRequest {
  /** Qualification de la maladie. */
  typeMaladie: MpFedrisReconnaissanceTypeMaladie;
  /**
   * Code de la maladie selon la nomenclature AR 28/03/1969 (sections
   * 1.x a 6.x). Obligatoire si typeMaladie = LISTE_FERMEE.
   */
  codeMaladieListe: string | null;
  /** Libelle medical de la maladie. */
  libelleMaladie: string;
  /** Date de premiere constatation medicale (ISO yyyy-MM-dd). */
  dateConstatationMedicale: string;
  /**
   * Date de connaissance du caractere professionnel — point de depart
   * du delai de prescription triennal. Si null, le calculateur prend
   * dateConstatationMedicale.
   */
  dateConnaissanceProfessionnel: string | null;
  /**
   * Date prevue de declaration Fedris (mode prospectif). Si null, le
   * calculateur prend la date du jour Europe/Brussels.
   */
  dateDeclarationEnvisagee: string | null;
  /** Exposition professionnelle au risque. */
  exposition: MpFedrisReconnaissanceExposition;
  /** Causalite directe et determinante (decisive en systeme ouvert). */
  causalite: MpFedrisReconnaissanceCausalite;
}

/**
 * Snapshot complet renvoye par POST / GET — inputs (echo) + outputs
 * (verdict, presomption, causalite, prescription) + metadonnees
 * legales.
 */
export interface MpFedrisReconnaissanceResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  typeMaladie: MpFedrisReconnaissanceTypeMaladie;
  codeMaladieListe: string | null;
  libelleMaladie: string;
  dateConstatationMedicale: string;
  dateConnaissanceProfessionnel: string | null;
  dateDeclarationEnvisagee: string | null;
  exposition: MpFedrisReconnaissanceExposition;
  causalite: MpFedrisReconnaissanceCausalite;

  // -- Outputs calcules --
  /** Verdict 6 etats. */
  verdict: MpFedrisReconnaissanceVerdict;
  /** Presomption art. 32 lois coordonnees applicable. */
  presomptionApplicable: boolean;
  /** Causalite a etablir (systeme ouvert art. 30bis). */
  causaliteRequise: boolean;
  /** Date de prescription (date ref + 3 ans, ISO yyyy-MM-dd). */
  datePrescription: string;
  /** Drapeau action prescrite au jour. */
  prescriteAuJour: boolean;
  /**
   * Jours avant prescription (negatif = jours de retard depasses,
   * positif = marge restante).
   */
  joursAvantPrescription: number;

  // -- Verdict + metadonnees legales --
  /** Code raison du verdict. */
  raison: string;
  /** Synthese humaine. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (etapes procedurales, indexation). */
  avertissement: string;
}
