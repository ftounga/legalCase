import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-213-08b — Helper partagé pour le pré-remplissage IA de
 * {@code LicenciementBeProtectionDelegueeSectionComponent} (F-213, BE uniquement).
 *
 * Pattern canonique F-IA-04 (F-236 SF-236-02) — runtime
 * `prefillFromAi()` et static `getPrefillCount()` consomment le MÊME
 * helper pur ; divergence impossible par construction.
 *
 * <h2>V1 — aucun champ pré-remplissable depuis {@code TravailExtractedData}</h2>
 *
 * <p>Le pipeline IA Travail BE actuel n'extrait <b>aucun</b> champ
 * exploitable pour piloter l'analyseur de protection délégué :</p>
 *
 * <ul>
 *   <li>{@code statutProtege} — le statut représentatif (délégué syndical
 *       élu, candidat non élu, délégué CCT n° 5, conseiller CPPT) n'est
 *       pas extrait par le prompt Travail BE actuel — donnée structurelle
 *       sociale plutôt qu'une mention récurrente dans les pièces
 *       contractuelles. Saisie avocat (CCT, PV élections sociales).</li>
 *   <li>{@code ancienneteAnnees} — l'extraction IA Travail BE actuelle
 *       capture {@code dateEntree} mais pas une ancienneté en années
 *       directement utilisable ici sans dérivation supplémentaire — à
 *       arbitrer dans une SF future (pré-fill via dateEntree → années
 *       arrondies). Saisie avocat V1.</li>
 *   <li>{@code remunerationAnnuelleBrute} — l'extraction IA Travail BE
 *       fournit {@code salaireBrutMensuel} et/ou {@code salaireBrutAnnuel}
 *       ; dériver une rémunération annuelle exacte demande une décision
 *       (12 × mensuel, +13e mois, +pécule de vacances…) qui dépasse le
 *       pré-fill mécanique. Saisie avocat V1 — le composant Protection
 *       Grossesse (SF-213-05b) a la même limitation et opère également
 *       sans pré-fill rémunération.</li>
 *   <li>{@code dateLicenciement} — extraite par le pipeline IA Travail
 *       BE ({@code dateLicenciement}) mais non pré-remplie en V1 par
 *       cohérence avec le pattern uniforme des outils F-213 vague 7
 *       (SF-213-07b = 0 champ également) — à arbitrer dans une SF future
 *       d'enrichissement IA Travail BE transverse.</li>
 *   <li>{@code dateElectionOuMandat} — non extraite par le prompt actuel.
 *       Donnée structurelle (PV d'élections sociales, mandat CCT n° 5).
 *       Saisie avocat systématique.</li>
 *   <li>{@code demandeReintegrationDansTrente},
 *       {@code employeurRefuseReintegration},
 *       {@code circonstancesAggravantes} — appréciations juridiques
 *       post-licenciement, saisie avocat systématique.</li>
 * </ul>
 *
 * <p>Décision alignée sur le pattern F-213 frontend (vagues 6b/7b — 0
 * champ pré-rempli). Enrichissement éventuel à arbitrer dans une SF
 * future dédiée (extraction des marqueurs représentativité +
 * dérivation rémunération annuelle).</p>
 *
 * <p>Gating : la valeur {@code workspaceCountry !== 'BELGIQUE'} retourne
 * trivialement 0 par symétrie avec les autres helpers F-213.</p>
 *
 * <p><b>Comptage parité F-236</b> : count = 0 quel que soit l'input —
 * testé par
 * {@code licenciement-be-protection-deleguee-section-prefill-rules.spec.ts}.</p>
 */
export const LicenciementBeProtectionDelegueeSectionPrefillRules = {

  /**
   * Comptage parité F-236 — V1 : aucun champ pré-rempli ⇒ 0.
   *
   * <p>Le param {@code input} est conservé pour respecter le contrat
   * {@code PrefillCountInput} (F-177 SF-177-12) — non utilisé en V1.
   * Sans pré-fill IA, le panel F-IA-04 n'affiche pas de badge
   * {@code auto_awesome +N} et l'avocat saisit les champs depuis les
   * éléments du dossier (CCT, PV élections sociales, lettre de
   * licenciement).</p>
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  computePrefillCount(input: PrefillCountInput): number {
    return 0;
  },
} as const;
