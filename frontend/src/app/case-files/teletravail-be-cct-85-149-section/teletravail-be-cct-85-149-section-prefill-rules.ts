import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-16b — Helper partagé pour le pré-remplissage IA de
 * {@code TeletravailBeCct85149SectionComponent} (F-219, BE uniquement —
 * CCT n° 85 du 09/11/2005 + CCT n° 149 du 26/01/2021 + Loi du
 * 03/10/2022 « Deal pour l'emploi »).
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
 *   <li>{@code dateDebutTeletravail} — date d'entrée en vigueur de la
 *       formule télétravail (avenant / annexe contrat). Donnée
 *       organisationnelle employeur non extractible du dossier salarié
 *       principal (qui porte plutôt la trajectoire rupture / ancienneté).</li>
 *   <li>{@code typeTeletravail} — qualification STRUCTUREL_CCT_85 (régulier)
 *       vs OCCASIONNEL_CCT_149 (force majeure) vs INDETERMINE. Décision
 *       juridique de l'avocat selon la fréquence et le cadre.</li>
 *   <li>{@code volontariatReciproque} — flag art. 5 CCT n° 85. Information
 *       organisationnelle non extractible.</li>
 *   <li>{@code conventionEcriteIndividuelle} — formalisme art. 6 CCT n° 85
 *       (annexe / avenant individuel par télétravailleur). Pièce non
 *       systématiquement présente dans le dossier salarié rupture.</li>
 *   <li>{@code equipementFourniOuIndemnise} — flag art. 9 CCT n° 85
 *       (ordinateur, accès, support technique ou remboursement). Donnée
 *       opérationnelle employeur non extractible.</li>
 *   <li>{@code indemniteForfaitaireMensuelleEuros} — montant mensuel
 *       remboursement frais (internet, électricité, ergonomie). Donnée
 *       fiche de paie spécifique non rattachable au salaire brut mensuel
 *       extrait par le pipeline.</li>
 *   <li>{@code plafondIndemniteMensuelleEuros} — plafond ONSS/SPF Finances
 *       révisé annuellement (constante réglementaire — saisie par l'avocat
 *       selon la période d'occupation).</li>
 *   <li>{@code droitsSociauxMaintenus} — flag art. 4 CCT n° 85 (formation,
 *       carrière, droits collectifs identiques aux présentiels). Donnée
 *       déclarative non extractible.</li>
 *   <li>{@code deconnexionDefinie} — flag Loi 03/10/2022 (CCT d'entreprise
 *       ou règlement de travail fixant les modalités). Donnée
 *       organisationnelle employeur non extractible.</li>
 *   <li>{@code effectifEntreprise} — effectif total entreprise (gating
 *       Loi 03/10/2022 ≥ 20). Donnée employeur non rattachable au dossier
 *       salarié principal.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code teletravail-be-cct-85-149-section-prefill-rules.spec.ts}.</p>
 */
export const TeletravailBeCct85149SectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier organisationnel (avenant télétravail, règlement de travail,
   * convention individuelle écrite, fiches de paie indemnité, paramètres
   * ONSS/SPF Finances applicables).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
