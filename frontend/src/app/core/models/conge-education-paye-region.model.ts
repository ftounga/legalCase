/**
 * SF-219-11b : modèle frontend de l'outil F-219 — Congé-éducation payé
 * régionalisé BE (Loi du 22/01/1985 contenant des dispositions sociales,
 * Section 6 « Congé-éducation payé » art. 108 à 117 ; Loi spéciale du
 * 06/01/2014 — 6e réforme de l'État, transfert de compétence aux Régions
 * au 01/07/2014 ; Décret WBR 19/02/2014 + AGW 19/12/2014 ; Décret
 * 12/12/2014 — Vlaams Opleidingsverlof VOV ; Ordonnance 02/07/2015 + AGBR
 * 14/04/2016).
 *
 * <p><b>BE uniquement</b> — pas d'équivalent strict FR (le régime
 * français de financement de la formation salariée relève du CPF /
 * Pro-A / Plan de développement des compétences, art. L. 6111-1 et s.
 * du Code du travail français, non mappable). Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Outil unique qui branche en interne sur le régime régional
 * applicable (Wallonie / Flandre VOV / Bruxelles-Capitale) — la région
 * compétente est celle du <b>lieu de travail</b> du travailleur, pas
 * du domicile.</p>
 *
 * <p>Contrat figé dans SF-219-11 backend
 * ({@code CongeEducationPayeRegionRequest} +
 * {@code CongeEducationPayeRegionResponse}).</p>
 */

/** Verdict 5 états de l'outil congé-éducation payé régionalisé. */
export type CongeEducationPayeRegionVerdict =
  | 'ELIGIBLE_PLEIN_DROIT'
  | 'ELIGIBLE_PRORATA'
  | 'INELIGIBLE_HORS_FORMATION_AGREEE'
  | 'INELIGIBLE_OCCUPATION_INSUFFISANTE'
  | 'A_ANALYSER';

/**
 * Région compétente (post-6e réforme — la région pertinente est celle
 * du lieu de travail, pas du domicile).
 */
export type CongeEducationPayeRegionRegion =
  | 'WALLONIE'
  | 'FLANDRE'
  | 'BRUXELLES';

/**
 * Typologie de formation suivie au sens des régimes régionaux.
 *
 * <ul>
 *   <li>{@code PROFESSIONNELLE_QUALIFIANTE} — plafond le plus élevé.</li>
 *   <li>{@code GENERALE} — plafond intermédiaire.</li>
 *   <li>{@code ENSEIGNEMENT_SUPERIEUR_ALTERNANCE} — FLA = 250 h VOV.</li>
 *   <li>{@code RECONVERSION_PUBLIC_FRAGILISE} — régime dérogatoire.</li>
 *   <li>{@code HORS_LISTE_AGREEE} — formation absente de la liste
 *       agréée régionale — verdict INELIGIBLE_HORS_FORMATION_AGREEE.</li>
 * </ul>
 */
export type CongeEducationPayeRegionTypeFormation =
  | 'PROFESSIONNELLE_QUALIFIANTE'
  | 'GENERALE'
  | 'ENSEIGNEMENT_SUPERIEUR_ALTERNANCE'
  | 'RECONVERSION_PUBLIC_FRAGILISE'
  | 'HORS_LISTE_AGREEE';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/conge-education-paye-region}.
 *
 * <p>Tous les champs sont obligatoires (alignés sur le backend
 * {@code @NotNull}). {@code tauxOccupation} : 1.0 = temps plein,
 * 0.5 = mi-temps, etc. — valeur entre 0.0 et 1.0 strict (le backend
 * applique {@code @DecimalMin 0.0 / @DecimalMax 1.0}).</p>
 */
export interface CongeEducationPayeRegionRequest {
  /** Région du lieu de travail (gate régional). */
  region: CongeEducationPayeRegionRegion;
  /** Type de formation suivie. */
  typeFormation: CongeEducationPayeRegionTypeFormation;
  /** Heures de formation suivies par an (présentiel + assimilés). */
  heuresFormationAnnuelles: number;
  /** Taux d'occupation contractuel (1.0 = temps plein, 0.5 = mi-temps). */
  tauxOccupation: number;
  /**
   * Travailleur public fragilisé (peu qualifié au sens du décret VOV
   * FLA — déclenche la dérogation occupation 1/5 temps en Flandre
   * uniquement, sans effet WBR / BXL).
   */
  publicFragilise: boolean;
  /** Formation présente dans la liste agréée régionale. */
  formationAgreeeRegion: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs métier
 * (verdict, plafond régional applicable, heures effectivement allouées
 * prorata, libellé du régime régional).
 */
export interface CongeEducationPayeRegionResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  region: CongeEducationPayeRegionRegion;
  typeFormation: CongeEducationPayeRegionTypeFormation;
  heuresFormationAnnuelles: number;
  tauxOccupation: number;
  publicFragilise: boolean;
  formationAgreeeRegion: boolean;

  // -- Outputs métier --
  /** Verdict 5 états. */
  verdict: CongeEducationPayeRegionVerdict;
  /**
   * Plafond régional applicable selon (région × typeFormation), AVANT
   * proratisation au taux d'occupation. 0 si verdict inéligible.
   */
  plafondRegionalHeures: number;
  /**
   * Heures effectivement allouées =
   * min(heuresFormationAnnuelles, plafondRegional × tauxOccupation).
   * 0 si verdict inéligible.
   */
  heuresCongeAllouees: number;
  /** Libellé court du régime régional applicable. */
  regimeApplicable: string;

  // -- Synthèse + métadonnées légales --
  /** Code raison du verdict. */
  raison: string;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement éventuel. */
  avertissement: string;
}
