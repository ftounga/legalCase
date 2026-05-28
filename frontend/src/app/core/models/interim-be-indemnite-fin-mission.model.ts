/**
 * SF-219-15b : modèle frontend de l'outil F-219 — indemnité de fin
 * de mission intérim (Belgique).
 *
 * <p><b>BE uniquement</b> — Loi du 24/07/1987 relative au travail
 * temporaire, au travail intérimaire et à la mise de travailleurs à
 * la disposition d'utilisateurs (M.B. 20/08/1987) ; CCT n° 322 du
 * 14/06/2010 (statut intérim) ; CCT n° 322bis du 16/12/2010 (pécule
 * de vacances intérim 15,38 % brut prestations payé par le FSI) ;
 * AR du 30/03/1967 sur les vacances annuelles (art. 19) ; CCT
 * sectorielles propres à la commission paritaire de l'utilisateur
 * (prime fin d'année 1/12 brut annuel — variable selon CP) ;
 * jurisprudence Cass. BE 04/02/1991 + 16/12/2002 (rupture anticipée
 * injustifiée = rémunérations restant à courir) ; Cass. BE
 * 03/05/2010 + 23/06/2003 (refus de transposer la prime de précarité
 * forfaitaire 10 % FR au régime intérim BE) ; Loi 16/03/1971 art. 29
 * (sursalaire heures supplémentaires).</p>
 *
 * <p><b>Spécificité BE vs FR</b> : contrairement à l'art. L. 1251-32
 * C. trav. fr. qui impose une <b>IFM forfaitaire 10 %</b> du salaire
 * brut total à toute fin de mission intérimaire, le droit belge n'a
 * <b>aucune prime de précarité équivalente</b>. La jurisprudence Cass.
 * (BE) refuse explicitement cette transposition (Cass. 03/05/2010,
 * S.09.0086.F ; Cass. 23/06/2003, S.02.0103.F). Aucun mapping
 * mécanique n'est possible avec l'outil FR
 * {@code F-DT-18-fin-mission-interim}.</p>
 *
 * <p>Contrat figé dans SF-219-15 backend
 * ({@code InterimBeIndemniteFinMissionRequest} +
 * {@code InterimBeIndemniteFinMissionResponse}).</p>
 */

/**
 * Code de la commission paritaire (CP) sectorielle de l'entreprise
 * utilisatrice — détermine la CCT sectorielle applicable pour le
 * calcul d'une éventuelle <b>prime de fin d'année (13e mois)</b>
 * due à l'intérimaire (art. 10 Loi 24/07/1987 — parité salariale).
 *
 * <ul>
 *   <li>{@code CP_124_CONSTRUCTION} — CP 124 Construction. Prime
 *       fin d'année 9,12 % du brut de référence (CCT 12/06/2014
 *       rendue obligatoire par AR du 11/02/2015), versée via le
 *       Fonds de sécurité d'existence Constructiv.</li>
 *   <li>{@code CP_200_AUXILIAIRE_EMPLOYES} — CP 200 (anc. CP 218
 *       jusqu'au 01/04/2015). Prime fin d'année 1/12 du brut annuel
 *       (CCT 09/06/2016 rendue obligatoire par AR du 13/06/2017).
 *       Ancienneté requise : 6 mois ininterrompus dans le même
 *       secteur (art. 4 CCT).</li>
 *   <li>{@code CP_218_NON_MARCHAND} — CP 218 employés du non-marchand
 *       (santé, enseignement libre, secteur socio-culturel). Prime
 *       fin d'année variable selon sous-commission.</li>
 *   <li>{@code CP_322_INTERIM} — CP 322 (Travail intérimaire). Pas
 *       de prime fin d'année propre à la CP 322 (renvoi à la CP
 *       de l'utilisateur).</li>
 *   <li>{@code CP_140_TRANSPORT_LOGISTIQUE} — CP 140 transport et
 *       logistique. Prime fin d'année 8,33 % du brut annuel via
 *       Fonds Social Transport (CCT 27/01/2011 RA 30/03/2011).</li>
 *   <li>{@code CP_209_METAUX_FABRICATIONS_METALLIQUES} — CP 209
 *       métaux et fabrications métalliques. Prime conventionnelle
 *       variable.</li>
 *   <li>{@code AUTRE_NON_RECENSE} — toute autre CP non recensée
 *       par l'outil V1 → verdict
 *       {@code A_ANALYSER_SECTEUR_NON_RECONNU} et renvoi avocat.</li>
 * </ul>
 */
export type InterimBeCommissionParitaire =
  | 'CP_124_CONSTRUCTION'
  | 'CP_200_AUXILIAIRE_EMPLOYES'
  | 'CP_218_NON_MARCHAND'
  | 'CP_322_INTERIM'
  | 'CP_140_TRANSPORT_LOGISTIQUE'
  | 'CP_209_METAUX_FABRICATIONS_METALLIQUES'
  | 'AUTRE_NON_RECENSE';

/**
 * Verdict hiérarchisé 4 états de l'outil — ordre de priorité :
 * rupture anticipée (substitue les autres composantes) → secteur
 * non reconnu → indemnités dues → aucune indemnité due.
 *
 * <ul>
 *   <li>{@code RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR} —
 *       mission rompue avant terme par l'ETI sans motif grave :
 *       droit aux rémunérations restant à courir jusqu'au terme
 *       prévu (Cass. BE 04/02/1991, S.90.0024.F).</li>
 *   <li>{@code A_ANALYSER_SECTEUR_NON_RECONNU} — commission
 *       paritaire utilisateur {@code AUTRE_NON_RECENSE} : prime
 *       fin d'année non calculable en automatique, renvoi avocat
 *       à la CCT sectorielle.</li>
 *   <li>{@code INDEMNITES_DUES} — au moins une composante
 *       d'indemnité (pécule de vacances OU prime fin d'année OU
 *       heures sup) est non nulle.</li>
 *   <li>{@code AUCUNE_INDEMNITE_DUE} — fin de mission au terme prévu,
 *       pécule de vacances déjà payé par le FSI, pas de CCT
 *       sectorielle applicable, pas d'heures sup. <b>Pas de prime
 *       de précarité 10 % style FR en droit belge.</b></li>
 * </ul>
 */
export type InterimBeIndemniteFinMissionVerdict =
  | 'INDEMNITES_DUES'
  | 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR'
  | 'AUCUNE_INDEMNITE_DUE'
  | 'A_ANALYSER_SECTEUR_NON_RECONNU';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/interim-be-indemnite-fin-mission}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface InterimBeIndemniteFinMissionRequest {
  /** Date de début de la mission intérimaire. */
  dateDebutMission: string;
  /** Date de fin initialement prévue (terme contractuel). */
  dateFinPrevue: string;
  /** Date de fin réelle (peut être antérieure si rupture anticipée). */
  dateFinReelle: string;
  /** Durée réelle prestée en jours (≥ 0). */
  dureeReellePrestationJours: number;
  /** Durée initialement prévue en jours (&gt; 0). */
  dureePrevueJours: number;
  /** Salaire horaire brut intérimaire en € (&gt; 0). */
  salaireHoraireBrut: number;
  /** Heures normales prestées sur la mission (≥ 0). */
  heuresPrestees: number;
  /** Heures supplémentaires en semaine (sursalaire 50 %) (≥ 0). */
  heuresSupplementairesSemaine: number;
  /** Heures supplémentaires dimanche / jour férié (sursalaire 100 %) (≥ 0). */
  heuresSupplementairesDimancheFerie: number;
  /** Pécule de vacances déjà versé par le FSI (AR 30/03/1967 art. 19). */
  peculeDejaVerseParFsi: boolean;
  /** Rupture anticipée par l'ETI sans motif grave (Cass. 04/02/1991). */
  ruptureAnticipeeParEtiSansMotifGrave: boolean;
  /** Commission paritaire sectorielle de l'utilisateur (parité art. 10). */
  commissionParitaireUtilisateur: InterimBeCommissionParitaire;
  /** Ancienneté sectorielle cumulée chez le même utilisateur en jours (≥ 0). */
  ancienneteSectorielleJours: number;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + décomposition
 * des composantes (pécule vacances, prime fin d'année, rupture anticipée,
 * sursalaire heures sup) + verdict + prime de précarité FR présumée à
 * titre informatif (montant que l'avocat FR appliquerait — mis à 0 en
 * BE) + métadonnées légales.
 */
export interface InterimBeIndemniteFinMissionResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateDebutMission: string;
  dateFinPrevue: string;
  dateFinReelle: string;
  dureeReellePrestationJours: number;
  dureePrevueJours: number;
  salaireHoraireBrut: number;
  heuresPrestees: number;
  heuresSupplementairesSemaine: number;
  heuresSupplementairesDimancheFerie: number;
  peculeDejaVerseParFsi: boolean;
  ruptureAnticipeeParEtiSansMotifGrave: boolean;
  commissionParitaireUtilisateur: InterimBeCommissionParitaire;
  ancienneteSectorielleJours: number;

  // -- Verdict --
  verdict: InterimBeIndemniteFinMissionVerdict;

  // -- Décomposition (composantes en €) --
  /** Salaire brut total des prestations (heures normales × taux horaire). */
  salaireBrutTotalPrestations: number;
  /** Pécule de vacances intérim 15,38 % (0 si déjà versé par le FSI). */
  peculeVacancesInterim: number;
  /** Prime fin d'année sectorielle (€, selon CP et ancienneté). */
  primeFinAnneeSectorielle: number;
  /** Indemnité de rupture anticipée (rémunérations restant à courir, Cass. 04/02/1991). */
  indemniteRuptureAnticipee: number;
  /** Sursalaire heures supplémentaires (50 % semaine + 100 % dimanche/férié). */
  sursalaireHeuresSupplementaires: number;
  /** Total des indemnités brutes effectivement dues. */
  totalIndemnitesBrutes: number;
  /**
   * Prime de précarité 10 % à titre informatif (ce que l'avocat FR
   * appliquerait sous L. 1251-32) — <b>toujours informatif</b>,
   * jamais due en BE.
   */
  primePrecaritePresumee: number;
  /** Taux de prime fin d'année appliqué (décimal, ex. 0.0833 = 8,33 %). */
  tauxPrimeFinAnneeApplique: number;
  /** Jours restant à courir jusqu'au terme prévu (0 si pas de rupture anticipée). */
  joursRestantsACourirJusquAuTerme: number;
  /** Ancienneté sectorielle suffisante pour la prime fin d'année. */
  ancienneteSectorielleSuffisante: boolean;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement (notamment absence prime de précarité 10 % style FR). */
  avertissement: string;
}
