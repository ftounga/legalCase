/**
 * SF-213-07b : modèle frontend de l'outil F-213 — Procédure interne BE de
 * plainte pour harcèlement moral / sexuel.
 *
 * <p>BE uniquement — Loi du 4 août 1996 art. 32bis–32sexies + AR du
 * 10 avril 2014 relatif à la prévention des risques psychosociaux au
 * travail.</p>
 *
 * <p>Outil complémentaire de {@code F-DT-11} (qui couvre la nullité du
 * licenciement représailles BE) — {@code harcelement-be-procedure-formelle}
 * intervient <i>avant</i> toute rupture pour guider la procédure interne
 * (CPAP, demande informelle/formelle, délai 90 j d'enquête, protection
 * 12 mois représailles).</p>
 *
 * <p>Contrat figé dans SF-213-07 backend
 * ({@code HarcelementBeProcedureFormelleResponse} +
 * {@code HarcelementBeProcedureFormelleRequest}).</p>
 *
 * <p>Verdict implicite — l'outil n'est pas un analyseur de validité (pas
 * de verdict 4 états). Il pilote l'avocat via une <b>checklist</b>
 * d'actions / d'états avec statuts, dates fatales et avertissements
 * conditionnels.</p>
 */

/**
 * Type de harcèlement détecté en droit belge.
 *
 * <ul>
 *   <li>{@code MORAL} — comportement abusif portant atteinte à la dignité.</li>
 *   <li>{@code SEXUEL} — comportement non désiré à connotation sexuelle.</li>
 *   <li>{@code LES_DEUX} — cumul moral + sexuel.</li>
 * </ul>
 */
export type HarcelementBeType = 'MORAL' | 'SEXUEL' | 'LES_DEUX';

/**
 * Étape de la procédure interne (5 étapes — Loi 04/08/1996 + AR 10/04/2014).
 *
 * <ul>
 *   <li>{@code AVANT_DEPOT} — pas de plainte déposée.</li>
 *   <li>{@code DEMANDE_INFORMELLE_EN_COURS} — démarche informelle CPAP.</li>
 *   <li>{@code DEMANDE_FORMELLE_EN_COURS} — plainte formelle, enquête (90 j).</li>
 *   <li>{@code ENQUETE_TERMINEE} — examen mesures employeur / recours.</li>
 *   <li>{@code MESURE_DEFAVORABLE_APRES_PLAINTE} — présomption représailles.</li>
 * </ul>
 */
export type HarcelementBeEtape =
  | 'AVANT_DEPOT'
  | 'DEMANDE_INFORMELLE_EN_COURS'
  | 'DEMANDE_FORMELLE_EN_COURS'
  | 'ENQUETE_TERMINEE'
  | 'MESURE_DEFAVORABLE_APRES_PLAINTE';

/**
 * Taille d'entreprise (seuil 50 travailleurs — pivot CPP + CPAP).
 */
export type HarcelementBeTailleEntreprise =
  | 'MOINS_DE_50'
  | 'CINQUANTE_ET_PLUS';

/**
 * Statut d'un item de checklist.
 *
 * <ul>
 *   <li>{@code A_FAIRE} — action à initier.</li>
 *   <li>{@code EN_COURS} — action en cours.</li>
 *   <li>{@code TERMINE} — action close.</li>
 *   <li>{@code ACTIF} — état actif (protection juridique).</li>
 * </ul>
 */
export type HarcelementBeChecklistStatut =
  | 'A_FAIRE'
  | 'EN_COURS'
  | 'TERMINE'
  | 'ACTIF';

/**
 * Item de la checklist procédurale (miroir record backend
 * {@code HarcelementBeChecklistItemRecord}).
 */
export interface HarcelementBeChecklistItem {
  /** Libellé clair de l'action ou de l'état (ex. « Enquête CPAP — 90 j »). */
  item: string;
  /** Statut courant. */
  statut: HarcelementBeChecklistStatut;
  /** Date d'échéance ISO (peut être null). */
  dateEcheance: string | null;
}

/**
 * Body POST `/api/v1/case-files/{caseFileId}/decision-tools/harcelement-be-procedure-formelle`.
 *
 * <p>{@code typeHarcelement}, {@code etapeProcedure} et
 * {@code entrepriseTaille} sont obligatoires (validation backend).
 * {@code dateDepotPlainte} est requis si {@code etapeProcedure !=
 * AVANT_DEPOT}.</p>
 */
export interface HarcelementBeProcedureFormelleRequest {
  /** Obligatoire. */
  typeHarcelement: HarcelementBeType;
  /** Obligatoire. */
  etapeProcedure: HarcelementBeEtape;
  /** Requis si etapeProcedure != AVANT_DEPOT, sinon null. ISO. */
  dateDepotPlainte: string | null;
  /** L'entreprise possède-t-elle un CPAP ? */
  entreprisePossedeCPAP: boolean;
  /** Obligatoire. */
  entrepriseTaille: HarcelementBeTailleEntreprise;
  /** Mesure défavorable observée après dépôt. Défaut false. */
  mesureDefavorableApres: boolean;
  /** Optionnel — nombre de jours depuis le dépôt. */
  delaiDepuisDepotJours: number | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 */
export interface HarcelementBeProcedureFormelleResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;

  // -- Echo inputs --
  typeHarcelement: HarcelementBeType;
  etapeProcedure: HarcelementBeEtape;
  dateDepotPlainte: string | null;
  entreprisePossedeCPAP: boolean;
  entrepriseTaille: HarcelementBeTailleEntreprise;
  mesureDefavorableApres: boolean;
  delaiDepuisDepotJours: number | null;

  // -- Outputs métier --
  /** Liste contextuelle d'actions / d'états selon l'étape procédurale. */
  checklistItems: HarcelementBeChecklistItem[];
  /** Présomption de représailles active (art. 32sexies). */
  representaillesPossibles: boolean;
  /** Début protection représailles 12 mois (ISO). */
  dateDebutProtectionRepresailles: string | null;
  /** Fin protection représailles 12 mois (ISO). */
  dateFinProtectionRepresailles: string | null;
  /** Prochaine date fatale (ISO) — utilisée pour le surlignage UI. */
  prochainDelaiFatal: string | null;
  /** Base juridique humanisée (Loi 04/08/1996 + AR 10/04/2014). */
  baseJuridique: string;
  /** Avertissement éventuel (CPAP absent, fenêtre 12 mois dépassée…). */
  avertissement: string | null;
}
