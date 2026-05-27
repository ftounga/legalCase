/**
 * SF-215-07 / SF-215-08 — modèles miroirs du contrat API (backend) pour l'outil
 * décisionnel « Naturalisation art. 12bis (BE) » (F-IM-28-naturalisation-12bis-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité pour la déclaration de
 * nationalité belge sous l'art. 12bis du Code de la nationalité belge
 * (CNB — Loi du 28/06/1984, mod. Loi du 04/12/2012). Deux voies :
 *  - voie 5 ans  : séjour légal ≥ 5 ans (illimité) + langue ≥ A2 +
 *                  preuve intégration sociale + preuve intégration économique
 *                  (emploi 468 jours / 5 ans).
 *  - voie 10 ans : séjour légal ≥ 10 ans (illimité) + langue ≥ A2 +
 *                  participation sociale (raisons humanitaires / médicales).
 *
 * Le verdict expose `voieEligible` = `VOIE_5_ANS` / `VOIE_10_ANS` / `AUCUNE`,
 * la liste des critères non remplis, et la `dureeManquante` en mois (0 si
 * voie éligible). Les conditions d'ordre public (`menaceOrdrePublic`,
 * `condamnationPenale`) sont rédhibitoires.
 */

export type Naturalisation12bisTypeSejour = 'LIMITE' | 'ILLIMITE';

export type Naturalisation12bisNiveauLangue =
  | 'INFERIEUR_A2'
  | 'A2'
  | 'SUPERIEUR_A2';

export type Naturalisation12bisVoie = 'VOIE_5_ANS' | 'VOIE_10_ANS' | 'AUCUNE';

export interface Naturalisation12bisBeRequest {
  dureeSejour: number;        // mois 0-600
  typeSejour: Naturalisation12bisTypeSejour;
  niveauLangue: Naturalisation12bisNiveauLangue;
  preuveIntegration: boolean;
  preuveEmploi: boolean;
  menaceOrdrePublic: boolean;
  condamnationPenale: boolean;
}

export interface Naturalisation12bisBeResponse {
  caseFileId: string;
  dureeSejour: number;
  typeSejour: string;
  niveauLangue: string;
  preuveIntegration: boolean;
  preuveEmploi: boolean;
  menaceOrdrePublic: boolean;
  condamnationPenale: boolean;
  voie5Ans: boolean;
  voie10Ans: boolean;
  voieEligible: Naturalisation12bisVoie;
  dureeManquante: number;     // mois, 0 si voie éligible
  criteresNonRemplis: string[];
  procedureReference: string; // « Chambre des représentants — Commission des naturalisations »
  baseJuridique: string;
}

export interface Naturalisation12bisTypeSejourOption {
  code: Naturalisation12bisTypeSejour;
  label: string;
}

export interface Naturalisation12bisNiveauLangueOption {
  code: Naturalisation12bisNiveauLangue;
  label: string;
}

export const NATURALISATION_12BIS_TYPES_SEJOUR: ReadonlyArray<Naturalisation12bisTypeSejourOption> = [
  { code: 'LIMITE', label: 'Séjour limité (carte A)' },
  { code: 'ILLIMITE', label: 'Séjour illimité (carte B / C / D / F+ / K / L)' },
];

export const NATURALISATION_12BIS_NIVEAUX_LANGUE: ReadonlyArray<Naturalisation12bisNiveauLangueOption> = [
  { code: 'INFERIEUR_A2', label: 'Inférieur à A2' },
  { code: 'A2', label: 'A2 — niveau requis' },
  { code: 'SUPERIEUR_A2', label: 'Supérieur à A2 (B1 / B2 / C1 / C2)' },
];

/** Durée requise (mois) pour la voie 5 ans (60 mois = 5 ans calendaires). */
export const DUREE_SEJOUR_VOIE_5_ANS_MOIS = 60;
/** Durée requise (mois) pour la voie 10 ans (120 mois = 10 ans calendaires). */
export const DUREE_SEJOUR_VOIE_10_ANS_MOIS = 120;
