/**
 * SF-215-09 / SF-215-10 — modèles miroirs du contrat API (backend) pour
 * l'outil décisionnel « Naturalisation conjoint Belge — art. 16 »
 * (F-IM-29-naturalisation-conjoint-belge-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité à la déclaration de
 * nationalité belge par voie de l'article 16 du Code de la nationalité
 * belge (CNB), ouverte au conjoint étranger d'un(e) Belge qui justifie :
 *  - d'un mariage avec un(e) Belge (et vie commune en Belgique sous le
 *    régime du mariage ou de la cohabitation légale),
 *  - de 5 ans de séjour légal commun (cohabitation effective) en
 *    Belgique sans interruption notable,
 *  - d'une connaissance d'une des trois langues officielles (FR / NL /
 *    DE) au niveau A2 minimum,
 *  - d'une preuve d'intégration sociale (cours régional / inscription).
 *
 * Le verdict expose `eligible` (booléen), la liste des critères non
 * remplis et la `dureeManquante` en mois (0 si éligible). Les conditions
 * d'ordre public (`menaceOrdrePublic`, `condamnationPenale`) sont
 * rédhibitoires (un seul `true` => `eligible = false`).
 *
 * Pattern miroir {@link Naturalisation12bisBeRequest} (SF-215-08) — l'art. 16
 * réutilise l'enum `Naturalisation12bisNiveauLangue` pour le niveau de
 * langue (la voie « conjoint Belge » impose le même seuil A2 CECRL).
 */

import { Naturalisation12bisNiveauLangue } from './naturalisation-12bis-be.model';

export type NaturalisationConjointBelgeBeNiveauLangue = Naturalisation12bisNiveauLangue;

export interface NaturalisationConjointBelgeBeRequest {
  /** Date du mariage (ISO yyyy-MM-dd). */
  dateMarriage: string;
  /** Cohabitation légale (si pas mariage civil) — informatif. */
  cohabitationLegale: boolean;
  /** Durée cohabitation effective en mois (entier 0-600). */
  dureeCohabitationMois: number;
  /** Niveau de langue CECRL (FR/NL/DE) — A2 minimum requis. */
  niveauLangue: NaturalisationConjointBelgeBeNiveauLangue;
  /** Preuve d'intégration sociale (cours régional validé). */
  preuveIntegration: boolean;
  /** Menace pour l'ordre public — rédhibitoire. */
  menaceOrdrePublic: boolean;
  /** Condamnation pénale (peine effective) — rédhibitoire. */
  condamnationPenale: boolean;
}

export interface NaturalisationConjointBelgeBeResponse {
  caseFileId: string;
  dateMarriage: string;
  cohabitationLegale: boolean;
  dureeCohabitationMois: number;
  niveauLangue: string;
  preuveIntegration: boolean;
  menaceOrdrePublic: boolean;
  condamnationPenale: boolean;
  /** Verdict global : true = ELIGIBLE, false = INELIGIBLE. */
  eligible: boolean;
  /** Durée restante (mois) avant la cohabitation 5 ans (0 si remplie). */
  dureeManquante: number;
  /** Critères non remplis explicitement (rendu mat-list rouge). */
  criteresNonRemplis: string[];
  /** Bloc info procédure : « Officier d'état civil — délai 3-6 mois ». */
  delaiDeclaration: string;
  /** Citation juridique en bas du verdict (JetBrains Mono). */
  baseJuridique: string;
}

export interface NaturalisationConjointBelgeBeNiveauLangueOption {
  code: NaturalisationConjointBelgeBeNiveauLangue;
  label: string;
}

/**
 * Réutilise l'enum partagé SF-215-07 — la voie « conjoint Belge »
 * (art. 16 CNB) impose le même seuil A2 CECRL que la voie 12bis 5 ans.
 */
export const NATURALISATION_CONJOINT_BELGE_BE_NIVEAUX_LANGUE:
  ReadonlyArray<NaturalisationConjointBelgeBeNiveauLangueOption> = [
    { code: 'INFERIEUR_A2', label: 'Inférieur à A2' },
    { code: 'A2', label: 'A2 — niveau requis' },
    { code: 'SUPERIEUR_A2', label: 'Supérieur à A2 (B1 / B2 / C1 / C2)' },
  ];

/** Durée requise (mois) de cohabitation effective — 5 ans = 60 mois. */
export const NATURALISATION_CONJOINT_BELGE_DUREE_COHABITATION_MOIS = 60;
