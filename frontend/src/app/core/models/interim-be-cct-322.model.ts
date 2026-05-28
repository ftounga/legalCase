/**
 * SF-219-14b : modèle frontend de l'outil F-219 — statut intérim
 * (Belgique, Loi du 24/07/1987 + CCT n° 322 du 14/06/2010).
 *
 * <p><b>BE uniquement</b> — Loi du 24/07/1987 relative au travail
 * temporaire, au travail intérimaire et à la mise de travailleurs à la
 * disposition d'utilisateurs (M.B. 20/08/1987) ; CCT n° 322 du
 * 14/06/2010 conclue au CNT ; CCT n° 108 du 16/07/2013 (insertion en
 * vue d'embauche durable) ; AR du 11/10/1976 (liste limitative cas de
 * travail exceptionnel) ; Loi-programme du 27/12/2012 (sanctions ONSS
 * renforcées en cas de fraude).</p>
 *
 * <p>La France a un régime distinct (C. trav. art. L. 1251-1 et s.) avec
 * une logique de mission similaire mais des plafonds, motifs, parité et
 * formalisme structurellement différents (DPAE vs Dimona, contrats de
 * mise à disposition différents). Aucun mapping mécanique.</p>
 *
 * <p>Contrat figé dans SF-219-14 backend
 * ({@code InterimBeCct322Request} + {@code InterimBeCct322Response}).</p>
 */

/**
 * Motif justifiant le recours au travail intérimaire (Loi du 24/07/1987,
 * art. 1er § 1 et art. 1bis ; CCT n° 108 du 16/07/2013 pour l'insertion).
 *
 * <p>La liste est <b>limitative</b>. Tout autre motif (cf.
 * {@code AUTRE_NON_AUTORISE}) entraîne la requalification immédiate en
 * CDI utilisateur (art. 20 Loi 24/07/1987 + CCT n° 322).</p>
 *
 * <ul>
 *   <li>{@code REMPLACEMENT_TRAVAILLEUR_PERMANENT} — remplacement d'un
 *       permanent en suspension / rupture (hors grève / lock-out).
 *       Durée = suspension + 7 j (art. 8 § 2).</li>
 *   <li>{@code SURCROIT_TEMPORAIRE_DE_TRAVAIL} — accroissement temporaire.
 *       Max 6 mois renouvelable 1 fois → 12 mois (CCT n° 108).</li>
 *   <li>{@code EXECUTION_TRAVAIL_EXCEPTIONNEL} — liste limitative AR
 *       11/10/1976 (inventaires, déménagements, foires, expositions, etc.).</li>
 *   <li>{@code INSERTION_EMBAUCHE_DURABLE} — art. 1bis Loi 24/07/1987
 *       + CCT n° 108 : max 3 tentatives, 9 mois cumulés au total.</li>
 *   <li>{@code MOTIF_ARTISTIQUE} — art. 1bis § 4 (spectacle, audiovisuel).</li>
 *   <li>{@code FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE} — AR sectoriels
 *       étroits (transport scolaire, flux saisonniers).</li>
 *   <li>{@code AUTRE_NON_AUTORISE} — rejet immédiat + requalification CDI.</li>
 * </ul>
 */
export type InterimBeCct322MotifMission =
  | 'REMPLACEMENT_TRAVAILLEUR_PERMANENT'
  | 'SURCROIT_TEMPORAIRE_DE_TRAVAIL'
  | 'EXECUTION_TRAVAIL_EXCEPTIONNEL'
  | 'INSERTION_EMBAUCHE_DURABLE'
  | 'MOTIF_ARTISTIQUE'
  | 'FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE'
  | 'AUTRE_NON_AUTORISE';

/**
 * Verdict hiérarchisé 7 états de l'outil intérim BE — ordre de priorité :
 * grève/lock-out → motif non autorisé → zone grise → durée → parité →
 * formalisme → éligible.
 *
 * <ul>
 *   <li>{@code ELIGIBLE_MISSION_REGULIERE} — toutes conditions cumulatives
 *       réunies (vert).</li>
 *   <li>{@code FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} — fond OK mais
 *       formalisme défaillant (ambre — requalification automatique).</li>
 *   <li>{@code INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT} — art. 19 :
 *       interdiction absolue, présomption irréfragable de fraude (rouge).</li>
 *   <li>{@code INELIGIBLE_MOTIF_NON_AUTORISE} — motif hors liste
 *       limitative art. 1er § 1 / 1bis (rouge).</li>
 *   <li>{@code INELIGIBLE_DUREE_MAX_DEPASSEE} — durée totale &gt; plafond
 *       légal applicable au motif (rouge).</li>
 *   <li>{@code INELIGIBLE_PARITE_SALARIALE_VIOLEE} — salaire intérimaire
 *       &lt; permanent de référence (art. 10) (rouge).</li>
 *   <li>{@code A_ANALYSER} — motif artistique / flux exceptionnel : analyse
 *       cas par cas requise (gris info).</li>
 * </ul>
 */
export type InterimBeCct322Verdict =
  | 'ELIGIBLE_MISSION_REGULIERE'
  | 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT'
  | 'INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT'
  | 'INELIGIBLE_MOTIF_NON_AUTORISE'
  | 'INELIGIBLE_DUREE_MAX_DEPASSEE'
  | 'INELIGIBLE_PARITE_SALARIALE_VIOLEE'
  | 'A_ANALYSER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/interim-be-cct-322}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface InterimBeCct322Request {
  /** Date de début de la première prestation intérimaire. */
  dateDebutMission: string;
  /** Motif invoqué pour le recours à l'intérim. */
  motifMission: InterimBeCct322MotifMission;
  /** Recours pour remplacer un travailleur en grève / lock-out (art. 19, interdit). */
  remplacementGreveOuLockout: boolean;
  /** Durée totale cumulée de la mission en jours (contrats successifs inclus, ≥ 0). */
  dureeTotaleMissionJours: number;
  /** Plafond légal applicable au motif en jours (&gt; 0, paramétrable selon AR / CCT). */
  dureeMaxLegaleJours: number;
  /** Salaire horaire brut intérimaire en € (≥ 0). */
  salaireHoraireIntimaireBrut: number;
  /** Salaire horaire brut du permanent de référence chez l'utilisateur en € (&gt; 0). */
  salaireHoraireReferenceBrut: number;
  /** Contrat de travail écrit signé entre ETI et intérimaire (art. 8 § 1). */
  contratEcritSigne: boolean;
  /** Déclaration Dimona effectuée par l'ETI (employeur formel). */
  dimonaDeclareeParEti: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + verdict +
 * ventilation 4 dimensions cumulatives + écarts calculés (durée, parité).
 */
export interface InterimBeCct322Response {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateDebutMission: string;
  motifMission: InterimBeCct322MotifMission;
  remplacementGreveOuLockout: boolean;
  dureeTotaleMissionJours: number;
  dureeMaxLegaleJours: number;
  salaireHoraireIntimaireBrut: number;
  salaireHoraireReferenceBrut: number;
  contratEcritSigne: boolean;
  dimonaDeclareeParEti: boolean;

  // -- Verdict --
  verdict: InterimBeCct322Verdict;
  /** Motif listé dans la liste limitative art. 1er § 1 / 1bis. */
  motifAutorise: boolean;
  /** Durée totale &le; plafond légal applicable. */
  dureeRespectee: boolean;
  /** Parité salariale respectée (intérimaire ≥ référence, art. 10). */
  pariteRespectee: boolean;
  /** Formalisme cumulatif respecté (contrat écrit + Dimona ETI). */
  formalismeRespecte: boolean;
  /** Jours excédentaires au-delà du plafond légal (0 si dans les clous). */
  joursExcedentaires: number;
  /** Écart de parité salariale en € (≥ 0 si infraction, 0 si OK). */
  ecartPariteSalariale: number;

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
