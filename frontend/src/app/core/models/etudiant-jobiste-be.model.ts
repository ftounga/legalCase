/**
 * SF-219-13b : modèle frontend de l'outil F-219 — Étudiant jobiste (Belgique).
 *
 * <p><b>BE uniquement</b> — Loi du 03/07/1978 sur les contrats de travail
 * (Titre VII, art. 120 à 130ter — contrat d'occupation d'étudiants) +
 * Loi-programme du 24/12/2002 (cotisations de solidarité réduites) +
 * AR du 14/07/1995 (modalités — 5,42 % patronale + 2,71 % personnelle =
 * 8,13 %) + AR du 05/11/2002 (Dimona spécifique type STU) + Loi du
 * 18/12/2016 (passage 50 jours → 475 heures/an) + Loi-programme du
 * 28/12/2022 + AR du 06/03/2023 (relèvement transitoire 600h/an
 * 2023-2024) + Loi-programme du 22/12/2023 (M.B. 29/12/2023) pérennisant
 * le quota à 600 heures/an.</p>
 *
 * <p>La France n'a pas d'équivalent direct au régime étudiant jobiste BE
 * (le « job étudiant » FR relève du droit commun salarié avec
 * abattements de cotisations limités par l'art. L. 241-3-2 CSS, mais
 * sans quota horaire annuel ni régime spécifique unifié). Aucun mapping
 * mécanique possible.</p>
 *
 * <p>Contrat figé dans SF-219-13 backend
 * ({@code EtudiantJobisteBeRequest} + {@code EtudiantJobisteBeResponse}).</p>
 */

/**
 * Statut du candidat à l'occupation étudiant jobiste BE (Loi du
 * 03/07/1978, art. 120 — conditions d'accès au régime).
 *
 * <ul>
 *   <li>{@code ETUDIANT_PRINCIPAL} — inscription régulière en cours
 *       d'année académique ; statut étudiant pleinement reconnu (accès
 *       au régime jobiste sans restriction de fond).</li>
 *   <li>{@code INTERRUPTION_COURTE_MOINS_12_MOIS} — entre deux cycles
 *       (p. ex. juillet-août-septembre entre secondaire et supérieur,
 *       ou entre bachelier et master) ; tolérance ≤ 12 mois consécutifs
 *       — accès maintenu.</li>
 *   <li>{@code VIDE_PEDAGOGIQUE_PROLONGE} — interruption &gt; 12 mois
 *       mais inscription confirmée pour l'année suivante ; statut en
 *       <b>zone grise</b> nécessitant une analyse juridique (avocat BE).</li>
 *   <li>{@code NON_ETUDIANT} — pas d'inscription régulière, pas
 *       d'interruption assimilable ; <b>pas de statut jobiste</b>
 *       possible (catégorie résiduelle).</li>
 * </ul>
 */
export type EtudiantJobisteBeStatutEtudiant =
  | 'ETUDIANT_PRINCIPAL'
  | 'INTERRUPTION_COURTE_MOINS_12_MOIS'
  | 'VIDE_PEDAGOGIQUE_PROLONGE'
  | 'NON_ETUDIANT';

/**
 * Verdict hiérarchisé 6 états de l'outil étudiant jobiste BE — ordre
 * de priorité statut → vide pédagogique → quota → formalisme →
 * cotisations.
 *
 * <ul>
 *   <li>{@code ELIGIBLE} — occupation pleinement régulière (vert).</li>
 *   <li>{@code FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} — statut + quota OK
 *       mais formalisme défaillant (ambre — risque requalification
 *       ONSS au régime ordinaire + récupération cotisations).</li>
 *   <li>{@code FRAGILE_COTISATIONS_NON_REDUITES} — statut + formalisme
 *       OK mais cotisations déclarées hors barème AR 14/07/1995 (ambre
 *       — risque redressement ONSS pour calcul erroné).</li>
 *   <li>{@code INELIGIBLE_STATUT_NON_ETUDIANT} — pas étudiant à titre
 *       principal ni en interruption courte ≤ 12 mois (rouge).</li>
 *   <li>{@code INELIGIBLE_QUOTA_DEPASSE} — les heures du contrat font
 *       basculer le compteur annuel au-delà du quota 600h ; la part
 *       excédentaire passe au régime ordinaire (rouge).</li>
 *   <li>{@code A_ANALYSER} — vide pédagogique &gt; 12 mois mais
 *       inscription confirmée pour l'année suivante — zone grise
 *       jurisprudentielle, analyse avocat BE requise (gris info).</li>
 * </ul>
 */
export type EtudiantJobisteBeVerdict =
  | 'ELIGIBLE'
  | 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT'
  | 'FRAGILE_COTISATIONS_NON_REDUITES'
  | 'INELIGIBLE_STATUT_NON_ETUDIANT'
  | 'INELIGIBLE_QUOTA_DEPASSE'
  | 'A_ANALYSER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/etudiant-jobiste-be}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface EtudiantJobisteBeRequest {
  /** Date de début des prestations. */
  dateDebutOccupation: string;
  /** Catégorie du candidat (étudiant principal / interruption / vide / non). */
  statutEtudiant: EtudiantJobisteBeStatutEtudiant;
  /** Compteur Student@work consolidé avant signature du contrat (≥ 0). */
  heuresDejaPresteesDansAnnee: number;
  /** Durée du contrat en heures (&gt; 0). */
  heuresContratEnCours: number;
  /** Quota légal en vigueur (= 600 depuis 2023, &gt; 0). */
  quotaAnnuelHeures: number;
  /** Contrat d'occupation étudiant écrit signé (art. 123 Loi 03/07/1978). */
  contratEcritSigne: boolean;
  /** Déclaration Dimona spécifique STU faite (AR 05/11/2002). */
  dimonaStuDeclaree: boolean;
  /** Cotisations de solidarité réduites appliquées (5,42 % + 2,71 % = 8,13 %). */
  cotisationsReduitesAppliquees: boolean;
  /** Taux horaire brut convenu en € (≥ 0). */
  remunerationBruteHoraire: number;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + verdict +
 * ventilation 4 dimensions cumulatives (statut / quota / formalisme /
 * cotisations) + heures restantes + heures hors quota + montant
 * redressement estimé.
 */
export interface EtudiantJobisteBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateDebutOccupation: string;
  statutEtudiant: EtudiantJobisteBeStatutEtudiant;
  heuresDejaPresteesDansAnnee: number;
  heuresContratEnCours: number;
  quotaAnnuelHeures: number;
  contratEcritSigne: boolean;
  dimonaStuDeclaree: boolean;
  cotisationsReduitesAppliquees: boolean;
  remunerationBruteHoraire: number;

  // -- Verdict --
  verdict: EtudiantJobisteBeVerdict;
  /** Condition statut respectée. */
  statutEligible: boolean;
  /** Condition quota respectée (heuresDeja + heuresContrat ≤ quota). */
  quotaRespecte: boolean;
  /** Formalisme cumulatif respecté (contrat écrit + Dimona STU). */
  formalismeRespecte: boolean;
  /** Cotisations de solidarité réduites appliquées (8,13 %). */
  cotisationsConformes: boolean;
  /** Heures restantes au quota avant ce contrat. */
  heuresRestantesAvantContrat: number;
  /** Heures hors quota (basculent au régime ordinaire). */
  heuresHorsQuota: number;
  /** Montant indicatif du redressement (différentiel ~41,87 %), en €. */
  montantRedressementEstime: number;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}
