import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-03b — Helper partagé pour le pré-remplissage IA de
 * {@code RccBeEntrepriseDifficulteSectionComponent} (F-219, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * `prefillFromAi()` et static `getPrefillCount()` consomment le MÊME
 * helper pur ; divergence impossible par construction.
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Le pipeline IA Travail BE actuel n'extrait <b>aucun</b> champ
 * directement exploitable pour piloter l'analyseur RCC entreprise en
 * difficulté :</p>
 *
 * <ul>
 *   <li>{@code typeReconnaissance} — la reconnaissance ministérielle
 *       d'entreprise en difficulté ou en restructuration est une
 *       <b>décision administrative externe</b> (arrêté ministre Emploi,
 *       publication au Moniteur belge) jamais portée par le contrat de
 *       travail ni la lettre de licenciement individuels. Saisie avocat
 *       systématique sur la base de l'AR de reconnaissance ou de
 *       l'attestation employeur ad hoc.</li>
 *   <li>{@code ageReduitPlan} — paramètre fixé par le plan agréé par le
 *       ministre (typiquement 55 ans, parfois 50, parfois 52). Donnée
 *       absente des pièces dossier — l'avocat la lit dans l'AR de
 *       reconnaissance / la CCT sectorielle ad hoc.</li>
 *   <li>{@code ageFinContrat} — l'extraction IA Travail BE capture
 *       {@code dateNaissanceSalarie} et {@code dateLicenciement} mais pas
 *       un âge en années à la fin du contrat directement utilisable ici
 *       sans dérivation supplémentaire (différence date à date + arrondi,
 *       cas pivot autour de l'âge réduit). Saisie avocat V1 — à arbitrer
 *       dans une SF future dédiée d'enrichissement IA Travail BE.</li>
 *   <li>{@code anneesCarriereTotale} — la carrière professionnelle totale
 *       (jours équivalents temps plein, sur l'ensemble de la vie active
 *       au-delà du seul employeur courant) n'est pas extraite par le prompt
 *       Travail BE actuel — donnée structurelle issue du relevé ONSS / DRS
 *       (Dimona, parcours professionnel) plutôt qu'une mention récurrente
 *       dans les pièces contractuelles. Saisie avocat (relevé ONSS).</li>
 *   <li>{@code anneesAncienneteSecteur} — ancienneté dans le secteur /
 *       l'entreprise reconnue, distincte de l'ancienneté contrat courant.
 *       Demande une qualification poste-par-poste / employeur-par-employeur
 *       que l'extraction IA Travail BE actuelle ne couvre pas. Saisie
 *       avocat V1.</li>
 *   <li>{@code dateFinContrat} — extraite par le pipeline IA Travail BE
 *       ({@code dateLicenciement}) mais non pré-remplie en V1 par cohérence
 *       avec le pattern uniforme des outils F-213 vague 7-10 / SF-219-01b /
 *       SF-219-02b (0 champ également).</li>
 *   <li>{@code licenciementEffectif} — appréciation juridique (licenciement
 *       par l'employeur vs démission du travailleur), saisie avocat
 *       systématique — non extractible sans risque depuis le narratif IA.</li>
 *   <li>{@code remunerationNetteMensuelleReference} — l'extraction IA Travail
 *       BE fournit {@code salaireBrutMensuel} et/ou {@code salaireBrutAnnuel}
 *       (brut), pas le <b>net</b> de référence RCC ; dériver une rémunération
 *       nette mensuelle exacte demande une décision (précompte professionnel,
 *       cotisations ONSS personnelles, avantages, …) qui dépasse le pré-fill
 *       mécanique. Saisie avocat V1.</li>
 *   <li>{@code allocationChomageMensuelleEstimee} — estimation ONEM
 *       (calculateur officiel pension/chômage avec barèmes et plafonds
 *       actualisés), saisie avocat V1.</li>
 * </ul>
 *
 * <p>Décision alignée sur le pattern F-213 frontend (vagues 7b-10b) +
 * SF-219-01b + SF-219-02b — 0 champ pré-rempli pour les outils BE-only
 * V1. Enrichissement éventuel à arbitrer dans une SF future dédiée
 * (extraction d'âge dérivé + carrière totale ONSS + dérivation
 * rémunération nette).</p>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code rcc-be-entreprise-difficulte-section-prefill-rules.spec.ts}.</p>
 */
export const RccBeEntrepriseDifficulteSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis l'AR
   * de reconnaissance, le relevé ONSS, la lettre de fin de contrat et
   * l'estimation ONEM.</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
