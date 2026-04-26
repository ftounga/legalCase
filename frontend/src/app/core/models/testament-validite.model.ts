/**
 * SF-FA-24-04 : modèles TypeScript pour l'outil décisionnel "Validité
 * testament" (F-FA-24). FR uniquement — art. 967-1035 + 901-911 + 920+ Cciv
 * (4 formes + capacité + vices consentement + révocation + quotité).
 *
 * Contrat figé dans SF-FA-24-03 (backend, mergé PR #661).
 */

/** Forme du testament (art. 967, 970, 971-975, 976-980, Convention Washington 1973). */
export type FormeTestament =
  | 'TESTAMENT_OLOGRAPHE'
  | 'TESTAMENT_AUTHENTIQUE'
  | 'TESTAMENT_MYSTIQUE'
  | 'TESTAMENT_INTERNATIONAL';

/** Verdict de validité. */
export type VerdictValidite = 'VALIDE' | 'CONTESTABLE' | 'NUL';

/** Codes des vices identifiés (alignés sur l'enum backend `CodeVice`). */
export type CodeVice =
  | 'FORME_OLOGRAPHE_NON_MANUSCRITE'
  | 'FORME_OLOGRAPHE_NON_DATE'
  | 'FORME_OLOGRAPHE_NON_SIGNE'
  | 'FORME_AUTHENTIQUE_NOTAIRES_TEMOINS'
  | 'FORME_AUTHENTIQUE_DICTEE_MANQUANTE'
  | 'FORME_AUTHENTIQUE_LECTURE_MANQUANTE'
  | 'FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES'
  | 'FORME_MYSTIQUE_PLI_NON_CACHE'
  | 'FORME_MYSTIQUE_TEMOINS'
  | 'FORME_MYSTIQUE_SUSCRIPTION'
  | 'FORME_INTERNATIONAL_WASHINGTON'
  | 'INCAPACITE_MINEUR_MOINS_16_ANS'
  | 'INSANITE_ESPRIT'
  | 'MAJEUR_PROTEGE_SANS_ASSISTANCE'
  | 'VICE_CONSENTEMENT_DOL'
  | 'VICE_CONSENTEMENT_ERREUR'
  | 'REVOCATION_TESTAMENT_POSTERIEUR'
  | 'REVOCATION_DECHIRURE';

/** Vice identifié avec libellé explicite (forme `{ code, description }`). */
export interface ViceIdentifie {
  code: CodeVice;
  description: string;
}

/** Libellés humains des formes (radio + chip). */
export const FORME_TESTAMENT_LABELS: Readonly<Record<FormeTestament, string>> = {
  TESTAMENT_OLOGRAPHE: 'Olographe (art. 970)',
  TESTAMENT_AUTHENTIQUE: 'Authentique (art. 971-975)',
  TESTAMENT_MYSTIQUE: 'Mystique (art. 976-980)',
  TESTAMENT_INTERNATIONAL: 'International (Conv. Washington 1973)',
};

/** Libellés humains des verdicts. */
export const VERDICT_VALIDITE_LABELS: Readonly<Record<VerdictValidite, string>> = {
  VALIDE: 'Testament valide',
  CONTESTABLE: 'Testament contestable',
  NUL: 'Testament nul',
};

/** Libellés humains des codes de vices (chip alerte dans le résultat). */
export const CODE_VICE_LABELS: Readonly<Record<CodeVice, string>> = {
  FORME_OLOGRAPHE_NON_MANUSCRITE: 'Olographe non manuscrit',
  FORME_OLOGRAPHE_NON_DATE: 'Olographe non daté',
  FORME_OLOGRAPHE_NON_SIGNE: 'Olographe non signé',
  FORME_AUTHENTIQUE_NOTAIRES_TEMOINS: 'Authentique : notaires/témoins non conformes',
  FORME_AUTHENTIQUE_DICTEE_MANQUANTE: 'Authentique : dictée manquante',
  FORME_AUTHENTIQUE_LECTURE_MANQUANTE: 'Authentique : lecture finale manquante',
  FORME_AUTHENTIQUE_SIGNATURES_INCOMPLETES: 'Authentique : signatures incomplètes',
  FORME_MYSTIQUE_PLI_NON_CACHE: 'Mystique : pli non cacheté',
  FORME_MYSTIQUE_TEMOINS: 'Mystique : témoins manquants',
  FORME_MYSTIQUE_SUSCRIPTION: 'Mystique : acte de suscription manquant',
  FORME_INTERNATIONAL_WASHINGTON: 'International : forme Washington non respectée',
  INCAPACITE_MINEUR_MOINS_16_ANS: 'Incapacité — mineur < 16 ans (art. 904)',
  INSANITE_ESPRIT: "Insanité d'esprit (art. 901)",
  MAJEUR_PROTEGE_SANS_ASSISTANCE: 'Majeur protégé sans assistance (art. 470, 476)',
  VICE_CONSENTEMENT_DOL: 'Vice consentement — dol/violence',
  VICE_CONSENTEMENT_ERREUR: 'Vice consentement — erreur substantielle',
  REVOCATION_TESTAMENT_POSTERIEUR: 'Révocation par testament postérieur (art. 1036)',
  REVOCATION_DECHIRURE: 'Révocation par déchirure volontaire (art. 1038)',
};

/**
 * Requête d'analyse (alignée sur le record backend
 * `TestamentValiditeRequest`). Tous les champs `Boolean` sont nullable —
 * seuls les champs spécifiques à la forme retenue sont exigés par le backend.
 */
export interface TestamentValiditeRequest {
  formeTestament: FormeTestament;
  dateRedaction: string; // ISO YYYY-MM-DD
  ageTestateurAnsRedaction: number;
  saineDEsprit: boolean;
  majeurProtegeAvecAssistance: boolean | null;
  // Olographe
  ecritureManuscritIntegrale: boolean | null;
  dateComplete: boolean | null;
  signatureTestateur: boolean | null;
  // Authentique
  presenceNotaireEtTemoinsConforme: boolean | null;
  dicteEnPresence: boolean | null;
  lectureFinaleAuTestateur: boolean | null;
  signaturesCompletes: boolean | null;
  // Mystique
  remiseSousPliCache: boolean | null;
  declarationDevant2Temoins: boolean | null;
  acteSuscriptionNotaire: boolean | null;
  // International
  respecteFormeWashington: boolean | null;
  // Vices consentement
  vicesConsentementDol: boolean;
  erreurSubstantielle: boolean;
  // Révocation
  testamentPosterieurContradictoire: boolean;
  dechirureVolontaireOriginal: boolean;
  // Quotité disponible
  legsExcedeQuotiteDisponible: boolean;
}

/** Réponse de l'endpoint d'analyse. */
export interface TestamentValiditeResponse {
  caseFileId: string;
  formeTestament: FormeTestament;
  verdictValidite: VerdictValidite;
  vicesIdentifies: ViceIdentifie[];
  actionEnReductionPossible: boolean;
  delaiContestationAns: number;
  scoreEligibilite: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
