/**
 * SF-215-11 / SF-215-12 — modèles miroirs du contrat API (backend) pour
 * l'outil décisionnel composite « AESM + tutelle DGDE (MENA) »
 * (F-IM-30-aesm-mena-be).
 *
 * BELGIQUE uniquement — outil destiné aux Mineurs Étrangers Non
 * Accompagnés (MENA) présents sur le territoire belge :
 *  - Volet 1 — Tutelle DGDE : Loi-programme du 24/12/2002, Titre XIII,
 *    Chapitre VI (Loi du 04/05/2007 portant tutelle des MENA) — saisine
 *    obligatoire du Service des Tutelles (DGDE) dans les 24h suivant
 *    l'identification du mineur.
 *  - Volet 2 — AESM scoring : Autorisation Exceptionnelle de Séjour aux
 *    Mineurs étrangers (Art. 9bis Loi 15/12/1980 adapté + Circulaire OE
 *    15/09/2005) — scoring 0-100 d'éligibilité au séjour humanitaire
 *    spécifique mineurs.
 *
 * Le verdict expose `etapeTutelle` (tutelle DGDE — null si déjà désigné),
 * `delaiDesignationTuteur` (ISO yyyy-MM-dd — délai légal 24h ouvrables),
 * `scoreIntegration` (0-100), `bonus` (0 ou +5 si scolarisation longue
 * durée), `verdictAESM` (FAVORABLE / SOUS_RESERVE / DEFAVORABLE),
 * `prioriteUrgence` (true si ageActuel ≥ 17 → bandeau rouge UI),
 * `criteresNonRemplis` (mat-list rouge).
 *
 * Pattern miroir {@link NaturalisationConjointBelgeBeRequest} (SF-215-10)
 * — composite 2 volets, BE-only, CONTEXTUAL flag
 * `mineur_non_accompagne_be_detecte`.
 */

export type AesmMenaBeVerdict = 'FAVORABLE' | 'SOUS_RESERVE' | 'DEFAVORABLE';

export interface AesmMenaBeRequest {
  /** Âge actuel du MENA (0-17, entier). */
  ageActuel: number;
  /** Date d'arrivée en Belgique (ISO yyyy-MM-dd, non future). */
  dateArriveeBelgique: string;
  /** Tuteur DGDE déjà désigné (cf. Loi 04/05/2007). */
  tuteurDesigne: boolean;
  /** Inscription scolaire effective sur le territoire belge. */
  integrationScolaire: boolean;
  /** Durée d'inscription scolaire en mois (entier 0-120). Requis si integrationScolaire=true. */
  dureeScolaire: number | null;
  /** Projet de vie élaboré (rapport tutelle / éducateur). */
  projetVieElabore: boolean;
  /** Perspective d'autonomie post-majorité (apprentissage, contrat employeur). */
  perspectiveAutonomie: boolean;
  /** Menace ordre public — rédhibitoire. */
  menaceOrdrePublic: boolean;
}

export interface AesmMenaBeResponse {
  caseFileId: string;
  ageActuel: number;
  dateArriveeBelgique: string;
  tuteurDesigne: boolean;
  integrationScolaire: boolean;
  dureeScolaire: number | null;
  projetVieElabore: boolean;
  perspectiveAutonomie: boolean;
  menaceOrdrePublic: boolean;
  /**
   * Volet tutelle — étape à initier si `tuteurDesigne=false`.
   * `null` si tuteur déjà désigné (volet tutelle clos).
   */
  etapeTutelle: string | null;
  /**
   * Délai légal limite pour désigner un tuteur DGDE (ISO yyyy-MM-dd).
   * Calculé à partir de `dateArriveeBelgique + 24h ouvrables` côté backend.
   */
  delaiDesignationTuteur: string;
  /** Score d'intégration AESM (0-100). */
  scoreIntegration: number;
  /** Bonus scolaire (0 ou 5 — scolarisation > 24 mois). */
  bonus: number;
  /** Verdict AESM : FAVORABLE / SOUS_RESERVE / DEFAVORABLE. */
  verdictAESM: AesmMenaBeVerdict;
  /**
   * Priorité urgence — true si `ageActuel >= 17`. Déclenche le bandeau
   * rouge UI (« Approche de la majorité — procédure urgente »).
   */
  prioriteUrgence: boolean;
  /** Critères non remplis (rendu mat-list rouge). */
  criteresNonRemplis: string[];
  /** Prochain acte de procédure (info). */
  prochainActe: string;
  /** Citation juridique en bas du verdict (JetBrains Mono). */
  baseJuridique: string;
}
