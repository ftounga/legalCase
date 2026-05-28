/**
 * SF-219-21b : modèle frontend de l'outil F-219 — éco-chèques +
 * chèques-repas BE (CCT n°98 du CNT du 20/02/2009 pour les éco-chèques
 * + Loi du 25/04/2014 portant des dispositions diverses + AR du
 * 03/02/2010 modifiant l'art. 19bis de l'AR du 28/11/1969 portant
 * exécution de la loi du 27/06/1969 sur la sécurité sociale des
 * travailleurs salariés).
 *
 * <p><b>BE uniquement</b> — les titres-restaurant français (CGI
 * art. 81 19° ter) reposent sur un mécanisme analogue mais avec un
 * plafond d'exonération distinct (7,18 EUR/jour en 2024, contribution
 * employeur 50-60 %) et il n'existe pas d'équivalent direct des
 * éco-chèques en droit français. Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence (pattern miroir SF-219-19/20).</p>
 *
 * <p>Contrat figé dans SF-219-21 backend
 * ({@code EcoChequesChequesRepasBeRequest} +
 * {@code EcoChequesChequesRepasBeResponse}).</p>
 */

/**
 * Type d'avantage extra-légal analysé.
 *
 * <ul>
 *   <li>{@code ECO_CHEQUES} — CCT n°98 du CNT du 20/02/2009, plafond
 *       250 EUR/an/travailleur (art. 6), validité 24 mois (art. 13),
 *       requiert CCT sectorielle / d'entreprise ou convention
 *       individuelle écrite (art. 3).</li>
 *   <li>{@code CHEQUES_REPAS} — Loi 25/04/2014 + AR 03/02/2010 (art.
 *       19bis AR 28/11/1969), plafond intervention employeur
 *       6,91 EUR/chèque, contribution travailleur min 1,09 EUR,
 *       valeur faciale max 8 EUR, conditions cumulatives.</li>
 *   <li>{@code INDETERMINE} — type non précisé → verdict
 *       {@code A_ANALYSER}.</li>
 * </ul>
 */
export type EcoChequesChequesRepasBeTypeAvantage =
  | 'ECO_CHEQUES'
  | 'CHEQUES_REPAS'
  | 'INDETERMINE';

/**
 * Verdict hiérarchisé (court-circuits backend
 * {@code EcoChequesChequesRepasBeCalculator}) :
 *
 * <ol>
 *   <li>{@code A_ANALYSER} — type d'avantage indéterminé.</li>
 *   <li>{@code NON_CONFORME_SUBSTITUTION_REMUNERATION} — substitution
 *       avérée (Cass. 16/10/2017 S.16.0042.F + art. 4 CCT n°98) →
 *       requalification intégrale rétroactive.</li>
 *   <li>{@code NON_CONFORME_CONDITION_MANQUANTE} — condition cumulative
 *       non remplie (CCT/convention absente, paiement non électronique
 *       post 01/01/2016, contribution travailleur insuffisante, cumul
 *       avec frais de bouche, validité dépassée).</li>
 *   <li>{@code NON_CONFORME_DEPASSEMENT_PLAFOND} ou
 *       {@code CONFORME_PARTIELLEMENT_EXONERE} — montant supérieur
 *       au plafond (la fraction excédentaire est requalifiée).</li>
 *   <li>{@code CONFORME_EXONERATION_INTEGRALE} — toutes conditions
 *       remplies, exonération ONSS et impôt intégrale.</li>
 * </ol>
 */
export type EcoChequesChequesRepasBeVerdict =
  | 'CONFORME_EXONERATION_INTEGRALE'
  | 'CONFORME_PARTIELLEMENT_EXONERE'
  | 'NON_CONFORME_DEPASSEMENT_PLAFOND'
  | 'NON_CONFORME_SUBSTITUTION_REMUNERATION'
  | 'NON_CONFORME_CONDITION_MANQUANTE'
  | 'A_ANALYSER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/eco-cheques-cheques-repas-be}.
 *
 * <p>Tous les champs marqués requis sont {@code @NotNull} côté backend
 * (cf. {@code EcoChequesChequesRepasBeRequest}). {@code dateAttribution}
 * est optionnelle — sert au contrôle de validité (2 ans éco-chèques).</p>
 */
export interface EcoChequesChequesRepasBeRequest {
  /** Type d'avantage (ECO_CHEQUES / CHEQUES_REPAS / INDETERMINE). */
  typeAvantage: EcoChequesChequesRepasBeTypeAvantage;
  /** Montant annuel total attribué sur l'année civile (€). */
  montantAnnuelEur: number;
  /** Valeur faciale unitaire d'un chèque (€) — max 8 EUR chèques-repas. */
  valeurFacialeUnitaireEur: number;
  /** Contribution travailleur unitaire (€) — min 1,09 EUR chèques-repas. */
  contributionTravailleurUnitaireEur: number;
  /** Jours effectivement prestés sur la période (proratisation chèques-repas). */
  joursEffectivementPrestes: number;
  /** CCT sectorielle ou d'entreprise existe (art. 3 CCT n°98 / art. 19bis §2 1° AR 28/11/1969). */
  cctSectorielleOuEntrepriseExiste: boolean;
  /** Convention individuelle écrite (alternative si pas de CCT). */
  conventionIndividuelleEcrite: boolean;
  /** Paiement électronique — obligatoire chèques-repas depuis 01/01/2016. */
  paiementElectronique: boolean;
  /** Cumul avec indemnité forfaitaire frais de bouche (interdit chèques-repas). */
  cumulFraisBouche: boolean;
  /** Substitution à une rémunération préexistante (interdit — art. 4 CCT n°98). */
  substitutionRemuneration: boolean;
  /** Date d'attribution (sert au contrôle de validité 24 mois éco-chèques). */
  dateAttribution: string | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + montants
 * calculés + verdict + métadonnées légales.
 */
export interface EcoChequesChequesRepasBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  typeAvantage: EcoChequesChequesRepasBeTypeAvantage;
  montantAnnuelEur: number;
  valeurFacialeUnitaireEur: number;
  contributionTravailleurUnitaireEur: number;
  joursEffectivementPrestes: number;
  cctSectorielleOuEntrepriseExiste: boolean;
  conventionIndividuelleEcrite: boolean;
  paiementElectronique: boolean;
  cumulFraisBouche: boolean;
  substitutionRemuneration: boolean;
  dateAttribution: string | null;

  // -- Outputs calculés --
  /** Plafond légal applicable au cas (€). */
  plafondLegalApplicableEur: number;
  /** Montant exonéré ONSS et impôt (€). */
  montantExonereEur: number;
  /** Montant requalifié en rémunération (€) — soumis ONSS + impôt. */
  montantRequalifieEnRemunerationEur: number;
  /** Cotisations ONSS employeur estimées sur le requalifié (€, indicatif 25 %). */
  cotisationsOnssEstimeesEur: number;

  // -- Verdict + métadonnées légales --
  verdict: EcoChequesChequesRepasBeVerdict;
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement (taux ONSS indicatif, validité, prescription...). */
  avertissement: string;
}
