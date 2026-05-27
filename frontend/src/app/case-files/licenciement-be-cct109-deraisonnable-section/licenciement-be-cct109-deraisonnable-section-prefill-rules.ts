import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-10b — Helper partagé pour le pré-remplissage IA de
 * {@code LicenciementBeCct109DeraisonnableSectionComponent} (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * `prefillFromAi()` et static `getPrefillCount()` consomment le MÊME
 * helper pur ; divergence impossible par construction.
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Le pipeline IA Travail BE actuel n'extrait <b>aucun</b> champ
 * exploitable pour piloter l'analyseur de score CCT 109 :</p>
 *
 * <ul>
 *   <li>{@code motifCommunique} (Boolean) — le pipeline IA Travail BE
 *       capture parfois {@code motifLicenciementDetecte} (texte libre)
 *       mais ne tranche pas la question « le motif a-t-il été
 *       communiqué par écrit au sens CCT 109 art. 5-8 » — appréciation
 *       juridique avocat (la pièce C4 / L1 ou la lettre de motivation
 *       sont à examiner). Saisie avocat V1.</li>
 *   <li>{@code motifLieAPersonne} — qualification 4 cas (SANS_MOTIF /
 *       MOTIF_VAGUE / MOTIF_PRECIS_DISPUTE / MOTIF_PROUVE_VALIDE) qui
 *       suppose une analyse contradictoire du motif (étayé ou pas,
 *       contesté ou pas) — pas extractible mécaniquement par l'IA en
 *       V1. Saisie avocat.</li>
 *   <li>{@code discriminationSuspectee}, {@code represaillesSuspectees} —
 *       appréciations juridiques (présomption, faisceau d'indices)
 *       saisies par l'avocat.</li>
 *   <li>{@code proceduresRespectees} — vérification procédurale CCT 109
 *       art. 5 (audition préalable, lettre de notification, motif
 *       écrit dans 2 mois) — saisie avocat à partir des pièces du
 *       dossier (lettre, PV audition).</li>
 *   <li>{@code remunerationHebdomadaireBrute} — l'extraction IA Travail
 *       BE fournit {@code salaireBrutMensuel} et/ou
 *       {@code salaireBrutAnnuel} ; la dérivation hebdomadaire suppose
 *       une décision (mensuel × 12 / 52 ? annuel / 52 ? prise en
 *       compte des avantages ?) qui dépasse le pré-fill mécanique.
 *       Saisie avocat V1 — alignement vague 8b (Protection délégué
 *       SF-213-08b : même limitation sur la rémunération annuelle).</li>
 *   <li>{@code argumentsPatronal} — texte libre, optionnel, sans impact
 *       calcul. Saisie avocat.</li>
 * </ul>
 *
 * <p>Décision alignée sur le pattern F-213 frontend (vagues 6b/7b/8b —
 * 0 champ pré-rempli). Enrichissement éventuel à arbitrer dans une SF
 * future dédiée (extraction des marqueurs de communication écrite du
 * motif + dérivation rémunération hebdomadaire).</p>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code licenciement-be-cct109-deraisonnable-section-prefill-rules.spec.ts}.</p>
 */
export const LicenciementBeCct109DeraisonnableSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (lettre de licenciement, C4, fiche de paie).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
