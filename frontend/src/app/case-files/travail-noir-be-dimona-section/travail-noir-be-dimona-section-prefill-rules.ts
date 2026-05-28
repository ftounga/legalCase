import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-219-26b — Helper partagé pour le pré-remplissage IA de
 * {@code TravailNoirBeDimonaSectionComponent} (F-219, BE uniquement —
 * Loi-programme du 24/12/2002 art. 167-184 + AR du 05/11/2002 + Code
 * pénal social art. 181 niveau 4).
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
 *   <li>{@code statutDimona} — statut administratif de la déclaration
 *       DIMONA (DECLAREE_AVANT_DEBUT / DECLAREE_TARDIVE / ABSENTE /
 *       INDEPENDANT_FAUSSEMENT_QUALIFIE), donnée extraite du système
 *       ONSS / procès-verbal d'inspection sociale, non du dossier
 *       salarié individuel.</li>
 *   <li>{@code dateDebutOccupation} — date de début effectif de
 *       l'occupation (factuelle employeur — feuille de présence,
 *       audition travailleur, document de subordination), non
 *       extractible mécaniquement du dossier salarié.</li>
 *   <li>{@code dateDimonaEffective} — date d'enregistrement de la
 *       déclaration à l'ONSS (relevé Easybox employeur / extrait ONSS),
 *       déclarative externe.</li>
 *   <li>{@code dateControle} — date du contrôle inspection sociale
 *       (procès-verbal SPF Emploi / contrôle ONSS / auditorat),
 *       donnée procédurale externe.</li>
 *   <li>{@code salaireBrutMensuel} — rémunération brute moyenne sur la
 *       période non déclarée (base cotisations ONSS rétroactives),
 *       chiffrage post-instruction à partir de l'audition travailleur,
 *       écritures de caisse, virements bancaires.</li>
 *   <li>{@code nombreTravailleursConcernes} — multiplicateur art. 103
 *       § 2 (plafond 100 × max unitaire art. 105 pour personne morale),
 *       décompte RH inspecteur social / employeur.</li>
 *   <li>{@code personneMorale} — qualité de l'employeur (BCE / KBO),
 *       donnée juridique externe non issue du dossier salarié.</li>
 *   <li>{@code recidiveDansLAn} — antécédents condamnation /
 *       décision administrative définitive (art. 110), information
 *       judiciaire externe.</li>
 *   <li>{@code elementsSubordination} — caractérisation in concreto de
 *       la subordination juridique (horaires, instructions, lieu,
 *       exclusivité, intégration, fourniture matériel), appréciation
 *       avocat / juge — Cass. BE 06/12/2010 S.10.0067.F.</li>
 * </ul>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213 / F-219.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par {@code travail-noir-be-dimona-section-prefill-rules.spec.ts}.</p>
 */
export const TravailNoirBeDimonaSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis le
   * procès-verbal d'inspection sociale / extrait ONSS / audition
   * travailleur (statut DIMONA, dates, salaire, nombre travailleurs,
   * personne morale, récidive, éléments subordination).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
