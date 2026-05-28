import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-25b — Helper partagé pour le pré-remplissage IA de
 * {@code AuditoratTravailBeSectionComponent} (F-219, BE uniquement —
 * Code judiciaire art. 138bis + Code d'instruction criminelle art. 24
 * + Loi du 03/08/1992 sur le Code judiciaire + Loi du 06/06/2010
 * introduisant le Code pénal social).
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
 *   <li>{@code natureFait} — qualification pénale des faits (infraction
 *       pénale sociale / accident grave / travail non déclaré suspecté
 *       / discrimination pénale / harcèlement pénal art. 442bis C. pén.
 *       / entrave inspection / litige civil pur / autre), choix relevant
 *       de l'analyse stratégique avocat post-instruction.</li>
 *   <li>{@code modeSaisineEnvisage} — mode de saisine projeté
 *       (plainte directe art. 63 CIC, dénonciation simple art. 29 CIC,
 *       signalement inspection sociale art. 76 C. pén. soc., voie
 *       civile parallèle art. 578 C. jud.), choix stratégique avocat
 *       non extractible.</li>
 *   <li>{@code dateFaits} — date de commission des faits (procès-verbal
 *       inspection sociale / contrôle ONSS / signalement auditorat),
 *       donnée procédurale non issue du dossier salarié principal.</li>
 *   <li>{@code faitsPrescrits} — drapeau explicite avocat sur la
 *       prescription pénale (art. 81 C. pén. soc.), conclusion juridique
 *       non extractible.</li>
 *   <li>{@code inspectionDejaSaisie} — état procédural (PV inspection
 *       existant), donnée procédurale externe au dossier salarié.</li>
 *   <li>{@code plainteCivileDeposee} — état procédural civil parallèle,
 *       donnée procédurale externe.</li>
 *   <li>{@code urgenceSecuritePersonnes} — appréciation in concreto
 *       du danger immédiat, jugement avocat non extractible.</li>
 *   <li>{@code recoursPenalEnvisage} — choix stratégique avocat
 *       (axe pénal principal vs accessoire), non extractible.</li>
 *   <li>{@code employeurPersonneMorale} — qualité de l'employeur
 *       (BCE / KBO), donnée juridique non extractible du dossier salarié
 *       principal en V1.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code auditorat-travail-be-section-prefill-rules.spec.ts}.</p>
 */
export const AuditoratTravailBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * dossier d'instruction (nature des faits, mode de saisine, date
   * faits, prescription, inspection saisie, plainte civile, urgence,
   * recours pénal, personne morale).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
