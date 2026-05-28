import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-24b — Helper partagé pour le pré-remplissage IA de
 * {@code CodePenalSocialBeSectionComponent} (F-219, BE uniquement —
 * Loi du 06/06/2010 introduisant le Code pénal social, M.B. 01/07/2010,
 * e.e.v. 01/07/2011).
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
 *   <li>{@code typeInfraction} — qualification pénale (travail non
 *       déclaré / absence Dimona / absence DmfA / non-paiement
 *       rémunération / dépassement temps travail / manquement
 *       bien-être / accident grave non déclaré / discrimination
 *       emploi / entrave inspection / faux social / absence registre /
 *       infraction intérim / infraction détachement / autre), choix
 *       relevant de l'analyse juridique de l'avocat post-instruction.</li>
 *   <li>{@code niveauPropose} — niveau 1 à 4 explicitement forcé par
 *       l'avocat (override de la table par défaut ou usage avec
 *       AUTRE_QUALIFICATION), décision stratégique non extractible.</li>
 *   <li>{@code dateFaits} — date de commission des faits (procès-verbal
 *       inspection sociale / contrôle ONSS / signalement auditorat),
 *       donnée procédurale non issue du dossier salarié principal.</li>
 *   <li>{@code nombreTravailleursConcernes} — nombre de travailleurs
 *       impactés par l'infraction (multiplicateur art. 103 § 2),
 *       chiffre RH déclaratif employeur / inspecteur social.</li>
 *   <li>{@code personneMorale} — qualité de l'employeur (multiplicateur
 *       × 5 art. 105), donnée juridique (BCE / KBO).</li>
 *   <li>{@code recidiveDansLAn} — antécédents de condamnation /
 *       décision administrative définitive dans l'année (art. 110),
 *       information judiciaire externe au dossier salarié.</li>
 *   <li>{@code prevenuPreposeOuMandataire} — qualité de l'auteur
 *       (employeur en personne vs préposé / mandataire — art. 109),
 *       donnée procédurale issue de l'acte de poursuite auditorat.</li>
 *   <li>{@code elementMoralIntentionnel} — caractère intentionnel /
 *       frauduleux du manquement (dol — pertinent pour ABSENCE_DMFA
 *       art. 223 : niveau 4 si frauduleux, niveau 2 sinon), élément
 *       moral relevant de l'appréciation in concreto de l'avocat.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code code-penal-social-be-section-prefill-rules.spec.ts}.</p>
 */
export const CodePenalSocialBeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * procès-verbal d'inspection sociale / dossier auditorat (type
   * infraction, date faits, nombre travailleurs, personne morale,
   * récidive, qualité auteur, élément moral).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
