/**
 * F-193 SF-193-02 — Représentation frontend de l'alignement entre un check
 * procédural F-96 (statut décidé par l'avocat) et l'outil décisionnel cible.
 *
 * Miroir du DTO `ProcedureCheckAlignmentResponse` exposé par le backend
 * (cf. SF-193-01). Pattern jumeau de {@link RetainedPisteAlignment} F-192.
 *
 * <p>matchStatus :
 * <ul>
 *   <li>ALIGNED              : check VERIFIED + valeur attendue cohérente avec
 *       l'outil cible — signal de validation positif.</li>
 *   <li>DIVERGENT            : check VERIFIED mais expectedValue diverge des
 *       paramètres ou résultats de l'outil cible.</li>
 *   <li>NON_COMPLIANT_FLAG   : check NON_COMPLIANT — signal d'alerte dans la
 *       sortie de l'outil cible (border-left rouge subtil).</li>
 *   <li>TO_VERIFY_FLAG       : check TO_CHECK — incertitude à signaler à
 *       l'avocat.</li>
 *   <li>NOT_ANALYZED         : l'outil cible n'a pas encore été analysé
 *       pour ce dossier — affiché uniquement dans le résumé tile dashboard.</li>
 *   <li>NO_TARGET_TOOL       : aucun outil cible mappé pour ce check (check
 *       transversal — compté dans la tile dashboard, exclu des sorties
 *       outils).</li>
 * </ul></p>
 */
export type ProcedureCheckMatchStatus =
  | 'ALIGNED'
  | 'DIVERGENT'
  | 'NON_COMPLIANT_FLAG'
  | 'TO_VERIFY_FLAG'
  | 'NOT_ANALYZED'
  | 'NO_TARGET_TOOL';

export type ProcedureCheckStatutLawyer = 'TO_CHECK' | 'VERIFIED' | 'NON_COMPLIANT';

export interface ProcedureCheckAlignment {
  checkId: string;
  libelle: string;
  critereCode?: string | null;
  statut: ProcedureCheckStatutLawyer;
  expectedValue?: string | null;
  raison?: string | null;
  toolIdCible?: string | null;
  matchStatus: ProcedureCheckMatchStatus;
}
