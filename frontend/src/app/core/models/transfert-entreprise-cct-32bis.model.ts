/**
 * SF-219-08b : modèle frontend de l'outil F-219 — Transfert d'entreprise
 * CCT n° 32bis du 07/06/1985 (transfert conventionnel BE — Loi 17/03/1965 ;
 * Directive 2001/23/CE).
 *
 * <p><b>BE uniquement</b> — l'art. L. 1224-1 FR pose un régime parent
 * (maintien de plein droit des contrats en cas de transfert d'entité
 * économique gardant son identité) mais sans les spécificités
 * procédurales CCT 32bis / Loi 17/03/1965 (info-consultation préalable
 * sanctionnée pénalement, responsabilité solidaire 1 an cédant /
 * cessionnaire).</p>
 *
 * <p>Contrat figé dans SF-219-08 backend
 * ({@code TransfertEntrepriseCct32bisRequest} +
 * {@code TransfertEntrepriseCct32bisResponse}).</p>
 */

/** Verdict 5 états de l'outil transfert CCT 32bis BE. */
export type TransfertEntrepriseCct32bisVerdict =
  | 'TRANSFERT_CONFORME'
  | 'TRANSFERT_NON_CONFORME_INFO_CONSULT'
  | 'INELIGIBLE_TYPE_OPERATION'
  | 'INELIGIBLE_PAS_ENTITE_ECONOMIQUE'
  | 'A_ANALYSER';

/**
 * Type d'opération déclaré — détermine l'applicabilité CCT 32bis.
 *
 * <ul>
 *   <li>{@code VENTE_FONDS_COMMERCE} — vente du fonds / universalité
 *       (qualifie pleinement si entité gardée).</li>
 *   <li>{@code FUSION_ABSORPTION} — fusion par absorption (CSA)
 *       (qualifie).</li>
 *   <li>{@code SCISSION} — scission totale ou partielle (qualifie pour
 *       les branches transférées).</li>
 *   <li>{@code APPORT_UNIVERSALITE} — apport d'une branche d'activité ou
 *       d'une universalité (qualifie si entité gardée).</li>
 *   <li>{@code DEMEMBREMENT} — démembrement / réorganisation
 *       conventionnelle (qualifie selon analyse d'entité économique).</li>
 *   <li>{@code FAILLITE} — faillite judiciaire (non qualifiant — régime
 *       CCT 102).</li>
 *   <li>{@code LIQUIDATION_JUDICIAIRE} — liquidation judiciaire (non
 *       qualifiant).</li>
 *   <li>{@code CESSION_ACTIONS} — cession d'actions / parts (non
 *       qualifiant — la personne morale employeur ne change pas).</li>
 *   <li>{@code TRANSFERT_INTRA_GROUPE_SANS_CESSION} — réorganisation
 *       intra-groupe sans cession véritable (non qualifiant).</li>
 * </ul>
 */
export type TransfertEntrepriseCct32bisTypeOperation =
  | 'VENTE_FONDS_COMMERCE'
  | 'FUSION_ABSORPTION'
  | 'SCISSION'
  | 'APPORT_UNIVERSALITE'
  | 'DEMEMBREMENT'
  | 'FAILLITE'
  | 'LIQUIDATION_JUDICIAIRE'
  | 'CESSION_ACTIONS'
  | 'TRANSFERT_INTRA_GROUPE_SANS_CESSION';

/**
 * Appréciation de la préservation de l'identité économique de l'entité
 * transférée (CJUE 11/03/1997 Süzen, CJUE 20/11/2003 Abler, Cass. BE
 * 17/05/1999, Cass. BE 14/02/2011).
 */
export type TransfertEntrepriseCct32bisIdentiteEconomique =
  | 'PRESERVEE'
  | 'NON_PRESERVEE'
  | 'A_ANALYSER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/transfert-entreprise-cct-32bis}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface TransfertEntrepriseCct32bisRequest {
  /** Date effective du transfert (date de cession / fusion / scission). */
  dateTransfert: string;
  /** Nature juridique de l'opération. */
  typeOperation: TransfertEntrepriseCct32bisTypeOperation;
  /** Appréciation de la préservation de l'entité économique. */
  identiteEconomique: TransfertEntrepriseCct32bisIdentiteEconomique;
  /** Procédure d'information-consultation préalable respectée (CE / DS / CPPT). */
  infoConsultPrealableRespectee: boolean;
  /** Conseil d'entreprise consulté préalablement (CE existant). */
  conseilEntrepriseConsulte: boolean;
  /** Cessionnaire déclare maintenir automatiquement les contrats. */
  maintienContratsAutomatique: boolean;
  /** Ancienneté reprise intégralement par le cessionnaire. */
  maintienAncienneteRespecte: boolean;
  /** Rémunération maintenue au moins au niveau pré-transfert. */
  maintienRemunerationRespecte: boolean;
  /** CCT applicables au cédant maintenues jusqu'à échéance / nouvelle CCT. */
  conventionsCollectivesMaintenues: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs métier.
 */
export interface TransfertEntrepriseCct32bisResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateTransfert: string;
  typeOperation: TransfertEntrepriseCct32bisTypeOperation;
  identiteEconomique: TransfertEntrepriseCct32bisIdentiteEconomique;
  infoConsultPrealableRespectee: boolean;
  conseilEntrepriseConsulte: boolean;
  maintienContratsAutomatique: boolean;
  maintienAncienneteRespecte: boolean;
  maintienRemunerationRespecte: boolean;
  conventionsCollectivesMaintenues: boolean;

  // -- Outputs métier --
  verdict: TransfertEntrepriseCct32bisVerdict;
  /** True si transfert qualifié au sens CCT 32bis (qualif + identité). */
  eligibleCct32bis: boolean;
  /** True si procédure info-consultation préalable conforme. */
  procedureConforme: boolean;
  /** True si maintien automatique des contrats déclaré + ancienneté + rémunération + CCT. */
  droitsMaintenus: boolean;
  /** Date de fin de la responsabilité solidaire 1 an (dateTransfert + 1 an). */
  dateFinResponsabiliteSolidaire: string;

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
