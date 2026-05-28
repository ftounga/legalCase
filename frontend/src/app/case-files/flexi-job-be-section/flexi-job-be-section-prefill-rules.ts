import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-12b — Helper partagé pour le pré-remplissage IA de
 * {@code FlexiJobBeSectionComponent} (F-219, BE uniquement —
 * Loi-programme du 26/12/2013 art. 13 à 28 + Loi-programme 16/11/2015 +
 * Loi-programme 30/10/2018 + Loi-programme 28/12/2023 +
 * AR 02/06/2024).
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
 *   <li>{@code datePremiereOccupationFlexi} — date de début de la
 *       première prestation flexi pour CET employeur (date employeur,
 *       hors trajectoire salarié principal extraite par le pipeline).</li>
 *   <li>{@code statutTravailleur} — qualification métier (pensionné /
 *       4/5 ETP T-3 / autre) dépendant du parcours du candidat flexi
 *       et de sa carrière chez un AUTRE employeur (DmfA T-3 ONSS).
 *       Non extractible du dossier salarié principal.</li>
 *   <li>{@code secteurEmployeur} — commission paritaire de l'employeur
 *       flexi (HoReCa, boulangerie, soins de santé, secteur public, etc.).
 *       Donnée employeur flexi, pas extractible du dossier salarié.</li>
 *   <li>{@code dejaOccupeChezMemeEmployeur} — flag anti-substitution
 *       (cumul interdit même employeur). Information opérationnelle
 *       déclarative non extractible.</li>
 *   <li>{@code contratCadreSigne} — formalisme contrat-cadre écrit
 *       (art. 16 Loi-prog. 26/12/2013). Document non scanné dans le
 *       pipeline Travail BE standard.</li>
 *   <li>{@code dimonaFlxDeclaree} — déclaration Dimona FLX (DmfA ONSS).
 *       Donnée administrative employeur flexi, non extractible.</li>
 *   <li>{@code flexiSalaireHoraireBrut} — taux horaire flexi convenu
 *       (€/h). Donnée contractuelle flexi spécifique, non rattachable
 *       au {@code salaireBrutMensuel} du salarié principal.</li>
 *   <li>{@code flexiSalaireMinimumApplicable} — paramètre légal indexé
 *       (~12,33 €/h au 01/05/2024). Constante réglementaire — saisi
 *       par l'avocat selon la date d'occupation.</li>
 *   <li>{@code revenuAnnuelFlexiCumule} — revenu flexi cumulé sur
 *       l'année civile en cours (€). Agrégat annuel non disponible
 *       en extraction de dossier salarié principal.</li>
 *   <li>{@code plafondAnnuelExonere} — paramètre légal indexé
 *       (~12 000 € au 01/05/2024, inapplicable aux pensionnés).
 *       Constante réglementaire — saisi par l'avocat.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code flexi-job-be-section-prefill-rules.spec.ts}.</p>
 */
export const FlexiJobBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier flexi (contrat-cadre, attestation Dimona FLX, DmfA T-3
   * de l'autre employeur, paramètres légaux indexés).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
