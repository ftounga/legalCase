import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-17b — Helper partagé pour le pré-remplissage IA de
 * {@code ClauseEcolageBeSectionComponent} (F-219, BE uniquement —
 * art. 22bis Loi du 03/07/1978 sur les contrats de travail, inséré par
 * la Loi du 27/12/2006 M.B. 28/12/2006).
 *
 * <p>Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * {@code prefillFromAi()} et static {@code getPrefillCount()} consomment
 * le MÊME helper pur ; divergence impossible par construction.</p>
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Alignement strict avec le pattern uniforme F-213 / F-219 vagues
 * antérieures (0 champ pré-rempli). Justifications par champ :</p>
 *
 * <ul>
 *   <li>{@code typeFormation} — qualification SPECIFIQUE (CCT n° 13)
 *       vs OBLIGATOIRE_LEGALE (recyclage sécurité, KB/AR sectoriels)
 *       vs INDETERMINE. Décision juridique de l'avocat selon le
 *       contenu réel de la formation, non extractible mécaniquement
 *       du dossier salarié principal (qui porte plutôt la trajectoire
 *       rupture / ancienneté).</li>
 *   <li>{@code clauseEcriteAvantEntreeFormation} — formalisme art.
 *       22bis § 1er al. 1er (clause écrite reprenant description /
 *       durée / lieu / coût réel / dégressivité). Information
 *       formelle non extractible du modèle de base — nécessite la
 *       lecture de la clause elle-même (annexe au contrat ou
 *       convention de formation autonome).</li>
 *   <li>{@code coutReelFormationEuros} — coût réel de la formation
 *       supporté par l'employeur (frais d'inscription, salaire
 *       maintenu, déplacements). Donnée comptable non extractible
 *       du dossier salarié principal.</li>
 *   <li>{@code rmmmgMensuelEuros} — revenu minimum mensuel moyen
 *       garanti (CCT n° 43 du 02/05/1988 du CNT) au moment de la
 *       signature de la clause. Constante réglementaire indexée
 *       annuellement (saisie par l'avocat selon la période de
 *       signature, ex. 2 029,88 € en 2025 / 1 994,18 € en 2024).</li>
 *   <li>{@code dureeEfficaciteMois} — durée d'efficacité contractuelle
 *       de la clause (≤ 36 mois pour validité — art. 22bis § 4
 *       al. 1er). Stipulation clause à lire au cas par cas.</li>
 *   <li>{@code dateFinFormation} — date de fin effective de la
 *       formation. Information opérationnelle non extractible du
 *       dossier salarié principal (qui porte plutôt la date de
 *       rupture du contrat).</li>
 *   <li>{@code dateDepartTravailleur} — date de rupture du contrat.
 *       <i>Pourrait être pré-remplie depuis</i>
 *       {@code dateRuptureContrat} du modèle Travail BE — mais
 *       l'alignement pattern uniforme F-213 / F-219 vagues
 *       antérieures (0 champ pré-rempli) prime en V1 ; une V2 pourra
 *       l'activer une fois le mapping consolidé sur F-IA-03.</li>
 *   <li>{@code motifDepart} — qualification du motif de rupture sous
 *       l'angle art. 22bis § 3 (démission travailleur / licenciement
 *       sans motif grave employeur / licenciement motif grave
 *       travailleur / rupture motif grave employeur / fin CDD).
 *       Décision juridique de l'avocat — le mapping ne peut pas être
 *       dérivé mécaniquement du champ générique {@code motifRupture}
 *       du modèle Travail BE.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code clause-ecolage-be-section-prefill-rules.spec.ts}.</p>
 */
export const ClauseEcolageBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis la
   * clause elle-même (annexe contrat / convention de formation /
   * justificatifs comptables coût réel / paramètres CCT n° 43 du CNT
   * applicables à la date de signature).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
