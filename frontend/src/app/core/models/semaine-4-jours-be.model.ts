/**
 * SF-219-18b : modèle frontend de l'outil F-219 — semaine de 4 jours
 * BE (Loi du 03/10/2022 « Deal pour l'emploi » M.B. 10/11/2022,
 * art. 5).
 *
 * <p><b>BE uniquement</b> — la semaine de 4 jours française (C. trav.
 * art. L. 3122-2 et s. — aménagement du temps de travail par accord
 * d'entreprise, Loi 13/06/1998 dite « Aubry I » + Loi 19/01/2000
 * dite « Aubry II ») constitue un régime distinct (négociation
 * collective majoritairement, pas demande individuelle du salarié).
 * Aucun mapping mécanique avec l'outil FR. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-18 backend
 * ({@code Semaine4JoursBeRequest} + {@code Semaine4JoursBeResponse}).</p>
 */

/**
 * Statut de la demande de semaine de 4 jours — aiguille le verdict
 * (art. 5 § 4 et § 5 Loi 03/10/2022).
 *
 * <ul>
 *   <li>{@code ACCORDE_AVENANT_SIGNE} — demande acceptée, avenant
 *       écrit signé : on évalue les conditions de fond (durée
 *       hebdomadaire compressée, journée ≤ 9 h 30 ou 10 h CCT,
 *       avenant ≤ 6 mois renouvelable, règlement de travail
 *       modifié).</li>
 *   <li>{@code REFUSE_MOTIVE_PAR_ECRIT} — refus régulièrement motivé
 *       par écrit dans le délai d'1 mois — refus juridiquement
 *       opposable.</li>
 *   <li>{@code REFUSE_SANS_MOTIVATION_ECRITE} — refus sans motivation
 *       écrite ou hors délai — exposition à l'action en abus de
 *       droit.</li>
 *   <li>{@code EN_ATTENTE_REPONSE_EMPLOYEUR} — demande déposée,
 *       l'employeur n'a pas encore répondu — délai d'1 mois en
 *       cours.</li>
 *   <li>{@code LICENCIE_APRES_DEMANDE} — le travailleur a été
 *       licencié après le dépôt de la demande — bascule sur le
 *       régime de protection art. 5 § 6 (présomption de
 *       représailles).</li>
 *   <li>{@code INDETERMINE} — pas encore d'instruction (analyse
 *       cas par cas).</li>
 * </ul>
 */
export type Semaine4JoursBeStatutDemande =
  | 'ACCORDE_AVENANT_SIGNE'
  | 'REFUSE_MOTIVE_PAR_ECRIT'
  | 'REFUSE_SANS_MOTIVATION_ECRITE'
  | 'EN_ATTENTE_REPONSE_EMPLOYEUR'
  | 'LICENCIE_APRES_DEMANDE'
  | 'INDETERMINE';

/**
 * Verdict hiérarchisé 9 états — ordre de priorité (court-circuits
 * successifs côté backend {@code Semaine4JoursBeChecker}) :
 *
 * <ol>
 *   <li>{@code A_ANALYSER} — statut indéterminé, en attente
 *       employeur, refus motivé opposable ou licenciement avec motif
 *       objectif à vérifier — analyse cas par cas.</li>
 *   <li>{@code LICENCIEMENT_REPRESAILLES_PRESUME} — licenciement
 *       intervenu après la demande sans motif objectif établi par
 *       l'employeur : indemnité forfaitaire de protection = 6 mois
 *       de rémunération (art. 5 § 6).</li>
 *   <li>{@code REFUS_EMPLOYEUR_NON_MOTIVE} — refus sans motivation
 *       écrite ou hors délai d'1 mois : exposition action en abus
 *       de droit (art. 5 § 5).</li>
 *   <li>{@code NON_ELIGIBLE_TEMPS_PARTIEL} — travailleur à temps
 *       partiel : régime non ouvert (art. 5 § 1).</li>
 *   <li>{@code NON_CONFORME_DEMANDE_ECRITE_MANQUANTE} — pas de
 *       demande écrite du travailleur (art. 5 § 2) → mise en place
 *       irrégulière.</li>
 *   <li>{@code NON_CONFORME_JOURNEE_DEPASSE_9H30} — journée
 *       quotidienne &gt; 9 h 30 sans CCT autorisant 10 h
 *       (art. 5 § 3).</li>
 *   <li>{@code NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT} —
 *       absence d'avenant écrit au contrat ou de modification du
 *       règlement de travail (art. 5 § 4 + art. 6).</li>
 *   <li>{@code NON_CONFORME_DUREE_DEPASSE_6_MOIS} — durée de
 *       l'avenant &gt; 6 mois sans renouvellement explicite
 *       (art. 5 § 4).</li>
 *   <li>{@code CONFORME_REGIME_4_JOURS_VALIDE} — toutes conditions
 *       cumulatives remplies : régime valide.</li>
 * </ol>
 */
export type Semaine4JoursBeVerdict =
  | 'CONFORME_REGIME_4_JOURS_VALIDE'
  | 'NON_ELIGIBLE_TEMPS_PARTIEL'
  | 'NON_CONFORME_DEMANDE_ECRITE_MANQUANTE'
  | 'NON_CONFORME_JOURNEE_DEPASSE_9H30'
  | 'NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT'
  | 'NON_CONFORME_DUREE_DEPASSE_6_MOIS'
  | 'REFUS_EMPLOYEUR_NON_MOTIVE'
  | 'LICENCIEMENT_REPRESAILLES_PRESUME'
  | 'A_ANALYSER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/semaine-4-jours-be}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}) sauf
 * {@code dateLicenciement} qui n'est pertinente que si
 * {@code statutDemande === 'LICENCIE_APRES_DEMANDE'}.</p>
 */
export interface Semaine4JoursBeRequest {
  /** Statut courant de la demande (aiguille le verdict). */
  statutDemande: Semaine4JoursBeStatutDemande;
  /** Travailleur à temps plein (art. 5 § 1 — régime fermé aux temps partiels). */
  travailleurTempsPlein: boolean;
  /** Demande écrite du travailleur — lettre recommandée / accusé de réception (art. 5 § 2). */
  demandeEcriteTravailleur: boolean;
  /** Date de la demande écrite — point de départ du délai d'1 mois. */
  dateDemande: string;
  /** Durée hebdomadaire en heures (38 à 40 h selon CCT sectorielle). */
  dureeHebdomadaireHeures: number;
  /** Journée maximale en heures — doit être ≤ 9 h 30 (10 h si CCT) — art. 5 § 3. */
  journeeMaximaleHeures: number;
  /** CCT sectorielle autorise journée jusqu'à 10 h. */
  cctAutorise10h: boolean;
  /** Avenant écrit au contrat signé (art. 5 § 4). */
  avenantEcritSigne: boolean;
  /** Règlement de travail modifié pour inscrire l'horaire compressé (art. 6). */
  reglementTravailModifie: boolean;
  /** Durée de l'avenant en mois — 6 mois max renouvelable (art. 5 § 4). */
  dureeAvenantMois: number;
  /** Avenant explicitement renouvelé (autorise une durée &gt; 6 mois). */
  avenantRenouvele: boolean;
  /** Si statut REFUSE_* : refus motivé par écrit (art. 5 § 5). */
  refusMotiveParEcrit: boolean;
  /** Date du licenciement (si statut LICENCIE_APRES_DEMANDE). */
  dateLicenciement: string | null;
  /** Motif licenciement objectif sans rapport avec la demande (art. 5 § 6). */
  motifLicenciementObjectifEtabli: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + ventilation
 * des 5 conformités + journée maximale autorisée calculée + verdict
 * + métadonnées légales.
 */
export interface Semaine4JoursBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  statutDemande: Semaine4JoursBeStatutDemande;
  travailleurTempsPlein: boolean;
  demandeEcriteTravailleur: boolean;
  dateDemande: string;
  dureeHebdomadaireHeures: number;
  journeeMaximaleHeures: number;
  cctAutorise10h: boolean;
  avenantEcritSigne: boolean;
  reglementTravailModifie: boolean;
  dureeAvenantMois: number;
  avenantRenouvele: boolean;
  refusMotiveParEcrit: boolean;
  dateLicenciement: string | null;
  motifLicenciementObjectifEtabli: boolean;

  // -- Verdict + ventilation --
  verdict: Semaine4JoursBeVerdict;
  /** Travailleur à temps plein — art. 5 § 1. */
  eligibiliteRespectee: boolean;
  /** Demande écrite du travailleur — art. 5 § 2. */
  demandeRespectee: boolean;
  /** Journée quotidienne ≤ plafond autorisé — art. 5 § 3. */
  journeeRespectee: boolean;
  /** Avenant signé + règlement de travail modifié — art. 5 § 4 + art. 6. */
  formalisationRespectee: boolean;
  /** Durée avenant ≤ 6 mois ou renouvellement explicite — art. 5 § 4. */
  dureeRespectee: boolean;
  /** Journée maximale autorisée calculée (9 h 30 par défaut, 10 h si CCT). */
  journeeMaximaleAutoriseeHeures: number;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement (jurisprudence post-2023 limitée, CCT 10 h à vérifier, etc.). */
  avertissement: string;
}
