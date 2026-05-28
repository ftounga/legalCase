/**
 * SF-219-10b : modèle frontend de l'outil F-219 — Statut du délégué
 * syndical BE selon la <b>CCT n° 5 du 24/05/1971</b> (Conseil National
 * du Travail — statut des délégations syndicales du personnel des
 * entreprises) + <b>CCT n° 5bis / 5ter</b> (extensions) +
 * <b>AR 26/01/1972</b> + CCT sectorielles (seuils, durée mandat, crédit
 * d'heures).
 *
 * <p><b>BE uniquement</b> — pas d'équivalent strict FR (le délégué
 * syndical FR relève du Code du travail art. L. 2143-1 et s., procédure
 * de désignation et missions distinctes). Le gate
 * {@code workspaceCountry} est porté côté composant.</p>
 *
 * <p>Outil distinct de SF-213-08
 * ({@code licenciement-be-protection-deleguee}) qui couvre uniquement
 * le volet « protection contre le licenciement » (Loi 19/03/1991).</p>
 *
 * <p>Contrat figé dans SF-219-10 backend
 * ({@code DelegueSyndicalCct5Request} +
 * {@code DelegueSyndicalCct5Response}).</p>
 */

/** Verdict 5 états de l'outil statut DS CCT 5. */
export type DelegueSyndicalCct5Verdict =
  | 'STATUT_RECONNU'
  | 'STATUT_FRAGILE_NOTIFICATION_MANQUANTE'
  | 'INELIGIBLE_ENTREPRISE_HORS_CHAMP'
  | 'INELIGIBLE_PAS_DESIGNE_PAR_OS'
  | 'A_ANALYSER';

/**
 * Statut de la désignation du délégué syndical (CCT n° 5 art. 4 à 6).
 *
 * <ul>
 *   <li>{@code DESIGNE_PAR_OS_REPRESENTATIVE} — désignation régulière
 *       par une OS représentative (CSC, FGTB, CGSLB) + notification
 *       formelle à l'employeur.</li>
 *   <li>{@code NOTIFICATION_EMPLOYEUR_MANQUANTE} — désigné par OS mais
 *       notification écrite à l'employeur absente — statut fragile.</li>
 *   <li>{@code PAS_DESIGNE_PAR_OS} — auto-proclamation ou élection
 *       interne — pas de statut CCT 5.</li>
 *   <li>{@code DESIGNATION_CONTESTEE} — contestation pendante
 *       (conflit inter-syndical, représentativité) — analyse requise.</li>
 * </ul>
 */
export type DelegueSyndicalCct5StatutDesignation =
  | 'DESIGNE_PAR_OS_REPRESENTATIVE'
  | 'NOTIFICATION_EMPLOYEUR_MANQUANTE'
  | 'PAS_DESIGNE_PAR_OS'
  | 'DESIGNATION_CONTESTEE';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/delegue-syndical-cct-5}.
 *
 * <p>Tous les champs sont obligatoires (alignés sur le backend
 * {@code @NotNull}). {@code seuilSectorielRequis} par défaut métier = 50
 * (paramétrable selon CCT sectorielle — généralement 20 ou 50).</p>
 */
export interface DelegueSyndicalCct5Request {
  /** Date de désignation du délégué syndical par l'OS. */
  dateDesignation: string;
  /** Nombre de travailleurs dans l'entreprise (référence sectorielle). */
  effectifEntreprise: number;
  /** Seuil minimum sectoriel applicable (par défaut 50, paramétrable). */
  seuilSectorielRequis: number;
  /** Au moins une OS représentative (CSC / FGTB / CGSLB) présente. */
  presenceOsRepresentative: boolean;
  /** Statut de la désignation (4 cas). */
  statutDesignation: DelegueSyndicalCct5StatutDesignation;
  /** Conseil d'entreprise existant. */
  ceExistant: boolean;
  /** Comité PPT existant. */
  cpptExistant: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs métier.
 */
export interface DelegueSyndicalCct5Response {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateDesignation: string;
  effectifEntreprise: number;
  seuilSectorielRequis: number;
  presenceOsRepresentative: boolean;
  statutDesignation: DelegueSyndicalCct5StatutDesignation;
  ceExistant: boolean;
  cpptExistant: boolean;

  // -- Outputs métier --
  /** Verdict 5 états. */
  verdict: DelegueSyndicalCct5Verdict;
  /** Statut DS reconnu (RECONNU ou FRAGILE_NOTIFICATION_MANQUANTE). */
  eligibleStatutDs: boolean;
  /** Entreprise dans le champ (seuil sectoriel + présence OS). */
  entrepriseDansChamp: boolean;
  /** Désignation régulière par OS (cas 1 et 2). */
  designationReguliere: boolean;
  /** Missions exerçables (CCT 5 art. 3 + art. 24 supplétif CE/CPPT). */
  missionsExercables: string[];
  /** Date indicative de fin de mandat (+ 4 ans, durée sectorielle standard). */
  dateFinMandatIndicative: string | null;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict. */
  raison: string;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}
