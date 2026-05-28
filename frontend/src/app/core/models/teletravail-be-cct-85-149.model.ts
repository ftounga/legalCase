/**
 * SF-219-16b : modèle frontend de l'outil F-219 — télétravail Belgique
 * (CCT n° 85 du 09/11/2005 — structurel régulier ; CCT n° 149 du 26/01/2021 —
 * occasionnel / force majeure ; Loi du 03/10/2022 « Deal pour l'emploi » —
 * droit à la déconnexion entreprises ≥ 20 travailleurs).
 *
 * <p><b>BE uniquement</b> — La France dispose d'un cadre télétravail distinct
 * (C. trav. art. L. 1222-9 et s. + ANI 26/11/2020 + Accord-cadre européen
 * 16/07/2002) structurellement différent (pas de CCT contraignante CNT, pas
 * de plafond ONSS analogue, déconnexion encadrée par C. trav. L. 2242-17).
 * Aucun mapping mécanique.</p>
 *
 * <p>Contrat figé dans SF-219-16 backend
 * ({@code TeletravailBeCct85149Request} + {@code TeletravailBeCct85149Response}).</p>
 */

/**
 * Type de télétravail invoqué par les parties — détermine la convention
 * applicable (CCT n° 85 du 09/11/2005 du CNT pour le structurel ; CCT n° 149
 * du 26/01/2021 du CNT pour l'occasionnel).
 *
 * <ul>
 *   <li>{@code STRUCTUREL_CCT_85} — télétravail régulier exécuté à raison
 *       d'au moins une partie du temps de travail dans le cadre du contrat
 *       (art. 2 CCT n° 85). Exige convention individuelle écrite (art. 6).</li>
 *   <li>{@code OCCASIONNEL_CCT_149} — télétravail ponctuel (force majeure,
 *       raison personnelle, COVID). Pas de convention écrite obligatoire au
 *       cas par cas — accord cadre suffit.</li>
 *   <li>{@code INDETERMINE} — qualification non encore opérée ou litige sur
 *       la convention applicable — analyse au cas par cas.</li>
 * </ul>
 */
export type TeletravailBeCct85149TypeTeletravail =
  | 'STRUCTUREL_CCT_85'
  | 'OCCASIONNEL_CCT_149'
  | 'INDETERMINE';

/**
 * Verdict hiérarchisé 7 états — ordre de priorité : structurel conforme →
 * occasionnel conforme → convention écrite manquante (art. 6) → équipement
 * non fourni (art. 9) → droits réduits (art. 4) → déconnexion non définie
 * (Loi 03/10/2022) → à analyser.
 *
 * <ul>
 *   <li>{@code CONFORME_CCT_85_STRUCTUREL} (vert) — télétravail structurel
 *       régulier conforme aux conditions cumulatives CCT n° 85.</li>
 *   <li>{@code CONFORME_CCT_149_OCCASIONNEL} (vert) — télétravail occasionnel
 *       relevant de la CCT n° 149 (force majeure / raison personnelle /
 *       COVID), cadre minimal respecté.</li>
 *   <li>{@code NON_CONFORME_CONVENTION_ECRITE_MANQUANTE} (rouge) — télétravail
 *       structurel sans convention écrite individuelle (art. 6 CCT n° 85).</li>
 *   <li>{@code NON_CONFORME_EQUIPEMENT_NON_FOURNI} (rouge) — l'employeur ne
 *       fournit pas / n'indemnise pas l'équipement (art. 9 CCT n° 85).</li>
 *   <li>{@code NON_CONFORME_DROITS_REDUITS} (rouge) — droits sociaux réduits
 *       par rapport aux présentiels — discrimination art. 4 CCT n° 85.</li>
 *   <li>{@code FRAGILE_DECONNEXION_NON_DEFINIE} (ambre) — entreprise ≥ 20
 *       travailleurs sans accord collectif déconnexion (Loi 03/10/2022).</li>
 *   <li>{@code A_ANALYSER} (gris info) — type indéterminé ou configuration
 *       mixte — analyse cas par cas requise.</li>
 * </ul>
 */
export type TeletravailBeCct85149Verdict =
  | 'CONFORME_CCT_85_STRUCTUREL'
  | 'CONFORME_CCT_149_OCCASIONNEL'
  | 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE'
  | 'NON_CONFORME_EQUIPEMENT_NON_FOURNI'
  | 'NON_CONFORME_DROITS_REDUITS'
  | 'FRAGILE_DECONNEXION_NON_DEFINIE'
  | 'A_ANALYSER';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/teletravail-be-cct-85-149}.
 *
 * <p>Tous les champs sont requis côté backend ({@code @NotNull}).</p>
 */
export interface TeletravailBeCct85149Request {
  /** Date d'entrée en vigueur de la formule de télétravail. */
  dateDebutTeletravail: string;
  /** Type de télétravail (structurel CCT 85 / occasionnel CCT 149 / indéterminé). */
  typeTeletravail: TeletravailBeCct85149TypeTeletravail;
  /** Volontariat réciproque employeur / travailleur (art. 5 CCT n° 85). */
  volontariatReciproque: boolean;
  /** Convention individuelle écrite obligatoire pour le structurel (art. 6 CCT n° 85). */
  conventionEcriteIndividuelle: boolean;
  /** Équipement fourni ou indemnisé par l'employeur (art. 9 CCT n° 85). */
  equipementFourniOuIndemnise: boolean;
  /** Indemnité forfaitaire mensuelle versée au télétravailleur en € (≥ 0). */
  indemniteForfaitaireMensuelleEuros: number;
  /** Plafond mensuel admis (ONSS / SPF Finances) applicable à la période en € (&gt; 0). */
  plafondIndemniteMensuelleEuros: number;
  /** Droits sociaux maintenus (formation, carrière, droits collectifs — art. 4 CCT n° 85). */
  droitsSociauxMaintenus: boolean;
  /** Modalités de déconnexion définies (CCT d'entreprise / règlement de travail). */
  deconnexionDefinie: boolean;
  /** Effectif total de l'entreprise (gating Loi 03/10/2022 — exigible ≥ 20). */
  effectifEntreprise: number;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + verdict +
 * ventilation conformités cumulatives + indemnité excédentaire calculée.
 */
export interface TeletravailBeCct85149Response {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateDebutTeletravail: string;
  typeTeletravail: TeletravailBeCct85149TypeTeletravail;
  volontariatReciproque: boolean;
  conventionEcriteIndividuelle: boolean;
  equipementFourniOuIndemnise: boolean;
  indemniteForfaitaireMensuelleEuros: number;
  plafondIndemniteMensuelleEuros: number;
  droitsSociauxMaintenus: boolean;
  deconnexionDefinie: boolean;
  effectifEntreprise: number;

  // -- Verdict --
  verdict: TeletravailBeCct85149Verdict;
  /** Convention écrite individuelle respectée (art. 6 CCT n° 85). */
  conventionRespectee: boolean;
  /** Équipement fourni ou indemnisé (art. 9 CCT n° 85). */
  equipementRespecte: boolean;
  /** Droits sociaux maintenus (art. 4 CCT n° 85). */
  droitsRespectes: boolean;
  /** Modalités de déconnexion définies (Loi 03/10/2022, gating ≥ 20). */
  deconnexionRespectee: boolean;
  /** Indemnité excédentaire au plafond ONSS/SPF Finances en € (≥ 0 si excès, 0 si OK). */
  indemniteExcedentaire: number;

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
