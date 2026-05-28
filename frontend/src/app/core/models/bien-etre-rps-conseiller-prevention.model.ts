/**
 * SF-219-30b : modele frontend de l'outil F-219 — Saisine du Conseiller
 * en Prevention Aspects Psychosociaux (CPAP) procedure RPS interne BE
 * (Loi du 04/08/1996 art. 32sexies + AR du 10/04/2014).
 *
 * <p><b>BE uniquement</b> — le regime francais (Code du travail FR
 * art. L. 4121-1 et s. obligation employeur de securite, referent
 * harcelement art. L. 1153-5-1, document unique d'evaluation des
 * risques art. R. 4121-1) est juridiquement distinct (pas de CPAP
 * mais des dispositifs different : referent harcelement, CSE,
 * services de sante au travail SST AR 22/02/1989, mediation
 * interne facultative non encadree par les delais 10 j / 3 mois /
 * 2 mois). Aucune transposition mecanique. Le gate
 * {@code workspaceCountry} est porte cote composant ; le backend
 * renvoie 404 pour FR par coherence.</p>
 *
 * <p>Contrat fige dans SF-219-30 backend
 * ({@code BienEtreRpsConseillerPreventionRequest} +
 * {@code BienEtreRpsConseillerPreventionResponse}).</p>
 *
 * <p><b>Articulation avec les outils harcelement BE existants</b> :
 * cet outil intervient en <b>amont</b> et qualifie la conformite
 * procedurale de la saisine elle-meme (entretien prealable,
 * formalisme, delais legaux). La SF-213-07
 * {@code harcelement-be-procedure-formelle} couvre la <b>suite</b>
 * (checklist detaillee d'une demande formelle deja deposee +
 * protection 12 mois art. 32sexies). La F-DT-30
 * {@code harcelement-licenciement-nul-section} couvre la
 * <b>consequence</b> en cas de represailles (nullite licenciement).</p>
 */

/**
 * Type de risque psychosocial concerne par la saisine — Loi
 * 04/08/1996 art. 32/1 § 2.
 *
 * <ul>
 *   <li>{@code HARCELEMENT_MORAL} — Loi 04/08/1996 art. 32bis
 *       (conduite abusive et repetee).</li>
 *   <li>{@code HARCELEMENT_SEXUEL} — Loi 04/08/1996 art. 32bis al. 3
 *       (comportement non desire a connotation sexuelle).</li>
 *   <li>{@code VIOLENCE_AU_TRAVAIL} — Loi 04/08/1996 art. 32bis al. 1
 *       (menace ou agression).</li>
 *   <li>{@code AUTRES_RPS} — Loi 04/08/1996 art. 32/1 § 2 (stress,
 *       burn-out, conflits interpersonnels qualifies).</li>
 * </ul>
 */
export type BienEtreRpsConseillerPreventionTypeRisque =
  | 'HARCELEMENT_MORAL'
  | 'HARCELEMENT_SEXUEL'
  | 'VIOLENCE_AU_TRAVAIL'
  | 'AUTRES_RPS';

/**
 * Mode de demande d'intervention psychosociale — AR 10/04/2014
 * art. 11 a 32.
 *
 * <ul>
 *   <li>{@code INFORMELLE} — mediation interne / conciliation, sans
 *       rapport employeur (art. 14 a 16, confidentielle).</li>
 *   <li>{@code FORMELLE_INDIVIDUELLE} — enquete ecrite individualisee
 *       avec rapport au employeur (art. 16 a 22).</li>
 *   <li>{@code FORMELLE_COLLECTIVE} — analyse organisationnelle
 *       (art. 16 § 2).</li>
 * </ul>
 */
export type BienEtreRpsConseillerPreventionModeDemande =
  | 'INFORMELLE'
  | 'FORMELLE_INDIVIDUELLE'
  | 'FORMELLE_COLLECTIVE';

/**
 * Etape procedurale de la saisine — AR 10/04/2014 art. 11 a 32.
 *
 * <ol>
 *   <li>{@code INTENTION_DEMANDE} — consultation personne de
 *       confiance art. 31.</li>
 *   <li>{@code ENTRETIEN_PREALABLE_REALISE} — entretien obligatoire
 *       art. 16 § 1, delai 10 jours calendrier.</li>
 *   <li>{@code DEMANDE_INFORMELLE_EN_COURS} — mediation interne.</li>
 *   <li>{@code DEMANDE_FORMELLE_DEPOSEE} — accuse de reception remis,
 *       3 mois pour avis art. 22 § 1.</li>
 *   <li>{@code AVIS_RENDU} — rapport motive transmis ; employeur a
 *       2 mois art. 32.</li>
 *   <li>{@code MESURES_PRISES_PAR_EMPLOYEUR} — cloture procedure
 *       interne ; voies externes (Inspection art. 80, tribunal du
 *       travail art. 578 C. jud.).</li>
 * </ol>
 */
export type BienEtreRpsConseillerPreventionEtape =
  | 'INTENTION_DEMANDE'
  | 'ENTRETIEN_PREALABLE_REALISE'
  | 'DEMANDE_INFORMELLE_EN_COURS'
  | 'DEMANDE_FORMELLE_DEPOSEE'
  | 'AVIS_RENDU'
  | 'MESURES_PRISES_PAR_EMPLOYEUR';

/**
 * Verdict 8 etats de l'outil — qualification de la conformite
 * procedurale (l'outil ne se prononce pas sur le fond du
 * harcelement).
 *
 * <ul>
 *   <li>{@code SAISINE_CONFORME} — toutes formalites prealables
 *       remplies (CPAP identifie, entretien prealable realise,
 *       demande ecrite signee et accusee si formelle).</li>
 *   <li>{@code SAISINE_INFORMELLE_EN_COURS} — mediation interne en
 *       cours, pas de delai legal strict.</li>
 *   <li>{@code SAISINE_FORMELLE_EN_COURS} — demande formelle deposee,
 *       enquete dans les 3 mois legaux.</li>
 *   <li>{@code AVIS_RENDU_DELAI_RESPECTE} — avis motive rendu dans
 *       les 3 mois (ou prolongation motivee 6 mois max).</li>
 *   <li>{@code AVIS_RENDU_DELAI_DEPASSE} — avis rendu hors delai ;
 *       irregularite mais avis valide.</li>
 *   <li>{@code NON_CONFORME_FORMALITES_MANQUANTES} — entretien
 *       prealable non realise, demande non ecrite ou non signee,
 *       AR manquant.</li>
 *   <li>{@code NON_CONFORME_PAS_DE_CONSEILLER} — aucun CPAP identifie,
 *       manquement employeur art. 32sexies § 2.</li>
 *   <li>{@code A_QUALIFIER} — inputs insuffisants ou contradictoires.</li>
 * </ul>
 */
export type BienEtreRpsConseillerPreventionVerdict =
  | 'SAISINE_CONFORME'
  | 'SAISINE_INFORMELLE_EN_COURS'
  | 'SAISINE_FORMELLE_EN_COURS'
  | 'AVIS_RENDU_DELAI_RESPECTE'
  | 'AVIS_RENDU_DELAI_DEPASSE'
  | 'NON_CONFORME_FORMALITES_MANQUANTES'
  | 'NON_CONFORME_PAS_DE_CONSEILLER'
  | 'A_QUALIFIER';

/**
 * Body POST
 * /api/v1/case-files/:caseFileId/decision-tools/bien-etre-rps-conseiller-prevention.
 */
export interface BienEtreRpsConseillerPreventionRequest {
  /** Qualification provisoire du risque (Loi 04/08/1996 art. 32/1 § 2). */
  typeRisque: BienEtreRpsConseillerPreventionTypeRisque;
  /** Mode de demande (AR 10/04/2014 art. 14 a 16). */
  modeDemande: BienEtreRpsConseillerPreventionModeDemande;
  /** Etape de l'avancement de la procedure. */
  etapeProcedure: BienEtreRpsConseillerPreventionEtape;
  /** CPAP interne ou externe identifie et designe (art. 32sexies § 2). */
  conseillerIdentifie: boolean;
  /** Entretien prealable obligatoire avant demande formelle realise
   *  (art. 16 § 1 AR 10/04/2014, delai 10 jours). */
  entretienPrealableRealise: boolean;
  /** Demande formelle ecrite et signee par le travailleur
   *  (art. 16 § 2 AR 10/04/2014). */
  demandeEcriteSignee: boolean;
  /** Accuse de reception remis au travailleur par le CPAP
   *  (art. 16 § 4 AR 10/04/2014). */
  accuseReceptionRecu: boolean;
  /** Notification de la demande a l'employeur faite par le CPAP
   *  apres acceptation (art. 17 § 1 AR 10/04/2014). */
  notificationEmployeurFaite: boolean;
  /** Date de depot de la demande (informelle ou formelle, ISO yyyy-MM-dd) —
   *  base du calcul du delai 3 mois enquete et 2 mois mesures employeur. */
  dateDepotDemande: string | null;
  /** Date de l'avis motive rendu par le CPAP (ISO yyyy-MM-dd) —
   *  renseignee si etape AVIS_RENDU ou posterieure. */
  dateAvisRendu: string | null;
  /** Jours depuis le depot — calcule en amont par le frontend. */
  joursDepuisDepot: number | null;
}

/**
 * Snapshot complet renvoye par POST / GET — inputs (echo) + outputs
 * analytiques + metadonnees legales.
 */
export interface BienEtreRpsConseillerPreventionResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  typeRisque: BienEtreRpsConseillerPreventionTypeRisque;
  modeDemande: BienEtreRpsConseillerPreventionModeDemande;
  etapeProcedure: BienEtreRpsConseillerPreventionEtape;
  conseillerIdentifie: boolean;
  entretienPrealableRealise: boolean;
  demandeEcriteSignee: boolean;
  accuseReceptionRecu: boolean;
  notificationEmployeurFaite: boolean;
  dateDepotDemande: string | null;
  dateAvisRendu: string | null;
  joursDepuisDepot: number | null;

  // -- Outputs analytiques --
  /** Verdict 8 etats. */
  verdict: BienEtreRpsConseillerPreventionVerdict;
  /** Formalites prealables completes (entretien + ecrit + AR). */
  formalitesPrealablesCompletes: boolean;
  /** Delai 3 mois enquete (art. 22 § 1) respecte. */
  delaiEnqueteRespecte: boolean;
  /** Echeance du delai 3 mois enquete (ISO yyyy-MM-dd). */
  echeanceEnqueteTroisMois: string | null;
  /** Echeance du delai 2 mois mesures employeur (art. 32) (ISO yyyy-MM-dd). */
  echeanceMesuresEmployeurDeuxMois: string | null;
  /** Fin de la protection 12 mois contre les represailles
   *  (art. 32sexies) (ISO yyyy-MM-dd). */
  finProtectionRepresailles: string | null;
  /** Protection 12 mois actuellement active. */
  protectionRepresaillesActive: boolean;

  // -- Synthese + metadonnees legales --
  /** Code raison du verdict. */
  raison: string;
  /** Synthese humaine. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (etapes, protection, voies externes). */
  avertissement: string;
}
