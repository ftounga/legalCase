/**
 * SF-219-22b : modèle frontend de l'outil F-219 — égalité salariale
 * femmes / hommes BE (Loi du 22/04/2012 + AR du 17/08/2013 + AR du
 * 25/04/2014 + CCT n° 25 + C. pén. social art. 195/1).
 *
 * <p><b>BE uniquement</b> — le régime français d'index égalité
 * professionnelle (Code du travail art. L. 1142-7 et s., Décret
 * n° 2019-15 du 08/01/2019) constitue un dispositif juridiquement
 * distinct : notation 100 points, plan de rattrapage si &lt; 75/100,
 * publication obligatoire. Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 *
 * <p>Contrat figé dans SF-219-22 backend
 * ({@code EgaliteFemmesHommesBeRequest} +
 * {@code EgaliteFemmesHommesBeResponse}).</p>
 */

/**
 * Statut du rapport biennal d'analyse de la structure de rémunération
 * H/F (art. 2 Loi 22/04/2012 + AR 25/04/2014 — formulaires
 * standardisés ann. I ≥ 100 ETP / ann. II 50-99 ETP) que l'employeur
 * ≥ 50 travailleurs doit déposer tous les deux exercices.
 *
 * <ul>
 *   <li>{@code DEPOSE_FORMULAIRE_COMPLET} — rapport biennal déposé
 *       au Conseil d'entreprise (à défaut DS) sur le formulaire
 *       officiel complet.</li>
 *   <li>{@code DEPOSE_FORMULAIRE_INCOMPLET} — rapport déposé mais
 *       sections manquantes (non-conformité matérielle art. 4 AR
 *       17/08/2013).</li>
 *   <li>{@code EN_PREPARATION} — exercice biennal en cours, dépôt
 *       dans les délais à venir.</li>
 *   <li>{@code NON_DEPOSE_DELAI_DEPASSE} — délai dépassé sans
 *       rapport — manquement frontal.</li>
 *   <li>{@code INDETERMINE} — statut non encore déterminé.</li>
 * </ul>
 */
export type EgaliteFemmesHommesBeStatutRapport =
  | 'DEPOSE_FORMULAIRE_COMPLET'
  | 'DEPOSE_FORMULAIRE_INCOMPLET'
  | 'EN_PREPARATION'
  | 'NON_DEPOSE_DELAI_DEPASSE'
  | 'INDETERMINE';

/**
 * Verdict hiérarchisé 6 états — ordre de priorité (court-circuits
 * backend {@code EgaliteFemmesHommesBeChecker}) :
 *
 * <ol>
 *   <li>{@code A_ANALYSER} — statut indéterminé / en préparation.</li>
 *   <li>{@code HORS_CHAMP_SEUIL_NON_ATTEINT} — effectif &lt; 50.</li>
 *   <li>{@code MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE} — délai dépassé.</li>
 *   <li>{@code NON_CONFORME_RAPPORT_INCOMPLET} — ventilation art. 4
 *       AR 17/08/2013 incomplète.</li>
 *   <li>{@code NON_CONFORME_PLAN_ACTION_MANQUANT} — écart constaté
 *       sans plan d'action art. 5 Loi 22/04/2012.</li>
 *   <li>{@code CONFORME_RAPPORT_PLAN_COMPLETS} — conformité globale.</li>
 * </ol>
 */
export type EgaliteFemmesHommesBeVerdict =
  | 'HORS_CHAMP_SEUIL_NON_ATTEINT'
  | 'CONFORME_RAPPORT_PLAN_COMPLETS'
  | 'NON_CONFORME_RAPPORT_INCOMPLET'
  | 'NON_CONFORME_PLAN_ACTION_MANQUANT'
  | 'MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE'
  | 'A_ANALYSER';

/**
 * Body POST
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/egalite-femmes-hommes-be}.
 *
 * <p>Tous les champs marqués requis sont {@code @NotNull} côté backend
 * (cf. {@code EgaliteFemmesHommesBeRequest}). {@code dateDepotRapport}
 * et {@code pourcentageEcartConstate} sont optionnels.</p>
 */
export interface EgaliteFemmesHommesBeRequest {
  /** Effectif moyen de l'entreprise (ETP, Loi 04/12/2007 art. 7). */
  effectifEntreprise: number;
  /** Statut du rapport biennal — aiguille le verdict. */
  statutRapport: EgaliteFemmesHommesBeStatutRapport;
  /** Date de dépôt du rapport au CE / DS (facultatif). */
  dateDepotRapport: string | null;
  /** Ventilation par niveau de fonction (art. 4 AR 17/08/2013). */
  ventilationNiveauFonctionFournie: boolean;
  /** Ventilation par ancienneté (art. 4 AR 17/08/2013). */
  ventilationAncienneteFournie: boolean;
  /** Ventilation par niveau de qualification (art. 4 AR 17/08/2013). */
  ventilationQualificationFournie: boolean;
  /** Ventilation par régime de travail (art. 4 AR 17/08/2013). */
  ventilationRegimeTravailFournie: boolean;
  /** Ventilation par composants de rémunération (art. 4 AR 17/08/2013). */
  ventilationComposantsRemunerationFournie: boolean;
  /** Écart salarial non justifié constaté (art. 5 Loi 22/04/2012). */
  ecartSalarialNonJustifieConstate: boolean;
  /** Pourcentage indicatif de l'écart constaté (0..100, facultatif). */
  pourcentageEcartConstate: number | null;
  /** Plan d'action concret avec objectifs chiffrés établi (art. 5). */
  planActionEtabli: boolean;
  /** Médiateur égalité H/F désigné en interne (art. 14 + CCT n° 25). */
  mediateurDesigne: boolean;
  /** Plainte IEFH ou inspection SPF Emploi en cours. */
  plainteIefhOuInspectionEnCours: boolean;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs
 * calculés + verdict + métadonnées légales.
 */
export interface EgaliteFemmesHommesBeResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  effectifEntreprise: number;
  statutRapport: EgaliteFemmesHommesBeStatutRapport;
  dateDepotRapport: string | null;
  ventilationNiveauFonctionFournie: boolean;
  ventilationAncienneteFournie: boolean;
  ventilationQualificationFournie: boolean;
  ventilationRegimeTravailFournie: boolean;
  ventilationComposantsRemunerationFournie: boolean;
  ecartSalarialNonJustifieConstate: boolean;
  pourcentageEcartConstate: number | null;
  planActionEtabli: boolean;
  mediateurDesigne: boolean;
  plainteIefhOuInspectionEnCours: boolean;

  // -- Outputs calculés --
  /** Verdict hiérarchisé 6 états. */
  verdict: EgaliteFemmesHommesBeVerdict;
  /** Seuil 50 travailleurs atteint. */
  seuilAtteint: boolean;
  /** Rapport biennal effectivement déposé (complet ou incomplet). */
  rapportDepose: boolean;
  /** Les 5 sections de ventilation art. 4 AR 17/08/2013 sont fournies. */
  ventilationComplete: boolean;
  /** Plan d'action requis (= écart non justifié constaté). */
  planActionRequis: boolean;
  /** Plan d'action conforme (établi si requis). */
  planActionConforme: boolean;
  /**
   * Type de formulaire requis (ANNEXE_I_DETAILLE ≥ 100 ETP,
   * ANNEXE_II_SIMPLIFIE 50-99 ETP, NON_APPLICABLE &lt; 50).
   */
  typeFormulaireRequis: string;

  // -- Verdict + métadonnées légales --
  /** Code raison du verdict ou null. */
  raison: string | null;
  /** Synthèse humaine 1-2 phrases. */
  synthese: string;
  /** Base juridique courte. */
  baseJuridique: string;
  /** Avertissement avocat (effectif ETP, formulaire, ventilation, etc.). */
  avertissement: string;
}
