/**
 * SF-219-06b : modèle frontend de l'outil F-219 — Licenciement BE
 * fermeture d'entreprise (Loi du 26/06/2002 + AR du 23/03/2007 + CCT
 * n° 9bis, Fonds de Fermeture des Entreprises FFE).
 *
 * <p><b>BE uniquement</b> — aucun équivalent strict en droit français
 * (le FNGS français couvre la garantie des salaires en cas de
 * redressement / liquidation, mais ne porte pas d'indemnité de
 * fermeture forfaitaire indépendante).</p>
 *
 * <p>Contrat figé dans SF-219-06 backend
 * ({@code LicenciementBeFermetureEntrepriseRequest} +
 * {@code LicenciementBeFermetureEntrepriseResponse}).</p>
 */

/** Verdict 6 états de l'outil fermeture entreprise BE. */
export type LicenciementBeFermetureEntrepriseVerdict =
  | 'ELIGIBLE_FFE_COMPLET'
  | 'ELIGIBLE_FFE_REPRISE_CREANCES'
  | 'ELIGIBLE_INDEMNITE_SEULE'
  | 'INELIGIBLE_ANCIENNETE_INSUFFISANTE'
  | 'INELIGIBLE_TYPE_FERMETURE'
  | 'INELIGIBLE_SEUIL_EFFECTIF';

/**
 * Type de fermeture déclarée — détermine l'éligibilité aux régimes FFE.
 *
 * <ul>
 *   <li>{@code CESSATION_DEFINITIVE} — cessation totale et définitive
 *       (qualifie pleinement)</li>
 *   <li>{@code FAILLITE_LIQUIDATION_JUDICIAIRE} — faillite ou
 *       liquidation (qualifie + insolvabilité présumée)</li>
 *   <li>{@code CESSATION_PARTIELLE} — cessation d'une partie de
 *       l'activité (non qualifiant V1)</li>
 *   <li>{@code TRANSFERT_CCT_32BIS} — transfert d'entreprise CCT
 *       32bis (non qualifiant)</li>
 *   <li>{@code FUSION_SANS_SUPPRESSION} — fusion par absorption
 *       sans suppression d'emplois (non qualifiant)</li>
 *   <li>{@code FERMETURE_TEMPORAIRE} — chômage technique / force
 *       majeure (non qualifiant)</li>
 * </ul>
 */
export type LicenciementBeFermetureEntrepriseTypeFermeture =
  | 'CESSATION_DEFINITIVE'
  | 'FAILLITE_LIQUIDATION_JUDICIAIRE'
  | 'CESSATION_PARTIELLE'
  | 'TRANSFERT_CCT_32BIS'
  | 'FUSION_SANS_SUPPRESSION'
  | 'FERMETURE_TEMPORAIRE';

/**
 * Statut de solvabilité employeur — détermine l'intervention du FFE.
 *
 * <ul>
 *   <li>{@code SOLVABLE} — créances payées, FFE = indemnité seule.</li>
 *   <li>{@code INSOLVABLE_AVERE} — insolvabilité avérée, FFE reprend
 *       les créances impayées dans la limite des plafonds légaux.</li>
 *   <li>{@code FAILLITE_DECLAREE} — faillite déclarée Tribunal de
 *       l'entreprise, FFE intervient automatiquement.</li>
 * </ul>
 */
export type LicenciementBeFermetureEntrepriseStatutEmployeur =
  | 'SOLVABLE'
  | 'INSOLVABLE_AVERE'
  | 'FAILLITE_DECLAREE';

/**
 * Body POST {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-fermeture-entreprise}.
 *
 * <p>Tous les champs sont requis côté backend
 * ({@code @NotNull / @PositiveOrZero}).</p>
 */
export interface LicenciementBeFermetureEntrepriseRequest {
  /** Date de naissance — détermine l'âge à la fermeture (supplément ≥ 45 ans). */
  dateNaissance: string;
  /** Date de début de contrat — détermine l'ancienneté à la fermeture (seuil 1 an). */
  dateDebutContrat: string;
  /** Date de la fermeture (cessation officielle ou jugement de faillite). */
  dateFermeture: string;
  /** Rémunération mensuelle brute — base FFE pour reprise créances. */
  remunerationMensuelleBrute: number;
  /** Type de fermeture déclarée. */
  typeFermeture: LicenciementBeFermetureEntrepriseTypeFermeture;
  /** Solvabilité de l'employeur. */
  statutEmployeur: LicenciementBeFermetureEntrepriseStatutEmployeur;
  /** Effectif équivalent temps plein (seuil 20 régime général V1). */
  effectifEtp: number;
  /** Salaires impayés à reprendre par le FFE (0 si solvable). */
  salairesImpayes: number;
  /** Pécule de vacances dû et impayé. */
  peculeVacancesImpaye: number;
  /** Indemnité compensatoire de préavis due et impayée. */
  indemniteRuptureImpayee: number;
}

/**
 * Snapshot complet renvoyé par POST/GET — inputs (echo) + outputs métier.
 */
export interface LicenciementBeFermetureEntrepriseResponse {
  /** UUID du dossier. */
  caseFileId: string;

  // -- Echo inputs --
  dateNaissance: string;
  dateDebutContrat: string;
  dateFermeture: string;
  remunerationMensuelleBrute: number;
  typeFermeture: LicenciementBeFermetureEntrepriseTypeFermeture;
  statutEmployeur: LicenciementBeFermetureEntrepriseStatutEmployeur;
  effectifEtp: number;
  salairesImpayes: number;
  peculeVacancesImpaye: number;
  indemniteRuptureImpayee: number;

  // -- Outputs métier --
  verdict: LicenciementBeFermetureEntrepriseVerdict;
  /** True si l'un des 3 verdicts ELIGIBLE. */
  eligible: boolean;
  /** Âge à la date de fermeture (calcul anniversaire complet). */
  ageADateFermeture: number;
  /** Années d'ancienneté à la date de fermeture. */
  anneesAnciennete: number;
  /** Montant de l'indemnité de fermeture due (0 si inéligible). */
  indemniteFermeture: number;
  /** Composante forfaitaire annuelle (€/année d'ancienneté). */
  montantForfaitaireParAnnee: number;
  /** Supplément mensuel ≥ 45 ans (0 si < 45 ans ou inéligible). */
  supplementAgeMensuel: number;
  /** Montant total des créances FFE (salaires + pécule + rupture). */
  montantTotalCreancesFfe: number;

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
