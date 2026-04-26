/**
 * SF-FA-24-06 : modèles TypeScript pour l'outil décisionnel "Validité d'une
 * donation entre vifs" (F-FA-24). FR uniquement — art. 893-958, 902-906, 920+,
 * 931, 953-958 Cciv (4 formes + capacité + consentement + formalisme + quotité
 * disponible + révocation).
 *
 * Contrat figé dans SF-FA-24-05 (backend, mergé PR #671).
 */

/** Forme de la donation entre vifs. */
export type FormeDonation =
  | 'DONATION_NOTARIEE'
  | 'DONATION_MANUELLE'
  | 'DON_INDIRECT'
  | 'DONATION_DEGUISEE';

/** Verdict de validité. */
export type VerdictValiditeDonation = 'VALIDE' | 'CONTESTABLE' | 'NUL';

/** Codes de risques de requalification (alignés sur l'enum backend). */
export type CodeRisqueRequalification =
  | 'FORME_NOTARIEE_NON_AUTHENTIQUE'
  | 'FORME_NOTARIEE_SANS_ACCEPTATION'
  | 'FORME_MANUELLE_SANS_REMISE'
  | 'FORME_MANUELLE_BIEN_NON_MEUBLE'
  | 'DON_INDIRECT_INTENTION_LIBERALE'
  | 'REQUALIFICATION_DEGUISEMENT'
  | 'DEGUISEMENT_PRIX_VIL'
  | 'INCAPACITE_DONATEUR'
  | 'INSANITE_ESPRIT'
  | 'INCAPACITE_RECIPIENDAIRE'
  | 'VICE_CONSENTEMENT_DOL'
  | 'VICE_CONSENTEMENT_ERREUR'
  | 'OBJET_INDETERMINE'
  | 'FORMALISME_NON_RESPECTE'
  | 'EXCES_QUOTITE_DISPONIBLE'
  | 'REVOCATION_INGRATITUDE'
  | 'REVOCATION_INEXECUTION_CHARGE';

/** Risque de requalification renvoyé par le backend. */
export interface RisqueRequalification {
  code: CodeRisqueRequalification;
  description: string;
}

/** Libellés humains des formes (radio + chip). */
export const FORME_DONATION_LABELS: Readonly<Record<FormeDonation, string>> = {
  DONATION_NOTARIEE: 'Donation notariée (art. 931)',
  DONATION_MANUELLE: 'Donation manuelle (jurisprudence)',
  DON_INDIRECT: 'Don indirect',
  DONATION_DEGUISEE: 'Donation déguisée',
};

/** Libellés humains des verdicts. */
export const VERDICT_VALIDITE_DONATION_LABELS:
    Readonly<Record<VerdictValiditeDonation, string>> = {
  VALIDE: 'Donation valide',
  CONTESTABLE: 'Donation contestable',
  NUL: 'Donation nulle',
};

/** Libellés humains des codes de risque. */
export const CODE_RISQUE_REQUALIFICATION_LABELS:
    Readonly<Record<CodeRisqueRequalification, string>> = {
  FORME_NOTARIEE_NON_AUTHENTIQUE: 'Notariée : acte non authentique (art. 931)',
  FORME_NOTARIEE_SANS_ACCEPTATION: 'Notariée : acceptation non expresse',
  FORME_MANUELLE_SANS_REMISE: 'Manuelle : remise effective absente',
  FORME_MANUELLE_BIEN_NON_MEUBLE: 'Manuelle : bien immeuble (forme inadaptée)',
  DON_INDIRECT_INTENTION_LIBERALE: 'Don indirect : intention libérale non démontrée',
  REQUALIFICATION_DEGUISEMENT: 'Risque de requalification — donation déguisée',
  DEGUISEMENT_PRIX_VIL: 'Déguisement : prix manifestement vil',
  INCAPACITE_DONATEUR: 'Incapacité du donateur (art. 902)',
  INSANITE_ESPRIT: "Insanité d'esprit du donateur (art. 901)",
  INCAPACITE_RECIPIENDAIRE: 'Incapacité du récipiendaire (art. 906, 909)',
  VICE_CONSENTEMENT_DOL: 'Vice consentement — dol/violence (art. 901)',
  VICE_CONSENTEMENT_ERREUR: 'Vice consentement — erreur substantielle',
  OBJET_INDETERMINE: 'Objet indéterminé (art. 893+)',
  FORMALISME_NON_RESPECTE: 'Formalisme non respecté',
  EXCES_QUOTITE_DISPONIBLE: 'Excès de la quotité disponible (art. 913+)',
  REVOCATION_INGRATITUDE: 'Révocation possible — ingratitude (art. 955-958)',
  REVOCATION_INEXECUTION_CHARGE: 'Révocation possible — inexécution charge (art. 953)',
};

/**
 * Requête d'analyse — alignée sur le record backend `DonationRequest`.
 * Tous les booleans conditionnels à la forme sont nullable.
 */
export interface DonationRequest {
  formeDonation: FormeDonation;
  dateDonation: string; // ISO YYYY-MM-DD
  ageDonateurAns: number;
  saineDEsprit: boolean;
  capaciteDonateur: boolean;
  capaciteRecipiendaire: boolean;
  consentementLibre: boolean;
  objetDetermine: boolean;
  respectFormalisme: boolean;
  respectQuotiteDisponible: boolean;
  // Notariée
  acteAuthentique: boolean | null;
  acceptationExpresse: boolean | null;
  // Manuelle
  remiseEffective: boolean | null;
  bienMeuble: boolean | null;
  // Don indirect
  intentionLiberale: boolean | null;
  actePrincipalNeutre: boolean | null;
  // Donation déguisée
  apparenceOnerueuse: boolean | null;
  prixIncoherent: boolean | null;
  // Vices consentement
  vicesConsentementDol: boolean;
  erreurSubstantielle: boolean;
  // Révocation post-formation
  ingratitudeAvere: boolean;
  inexecutionCharge: boolean;
}

/** Réponse de l'endpoint d'analyse. */
export interface DonationResponse {
  caseFileId: string;
  formeDonation: FormeDonation;
  verdictValidite: VerdictValiditeDonation;
  risquesRequalification: RisqueRequalification[];
  actionEnReductionPossible: boolean;
  revocationPossible: boolean;
  delaiContestationAns: number;
  scoreEligibilite: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
