/**
 * SF-213-10b : modèle frontend de l'outil F-213 — score CCT n° 109 BE
 * (licenciement manifestement déraisonnable, art. 9 CCT 12/02/2014).
 *
 * <p>BE uniquement — la France a un dispositif distinct (barème
 * Macron L. 1235-3 C. trav. / licenciement sans cause réelle et
 * sérieuse) — outil dédié hors scope V1.</p>
 *
 * <p>Contrat figé dans SF-213-10 backend
 * ({@code LicenciementBeCct109DeraisonnableResponse} +
 * {@code LicenciementBeCct109DeraisonnableRequest}).</p>
 *
 * <p>Échelle de score CCT 109 (5 échelons gradués) :
 * <ul>
 *   <li>{@code NON_DERAISONNABLE} — vert : motif valable + procédures
 *       respectées, pas d'indemnité due.</li>
 *   <li>{@code 3_SEMAINES} — jaune : minimum légal (motif vague ou
 *       absent, sans aggravant).</li>
 *   <li>{@code 8_SEMAINES} — orange : licenciement manifestement
 *       déraisonnable de gravité ordinaire.</li>
 *   <li>{@code 12_SEMAINES} — rouge : gravité haute (discrimination ou
 *       représailles suspectées).</li>
 *   <li>{@code 17_SEMAINES} — rouge foncé : maximum légal (faits
 *       graves cumulés — discrimination + représailles).</li>
 * </ul></p>
 *
 * <p><b>Cumul ICP</b> : l'indemnité CCT 109 se cumule systématiquement
 * avec l'indemnité compensatoire de préavis (Loi 03/07/1978) — l'UI
 * affiche une bannière info systématique pour rappeler à l'avocat de
 * calculer l'ICP via le statut unique BE / Claeys.</p>
 */

/**
 * Échelon CCT 109 (5 valeurs) — sérialisation JSON backend préserve les
 * chiffres en tête ({@code "3_SEMAINES"}, {@code "8_SEMAINES"}, etc.).
 */
export type LicenciementBeCct109Echelon =
  | 'NON_DERAISONNABLE'
  | '3_SEMAINES'
  | '8_SEMAINES'
  | '12_SEMAINES'
  | '17_SEMAINES';

/**
 * Qualification du motif au sens de l'art. 8 CCT 109 — pilote l'échelon
 * de base (avant aggravants).
 */
export type LicenciementBeCct109MotifLieAPersonne =
  | 'SANS_MOTIF'
  | 'MOTIF_VAGUE'
  | 'MOTIF_PRECIS_DISPUTE'
  | 'MOTIF_PROUVE_VALIDE';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable}.
 *
 * <p>Champs obligatoires : {@code motifCommunique} (Boolean — peut être
 * {@code false}, mais doit être valorisé), {@code motifLieAPersonne},
 * {@code remunerationHebdomadaireBrute} (> 0).</p>
 *
 * <p>Défauts : {@code discriminationSuspectee=false},
 * {@code represaillesSuspectees=false}, {@code proceduresRespectees=true}.
 * {@code argumentsPatronal} optionnel (contexte IA, n'influence pas le
 * calcul).</p>
 */
export interface LicenciementBeCct109DeraisonnableRequest {
  /** Obligatoire — le motif a-t-il été communiqué par écrit (CCT 109 art. 5-8). */
  motifCommunique: boolean;
  /** Obligatoire — qualification du motif (4 cas, pilote l'échelon de base). */
  motifLieAPersonne: LicenciementBeCct109MotifLieAPersonne;
  /** Discrimination suspectée (porte l'échelon à 12 sem.). */
  discriminationSuspectee: boolean;
  /** Représailles suspectées (porte l'échelon à 12 sem.). */
  represaillesSuspectees: boolean;
  /** Procédures d'audition / notification respectées (CCT 109 art. 5). */
  proceduresRespectees: boolean;
  /** Obligatoire — base du calcul de l'indemnité (€), > 0. */
  remunerationHebdomadaireBrute: number;
  /** Optionnel — contexte employeur (libre, contexte IA). */
  argumentsPatronal?: string | null;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs.
 */
export interface LicenciementBeCct109DeraisonnableResponse {
  /** UUID du dossier (mirroring backend). */
  caseFileId: string;

  // -- Echo inputs --
  motifCommunique: boolean;
  motifLieAPersonne: LicenciementBeCct109MotifLieAPersonne;
  discriminationSuspectee: boolean;
  represaillesSuspectees: boolean;
  proceduresRespectees: boolean;
  remunerationHebdomadaireBrute: number;
  argumentsPatronal: string | null;

  // -- Outputs métier --
  /** Échelon CCT 109 (5 valeurs — voir {@link LicenciementBeCct109Echelon}). */
  echelonCct109: LicenciementBeCct109Echelon;
  /** Nombre de semaines d'indemnité (0 / 3 / 8 / 12 / 17). */
  nombreSemaines: number;
  /** Indemnité CCT 109 (€) = remunerationHebdomadaireBrute × nombreSemaines. */
  indemniteCct109: number;
  /** Justification textuelle de l'échelon retenu (affichée en JetBrains Mono). */
  justificationEchelon: string;
  /** Vrai si l'indemnité se cumule avec l'ICP (toujours vrai V1). */
  cumulAvecIcp: boolean;
  /** Base juridique humanisée (CCT 109 art. 9). */
  baseJuridique: string;
  /** Avertissement éventuel (motif requis, procédures incomplètes…). */
  avertissement: string | null;
}
