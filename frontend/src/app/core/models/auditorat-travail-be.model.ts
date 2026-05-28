/**
 * SF-219-25b : modèle frontend de l'outil F-219 — Auditorat du travail
 * BE (Code judiciaire art. 138bis + Code d'instruction criminelle
 * art. 24 + Loi du 03/08/1992 sur le Code judiciaire + Loi du
 * 06/06/2010 introduisant le Code pénal social).
 *
 * <p><b>BE uniquement</b> — la France n'a pas d'auditorat du travail :
 * les infractions au droit du travail FR sont poursuivies par le
 * procureur de la République sur PV de l'inspection du travail (Code
 * du travail art. L. 8112-1 et s.). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-25 backend
 * ({@code AuditoratTravailBeRequest} +
 * {@code AuditoratTravailBeResponse}).</p>
 */

/**
 * Nature des faits soumis à l'auditorat — détermine la pertinence et
 * la route de saisine (Code pénal social Livre 2 + Code judiciaire
 * art. 578).
 */
export type AuditoratTravailBeNatureFait =
  | 'INFRACTION_PENALE_SOCIALE'
  | 'ACCIDENT_TRAVAIL_GRAVE'
  | 'TRAVAIL_NON_DECLARE_SUSPECTE'
  | 'DISCRIMINATION_PENALE'
  | 'HARCELEMENT_PENAL'
  | 'ENTRAVE_INSPECTION'
  | 'LITIGE_CIVIL_PUR'
  | 'AUTRE';

/**
 * Mode de saisine de l'auditorat envisagé par l'avocat — détermine
 * la procédure pratique recommandée (art. 63 CIC, art. 29 CIC,
 * art. 76 C. pén. soc., art. 578 C. jud.).
 */
export type AuditoratTravailBeModeSaisine =
  | 'PLAINTE_DIRECTE'
  | 'DENONCIATION_SIMPLE'
  | 'SIGNALEMENT_INSPECTION_SOCIALE'
  | 'VOIE_CIVILE_PARALLELE'
  | 'INDETERMINE';

/**
 * Verdict de l'outil — 4 routes de saisine + qualification ouverte.
 *
 * <ul>
 *   <li>{@code SAISINE_AUDITORAT_RECOMMANDEE} — voie directe par
 *       plainte ou dénonciation (art. 24 CIC + art. 138bis C. jud.).</li>
 *   <li>{@code DENONCIATION_INSPECTION_PREALABLE} — orientation
 *       vers l'inspection sociale (PV transmis art. 76 C. pén. soc.).</li>
 *   <li>{@code SAISINE_NON_PERTINENTE} — compétence tribunal du
 *       travail (art. 578 C. jud.) ou faits prescrits (art. 81
 *       C. pén. soc.).</li>
 *   <li>{@code A_QUALIFIER} — qualification ouverte, examen avocat
 *       préalable requis.</li>
 * </ul>
 */
export type AuditoratTravailBeVerdict =
  | 'SAISINE_AUDITORAT_RECOMMANDEE'
  | 'DENONCIATION_INSPECTION_PREALABLE'
  | 'SAISINE_NON_PERTINENTE'
  | 'A_QUALIFIER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/auditorat-travail-be}.
 *
 * <p>Tous les champs marqués {@code @NotNull} backend sont requis ;
 * seule la date des faits est optionnelle.</p>
 */
export interface AuditoratTravailBeRequest {
  /** Qualification de la nature des faits (8 cas). */
  natureFait: AuditoratTravailBeNatureFait;
  /** Mode de saisine envisagé par l'avocat (5 cas). */
  modeSaisineEnvisage: AuditoratTravailBeModeSaisine;
  /**
   * Date de commission des faits (facultatif) — conditionne la
   * prescription pénale (art. 81 C. pén. soc. : 5 ans niveaux 2-4,
   * 3 ans niveau 1).
   */
  dateFaits: string | null;
  /** Faits prescrits (drapeau explicite de l'avocat — art. 81 C. pén. soc.). */
  faitsPrescrits: boolean;
  /** Inspection déjà saisie / PV existe (art. 76 C. pén. soc.). */
  inspectionDejaSaisie: boolean;
  /** Plainte civile parallèle déposée (art. 4 Titre prél. CIC). */
  plainteCivileDeposee: boolean;
  /** Urgence sécurité personnes (accident grave imminent, harcèlement avec atteinte). */
  urgenceSecuritePersonnes: boolean;
  /** L'avocat envisage la voie pénale comme axe principal. */
  recoursPenalEnvisage: boolean;
  /** Employeur personne morale (art. 105 C. pén. soc. multiplication × 5). */
  employeurPersonneMorale: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + verdict +
 * drapeaux d'analyse + métadonnées légales.
 */
export interface AuditoratTravailBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  natureFait: AuditoratTravailBeNatureFait;
  modeSaisineEnvisage: AuditoratTravailBeModeSaisine;
  dateFaits: string | null;
  faitsPrescrits: boolean;
  inspectionDejaSaisie: boolean;
  plainteCivileDeposee: boolean;
  urgenceSecuritePersonnes: boolean;
  recoursPenalEnvisage: boolean;
  employeurPersonneMorale: boolean;

  // -- Outputs --
  /** Verdict d'orientation (4 routes ou A_QUALIFIER). */
  verdict: AuditoratTravailBeVerdict;
  /** Mode de saisine recommandé par l'outil (5 cas). */
  modeSaisineRecommande: AuditoratTravailBeModeSaisine;
  /** Constitution partie civile recommandée (art. 63 CIC). */
  constitutionPartieCivileRecommandee: boolean;
  /** Inspection sociale préalable requise (art. 76 C. pén. soc.). */
  inspectionPrealableRequise: boolean;
  /** Voie civile concurrente possible (art. 4 Titre prél. CIC). */
  voieCivileConcurrente: boolean;
  /** Prescription pénale bloquante (art. 81 C. pén. soc.). */
  prescriptionBloquante: boolean;
  /** Urgence à signaler à l'auditeur (mesures conservatoires). */
  urgenceSignalee: boolean;
  /** Règle « Una Via » applicable au choix de l'auditeur (art. 73 C. pén. soc.). */
  unaViaApplicable: boolean;
  /** Avis d'auditeur obligatoire en voie civile (art. 138bis § 1er C. jud., art. 580/581). */
  avisAuditeurObligatoireCivil: boolean;

  // -- Verdict + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (Una Via, opportunité poursuites, prescription). */
  avertissement: string;
}
