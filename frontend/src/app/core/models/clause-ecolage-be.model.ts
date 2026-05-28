/**
 * SF-219-17b : modèle frontend de l'outil F-219 — clause d'écolage Belgique
 * (art. 22bis Loi du 03/07/1978 sur les contrats de travail, inséré par la
 * Loi du 27/12/2006 M.B. 28/12/2006 + modifié par la Loi du 22/12/2017
 * « Pacte de compétitivité » + CCT n° 43 du 02/05/1988 du CNT — RMMMG +
 * CCT n° 13 du 02/02/2013 du CNT — cadre subsidiaire formations
 * qualifiantes sectorielles).
 *
 * <p><b>BE uniquement</b> — La France dispose d'un régime distinct dit
 * « clause de dédit-formation » d'origine jurisprudentielle (Cass. soc.
 * 17/07/1991 n° 88-40.201 + suivants) ; ses conditions cumulatives
 * (formation entraînant des frais réels au-delà des dépenses imposées par
 * la loi ou la convention collective ; engagement écrit antérieur à la
 * formation ; durée d'efficacité proportionnée ; remboursement non
 * dissuasif vis-à-vis du droit de démissionner) sont fixées par le juge
 * et non par la loi — pas de plafonds chiffrés analogues au § 4 art.
 * 22bis BE. Aucun mapping mécanique.</p>
 *
 * <p>Contrat figé dans SF-219-17 backend ({@code ClauseEcolageBeRequest}
 * + {@code ClauseEcolageBeResponse}).</p>
 */

/**
 * Type de formation visé par la clause — détermine l'éligibilité de
 * principe de la clause d'écolage (art. 22bis § 2 Loi 03/07/1978).
 *
 * <ul>
 *   <li>{@code SPECIFIQUE} — formation spécifique au sens du § 2
 *       (apporte des connaissances ou compétences nouvelles, profite en
 *       premier lieu au travailleur). Clause valable possible si autres
 *       conditions remplies.</li>
 *   <li>{@code OBLIGATOIRE_LEGALE} — formation imposée par une norme
 *       légale ou réglementaire (KB, AR, CCT sectorielle obligatoire,
 *       recyclage sécurité). Clause nulle de plein droit (§ 2 al. 1er).</li>
 *   <li>{@code INDETERMINE} — qualification non encore opérée — analyse
 *       cas par cas requise.</li>
 * </ul>
 */
export type ClauseEcolageBeTypeFormation =
  | 'SPECIFIQUE'
  | 'OBLIGATOIRE_LEGALE'
  | 'INDETERMINE';

/**
 * Motif de départ du travailleur — détermine l'opposabilité de la
 * clause (art. 22bis § 3 Loi 03/07/1978).
 *
 * <ul>
 *   <li>{@code DEMISSION_TRAVAILLEUR} — démission volontaire — clause
 *       actionnable (sous réserve des autres conditions).</li>
 *   <li>{@code LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE} — licenciement
 *       sans motif grave par l'employeur — clause inopposable.</li>
 *   <li>{@code LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR} — licenciement
 *       pour motif grave imputable au travailleur — clause
 *       actionnable.</li>
 *   <li>{@code RUPTURE_MOTIF_GRAVE_EMPLOYEUR} — rupture pour motif
 *       grave dans le chef de l'employeur — clause inopposable.</li>
 *   <li>{@code FIN_CDD_NORMALE} — expiration normale d'un CDD — clause
 *       inopposable.</li>
 * </ul>
 */
export type ClauseEcolageBeMotifDepart =
  | 'DEMISSION_TRAVAILLEUR'
  | 'LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE'
  | 'LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR'
  | 'RUPTURE_MOTIF_GRAVE_EMPLOYEUR'
  | 'FIN_CDD_NORMALE';

/**
 * Verdict hiérarchisé 8 états — ordre de priorité backend :
 *   1. INOPPOSABLE_MOTIF_DEPART (motif art. 22bis § 3 exclu — court-circuite tout)
 *   2. NULLE_FORME_ECRITE_MANQUANTE (art. 22bis § 1er)
 *   3. NULLE_FORMATION_OBLIGATOIRE (art. 22bis § 2 al. 1er)
 *   4. NULLE_COUT_INSUFFISANT (art. 22bis § 2 al. 3 2°)
 *   5. NULLE_DUREE_EXCESSIVE (art. 22bis § 4 al. 1er)
 *   6. VALIDE_DUREE_EXPIREE (durée d'efficacité dépassée — pas de remboursement)
 *   7. VALIDE_REMBOURSEMENT_DEGRESSIF (clause valable — remboursement dégressif plafonné à 80 %)
 *   8. A_ANALYSER (type formation indéterminé — analyse cas par cas)
 */
export type ClauseEcolageBeVerdict =
  | 'INOPPOSABLE_MOTIF_DEPART'
  | 'NULLE_FORME_ECRITE_MANQUANTE'
  | 'NULLE_FORMATION_OBLIGATOIRE'
  | 'NULLE_COUT_INSUFFISANT'
  | 'NULLE_DUREE_EXCESSIVE'
  | 'VALIDE_REMBOURSEMENT_DEGRESSIF'
  | 'VALIDE_DUREE_EXPIREE'
  | 'A_ANALYSER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/clause-ecolage-be}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface ClauseEcolageBeRequest {
  /** Type de formation (spécifique CCT n° 13 / obligatoire légale / indéterminé). */
  typeFormation: ClauseEcolageBeTypeFormation;
  /** Clause rédigée par écrit au plus tard à l'entrée en formation (art. 22bis § 1er al. 1er). */
  clauseEcriteAvantEntreeFormation: boolean;
  /** Coût réel de la formation en € (≥ 0) — art. 22bis § 2 al. 3 2°. */
  coutReelFormationEuros: number;
  /** RMMMG mensuel applicable au moment de la signature en € (&gt; 0) — CCT n° 43 du CNT. */
  rmmmgMensuelEuros: number;
  /** Durée d'efficacité de la clause en mois (≥ 1, ≤ 36 pour validité — art. 22bis § 4 al. 1er). */
  dureeEfficaciteMois: number;
  /** Date de fin effective de la formation. */
  dateFinFormation: string;
  /** Date de départ effectif du travailleur (rupture du contrat). */
  dateDepartTravailleur: string;
  /** Motif de départ du travailleur (art. 22bis § 3 — détermine l'opposabilité). */
  motifDepart: ClauseEcolageBeMotifDepart;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + verdict +
 * calculs dégressifs (coût min légal, tiers atteint, quotité due,
 * montant brut, plafond 80 %, montant final).
 */
export interface ClauseEcolageBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  typeFormation: ClauseEcolageBeTypeFormation;
  clauseEcriteAvantEntreeFormation: boolean;
  coutReelFormationEuros: number;
  rmmmgMensuelEuros: number;
  dureeEfficaciteMois: number;
  dateFinFormation: string;
  dateDepartTravailleur: string;
  motifDepart: ClauseEcolageBeMotifDepart;

  // -- Verdict --
  verdict: ClauseEcolageBeVerdict;
  /** Coût minimum légal en € — seuil ½ × RMMMG mensuel (art. 22bis § 2 al. 3 2°). */
  coutMinLegalEuros: number;
  /** Nombre de mois écoulés entre fin formation et départ travailleur. */
  moisEcoulesDepuisFinFormation: number;
  /** Tiers de la durée d'efficacité au départ : 1 (100 %), 2 (66 %), 3 (33 %), 0 (au-delà). */
  tierDureeAuDepart: number;
  /** Quotité due selon dégressivité : 1.00 / 0.66 / 0.33 / 0.00 (art. 22bis § 4 al. 2). */
  quotiteDueRatio: number;
  /** Montant brut dû avant plafonnement 80 % en € (coût réel × quotité). */
  montantBrutDueEuros: number;
  /** Plafond absolu 80 % du coût réel en € (art. 22bis § 4 al. 4). */
  plafond80Euros: number;
  /** Montant final dû par le travailleur en € — min(brut, plafond 80 %). */
  montantDuFinalEuros: number;

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
