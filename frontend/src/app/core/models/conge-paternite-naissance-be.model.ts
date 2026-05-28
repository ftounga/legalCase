/**
 * SF-219-31b : modele frontend de l'outil F-219 — Conge paternite /
 * naissance BE (Loi du 03/07/1978 art. 30 paragr. 2 sur les contrats
 * de travail + Loi du 12/08/2000 portant des dispositions sociales,
 * budgetaires et diverses + Loi du 07/04/2023 portant des dispositions
 * en matiere de Deal pour l'emploi 2022 — extension a tous les
 * co-parents et alignement durees + AR du 17/10/1994 sur les
 * modalites d'information et de prise + AR du 03/07/1996 indemnisation
 * INAMI).
 *
 * <p><b>BE uniquement</b> — le regime francais du conge de paternite
 * et d'accueil de l'enfant (Code du travail FR art. L. 1225-35 et s.,
 * Loi de financement de la securite sociale 2021 portant la duree a
 * 25 jours calendaires dont 7 obligatoires, indemnise par la CPAM
 * via les IJSS Maternite art. L. 331-8 CSS) est juridiquement
 * distinct dans sa duree (25 jours calendaires FR vs 20 jours
 * ouvrables BE depuis 2023), son fractionnement, sa periode de
 * prise (4 mois post-naissance FR + reportable en France vs fenetre
 * de 4 mois BE plus stricte), et son indemnisation (CPAM IJSS
 * journalieres a 100 pour cent du gain journalier de base
 * plafonne FR vs employeur 100 pour cent les 3 premiers jours puis
 * INAMI 82 pour cent plafonne les jours suivants BE).
 * Aucune transposition mecanique. Le gate
 * {@code workspaceCountry} est porte cote composant ; le backend
 * renvoie 404 pour FR par coherence.</p>
 *
 * <p>Contrat fige dans SF-219-31 backend
 * ({@code CongePaterniteNaissanceBeRequest} +
 * {@code CongePaterniteNaissanceBeResponse}).</p>
 */

/**
 * Statut professionnel du travailleur.
 *
 * <ul>
 *   <li>{@code SALARIE} — sous contrat de travail (Loi 03/07/1978
 *       art. 30 paragr. 2).</li>
 *   <li>{@code INDEPENDANT} — releve du regime distinct Loi
 *       13/04/2019 + AR 23/05/2019 — 20 jours forfaitaires INASTI.</li>
 *   <li>{@code AGENT_PUBLIC_STATUTAIRE} — regime sectoriel AR
 *       19/11/1998 art. 23 et s.</li>
 *   <li>{@code AUTRE} — statut a qualifier par avocat.</li>
 * </ul>
 */
export type CongePaterniteNaissanceBeStatutTravailleur =
  | 'SALARIE'
  | 'INDEPENDANT'
  | 'AGENT_PUBLIC_STATUTAIRE'
  | 'AUTRE';

/**
 * Lien de filiation ou parental ouvrant droit au conge de naissance.
 *
 * <ul>
 *   <li>{@code PERE_BIOLOGIQUE} — pere etabli par reconnaissance ou
 *       presomption de paternite art. 315 C. civ.</li>
 *   <li>{@code COPARENT_COMATERNITE} — comaternite art. 325/2
 *       C. civ.</li>
 *   <li>{@code COPARENT_RECONNAISSANCE} — co-parent ayant reconnu
 *       l'enfant art. 327/1 C. civ.</li>
 *   <li>{@code COHABITANT_LEGAL_OU_MARIE} — cohabitant legal ou
 *       conjoint de la mere sans filiation propre (Loi
 *       07/04/2023).</li>
 *   <li>{@code AUTRE} — autre situation a qualifier.</li>
 * </ul>
 */
export type CongePaterniteNaissanceBeLienFiliation =
  | 'PERE_BIOLOGIQUE'
  | 'COPARENT_COMATERNITE'
  | 'COPARENT_RECONNAISSANCE'
  | 'COHABITANT_LEGAL_OU_MARIE'
  | 'AUTRE';

/**
 * Etape procedurale d'avancement du droit.
 *
 * <ul>
 *   <li>{@code AVANT_NAISSANCE} — droit en formation.</li>
 *   <li>{@code NAISSANCE_SURVENUE} — delai 4 mois ouvert.</li>
 *   <li>{@code NOTIFICATION_EMPLOYEUR_FAITE} — formalite AR
 *       17/10/1994 effectuee.</li>
 *   <li>{@code CONGE_EN_COURS_DE_PRISE} — protection 5 mois
 *       art. 30 paragr. 4 active.</li>
 *   <li>{@code CONGE_PRIS_ENTIEREMENT} — clos, protection 5 mois
 *       residuelle.</li>
 *   <li>{@code DELAI_QUATRE_MOIS_DEPASSE} — droit perdu sauf reports
 *       legaux.</li>
 * </ul>
 */
export type CongePaterniteNaissanceBeEtape =
  | 'AVANT_NAISSANCE'
  | 'NAISSANCE_SURVENUE'
  | 'NOTIFICATION_EMPLOYEUR_FAITE'
  | 'CONGE_EN_COURS_DE_PRISE'
  | 'CONGE_PRIS_ENTIEREMENT'
  | 'DELAI_QUATRE_MOIS_DEPASSE';

/**
 * Verdict 8 etats de l'outil.
 *
 * <ul>
 *   <li>{@code ELIGIBLE_CONGE_OUVERT} — droit ouvert, en attente de
 *       prise ou prise partielle.</li>
 *   <li>{@code CONGE_EN_COURS_PROTECTION_ACTIVE} — prise en cours,
 *       protection 5 mois licenciement active art. 30 paragr. 4.</li>
 *   <li>{@code CONGE_PRIS_PROTECTION_RESIDUELLE} — totalement pris,
 *       protection 5 mois residuelle.</li>
 *   <li>{@code INELIGIBLE_STATUT_NON_COUVERT} — statut hors champ
 *       Loi 03/07/1978.</li>
 *   <li>{@code INELIGIBLE_FILIATION_NON_ETABLIE} — lien parental non
 *       etabli.</li>
 *   <li>{@code DROIT_PERDU_DELAI_DEPASSE} — delai 4 mois depasse sans
 *       cas de report legal (incapacite mere, hospitalisation
 *       enfant).</li>
 *   <li>{@code INELIGIBLE_NAISSANCE_FUTURE} — calcul indicatif
 *       seulement.</li>
 *   <li>{@code A_QUALIFIER} — inputs insuffisants.</li>
 * </ul>
 */
export type CongePaterniteNaissanceBeVerdict =
  | 'ELIGIBLE_CONGE_OUVERT'
  | 'CONGE_EN_COURS_PROTECTION_ACTIVE'
  | 'CONGE_PRIS_PROTECTION_RESIDUELLE'
  | 'INELIGIBLE_STATUT_NON_COUVERT'
  | 'INELIGIBLE_FILIATION_NON_ETABLIE'
  | 'DROIT_PERDU_DELAI_DEPASSE'
  | 'INELIGIBLE_NAISSANCE_FUTURE'
  | 'A_QUALIFIER';

/**
 * Body POST
 * /api/v1/case-files/:caseFileId/decision-tools/conge-paternite-naissance-be.
 */
export interface CongePaterniteNaissanceBeRequest {
  /** Statut professionnel du travailleur. */
  statutTravailleur: CongePaterniteNaissanceBeStatutTravailleur;
  /** Lien de filiation ou parental ouvrant droit. */
  lienFiliation: CongePaterniteNaissanceBeLienFiliation;
  /** Etape procedurale d'avancement. */
  etapeProcedure: CongePaterniteNaissanceBeEtape;
  /** Filiation etablie (acte naissance, reconnaissance, comaternite). */
  filiationEtablie: boolean;
  /** Contrat de travail salarie en cours a la naissance. */
  contratTravailEnCours: boolean;
  /** Notification a l'employeur faite (AR 17/10/1994). */
  notificationEmployeurFaite: boolean;
  /** Date de naissance de l'enfant (ISO yyyy-MM-dd). Optionnelle. */
  dateNaissance: string | null;
  /** Date de notification a l'employeur (ISO yyyy-MM-dd). Optionnelle. */
  dateNotificationEmployeur: string | null;
  /** Nombre de jours ouvrables deja pris (entier &gt;= 0). */
  joursDejaPrisOuvrables: number | null;
  /** Date du dernier jour pris si conge termine (ISO yyyy-MM-dd). */
  dateFinPriseEffective: string | null;
}

/**
 * Snapshot complet renvoye par POST / GET — inputs (echo) + outputs
 * (verdict, duree applicable, jours restants, echeances, protection)
 * + metadonnees legales.
 */
export interface CongePaterniteNaissanceBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  statutTravailleur: CongePaterniteNaissanceBeStatutTravailleur;
  lienFiliation: CongePaterniteNaissanceBeLienFiliation;
  etapeProcedure: CongePaterniteNaissanceBeEtape;
  filiationEtablie: boolean;
  contratTravailEnCours: boolean;
  notificationEmployeurFaite: boolean;
  dateNaissance: string | null;
  dateNotificationEmployeur: string | null;
  joursDejaPrisOuvrables: number | null;
  dateFinPriseEffective: string | null;

  // -- Outputs calcules --
  /** Verdict 8 etats. */
  verdict: CongePaterniteNaissanceBeVerdict;
  /** Duree applicable en jours ouvrables selon date de naissance. */
  dureeApplicableJoursOuvrables: number;
  /** Jours restants a prendre (jours ouvrables). */
  joursRestantsAPrendre: number;
  /** Date d'echeance 4 mois post-naissance (ISO yyyy-MM-dd). */
  echeanceQuatreMoisPostNaissance: string | null;
  /** Date de fin de la protection 5 mois licenciement (ISO yyyy-MM-dd). */
  finProtectionLicenciement: string | null;
  /** Protection licenciement active a date d'analyse. */
  protectionLicenciementActive: boolean;
  /** Jours pris en charge a 100 pour cent par l'employeur (3 premiers). */
  indemniteEmployeurJoursCent: number;
  /** Jours pris en charge a 82 pour cent par la mutuelle (INAMI). */
  indemniteMutuelleJoursQuatreVingtDeux: number;

  // -- Verdict + metadonnees legales --
  /** Code raison du verdict (peut etre null). */
  raison: string | null;
  /** Synthese humaine. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (modalites, indexation, fenetre 4 mois). */
  avertissement: string;
}
