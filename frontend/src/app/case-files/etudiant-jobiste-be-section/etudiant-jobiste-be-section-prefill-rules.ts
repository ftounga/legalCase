import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-13b — Helper partagé pour le pré-remplissage IA de
 * {@code EtudiantJobisteBeSectionComponent} (F-219, BE uniquement —
 * Loi du 03/07/1978 Titre VII + Loi-programme du 24/12/2002 + AR du
 * 14/07/1995 + Loi-programme du 22/12/2023).
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
 *   <li>{@code dateDebutOccupation} — date de début de la prestation
 *       étudiante chez cet employeur (date employeur, hors trajectoire
 *       salarié principal extraite par le pipeline Travail BE).</li>
 *   <li>{@code statutEtudiant} — qualification métier (étudiant
 *       principal / interruption courte / vide pédagogique / non
 *       étudiant) dépendant du parcours académique du candidat
 *       (inscription régulière, calendrier scolaire). Non extractible
 *       du dossier salarié principal.</li>
 *   <li>{@code heuresDejaPresteesDansAnnee} — compteur Student@work
 *       consolidé (ONSS) — donnée employeurs cumulés, non extractible
 *       d'un seul dossier.</li>
 *   <li>{@code heuresContratEnCours} — volume horaire convenu du
 *       contrat étudiant à venir — donnée contractuelle spécifique,
 *       non extractible.</li>
 *   <li>{@code quotaAnnuelHeures} — paramètre légal (= 600 depuis 2023,
 *       475 antérieurement). Constante réglementaire — saisi par
 *       l'avocat selon l'année du contrat.</li>
 *   <li>{@code contratEcritSigne} — formalisme contrat écrit (art. 123
 *       Loi 03/07/1978). Document non scanné dans le pipeline Travail
 *       BE standard.</li>
 *   <li>{@code dimonaStuDeclaree} — déclaration Dimona STU (AR
 *       05/11/2002). Donnée administrative employeur, non extractible.</li>
 *   <li>{@code cotisationsReduitesAppliquees} — flag application
 *       barème AR 14/07/1995 (5,42 % + 2,71 %). Donnée DmfA ONSS, non
 *       extractible du dossier salarié principal.</li>
 *   <li>{@code remunerationBruteHoraire} — taux horaire brut convenu
 *       (€/h). Donnée contractuelle étudiant spécifique, non
 *       rattachable au {@code salaireBrutMensuel} du salarié principal.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code etudiant-jobiste-be-section-prefill-rules.spec.ts}.</p>
 */
export const EtudiantJobisteBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier étudiant (attestation d'inscription scolaire, compteur
   * Student@work, contrat d'occupation étudiant, DmfA ONSS).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
